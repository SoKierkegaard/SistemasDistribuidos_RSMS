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


public class ClienteInteractivo {

    private static final int PORT = 5003;

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";

        try (BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {
            boolean continuar = true;
            while (continuar) {
                try (Socket socket = new Socket(host, PORT);
                     PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader entrada = new BufferedReader(
                             new InputStreamReader(socket.getInputStream()))) {

                    if (!leerPreguntaYResponder(entrada, salida, teclado)) break;
                    if (!leerPreguntaYResponder(entrada, salida, teclado)) break;
                    if (!leerPreguntaYResponder(entrada, salida, teclado)) break;

                    String resultado = entrada.readLine();
                    if (resultado != null) System.out.println("Servidor: " + resultado);
                } catch (IOException ex) {
                    System.err.println("No se pudo comunicar con " + host + ":" + PORT
                            + ": " + ex.getMessage());
                }

                System.out.print("¿Desea realizar otra operacion? (s/n): ");
                String respuesta = teclado.readLine();
                continuar = respuesta != null && respuesta.trim().equalsIgnoreCase("s");
            }
            System.out.println("Cliente finalizado.");
        } catch (IOException ex) {
            System.err.println("Error leyendo el teclado: " + ex.getMessage());
        }
    }

    private static boolean leerPreguntaYResponder(
            BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        String pregunta = entrada.readLine();
        if (pregunta == null) {
            System.out.println("El servidor cerro la conexion.");
            return false;
        }

        System.out.print("Servidor: " + pregunta + ": ");
        String respuesta = teclado.readLine();
        if (respuesta == null) return false;
        salida.println(respuesta);
        return true;
    }
}
