package com.sis258.server.operacion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class ServerOperacion {
    private static final int PORT = 5002;

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado en el puerto " + PORT);
            while (true) {
                try (Socket client = server.accept();
                     BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
                     PrintWriter toClient = new PrintWriter(client.getOutputStream(), true)) {
                    String solicitud = fromClient.readLine();
                    String respuesta = procesarSolicitud(solicitud);
                    System.out.println("Solicitud: " + solicitud + " -> " + respuesta);
                    toClient.println(respuesta);
                } catch (IOException ex) {
                    System.err.println("Error atendiendo al cliente: " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            System.err.println("No se pudo iniciar el servidor: " + ex.getMessage());
        }
    }

    /**
     * Protocolo: OPERACION NUMERO1 NUMERO2.
     * Ejemplos: "suma 8 2", "resta,8,2" o "division:8:2".
     */
    public static String procesarSolicitud(String cadena) {
        if (cadena == null || cadena.isBlank()) return "ERROR solicitud vacia";
        String[] partes = cadena.trim().toLowerCase().split("[\\s,;:]+");
        if (partes.length != 3) return "ERROR formato esperado: OPERACION NUMERO1 NUMERO2";

        try {
            double numero1 = Double.parseDouble(partes[1]);
            double numero2 = Double.parseDouble(partes[2]);
            double resultado;
            switch (partes[0]) {
                case "1", "suma", "sumar" -> resultado = numero1 + numero2;
                case "2", "resta", "restar" -> resultado = numero1 - numero2;
                case "3", "multiplicacion", "multiplicar" -> resultado = numero1 * numero2;
                case "4", "division", "dividir" -> {
                    if (numero2 == 0) return "ERROR no se puede dividir entre cero";
                    resultado = numero1 / numero2;
                }
                default -> { return "ERROR operacion no valida"; }
            }
            return "OK " + formatear(resultado);
        } catch (NumberFormatException ex) {
            return "ERROR los operandos deben ser numeros";
        }
    }

    private static String formatear(double numero) {
        return numero == Math.rint(numero) ? Long.toString((long) numero) : Double.toString(numero);
    }
}
