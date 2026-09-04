package bo.edu.usfx.jgroups;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.util.Util;

/** Nodo identico de la subasta distribuida. No existe un proceso servidor. */
public class RemateUSFX implements Receiver {
    private final String nombre;
    private final Object estadoLock = new Object();
    private final Map<String, Subasta> subastas = new LinkedHashMap<>();
    private final Map<String, ScheduledFuture<?>> temporizadores = new ConcurrentHashMap<>();
    private final ExecutorService propuestas = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService reloj = Executors.newScheduledThreadPool(2);
    private volatile View vista;
    private JChannel canal;

    public RemateUSFX(String nombre) { this.nombre = nombre; }

    public void iniciar() throws Exception {
        canal = new JChannel(System.getProperty("config", "udp.xml"));
        canal.name(nombre);
        canal.setReceiver(this);
        canal.connect("RemateSIS258");
        canal.getState(null, 10_000);
        try { new ConsolaRemate(this).ejecutar(); }
        finally {
            temporizadores.values().forEach(f -> f.cancel(false));
            propuestas.shutdownNow();
            reloj.shutdownNow();
            canal.close();
        }
    }

    @Override
    public void viewAccepted(View nueva) {
        View anterior = vista;
        vista = nueva;
        if (anterior != null) {
            Address[][] cambios = View.diff(anterior, nueva);
            for (Address a : cambios[0]) System.out.println("** ENTRO participante: " + a);
            for (Address a : cambios[1]) System.out.println("** SALIO participante: " + a);
        }
        System.out.println("** Vista " + nueva.getViewId().getId() + " | coordinador: "
                + nueva.getCoord() + " | miembros: " + nueva.getMembers());
        // El callback de JGroups termina rapido; otro hilo retoma los cierres si somos coordinador.
        if (esCoordinador()) reloj.execute(this::reprogramarCierres);
        else cancelarTemporizadores();
    }

    @Override
    public void receive(Message msg) {
        Object objeto = msg.getObject();
        if (!(objeto instanceof MensajeRemate m)) return;
        switch (m.tipo()) {
            case PROPONER_CREACION, PROPONER_PUJA ->
                    propuestas.execute(() -> procesarPropuesta(msg.getSrc(), m));
            case CREACION_ACEPTADA -> aplicarCreacion(m);
            case PUJA_ACEPTADA -> aplicarPuja(m);
            case CIERRE -> aplicarCierre(m);
            case RECHAZO -> System.out.println("RECHAZADA: " + m.detalle());
        }
    }

    private void procesarPropuesta(Address solicitante, MensajeRemate m) {
        try {
            if (!esCoordinador()) {
                Address coordinador = vista == null ? null : vista.getCoord();
                if (coordinador != null)
                    canal.send(new ObjectMessage(coordinador, m));
                return;
            }
            if (m.tipo() == MensajeRemate.Tipo.PROPONER_CREACION)
                aceptarCreacion(solicitante, m);
            else aceptarPuja(solicitante, m);
        } catch (Exception e) {
            System.err.println("No se pudo procesar la propuesta: " + e.getMessage());
        }
    }

    private void aceptarCreacion(Address solicitante, MensajeRemate m) throws Exception {
        MensajeRemate decision;
        synchronized (estadoLock) {
            String clave = clave(m.articulo());
            if (clave.isBlank()) { rechazar(solicitante, "El articulo no puede estar vacio"); return; }
            if (subastas.containsKey(clave)) { rechazar(solicitante, "Ya existe la subasta " + m.articulo()); return; }
            if (m.monto() == null || m.monto().signum() < 0) { rechazar(solicitante, "Precio base invalido"); return; }
            if (m.instante() <= 0) { rechazar(solicitante, "La duracion debe ser mayor que cero"); return; }
            long cierre = System.currentTimeMillis() + m.instante();
            decision = MensajeRemate.creacionAceptada(m.articulo().trim(), m.monto(), m.participante(), cierre);
            // Reserva local: la siguiente propuesta se valida contra esta decision aun antes del loopback.
            subastas.put(clave, new Subasta(decision.articulo(), decision.monto(), decision.participante(), cierre));
        }
        canal.send(new ObjectMessage(null, decision));
        programarCierre(clave(decision.articulo()), decision.cierreEn());
    }

