package ucsur.pe.model;

import ucsur.pe.util.ConversorDinero;

/**
 * Por qué existe: guarda juntos los resultados del cálculo (subtotal, descuento, IGV y total), en céntimos.
 * Es inmutable: cada recálculo crea un resumen nuevo.
 */
public final class ResumenImporte {
    /** Importe de una compra que aún no tiene nada. */
    public static final ResumenImporte VACIO = new ResumenImporte(0, 0, 0, 0);

    private final long subtotalCentavos;
    private final long descuentoCentavos;
    private final long igvCentavos;
    private final long totalCentavos;

    public ResumenImporte(long subtotalCentavos, long descuentoCentavos, long igvCentavos, long totalCentavos) {
        this.subtotalCentavos = subtotalCentavos;
        this.descuentoCentavos = descuentoCentavos;
        this.igvCentavos = igvCentavos;
        this.totalCentavos = totalCentavos;
    }

    public long getSubtotalCentavos() { return subtotalCentavos; }
    public long getDescuentoCentavos() { return descuentoCentavos; }
    public long getIgvCentavos() { return igvCentavos; }
    public long getTotalCentavos() { return totalCentavos; }

    @Override
    public String toString() {
        return "Subtotal: " + ConversorDinero.formatear(subtotalCentavos)
                + " | Desc: " + ConversorDinero.formatear(descuentoCentavos)
                + " | IGV: " + ConversorDinero.formatear(igvCentavos)
                + " | Total: " + ConversorDinero.formatear(totalCentavos);
    }
}
