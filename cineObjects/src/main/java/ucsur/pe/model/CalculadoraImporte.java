package ucsur.pe.model;

import ucsur.pe.util.ConversorDinero;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Por qué existe: para que el cálculo del dinero (subtotal, descuentos, IGV y total) esté en un solo lugar.
 * Si cambia el IGV o cómo se combinan los descuentos, solo se toca esta clase.
 * Los precios de entradas y dulcería son precios al público: YA incluyen el IGV. Por eso el IGV no se suma:
 * se separa del total para mostrarlo en el comprobante (Op. Gravada + IGV = total).
 * Todo se calcula en céntimos (long): las sumas son exactas y solo el IGV y los porcentajes redondean,
 * siempre al céntimo más cercano (ConversorDinero).
 */
public class CalculadoraImporte {
    private static final int PORCENTAJE_IGV = 18; // IGV de Perú: 18%, incluido en el precio

    /**
     * Suma los precios (con IGV) y aplica los descuentos sobre ese monto, uno tras otro según su orden
     * de aplicación (las promociones antes que los cupones): cada uno descuenta sobre lo que dejó el anterior,
     * así que el descuento total nunca supera el subtotal. Lo que queda es el total a pagar, y de él se
     * separa el IGV que contiene.
     * @param entradasValidas entradas que cuentan para el cobro (ni anuladas ni expiradas)
     * @param dulceria el pedido de dulcería, o null; solo se necesita su total, por eso basta un Totalizable
     */
    public ResumenImporte calcular(List<Entrada> entradasValidas, Totalizable dulceria, List<Descuento> descuentos) {
        long sumaEntradas = 0;
        for (Entrada e : entradasValidas) {
            sumaEntradas += e.getPrecioCentavos(); // el precio pactado al elegir el tipo, aunque la tarifa cambie después
        }
        long sumaDulceria = (dulceria != null) ? dulceria.calcularTotalCentavos() : 0;
        long subtotal = sumaEntradas + sumaDulceria; // con IGV incluido

        List<Descuento> enOrden = new ArrayList<>(descuentos);
        enOrden.sort(Comparator.comparingInt(Descuento::getOrdenAplicacion));
        long restante = subtotal;
        for (Descuento d : enOrden) {
            restante -= d.calcularDescuentoCentavos(restante);
        }
        long descuento = subtotal - restante;

        long total = subtotal - descuento; // lo que paga el cliente, IGV incluido
        long igv = ConversorDinero.impuestoIncluido(total, PORCENTAJE_IGV);
        return new ResumenImporte(subtotal, descuento, igv, total);
    }
}
