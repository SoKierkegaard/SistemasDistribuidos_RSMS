package bo.edu.usfx.jgroups;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;

public class ConsolaRemate {
    private final RemateUSFX nodo;

    public ConsolaRemate(RemateUSFX nodo) { this.nodo = nodo; }

    public void ejecutar() throws Exception {
        System.out.println("Comandos: /crear <articulo> <precio> <segundos> | /pujar <articulo> <monto>");
        System.out.println("          /subastas | /estado <articulo> | /quien | /ganadas | /salir");
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        String linea;
        while ((linea = teclado.readLine()) != null) {
            linea = linea.trim();
            if (linea.equals("/salir")) return;
            try { procesar(linea); }
            catch (IllegalArgumentException e) { System.out.println("ERROR: " + e.getMessage()); }
        }
    }

    private void procesar(String linea) throws Exception {
        if (linea.startsWith("/crear ")) {
            String[] p = linea.substring(7).trim().split("\\s+");
            if (p.length < 3) throw new IllegalArgumentException("Uso: /crear <articulo> <precio> <segundos>");
            String articulo = unir(p, 0, p.length - 2);
            nodo.proponerCreacion(articulo, new BigDecimal(p[p.length - 2]),
                    Long.parseLong(p[p.length - 1]));
        } else if (linea.startsWith("/pujar ")) {
            String[] p = linea.substring(7).trim().split("\\s+");
            if (p.length < 2) throw new IllegalArgumentException("Uso: /pujar <articulo> <monto>");
            nodo.proponerPuja(unir(p, 0, p.length - 1), new BigDecimal(p[p.length - 1]));
        } else if (linea.equals("/subastas")) nodo.mostrarSubastas();
        else if (linea.startsWith("/estado ")) nodo.mostrarEstado(linea.substring(8).trim());
        else if (linea.equals("/quien")) nodo.mostrarParticipantes();
        else if (linea.equals("/ganadas")) nodo.mostrarGanadas();
        else if (!linea.isBlank()) throw new IllegalArgumentException("Comando desconocido");
    }

    private static String unir(String[] partes, int inicio, int finExclusivo) {
        StringBuilder resultado = new StringBuilder();
        for (int i = inicio; i < finExclusivo; i++) {
            if (resultado.length() > 0) resultado.append(' ');
            resultado.append(partes[i]);
        }
        if (resultado.length() == 0) throw new IllegalArgumentException("El articulo no puede estar vacio");
        return resultado.toString();
    }
}
