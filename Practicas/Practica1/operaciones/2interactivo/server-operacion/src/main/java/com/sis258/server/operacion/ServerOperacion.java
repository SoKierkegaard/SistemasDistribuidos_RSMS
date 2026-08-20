package com.sis258.server.operacion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class ServerOperacion {
    private static final int PORT = 5003;

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Servidor interactivo iniciado en el puerto " + PORT);
            while (true) {
                try (Socket client = server.accept();
                     BufferedReader entrada = new BufferedReader(new InputStreamReader(client.getInputStream()));
                     PrintWriter salida = new PrintWriter(client.getOutputStream(), true)) {
                    atenderCliente(entrada, salida);
                } catch (IOException ex) {
                    System.err.println("Error atendiendo al cliente: " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            System.err.println("No se pudo iniciar el servidor: " + ex.getMessage());
        }
    }

    private static void atenderCliente(BufferedReader entrada, PrintWriter salida) throws IOException {
        salida.println("Introduzca el primer numero");
        Double numero1 = leerNumero(entrada, salida);
        if (numero1 == null) return;

        salida.println("Introduzca el segundo numero");
        Double numero2 = leerNumero(entrada, salida);
        if (numero2 == null) return;

        salida.println("Operacion: 1.suma 2.resta 3.multiplicacion 4.division");
        String operacion = entrada.readLine();
        if (operacion == null) {
            salida.println("ERROR conexion cerrada antes de elegir la operacion");
            return;
        }

        double resultado;
        switch (operacion.trim().toLowerCase()) {
            case "1", "suma", "sumar" -> resultado = numero1 + numero2;
            case "2", "resta", "restar" -> resultado = numero1 - numero2;
            case "3", "multiplicacion", "multiplicar" -> resultado = numero1 * numero2;
            case "4", "division", "dividir" -> {
                if (numero2 == 0) {
                    salida.println("ERROR no se puede dividir entre cero");
                    return;
                }
                resultado = numero1 / numero2;
            }
            default -> {
                salida.println("ERROR operacion no valida");
                return;
            }
        }
        salida.println("Resultado: " + formatear(resultado));
    }

    private static Double leerNumero(BufferedReader entrada, PrintWriter salida) throws IOException {
        String texto = entrada.readLine();
        if (texto == null) {
            salida.println("ERROR conexion cerrada antes de recibir el numero");
            return null;
        }
        try {
            return Double.valueOf(texto.trim());
        } catch (NumberFormatException ex) {
            salida.println("ERROR debe introducir un numero valido");
            return null;
        }
    }

    private static String formatear(double numero) {
        return numero == Math.rint(numero) ? Long.toString((long) numero) : Double.toString(numero);
    }
}
