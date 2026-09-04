/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bo.edu.usfx.jgroups;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.jgroups.View;


/**
 *
 * @author aaran
 */
public class NodoBasico {

    public static void main(String[] args) throws Exception {
        String nombre = args.length > 0 ? args[0] : "nodo-" + (int) (Math.random() * 100);

        JChannel canal = new JChannel();
        canal.name(nombre);

        canal.setReceiver(new org.jgroups.Receiver() {

            @Override
            public void viewAccepted(View vista) {
                System.out.println("** Nueva vista: " + vista);
            }

            @Override
            public void receive(Message msg) {
                System.out.println("[" + msg.getSrc() + "] " + msg.getObject());
            }
        });

        canal.connect("ClusterSIS258");
        System.out.println("Conectado como " + canal.getAddress() + " | coordinador: " + canal.getView().getCoord());

        for (int i = 1; i <= 5; i++) {
            canal.send(new ObjectMessage(null, "Hola #" + i + " desde " + nombre));
            Thread.sleep(3000);
        }

        canal.close();

    }
}
