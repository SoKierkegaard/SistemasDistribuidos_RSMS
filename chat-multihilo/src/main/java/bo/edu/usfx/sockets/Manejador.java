/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bo.edu.usfx.sockets;

/**
 *
 * @author Sebastian
 */

import java.io.*;
import java.net.*;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class Manejador implements Runnable {
    
    // Estado COMPARTIDO por todos los hilos
    private static final Set<Manejador> CLIENTES = new CopyOnWriteArraySet<>();
    
    private final Socket cliente;
    private final int id;
    private PrintWriter salida; // Variable de instancia para guardar el flujo de salida

    public Manejador(Socket cliente, int id) {
        this.cliente = cliente;
        this.id = id;
    }

    @Override
    public void run() {
        String hilo = Thread.currentThread().getName();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
             PrintWriter out = new PrintWriter(cliente.getOutputStream(), true)) {
            
            // Guardamos la salida y registramos al cliente en la colección estática
            this.salida = out;
            CLIENTES.add(this);
            
            out.println("Bienvenido. Le atiende el hilo: " + hilo);
            String linea;
            
            // Bucle de lectura
            while ((linea = in.readLine()) != null) {
                System.out.println("[" + hilo + "] cliente " + id + ": " + linea);
                // En lugar de hacer ECO, difundimos el mensaje a todos
                difundir("cliente-" + id + "> " + linea);
            }
        } catch (IOException e) {
            System.err.println("Error con el cliente " + id + ": " + e.getMessage());
        } finally {
            // Al desconectarse, cerramos el socket y lo eliminamos de la colección
            try { cliente.close(); } catch (IOException e) { }
            CLIENTES.remove(this);
            System.out.println("Cliente " + id + " desconectado");
        }
    }

    // Método para enviar el mensaje a todos los clientes, excepto al que lo envía
    private void difundir(String mensaje) {
        for (Manejador m : CLIENTES) {
            if (m != this && m.salida != null) {
                m.salida.println(mensaje);
            }
        }
    }
}