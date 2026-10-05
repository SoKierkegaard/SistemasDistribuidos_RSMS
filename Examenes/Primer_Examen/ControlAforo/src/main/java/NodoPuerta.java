import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;
import org.jgroups.*;
import org.jgroups.util.Util;

public class NodoPuerta implements Receiver {
    private JChannel canal;
    private String nombre;
    private int aforoMaximo;
    private int ocupacion = 0;
    private View vistaAnterior;

    public NodoPuerta(String nombre, int aforoMaximo) {
        this.nombre = nombre;
        this.aforoMaximo = aforoMaximo;
    }

    public void iniciar() throws Exception {
        canal = new JChannel();
        canal.name(nombre);
        canal.setReceiver(this);
        canal.connect("AforoSIS258");
        try { canal.getState(null, 10000); } catch (Exception ignored) {}

        System.out.println("Puerta conectada como " + nombre + " | Aforo maximo: " + aforoMaximo);
        System.out.println("Comandos: /entrar <n> | /salir <n> | /estado | /salir");

        Scanner sc = new Scanner(System.in);
        while (sc.hasNextLine()) {
            String linea = sc.nextLine().trim();
            if (linea.equalsIgnoreCase("/salir")) { canal.close(); break; }
            String[] p = linea.split(" ");
            if (p[0].equalsIgnoreCase("/entrar") && p.length > 1) {
                int n = Integer.parseInt(p[1]);
                Address coord = canal.getView().getCoord();
                canal.send(new ObjectMessage(coord, new MensajeAforo(MensajeAforo.Tipo.SOLICITUD, nombre, n)));
            } else if (p[0].equalsIgnoreCase("/salir") && p.length > 1) {
                int n = Integer.parseInt(p[1]);
                canal.send(new ObjectMessage(null, new MensajeAforo(MensajeAforo.Tipo.SALIDA, nombre, n)));
            } else if (p[0].equalsIgnoreCase("/estado")) {
                imprimirEstado();
            }
        }
    }

    @Override
    public void viewAccepted(View vista) {
        if (vistaAnterior != null) {
            Address[][] diff = View.diff(vistaAnterior, vista);
            for (Address a : diff[0]) System.out.println("** ENTRO puerta: " + a);
            for (Address a : diff[1]) System.out.println("** SALIO puerta: " + a);
        }
        vistaAnterior = vista;
        System.out.println("** Coordinador actual: " + vista.getCoord() + " | Miembros: " + vista.getMembers());
    }

    @Override
    public void receive(Message msg) {
        MensajeAforo m = (MensajeAforo) msg.getObject();
        if (m.tipo == MensajeAforo.Tipo.SOLICITUD) {
            if (canal.getAddress().equals(canal.getView().getCoord())) {
                try {
                    synchronized (this) {
                        if (ocupacion + m.personas <= aforoMaximo) {
                            canal.send(new ObjectMessage(null, new MensajeAforo(MensajeAforo.Tipo.ACEPTADO, m.puerta, m.personas)));
                        } else {
                            canal.send(new ObjectMessage(msg.getSrc(), new MensajeAforo(MensajeAforo.Tipo.RECHAZADO, m.puerta, m.personas)));
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        } else if (m.tipo == MensajeAforo.Tipo.ACEPTADO) {
            synchronized (this) { ocupacion += m.personas; }
            System.out.println("[ACEPTADO] " + m.personas + " personas ingresaron por " + m.puerta + " (Ocupacion: " + ocupacion + "/" + aforoMaximo + ")");
            if (ocupacion >= aforoMaximo) System.out.println("AFORO COMPLETO");
        } else if (m.tipo == MensajeAforo.Tipo.RECHAZADO) {
            System.out.println("[RECHAZADO] Aforo insuficiente para " + m.personas + " personas por " + m.puerta);
        } else if (m.tipo == MensajeAforo.Tipo.SALIDA) {
            synchronized (this) { ocupacion = Math.max(0, ocupacion - m.personas); }
            System.out.println("[SALIDA] " + m.personas + " personas salieron por " + m.puerta + " (Ocupacion: " + ocupacion + "/" + aforoMaximo + ")");
        }
    }

    private void imprimirEstado() {
        System.out.println("Ocupacion actual: " + ocupacion + "/" + aforoMaximo);
        if (ocupacion >= aforoMaximo) System.out.println("AFORO COMPLETO");
    }

    @Override
    public void getState(OutputStream out) throws Exception {
        synchronized (this) { Util.objectToStream(ocupacion, new DataOutputStream(out)); }
    }

    @Override
    public void setState(InputStream in) throws Exception {
        synchronized (this) { ocupacion = (Integer) Util.objectFromStream(new DataInputStream(in)); }
        System.out.println("** Estado sincronizado: " + ocupacion + "/" + aforoMaximo);
    }

    public static void main(String[] args) throws Exception {
        String nombre = args.length > 0 ? args[0] : "Puerta1";
        int aforo = args.length > 1 ? Integer.parseInt(args[1]) : 50;
        new NodoPuerta(nombre, aforo).iniciar();
    }
}
