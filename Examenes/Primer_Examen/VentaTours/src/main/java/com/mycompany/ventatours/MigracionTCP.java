/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.ventatours;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author PC
 */
public class MigracionTCP {
    private static final int PUERTO=5001;
    public static void main(String[] args){
        try(ServerSocket server= new ServerSocket(PUERTO)){
            while(true){
                Socket cliente=server.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
                PrintStream out = new PrintStream(cliente.getOutputStream(),true);
                
                String linea=in.readLine();
                
                String respuesta="valido:BOLIVIA";
                out.println(respuesta);
                
            }
            
        }catch(IOException e){
            System.out.println("ERROR TCP");
        }
    }
}
