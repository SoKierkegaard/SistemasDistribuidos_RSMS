package bo.edu.usfx.jgroups;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

public record MensajeRemate(Tipo tipo, String articulo, BigDecimal monto,
                            String participante, long instante, long cierreEn,
                            String detalle) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public enum Tipo {
        PROPONER_CREACION, CREACION_ACEPTADA,
        PROPONER_PUJA, PUJA_ACEPTADA,
        CIERRE, RECHAZO
    }

    public static MensajeRemate proponerCreacion(String articulo, BigDecimal base,
                                                  String creador, long duracionMs) {
        return new MensajeRemate(Tipo.PROPONER_CREACION, articulo, base, creador,
                duracionMs, 0, null);
    }

    public static MensajeRemate creacionAceptada(String articulo, BigDecimal base,
                                                  String creador, long cierreEn) {
        return new MensajeRemate(Tipo.CREACION_ACEPTADA, articulo, base, creador,
                System.currentTimeMillis(), cierreEn, null);
    }

    public static MensajeRemate proponerPuja(String articulo, BigDecimal monto, String participante) {
        return new MensajeRemate(Tipo.PROPONER_PUJA, articulo, monto, participante,
                System.currentTimeMillis(), 0, null);
    }

    public static MensajeRemate pujaAceptada(String articulo, BigDecimal monto,
                                              String participante, long instante) {
        return new MensajeRemate(Tipo.PUJA_ACEPTADA, articulo, monto, participante,
                instante, 0, null);
    }

    public static MensajeRemate cierre(String articulo) {
        return new MensajeRemate(Tipo.CIERRE, articulo, null, null,
                System.currentTimeMillis(), 0, null);
    }

    public static MensajeRemate rechazo(String detalle) {
        return new MensajeRemate(Tipo.RECHAZO, null, null, null,
                System.currentTimeMillis(), 0, detalle);
    }
}
