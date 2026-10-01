/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.ventatours;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 *
 * @author PC
 */
public class ClienteTurista {
    public static void main(String[] args){
        try{
            Registry reg=LocateRegistry.getRegistry("localhost", 1098);
            IVoucher servicio=(IVoucher) reg.lookup("Voucher");
            
            Voucher voucher=servicio.ComprarTour("12345678", "1234", 4);
            
            String confirmado=voucher.getConfirmado()?"Confirmado":"No fue confirmado";
            System.out.println("Confirmado: "+confirmado);
            System.out.println("Codigo de Compra: "+voucher.getCodigoCompra());
            System.out.println("Motivo: "+voucher.getMotivo());
            System.out.println("Monto USD: "+voucher.getMontoUSD());
            
        }catch(Exception e){
            System.out.println("Error");
        }
    }
}
