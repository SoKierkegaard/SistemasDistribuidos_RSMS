package bo.edu.usfx.jgroups;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Subasta implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final String articulo;
    private final BigDecimal precioBase;
    private final String creador;
    private final long cierreEn;
    private final List<Puja> pujas = new ArrayList<>();
    private boolean cerrada;

    public Subasta(String articulo, BigDecimal precioBase, String creador, long cierreEn) {
        this.articulo = articulo;
        this.precioBase = precioBase;
        this.creador = creador;
        this.cierreEn = cierreEn;
    }

    public String articulo() { return articulo; }
    public BigDecimal precioBase() { return precioBase; }
    public String creador() { return creador; }
    public long cierreEn() { return cierreEn; }
    public boolean cerrada() { return cerrada; }
    public void cerrar() { cerrada = true; }
    public void agregarPuja(Puja puja) { pujas.add(puja); }
    public List<Puja> pujas() { return Collections.unmodifiableList(pujas); }
    public Puja mejorPuja() { return pujas.isEmpty() ? null : pujas.get(pujas.size() - 1); }
    public BigDecimal montoActual() { return mejorPuja() == null ? precioBase : mejorPuja().monto(); }
}
