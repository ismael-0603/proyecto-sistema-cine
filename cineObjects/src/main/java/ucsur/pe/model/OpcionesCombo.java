package ucsur.pe.model;

import java.util.List;

/**
 * Por qué existe: personaliza la dulcería (RF-13): el sabor de la canchita y la marca de la gaseosa.
 * Aplica a los combos y también a la canchita y la gaseosa sueltas, que se venden por tamaño (una tarjeta
 * por tamaño en vez de una por sabor o marca). En un combo lo decide la descripción: si incluye «canchita»
 * se pide el sabor (salvo que ya lo diga) y si incluye «gaseosa», la marca; así un combo nuevo se
 * personaliza solo. La elección se guarda como texto en detalle_pedido.personalizacion.
 */
public final class OpcionesCombo {

    public static final List<String> SABORES_CANCHITA = List.of("Salada", "Dulce", "Mixta");
    public static final List<String> GASEOSAS = List.of(
            "Inca Kola", "Coca-Cola", "Sprite", "Fanta", "Inca Kola Zero", "Coca-Cola Zero");

    private OpcionesCombo() {
    }

    /**
     * Se elige el sabor: en un combo con canchita cuyo sabor no viene fijo en la descripción, o en una
     * canchita suelta que no sea Gourmet (las Gourmet ya traen su sabor: queso, BBQ, chocolate).
     */
    public static boolean pideSabor(Producto producto) {
        if (producto == null) return false;
        if (!producto.isEsCombo()) {
            return "Canchita".equalsIgnoreCase(producto.getCategoria()) && !nombre(producto).contains("gourmet");
        }
        String d = descripcion(producto);
        if (!d.contains("canchita")) return false;
        for (String sabor : SABORES_CANCHITA) {
            if (d.contains("canchita " + sabor.toLowerCase())) return false;
        }
        return true;
    }

    /** Se elige la marca: en un combo que trae gaseosa (o gaseosas) o en una gaseosa suelta. */
    public static boolean pideGaseosa(Producto producto) {
        if (producto == null) return false;
        if (!producto.isEsCombo()) return nombre(producto).startsWith("gaseosa");
        return descripcion(producto).contains("gaseosa");
    }

    /** El producto tiene algo que elegir. */
    public static boolean esPersonalizable(Producto producto) {
        return pideSabor(producto) || pideGaseosa(producto);
    }

    /**
     * Texto que se guarda en la línea del pedido, por ejemplo «Canchita: Dulce · Gaseosa: Inca Kola».
     * Exige una opción válida para cada cosa que el producto pide y devuelve null si no pide nada.
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

    private static String nombre(Producto producto) {
        return producto.getNombre() == null ? "" : producto.getNombre().toLowerCase();
    }

    private static String descripcion(Producto producto) {
        return producto.getDescripcion() == null ? "" : producto.getDescripcion().toLowerCase();
    }
}
