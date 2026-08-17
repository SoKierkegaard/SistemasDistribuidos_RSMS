package bo.edu.usfx.sockets;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servidor de chat con salas.
 * El bucle principal SOLO acepta y delega (requisito tecnico 1).
 */
public class ServidorChat {

    // Mapa de salas: nombre -> Sala. ConcurrentHashMap permite que varios
    // hilos busquen/creen salas a la vez sin corromper la estructura.
    public static final Map<String, Sala> SALAS = new ConcurrentHashMap<>();

    // Mapa de apodos en uso: apodo -> ManejadorChat. Sirve para rechazar
    // apodos repetidos y para los mensajes privados.
    public static final Map<String, ManejadorChat> APODOS = new ConcurrentHashMap<>();

    // Contador de usuarios conectados AHORA. Atomic para que sumar/restar
    // desde muchos hilos a la vez sea seguro.
    public static final AtomicInteger CONECTADOS = new AtomicInteger(0);

    // Contador HISTORICO de conexiones. Un int normal fallaria si diez
    // clientes se conectan en el mismo instante (operacion no atomica).
    public static final AtomicInteger HISTORICO = new AtomicInteger(0);

    public static void main(String[] args) throws IOException {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        int hilos = args.length > 1 ? Integer.parseInt(args[1]) : 4;

        // Sala por defecto donde entran todos al conectarse
        SALAS.put("general", new Sala("general"));

        ServerSocket servidor = new ServerSocket(puerto);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        System.out.println("Servidor de chat en el puerto " + puerto
                + " (pool de " + hilos + " hilos)");

        while (true) {
            Socket cliente = servidor.accept(); // solo acepta
            int id = HISTORICO.incrementAndGet(); // conexion historica
            System.out.println("Conexion #" + id + " desde "
                    + cliente.getInetAddress().getHostAddress());
            pool.execute(new ManejadorChat(cliente, id)); // y delega
        }
    }
}