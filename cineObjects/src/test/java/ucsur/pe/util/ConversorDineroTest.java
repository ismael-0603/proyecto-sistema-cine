package ucsur.pe.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de ConversorDinero: el respaldo de cómo el sistema evita errores de punto flotante.
 *
 * Idea: decimal (texto "4.70") -> entero (470 céntimos) -> operación con enteros -> decimal ("S/ 4.70").
 * Nunca se pasa por double, porque double no puede representar exactamente valores como 0.1 o 0.29.
 *
 * Patrón AAA en cada prueba.
 */
@DisplayName("Pruebas unitarias: ConversorDinero (decimal -> entero -> decimal)")
class ConversorDineroTest {

    // ---------------- El problema que se evita ----------------

    @Test
    @DisplayName("Con double, 0.1 + 0.2 NO da 0.3 (por eso el sistema no usa double)")
    void double_tieneErrorDePuntoFlotante() {
        double suma = 0.1 + 0.2;
        assertNotEquals(0.3, suma);   // da 0.30000000000000004
    }

    @Test
    @DisplayName("Con double, (long)(0.29 * 100) da 28 y no 29")
    void double_pierdeUnCentimo() {
        assertEquals(28L, (long) (0.29 * 100));   // 0.29 * 100 = 28.999999999999996
    }

    // ---------------- Decimal -> entero ----------------

    @Test
    @DisplayName("aCentavos(\"0.29\") da exactamente 29")
    void aCentavos_029_da29() {
        assertEquals(29L, ConversorDinero.aCentavos("0.29"));
    }

    @Test
    @DisplayName("aCentavos acepta \"10\", \"10.5\" (=10.50) y \"10,50\"")
    void aCentavos_variosFormatos() {
        assertEquals(1000L, ConversorDinero.aCentavos("10"));
        assertEquals(1050L, ConversorDinero.aCentavos("10.5"));
        assertEquals(1050L, ConversorDinero.aCentavos("10,50"));
    }

    // ---------------- Operación con enteros ----------------

    @Test
    @DisplayName("Sumar 10 veces S/ 0.10 da exactamente S/ 1.00")
    void sumaDeCentavos_esExacta() {
        // Arrange
        long diezCentimos = ConversorDinero.aCentavos("0.10");
        long total = 0;
        // Act
        for (int i = 0; i < 10; i++) {
            total += diezCentimos;
        }
        // Assert
        assertEquals(100L, total);
        assertEquals("S/ 1.00", ConversorDinero.formatear(total));
    }

    @Test
    @DisplayName("aplicarPorcentaje(1055, 18) = 190 (189.9 se redondea al céntimo)")
    void aplicarPorcentaje_redondeaAlCentimo() {
        assertEquals(190L, ConversorDinero.aplicarPorcentaje(1055, 18));
    }

    @Test
    @DisplayName("aplicarPorcentaje de 0 céntimos es 0 (valor límite)")
    void aplicarPorcentaje_deCero_esCero() {
        assertEquals(0L, ConversorDinero.aplicarPorcentaje(0, 18));
    }

    @Test
    @DisplayName("impuestoIncluido(2490, 18) = 380: S/ 24.90 trae S/ 3.80 de IGV (base 21.10)")
    void impuestoIncluido_entradaGeneral() {
        assertEquals(380L, ConversorDinero.impuestoIncluido(2490, 18));
        assertEquals(18L, ConversorDinero.impuestoIncluido(118, 18));   // S/ 1.18 = 1.00 + 0.18 exacto
        assertEquals(0L, ConversorDinero.impuestoIncluido(0, 18));      // valor límite
    }

    @Test
    @DisplayName("impuestoIncluido: base + IGV da siempre el precio y la base es la más cercana a precio / 1.18")
    void impuestoIncluido_cuadraSiempre() {
        for (long precio = 0; precio <= 20000; precio++) {
            long igv = ConversorDinero.impuestoIncluido(precio, 18);
            long base = precio - igv;
            // |base * 118 - precio * 100| <= 59  <=>  base es precio / 1.18 redondeado al céntimo
            assertTrue(Math.abs(base * 118 - precio * 100) <= 59, "precio " + precio);
        }
    }

    @Test
    @DisplayName("impuestoIncluido rechaza montos o porcentajes negativos")
    void impuestoIncluido_rechazaNegativos() {
        assertThrows(IllegalArgumentException.class, () -> ConversorDinero.impuestoIncluido(-1, 18));
        assertThrows(IllegalArgumentException.class, () -> ConversorDinero.impuestoIncluido(100, -1));
    }

    // ---------------- Entero -> decimal ----------------

    @Test
    @DisplayName("formatear(470) = \"S/ 4.70\", formatear(0) = \"S/ 0.00\", formatear(-470) = \"-S/ 4.70\"")
    void formatear_dosDecimales() {
        assertEquals("S/ 4.70", ConversorDinero.formatear(470));
        assertEquals("S/ 0.00", ConversorDinero.formatear(0));
        assertEquals("-S/ 4.70", ConversorDinero.formatear(-470));
    }

    // ---------------- Rechazos ----------------

    @Test
    @DisplayName("aCentavos rechaza null, texto, negativos y más de 2 decimales")
    void aCentavos_rechazaInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> ConversorDinero.aCentavos(null));
        assertThrows(IllegalArgumentException.class, () -> ConversorDinero.aCentavos("abc"));
        assertThrows(IllegalArgumentException.class, () -> ConversorDinero.aCentavos("-5"));
        assertThrows(IllegalArgumentException.class, () -> ConversorDinero.aCentavos("1.234"));
    }

    @Test
    @DisplayName("aplicarPorcentaje rechaza montos o porcentajes negativos")
    void aplicarPorcentaje_rechazaNegativos() {
        assertThrows(IllegalArgumentException.class, () -> ConversorDinero.aplicarPorcentaje(-1, 18));
        assertThrows(IllegalArgumentException.class, () -> ConversorDinero.aplicarPorcentaje(100, -1));
    }
}
