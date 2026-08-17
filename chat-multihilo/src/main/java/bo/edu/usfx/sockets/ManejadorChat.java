package bo.edu.usfx.sockets;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Atiende a UN cliente en su propio hilo. Guarda su apodo y su sala actual,
 * interpreta los comandos que empiezan con "/" y difunde los mensajes
 * normales a su sala.
 */
public class ManejadorChat implements Runnable {

    private final Socket socket;
    private final int id;
    private String apodo;
    private Sala salaActual;
    private PrintWriter salida;

    // Historial de la sala para el bonus /historial (ultimos 10 mensajes).
    // Se guarda por sala, sincronizado al escribir/leer.

    public ManejadorChat(Socket socket, int id) {
        this.socket = socket;
        this.id = id;
        this.apodo = "usuario-" + id; // apodo temporal hasta que use /nick
    }

    /**
     * El servidor llama a este metodo (desde OTRO hilo) para escribir a
     * este cliente. synchronized evita que dos hilos escriban al mismo
     * PrintWriter en el mismo instante (ej. dos privados simultaneos).
     */
    public synchronized void enviar(String mensaje) {
        if (salida != null) {
            salida.println(mensaje);
        }
    }

    public String getApodo() {
        return apodo;
    }

    @Override
    public void run() {
        String hilo = Thread.currentThread().getName();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            this.salida = out;
            ServidorChat.CONECTADOS.incrementAndGet();

            // Registrar apodo temporal y meter a sala general
            ServidorChat.APODOS.put(apodo, this);
            salaActual = ServidorChat.SALAS.get("general");
            salaActual.agregar(this);

            enviar("Bienvenido " + apodo + ". Estas en la sala 'general'.");
            enviar("Escribe /nick <apodo> para cambiar tu nombre. /salir para salir.");
            salaActual.difundir(">> " + apodo + " entro a la sala", this);

            String linea;
            while ((linea = in.readLine()) != null) {
                if (linea.startsWith("/")) {
                    if (!procesarComando(linea)) {
                        break; // /salir devuelve false para cortar el bucle
                    }
                } else {
                    // Mensaje normal: va a la sala actual
                    salaActual.difundir(apodo + "> " + linea, this);
                    guardarHistorial(salaActual, apodo + "> " + linea);
                }
            }
        } catch (IOException e) {
            System.err.println("Error con cliente " + id + ": " + e.getMessage());
        } finally {
            desconectar();
            System.out.println("Cliente " + id + " (" + apodo + ") desconectado");
        }
    }

    /**
     * Interpreta un comando. Devuelve false solo si el cliente pidio /salir.
     */
    private boolean procesarComando(String linea) {
        String[] partes = linea.split(" ", 3);
        String cmd = partes[0].toLowerCase();

        switch (cmd) {
            case "/nick":
                cambiarNick(partes);
                break;
            case "/salas":
                listarSalas();
                break;
            case "/crear":
                crearSala(partes);
                break;
            case "/unirse":
                unirseSala(partes);
                break;
            case "/quien":
                quien();
                break;
            case "/privado":
                privado(partes);
                break;
            case "/estado":
                estado();
                break;
            case "/historial":
                enviarHistorial();
                break;
            case "/salir":
                enviar("Cerrando conexion. Hasta pronto.");
                return false;
            default:
                enviar("Comando desconocido: " + cmd);
        }
        return true;
    }

    private void cambiarNick(String[] partes) {
        if (partes.length < 2) {
            enviar("Uso: /nick <apodo>");
            return;
        }
        String nuevo = partes[1];
        // putIfAbsent es atomico: si el apodo ya existe, no lo pisa y avisa.
        if (ServidorChat.APODOS.putIfAbsent(nuevo, this) != null) {
            enviar("El apodo '" + nuevo + "' ya esta en uso. Conservas '" + apodo + "'.");
            return;
        }
        ServidorChat.APODOS.remove(apodo); // libera el apodo anterior
        String anterior = apodo;
        apodo = nuevo;
        enviar("Ahora eres '" + apodo + "'.");
        salaActual.difundir(">> " + anterior + " ahora se llama " + apodo, this);
    }

    private void listarSalas() {
        StringBuilder sb = new StringBuilder("Salas existentes:");
        for (Sala s : ServidorChat.SALAS.values()) {
            sb.append("\n  - ").append(s.getNombre())
              .append(" (").append(s.cantidad()).append(" usuarios)");
        }
        enviar(sb.toString());
    }

    private void crearSala(String[] partes) {
        if (partes.length < 2) {
            enviar("Uso: /crear <sala>");
            return;
        }
        String nombre = partes[1];
        // putIfAbsent evita que dos hilos creen la misma sala a la vez.
        Sala nueva = new Sala(nombre);
        if (ServidorChat.SALAS.putIfAbsent(nombre, nueva) != null) {
            enviar("La sala '" + nombre + "' ya existe.");
            return;
        }
        moverASala(nueva);
        enviar("Sala '" + nombre + "' creada. Ahora estas dentro.");
    }

    private void unirseSala(String[] partes) {
        if (partes.length < 2) {
            enviar("Uso: /unirse <sala>");
            return;
        }
        Sala destino = ServidorChat.SALAS.get(partes[1]);
        if (destino == null) {
            enviar("La sala '" + partes[1] + "' no existe. Usa /crear.");
            return;
        }
        moverASala(destino);
        enviar("Te uniste a '" + destino.getNombre() + "'.");
    }

    /**
     * Saca al usuario de su sala actual y lo mete en otra, avisando a ambas.
     */
    private void moverASala(Sala destino) {
        salaActual.difundir(">> " + apodo + " salio de la sala", this);
        salaActual.quitar(this);
        salaActual = destino;
        destino.agregar(this);
        destino.difundir(">> " + apodo + " entro a la sala", this);
    }

    private void quien() {
        StringBuilder sb = new StringBuilder("En '" + salaActual.getNombre() + "':");
        for (ManejadorChat m : salaActual.getMiembros()) {
            sb.append("\n  - ").append(m.getApodo());
        }
        enviar(sb.toString());
    }

    private void privado(String[] partes) {
        if (partes.length < 3) {
            enviar("Uso: /privado <apodo> <mensaje>");
            return;
        }
        String destino = partes[1];
        String mensaje = partes[2];
        ManejadorChat receptor = ServidorChat.APODOS.get(destino);
        if (receptor == null) {
            enviar("No existe el usuario '" + destino + "'.");
            return;
        }
        receptor.enviar("[privado de " + apodo + "]: " + mensaje);
        enviar("[privado para " + destino + "]: " + mensaje);
    }

    private void estado() {
        enviar("Estado del servidor:"
                + "\n  Conectados ahora: " + ServidorChat.CONECTADOS.get()
                + "\n  Total historico: " + ServidorChat.HISTORICO.get()
                + "\n  Salas: " + ServidorChat.SALAS.size());
    }

    // ---- Bonus: historial de los ultimos 10 mensajes por sala ----
    private static final Map<String, Deque<String>> HISTORIAL = new java.util.concurrent.ConcurrentHashMap<>();

    private void guardarHistorial(Sala sala, String mensaje) {
        Deque<String> cola = HISTORIAL.computeIfAbsent(sala.getNombre(),
                k -> new ArrayDeque<>());
        // synchronized sobre la cola: ArrayDeque no es thread-safe por si sola
        synchronized (cola) {
            cola.addLast(mensaje);
            if (cola.size() > 10) {
                cola.removeFirst();
            }
        }
    }

    private void enviarHistorial() {
        Deque<String> cola = HISTORIAL.get(salaActual.getNombre());
        if (cola == null || cola.isEmpty()) {
            enviar("No hay historial en esta sala.");
            return;
        }
        enviar("--- Ultimos mensajes de '" + salaActual.getNombre() + "' ---");
        synchronized (cola) {
            for (String m : cola) {
                enviar("  " + m);
            }
        }
    }

    private void desconectar() {
        ServidorChat.CONECTADOS.decrementAndGet();
        ServidorChat.APODOS.remove(apodo);
        if (salaActual != null) {
            salaActual.quitar(this);
            salaActual.difundir(">> " + apodo + " se desconecto", this);
        }
        try { socket.close(); } catch (IOException e) { }
    }
}