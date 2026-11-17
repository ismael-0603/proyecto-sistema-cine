package ucsur.pe.model;

import ucsur.pe.util.ConversorDinero;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Por qué existe: para que el cálculo del dinero (subtotal, descuentos, IGV y total) esté en un solo lugar.
 * Si cambia el IGV o cómo se combinan los descuentos, solo se toca esta clase.
 * Todo se calcula en céntimos (long): las sumas son exactas y solo el IGV y los porcentajes redondean,
 * siempre al céntimo más cercano (ConversorDinero.aplicarPorcentaje).
 */
public class CalculadoraImporte {
    private static final int PORCENTAJE_IGV = 18; // IGV de Perú: 18%

    /**
     * Aplica los descuentos uno tras otro según su orden de aplicación (las promociones antes que los cupones):
     * cada uno descuenta sobre lo que dejó el anterior, así que el descuento total nunca supera el subtotal.
     * @param entradasValidas entradas que cuentan para el cobro (ni anuladas ni expiradas)
     * @param dulceria el pedido de dulcería, o null; solo se necesita su total, por eso basta un Totalizable
     */
    public ResumenImporte calcular(List<Entrada> entradasValidas, Totalizable dulceria, List<Descuento> descuentos) {
        long sumaEntradas = 0;
        for (Entrada e : entradasValidas) {
            sumaEntradas += e.calculaPrecioFinalCentavos();
        }
        long sumaDulceria = (dulceria != null) ? dulceria.calcularTotalCentavos() : 0;
        long subtotal = sumaEntradas + sumaDulceria;

        List<Descuento> enOrden = new ArrayList<>(descuentos);
        enOrden.sort(Comparator.comparingInt(Descuento::getOrdenAplicacion));
        long restante = subtotal;
        for (Descuento d : enOrden) {
            restante -= d.calcularDescuentoCentavos(restante);
        }
        long descuento = subtotal - restante;

        long baseImponible = subtotal - descuento;
        long igv = ConversorDinero.aplicarPorcentaje(baseImponible, PORCENTAJE_IGV);
        return new ResumenImporte(subtotal, descuento, igv, baseImponible + igv);
    }
}
