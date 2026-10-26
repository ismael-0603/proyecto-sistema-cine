package ucsur.pe.cineobjects;

/**
 * Por qué existe: guarda juntos los resultados del cálculo (subtotal, descuento, IGV y total).
 * Es inmutable: cada recálculo crea un resumen nuevo.
 */
public final class ResumenImporte {
    /** Importe de una compra que aún no tiene nada. */
    public static final ResumenImporte VACIO = new ResumenImporte(0, 0, 0, 0);

    private final double subtotal;
    private final double descuento;
    private final double igv;
    private final double total;

    public ResumenImporte(double subtotal, double descuento, double igv, double total) {
        this.subtotal = subtotal;
        this.descuento = descuento;
        this.igv = igv;
        this.total = total;
    }

    public double getSubtotal() { return subtotal; }
    public double getDescuento() { return descuento; }
    public double getIgv() { return igv; }
    public double getTotal() { return total; }

    @Override
    public String toString() {
        return String.format("Subtotal: S/ %.2f | Desc: S/ %.2f | IGV: S/ %.2f | Total: S/ %.2f",
                subtotal, descuento, igv, total);
    }
}
