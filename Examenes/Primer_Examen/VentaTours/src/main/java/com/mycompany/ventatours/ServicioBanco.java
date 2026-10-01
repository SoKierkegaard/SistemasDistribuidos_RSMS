/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.ventatours;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.registry.Registry;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;

/**
 *
 * @author PC
 */
public class ServicioBanco extends UnicastRemoteObject implements IPago {
    private static final long serialVersionUID = 1L;
    private static final String host = "localhost";
    private static final int puerto = 6789;

    public ServicioBanco() throws RemoteException {
        super();
    }

    @Override
    public Pago Debitar(String pasaporte, int montoUSD) throws RemoteException {
        Boolean aprobado = true;
        String respuesta = "";

        try (DatagramSocket socketUDP = new DatagramSocket()) {
            InetAddress ipDestino = InetAddress.getByName(host);
            byte[] buferRecepcion = new byte[1024];

            String linea = "riesgo:" + pasaporte + "-" + montoUSD;

            byte[] datosEnvio = linea.getBytes();
            DatagramPacket paqueteEnvio = new DatagramPacket(datosEnvio, datosEnvio.length, ipDestino, puerto);
            socketUDP.send(paqueteEnvio);

            DatagramPacket paqueteRespuesta = new DatagramPacket(buferRecepcion, buferRecepcion.length);
            socketUDP.receive(paqueteRespuesta);
            respuesta = new String(paqueteRespuesta.getData(), 0, paqueteRespuesta.getLength()).trim();

            if (respuesta.equalsIgnoreCase("alto")) {
                aprobado = false;
            }
        } catch (SocketException e) {
            System.out.println("Error Banco");
        } catch (IOException z) {
            System.out.println("Error mandar");
        }
        return new Pago(aprobado, "789", respuesta);
    }

    public static void main(String[] args) {
        try {
            Registry reg;
            reg = LocateRegistry.createRegistry(1099);
            reg.rebind("Banco", new ServicioBanco());
        } catch (Exception e) {
            System.out.println("Error Servicio Banco");
        }
    }

}
