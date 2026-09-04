package bo.edu.usfx.jgroups;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.util.Util;

public class PizarraGrupo implements Receiver {
    private JChannel canal;
    private final String nombre;
    private View vistaAnterior;
    private final List<String> historial = new ArrayList<>();

    public PizarraGrupo(String nombre) { this.nombre = nombre; }

    @Override
    public void viewAccepted(View vista) {
        if (vistaAnterior != null) {
            Address[][] cambios = View.diff(vistaAnterior, vista);
            for (Address a : cambios[0]) System.out.println("** ENTRO: " + a);
            for (Address a : cambios[1]) System.out.println("** SALIO: " + a);
        }
        vistaAnterior = vista;
        System.out.println("** Vista " + vista.getViewId().getId() + " | coordinador: "
                + vista.getCoord() + " | miembros: " + vista.getMembers());
    }

    @Override
    public void receive(Message msg) {
        String texto = msg.getSrc() + ": " + msg.getObject();
        synchronized (historial) { historial.add(texto); }
        System.out.println((msg.getDest() == null ? "" : "(privado) ") + texto);
    }

    @Override
    public void getState(OutputStream salida) throws Exception {
        synchronized (historial) {
            Util.objectToStream(new ArrayList<>(historial), new DataOutputStream(salida));
            System.out.println("** Estado enviado (" + historial.size() + " lineas)");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setState(InputStream entrada) throws Exception {
        List<String> recibido = Util.objectFromStream(new DataInputStream(entrada));
        synchronized (historial) { historial.clear(); historial.addAll(recibido); }
        System.out.println("** Estado recibido: " + recibido.size() + " lineas");
        recibido.forEach(linea -> System.out.println("  " + linea));
    }

    public void iniciar() throws Exception {
        canal = new JChannel(System.getProperty("config", "udp.xml"));
        canal.name(nombre);
        canal.setReceiver(this);
        canal.connect("PizarraSIS258");
        canal.getState(null, 10_000);
        try { leerTeclado(); } finally { canal.close(); }
    }

    private void leerTeclado() throws Exception {
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Comandos: /historial | /privado <miembro> <texto> | /salir");
        String linea;
        while ((linea = teclado.readLine()) != null) {
            if (linea.equals("/salir")) return;
            if (linea.equals("/historial")) {
                synchronized (historial) { historial.forEach(item -> System.out.println("  " + item)); }
            } else if (linea.startsWith("/privado ")) enviarPrivado(linea);
            else canal.send(new ObjectMessage(null, linea));
        }
    }

    private void enviarPrivado(String linea) throws Exception {
        String[] partes = linea.split("\\s+", 3);
        if (partes.length < 3) { System.out.println("Uso: /privado <miembro> <texto>"); return; }
        Address destino = canal.getView().getMembers().stream()
                .filter(a -> a.toString().equals(partes[1])).findFirst().orElse(null);
        if (destino == null) { System.out.println("No existe el miembro " + partes[1]); return; }
        canal.send(new ObjectMessage(destino, partes[2]));
    }

    public static void main(String[] args) throws Exception {
        new PizarraGrupo(args.length > 0 ? args[0] : "anonimo").iniciar();
    }
}