    private void aceptarPuja(Address solicitante, MensajeRemate m) throws Exception {
        MensajeRemate decision;
        synchronized (estadoLock) {
            Subasta s = subastas.get(clave(m.articulo()));
            if (s == null) { rechazar(solicitante, "No existe la subasta " + m.articulo()); return; }
            if (s.cerrada() || System.currentTimeMillis() >= s.cierreEn()) {
                rechazar(solicitante, "La subasta ya esta cerrada"); return;
            }
            if (m.monto() == null || m.monto().compareTo(s.montoActual()) <= 0) {
                rechazar(solicitante, "La puja debe superar " + s.montoActual().toPlainString()); return;
            }
            long instante = System.currentTimeMillis();
            s.agregarPuja(new Puja(m.participante(), m.monto(), instante));
            decision = MensajeRemate.pujaAceptada(s.articulo(), m.monto(), m.participante(), instante);
        }
        // Todas las decisiones salen del mismo emisor; FIFO del coordinador fija el mismo orden global.
        canal.send(new ObjectMessage(null, decision));
    }

    private void rechazar(Address destino, String causa) throws Exception {
        if (canal.getAddress().equals(destino)) System.out.println("RECHAZADA: " + causa);
        else canal.send(new ObjectMessage(destino, MensajeRemate.rechazo(causa)));
    }

    private void aplicarCreacion(MensajeRemate m) {
        synchronized (estadoLock) {
            subastas.putIfAbsent(clave(m.articulo()),
                    new Subasta(m.articulo(), m.monto(), m.participante(), m.cierreEn()));
        }
        System.out.println("NUEVA SUBASTA: " + m.articulo() + " | base " + m.monto()
                + " | creador " + m.participante());
        if (esCoordinador()) programarCierre(clave(m.articulo()), m.cierreEn());
    }

    private void aplicarPuja(MensajeRemate m) {
        synchronized (estadoLock) {
            Subasta s = subastas.get(clave(m.articulo()));
            if (s != null && !s.cerrada() && m.monto().compareTo(s.montoActual()) > 0)
                s.agregarPuja(new Puja(m.participante(), m.monto(), m.instante()));
        }
        System.out.println("PUJA ACEPTADA: " + m.articulo() + " | " + m.monto()
                + " por " + m.participante());
    }

    private void aplicarCierre(MensajeRemate m) {
        Subasta s;
        synchronized (estadoLock) {
            s = subastas.get(clave(m.articulo()));
            if (s != null) s.cerrar();
        }
        ScheduledFuture<?> tarea = temporizadores.remove(clave(m.articulo()));
        if (tarea != null) tarea.cancel(false);
        if (s == null) return;
        Puja ganadora = s.mejorPuja();
        System.out.println(ganadora == null
                ? "SUBASTA CERRADA: " + s.articulo() + " sin pujas"
                : "SUBASTA CERRADA: " + s.articulo() + " | ganador "
                    + ganadora.participante() + " | " + ganadora.monto());
    }

    private void reprogramarCierres() {
        cancelarTemporizadores();
        synchronized (estadoLock) {
            subastas.forEach((clave, s) -> { if (!s.cerrada()) programarCierre(clave, s.cierreEn()); });
        }
    }

    private void programarCierre(String clave, long cierreEn) {
        if (!esCoordinador()) return;
        ScheduledFuture<?> anterior = temporizadores.remove(clave);
        if (anterior != null) anterior.cancel(false);
        long demora = Math.max(0, cierreEn - System.currentTimeMillis());
        temporizadores.put(clave, reloj.schedule(() -> cerrarSiCorresponde(clave), demora, TimeUnit.MILLISECONDS));
    }

    private void cerrarSiCorresponde(String clave) {
        if (!esCoordinador()) return;
        String articulo;
        synchronized (estadoLock) {
            Subasta s = subastas.get(clave);
            if (s == null || s.cerrada()) return;
            long restante = s.cierreEn() - System.currentTimeMillis();
            if (restante > 0) { programarCierre(clave, s.cierreEn()); return; }
            s.cerrar();
            articulo = s.articulo();
        }
        try { canal.send(new ObjectMessage(null, MensajeRemate.cierre(articulo))); }
        catch (Exception e) { System.err.println("No se pudo anunciar el cierre: " + e.getMessage()); }
    }

