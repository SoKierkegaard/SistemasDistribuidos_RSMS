package bo.edu.usfx.jgroups;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record Puja(String participante, BigDecimal monto, long instante) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return participante + " ofrecio " + monto.toPlainString() + " (" + Instant.ofEpochMilli(instante) + ")";
    }
}
