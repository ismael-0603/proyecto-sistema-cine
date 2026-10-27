package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import ucsur.pe.cineInterfaz.Descuento;
import ucsur.pe.cineInterfaz.Totalizable;

/**
 * Por qué existe: para que el cálculo del dinero (subtotal, descuentos, IGV y total) esté en un solo lugar.
 * Si cambia el IGV o cómo se combinan los descuentos, solo se toca esta clase.
 */
public class CalculadoraImporte {
    private static final double TASA_IGV = 0.18; // IGV de Perú: 18%

    /**
     * Aplica los descuentos uno tras otro según su orden de aplicación (las promociones antes que los cupones):
     * cada uno descuenta sobre lo que dejó el anterior, así que el descuento total nunca supera el subtotal.
     * @param entradasValidas entradas que cuentan para el cobro (ni anuladas ni expiradas)
     * @param dulceria el pedido de dulcería, o null; solo se necesita su total, por eso basta un Totalizable
     */
    public ResumenImporte calcular(List<Entrada> entradasValidas, Totalizable dulceria, List<Descuento> descuentos) {
        double sumaEntradas = 0;
        for (Entrada e : entradasValidas) {
            sumaEntradas += e.calculaPrecioFinal();
        }
        double sumaDulceria = (dulceria != null) ? dulceria.calcularTotal() : 0.0;
        double subtotal = sumaEntradas + sumaDulceria;

        List<Descuento> enOrden = new ArrayList<>(descuentos);
        enOrden.sort(Comparator.comparingInt(Descuento::getOrdenAplicacion));
        double restante = subtotal;
        for (Descuento d : enOrden) {
            restante -= d.calcularDescuento(restante);
        }
        double descuento = subtotal - restante;

        double baseImponible = subtotal - descuento;
        double igv = baseImponible * TASA_IGV;
        return new ResumenImporte(subtotal, descuento, igv, baseImponible + igv);
    }
}