    private void cancelarTemporizadores() {
        temporizadores.values().forEach(f -> f.cancel(false));
        temporizadores.clear();
    }

    private boolean esCoordinador() {
        return canal != null && canal.getAddress() != null && vista != null
                && canal.getAddress().equals(vista.getCoord());
    }

    @Override
    public void getState(OutputStream salida) throws Exception {
        synchronized (estadoLock) {
            Util.objectToStream(new LinkedHashMap<>(subastas), new DataOutputStream(salida));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setState(InputStream entrada) throws Exception {
        Map<String, Subasta> recibido = Util.objectFromStream(new DataInputStream(entrada));
        synchronized (estadoLock) { subastas.clear(); subastas.putAll(recibido); }
        System.out.println("** Estado recibido: " + recibido.size() + " subastas");
        if (esCoordinador()) reloj.execute(this::reprogramarCierres);
    }

    public void proponerCreacion(String articulo, BigDecimal base, long segundos) throws Exception {
        if (segundos <= 0) throw new IllegalArgumentException("Los segundos deben ser mayores que cero");
        if (base.signum() < 0) throw new IllegalArgumentException("El precio no puede ser negativo");
        MensajeRemate propuesta = MensajeRemate.proponerCreacion(
                articulo, base, nombre, Math.multiplyExact(segundos, 1000));
        if (esCoordinador()) propuestas.execute(() -> procesarPropuesta(canal.getAddress(), propuesta));
        else canal.send(new ObjectMessage(vista.getCoord(), propuesta));
    }

    public void proponerPuja(String articulo, BigDecimal monto) throws Exception {
        if (monto.signum() < 0) throw new IllegalArgumentException("El monto no puede ser negativo");
        MensajeRemate propuesta = MensajeRemate.proponerPuja(articulo, monto, nombre);
        if (esCoordinador()) propuestas.execute(() -> procesarPropuesta(canal.getAddress(), propuesta));
        else canal.send(new ObjectMessage(vista.getCoord(), propuesta));
    }

    public void mostrarSubastas() {
        synchronized (estadoLock) {
            System.out.println("--- SUBASTAS ABIERTAS ---");
            subastas.values().stream().filter(s -> !s.cerrada()).forEach(s -> {
                Puja mejor = s.mejorPuja();
                long segundos = Math.max(0, (s.cierreEn() - System.currentTimeMillis() + 999) / 1000);
                System.out.println(s.articulo() + " | actual " + s.montoActual()
                        + " | postor " + (mejor == null ? "sin pujas" : mejor.participante())
                        + " | quedan " + segundos + " s");
            });
        }
    }

    public void mostrarEstado(String articulo) {
        synchronized (estadoLock) {
            Subasta s = subastas.get(clave(articulo));
            if (s == null) { System.out.println("No existe la subasta " + articulo); return; }
            System.out.println(s.articulo() + " | " + (s.cerrada() ? "cerrada" : "abierta")
                    + " | base " + s.precioBase());
            if (s.pujas().isEmpty()) System.out.println("  Sin pujas");
            else s.pujas().forEach(p -> System.out.println("  " + p));
        }
    }

    public void mostrarParticipantes() {
        View v = vista;
        System.out.println("Participantes: " + (v == null ? "[]" : v.getMembers()));
        System.out.println("Coordinador: " + (v == null ? "-" : v.getCoord()));
    }

    public void mostrarGanadas() {
        BigDecimal total = BigDecimal.ZERO;
        synchronized (estadoLock) {
            for (Subasta s : subastas.values()) {
                Puja p = s.mejorPuja();
                if (s.cerrada() && p != null && p.participante().equals(nombre)) {
                    System.out.println(s.articulo() + " | " + p.monto());
                    total = total.add(p.monto());
                }
            }
        }
        System.out.println("Total a pagar: " + total);
    }

    private static String clave(String articulo) {
        return articulo == null ? "" : articulo.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static void main(String[] args) throws Exception {
        new RemateUSFX(args.length > 0 ? args[0] : "anonimo").iniciar();
    }
}
