package bo.edu.usfx.sockets;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Modela una sala de chat: un nombre y el conjunto de clientes dentro.
 * Usamos CopyOnWriteArraySet porque varios hilos (uno por cliente) pueden
 * entrar, salir y recorrer la sala AL MISMO TIEMPO. Esta coleccion crea
 * una copia interna al escribir, evitando ConcurrentModificationException.
 */
public class Sala {
    private final String nombre;
    private final Set<ManejadorChat> miembros = new CopyOnWriteArraySet<>();

    public Sala(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public Set<ManejadorChat> getMiembros() {
        return miembros;
    }

    public void agregar(ManejadorChat m) {
        miembros.add(m);
    }

    public void quitar(ManejadorChat m) {
        miembros.remove(m);
    }

    public int cantidad() {
        return miembros.size();
    }

    /**
     * Envia un mensaje a todos los miembros de la sala, excepto al emisor.
     * Recorrer un CopyOnWriteArraySet es seguro aunque otro hilo modifique.
     */
    public void difundir(String mensaje, ManejadorChat emisor) {
        for (ManejadorChat m : miembros) {
            if (m != emisor) {
                m.enviar(mensaje);
            }
        }
    }
}