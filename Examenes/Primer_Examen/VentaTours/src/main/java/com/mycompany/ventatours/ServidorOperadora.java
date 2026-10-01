/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.ventatours;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 *
 * @author PC
 */
public class ServidorOperadora extends UnicastRemoteObject implements IVoucher {

    private static final long serialVersionUID = 1L;

    public ServidorOperadora() throws RemoteException {
        super();
    }

    @Override
    public Voucher ComprarTour(String pasaporte, String codigoTour, int personas) throws RemoteException {
        Boolean Confirmado = false;
        int montoUSD = 0;

        // socket tcp migracion

        try (
                Socket socket = new Socket("localhost", 5001);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintStream out = new PrintStream(socket.getOutputStream(), true);) {

            out.println("pasaporte:" + pasaporte);
            String resp = in.readLine();
            String partes[] = resp.split(":");

            Confirmado = partes[0].trim().equalsIgnoreCase("valido");
            if (Confirmado) {
                montoUSD = 180 * personas;
                if (partes[1].trim().equalsIgnoreCase("bolivia")) {
                    montoUSD = montoUSD / 2;
                }
            }

        } catch (IOException e) {
            System.out.println("Error migracion");
        }

        // banco rmi
        String codigoCompra = "";
        String motivo = "";

        try {
            Registry reg = LocateRegistry.getRegistry("localhost", 1099);
            IPago servicio = (IPago) reg.lookup("Banco");
            Pago pago = servicio.Debitar(pasaporte, montoUSD);

            if (pago.getAprobado()) {
                codigoCompra = "C-001";
                motivo = "Riesgo Bajo";
            }

        } catch (Exception e) {
            System.out.println("Error banco");
        }

        return new Voucher(Confirmado, codigoCompra, motivo, montoUSD);

    }

    public static void main(String[] args) {
        try {
            Registry reg;
            try {
                reg = LocateRegistry.createRegistry(1098);
            } catch (Exception e) {
                reg = LocateRegistry.getRegistry(1098);
            }
            reg.rebind("Voucher", new ServidorOperadora());

        } catch (Exception e) {
            System.out.println("Error Servicio Banco");
        }
    }
}
