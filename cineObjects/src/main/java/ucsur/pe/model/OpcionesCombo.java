package ucsur.pe.model;

import java.util.List;

/**
 * Por qué existe: personaliza los combos de dulcería (RF-13): el sabor de la canchita y la marca de la gaseosa.
 * Lo decide la descripción del combo: si incluye «canchita» se pide el sabor (salvo que ya lo diga) y si
 * incluye «gaseosa», la marca; así un combo nuevo se personaliza solo. La elección se guarda como texto
 * en detalle_pedido.personalizacion.
 */
public final class OpcionesCombo {

    public static final List<String> SABORES_CANCHITA = List.of("Salada", "Dulce", "Mixta");
    public static final List<String> GASEOSAS = List.of("Inca Kola", "Coca-Cola", "Sprite", "Fanta");

    private OpcionesCombo() {
    }

    /** El combo trae canchita y su sabor no viene fijo en la descripción. */
    public static boolean pideSabor(Producto producto) {
        if (!esCombo(producto)) return false;
        String d = descripcion(producto);
        if (!d.contains("canchita")) return false;
        for (String sabor : SABORES_CANCHITA) {
            if (d.contains("canchita " + sabor.toLowerCase())) return false;
        }
        return true;
    }

    /** El combo trae gaseosa (o gaseosas). */
    public static boolean pideGaseosa(Producto producto) {
        return esCombo(producto) && descripcion(producto).contains("gaseosa");
    }

    /** El combo tiene algo que elegir. */
    public static boolean esPersonalizable(Producto producto) {
        return pideSabor(producto) || pideGaseosa(producto);
    }

    /**
     * Texto que se guarda en la línea del pedido, por ejemplo «Canchita: Dulce · Gaseosa: Inca Kola».
     * Exige una opción válida para cada cosa que el combo pide y devuelve null si no pide nada.
     */
    public static String describir(Producto producto, String sabor, String gaseosa) {
        StringBuilder texto = new StringBuilder();
        if (pideSabor(producto)) {
            exigirOpcion(SABORES_CANCHITA, sabor, "el sabor de la canchita");
            texto.append("Canchita: ").append(sabor);
        }
        if (pideGaseosa(producto)) {
            exigirOpcion(GASEOSAS, gaseosa, "la gaseosa");
            if (texto.length() > 0) texto.append(" · ");
            texto.append("Gaseosa: ").append(gaseosa);
        }
        return texto.length() == 0 ? null : texto.toString();
    }

    private static void exigirOpcion(List<String> opciones, String elegida, String que) {
        if (elegida == null || !opciones.contains(elegida)) {
            throw new IllegalArgumentException("Elige " + que);
        }
    }

    private static boolean esCombo(Producto producto) {
        return producto != null && producto.isEsCombo();
    }

    private static String descripcion(Producto producto) {
        return producto.getDescripcion() == null ? "" : producto.getDescripcion().toLowerCase();
    }
}
