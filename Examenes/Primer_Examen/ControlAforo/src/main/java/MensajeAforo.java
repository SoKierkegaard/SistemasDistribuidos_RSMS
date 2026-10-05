import java.io.Serializable;

public class MensajeAforo implements Serializable {
    public enum Tipo { SOLICITUD, ACEPTADO, RECHAZADO, SALIDA }

    public Tipo tipo;
    public String puerta;
    public int personas;

    public MensajeAforo(Tipo tipo, String puerta, int personas) {
        this.tipo = tipo;
        this.puerta = puerta;
        this.personas = personas;
    }
}
