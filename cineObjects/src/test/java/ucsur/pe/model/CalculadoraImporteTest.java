package ucsur.pe.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ucsur.pe.tipo.CalculoDescuento;
import ucsur.pe.tipo.CalculoMontoFijo;
import ucsur.pe.tipo.CalculoPorcentaje;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CalculadoraImporte: los precios ya incluyen el IGV.
 *
 * Se verifica:
 *   - total = subtotal - descuento (el IGV no se suma encima del precio)
 *   - el descuento se aplica sobre el precio con IGV y el IGV se separa del total que queda
 *   - las promociones (orden 1) van antes que los cupones (orden 2), aunque lleguen en otro orden
 *   - op. gravada + IGV = total, siempre
 *
 * La dulcería llega como Totalizable (basta su total) y los descuentos con una forma de cálculo real
 * (porcentaje o monto fijo). Patrón AAA. No necesita base de datos ni JavaFX.
 */
@DisplayName("Pruebas unitarias: CalculadoraImporte (IGV incluido en el precio)")
class CalculadoraImporteTest {

    private final CalculadoraImporte calculadora = new CalculadoraImporte();

    /** Descuento mínimo para la prueba: siempre aplica y calcula con la forma indicada. */
    private static Descuento descuento(int orden, CalculoDescuento calculo) {
        return new Descuento() {
            @Override public boolean validarVigencia(DateTime fecha) { return true; }
            @Override public long calcularDescuentoCentavos(long total) { return calculo.calcularCentavos(total); }
            @Override public boolean esAplicable(ContextoDescuento contexto) { return true; }
            @Override public int getOrdenAplicacion() { return orden; }
            @Override public String getCategoria() { return "PRUEBA-" + orden; }
        };
    }

    private ResumenImporte calcular(long dulceria, List<Descuento> descuentos) {
        Totalizable pedido = () -> dulceria;
        return calculadora.calcular(List.of(), pedido, descuentos);
    }

    @Test
    @DisplayName("Sin descuento: S/ 24.90 se cobra S/ 24.90 (op. gravada 21.10 + IGV 3.80)")
    void sinDescuento_totalEsElPrecio() {
        // Act
        ResumenImporte r = calcular(2490, List.of());
        // Assert
        assertEquals(2490, r.getSubtotalCentavos());
        assertEquals(0, r.getDescuentoCentavos());
        assertEquals(2490, r.getTotalCentavos());
        assertEquals(380, r.getIgvCentavos());
        assertEquals(2110, r.getTotalCentavos() - r.getIgvCentavos());
    }

    @Test
    @DisplayName("15% sobre S/ 24.90 (con IGV): descuento 3.74, total 21.16, IGV incluido 3.23")
    void porcentaje_seAplicaSobreElPrecioConIgv() {
        ResumenImporte r = calcular(2490, List.of(descuento(1, new CalculoPorcentaje(15))));
        assertEquals(374, r.getDescuentoCentavos());
        assertEquals(2116, r.getTotalCentavos());
        assertEquals(323, r.getIgvCentavos());
    }

    @Test
    @DisplayName("S/ 5.00 de descuento sobre S/ 49.80: total 44.80, IGV incluido 6.83")
    void montoFijo_seRestaDelPrecioConIgv() {
        ResumenImporte r = calcular(4980, List.of(descuento(2, new CalculoMontoFijo(500))));
        assertEquals(500, r.getDescuentoCentavos());
        assertEquals(4480, r.getTotalCentavos());
        assertEquals(683, r.getIgvCentavos());
    }

    @Test
    @DisplayName("La promoción (10%) va antes que el cupón (S/ 5) aunque lleguen al revés: S/ 100 -> S/ 85")
    void promocionAntesQueCupon() {
        Descuento cupon = descuento(2, new CalculoMontoFijo(500));
        Descuento promocion = descuento(1, new CalculoPorcentaje(10));
        ResumenImporte r = calcular(10000, List.of(cupon, promocion));
        assertEquals(1500, r.getDescuentoCentavos());
        assertEquals(8500, r.getTotalCentavos());
    }

    @Test
    @DisplayName("Un descuento mayor que el precio deja el total y el IGV en cero (valor límite)")
    void descuentoMayorQueElPrecio_totalCero() {
        ResumenImporte r = calcular(2490, List.of(descuento(2, new CalculoMontoFijo(5000))));
        assertEquals(2490, r.getDescuentoCentavos());
        assertEquals(0, r.getTotalCentavos());
        assertEquals(0, r.getIgvCentavos());
    }

    @Test
    @DisplayName("Para cualquier monto: total = subtotal - descuento y el IGV nunca supera el total")
    void totalNuncaSumaElIgv() {
        Descuento promocion = descuento(1, new CalculoPorcentaje(20));
        for (long monto = 0; monto <= 20000; monto += 7) {
            ResumenImporte r = calcular(monto, List.of(promocion));
            assertEquals(r.getSubtotalCentavos() - r.getDescuentoCentavos(), r.getTotalCentavos());
            assertTrue(r.getIgvCentavos() >= 0 && r.getIgvCentavos() <= r.getTotalCentavos());
        }
    }
}
