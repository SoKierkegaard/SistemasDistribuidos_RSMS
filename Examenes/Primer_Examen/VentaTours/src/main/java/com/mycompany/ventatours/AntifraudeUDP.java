/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.ventatours;

import java.net.*;

/**
 *
 * @author PC
 */
public class AntifraudeUDP {
    private static final int PUERTO=6789;
    public static void main(String[] args){
        try(DatagramSocket socket = new DatagramSocket(PUERTO)){
            byte[] bufer=new byte[1024];
            
            while(true){
                DatagramPacket peticion = new DatagramPacket(bufer,bufer.length);
                socket.receive(peticion);
                String mensaje=new String(peticion.getData(),0,peticion.getLength()).trim();
                
                String respuesta="";
                String partes[] =mensaje.split("-");
                
                int monto=Integer.parseInt(partes[1]);
                respuesta=(monto>1000)?"alto":"bajo";
                
                byte[] datos=respuesta.getBytes();
                socket.send(new DatagramPacket(datos,datos.length,peticion.getAddress(),peticion.getPort()));
            }
        }catch(Exception e){
            System.out.println("Error Antifraude UDP");
        }
    }
}
