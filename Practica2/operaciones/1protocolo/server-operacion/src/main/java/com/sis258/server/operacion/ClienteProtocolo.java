/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sis258.server.operacion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class ClienteProtocolo {

    private static final int PORT = 5002;

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";

        try (BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                mostrarMenu();
                String operacion = teclado.readLine();
                if (operacion == null || operacion.trim().equals("0")) {
                    System.out.println("Cliente finalizado.");
                    break;
                }

                if (!esOperacionValida(operacion)) {
                    System.out.println("Opcion no valida.\n");
                    continue;
                }

                System.out.print("Primer numero: ");
                String numero1 = teclado.readLine();
                System.out.print("Segundo numero: ");
                String numero2 = teclado.readLine();

                // El cliente y el servidor comparten este formato de protocolo.
                String solicitud = operacion.trim() + " " + numero1 + " " + numero2;

                try (Socket socket = new Socket(host, PORT);
                     PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader entrada = new BufferedReader(
                             new InputStreamReader(socket.getInputStream()))) {
                    salida.println(solicitud);
                    System.out.println("Servidor: " + entrada.readLine() + "\n");
                } catch (IOException ex) {
                    System.err.println("No se pudo comunicar con " + host + ":" + PORT
                            + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            System.err.println("Error leyendo el teclado: " + ex.getMessage());
        }
    }

    private static void mostrarMenu() {
        System.out.println("1. Suma");
        System.out.println("2. Resta");
        System.out.println("3. Multiplicacion");
        System.out.println("4. Division");
        System.out.println("0. Salir");
        System.out.print("Seleccione una operacion: ");
    }

    private static boolean esOperacionValida(String operacion) {
        return operacion.trim().matches("[1-4]");
    }
}
