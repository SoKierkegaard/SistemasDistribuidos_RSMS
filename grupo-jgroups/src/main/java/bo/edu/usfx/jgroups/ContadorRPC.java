package bo.edu.usfx.jgroups;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;

public class ContadorRPC implements Receiver {
    private JChannel canal;
    private RpcDispatcher despachador;
    private final AtomicInteger contador = new AtomicInteger();

    public int incrementar(int cantidad) {
        int nuevo = contador.addAndGet(cantidad);
        System.out.println(" -> incrementar(" + cantidad + ") ejecutado aqui. Ahora vale " + nuevo);
        return nuevo;
    }
    public int consultar() { return contador.get(); }
    public String saludar(String quien) { return "Hola " + quien + ", soy " + canal.getAddress(); }

    @Override
    public void viewAccepted(View vista) { System.out.println("** Miembros: " + vista.getMembers()); }

    public void iniciar(String nombre) throws Exception {
        canal = new JChannel(System.getProperty("config", "udp.xml"));
        canal.name(nombre);
        despachador = new RpcDispatcher(canal, this);
        despachador.setReceiver(this);
        canal.connect("ContadorSIS258");
        try { leerTeclado(); } finally { despachador.close(); canal.close(); }
    }

    private void leerTeclado() throws Exception {
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Comandos: inc <n> | ver | saludar | /salir");
        String linea;
        while ((linea = teclado.readLine()) != null && !linea.equals("/salir")) {
            try {
                if (linea.startsWith("inc ")) invocar(new MethodCall("incrementar",
                        new Object[]{Integer.parseInt(linea.substring(4).trim())}, new Class<?>[]{int.class}));
                else if (linea.equals("ver")) invocar(new MethodCall("consultar", new Object[0], new Class<?>[0]));
                else if (linea.equals("saludar")) invocar(new MethodCall("saludar",
                        new Object[]{canal.getName()}, new Class<?>[]{String.class}));
                else System.out.println("Comando desconocido");
            } catch (NumberFormatException e) { System.out.println("La cantidad debe ser un entero"); }
        }
    }

    private void invocar(MethodCall llamada) throws Exception {
        RspList<Object> respuestas = despachador.callRemoteMethods(null, llamada,
                new RequestOptions(ResponseMode.GET_ALL, 5_000));
        System.out.println("Respuestas a " + llamada.getMethodName() + "():");
        for (Map.Entry<Address, Rsp<Object>> entrada : respuestas.entrySet()) {
            Rsp<Object> r = entrada.getValue();
            String estado = r.wasReceived() ? String.valueOf(r.getValue())
                    : (r.wasSuspected() ? "SOSPECHOSO" : "SIN RESPUESTA (timeout)");
            System.out.println("  " + entrada.getKey() + " => " + estado);
        }
    }

    public static void main(String[] args) throws Exception {
        new ContadorRPC().iniciar(args.length > 0 ? args[0] : "anonimo");
    }
}
