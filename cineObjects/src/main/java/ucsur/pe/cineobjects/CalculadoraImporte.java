package ucsur.pe.cineobjects;

import java.util.List;

/**
 * Por qué existe: para que el cálculo del dinero (subtotal, descuentos, IGV y total) esté en un solo lugar.
 * Si cambia el IGV o cómo se combinan los descuentos, solo se toca esta clase.
 */
public class CalculadoraImporte {
    private static final double TASA_IGV = 0.18; // IGV de Perú: 18%

    /**
     * Primero se aplica la promoción sobre el subtotal y luego el cupón sobre lo que queda;
     * el descuento total nunca supera el subtotal.
     * @param entradasValidas entradas que cuentan para el cobro (ni anuladas ni expiradas)
     */
    public ResumenImporte calcular(List<Entrada> entradasValidas, Pedido pedido,
                                   Promocion promocion, Cupon cupon) {
        double sumaEntradas = 0;
        for (Entrada e : entradasValidas) {
            sumaEntradas += e.calculaPrecioFinal();
        }
        double sumaPedido = (pedido != null) ? pedido.calcularTotal() : 0.0;
        double subtotal = sumaEntradas + sumaPedido;

        double descPromo = (promocion != null) ? promocion.calcularDescuento(subtotal) : 0.0;
        double descCupon = (cupon != null) ? cupon.calcularDescuento(subtotal - descPromo) : 0.0;
        double descuento = Math.min(subtotal, descPromo + descCupon);

        double baseImponible = subtotal - descuento;
        double igv = baseImponible * TASA_IGV;
        return new ResumenImporte(subtotal, descuento, igv, baseImponible + igv);
    }
}
