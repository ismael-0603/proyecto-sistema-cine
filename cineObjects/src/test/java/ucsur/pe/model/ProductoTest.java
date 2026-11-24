package ucsur.pe.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de Producto: un producto de dulcería con precio en céntimos y stock por sede.
 *
 * Se verifica:
 *   - obtenerPuntos()      -> combo activo = 2; no combo o inactivo = 0
 *   - toString()           -> "Nombre (S/ 12.50)" usando ConversorDinero
 *   - reponerStock / descontarStock -> suman y restan; rechazan cantidades inválidas o stock insuficiente
 *   - tieneStockBajo / estaAgotado  -> umbral de 15 unidades y stock 0
 *   - Validaciones del precio (0 válido, -1 rechazado)
 *
 * Patrón AAA en cada prueba. No necesita base de datos ni JavaFX.
 */
@DisplayName("Pruebas unitarias: Producto")
class ProductoTest {

    private static final long PRECIO = 1250;   // S/ 12.50 en céntimos

    private Producto canchita;   // producto normal (no combo), activo
    private Producto combo;      // combo activo
    private Sede sede;

    @BeforeEach
    void setUp() {
        canchita = new Producto(1, "Canchita", "Canchita salada grande", "Canchita", PRECIO, false, true);
        combo    = new Producto(2, "Combo Pareja", "2 canchitas + 2 gaseosas", "Combos", 3990, true, true);
        // La sede necesita id > 0: su equals/hashCode usan el id y es la clave del stock por sede.
        sede = new Sede(1, "Cine Centro", "Av. Principal 123", "Lima", null, null, Sede.ACTIVA);
    }

    // ---------------- Puntos ----------------

    @Test
    @DisplayName("obtenerPuntos() de un combo activo es 2")
    void comboActivo_da2Puntos() {
        assertEquals(2, combo.obtenerPuntos());
    }

    @Test
    @DisplayName("obtenerPuntos() de un producto que no es combo es 0")
    void noCombo_da0Puntos() {
        assertEquals(0, canchita.obtenerPuntos());
    }

    @Test
    @DisplayName("obtenerPuntos() de un combo desactivado es 0")
    void comboInactivo_da0Puntos() {
        // Arrange
        combo.desactivar();
        // Act
        int puntos = combo.obtenerPuntos();
        // Assert
        assertEquals(0, puntos);
    }

    // ---------------- Formato y precio ----------------

    @Test
    @DisplayName("toString() muestra el nombre y el precio formateado: 'Canchita (S/ 12.50)'")
    void toString_muestraNombreYPrecio() {
        assertEquals("Canchita (S/ 12.50)", canchita.toString());
    }

    @Test
    @DisplayName("getPrecioCentavos() devuelve 1250 (el dinero se guarda en céntimos)")
    void precio_seGuardaEnCentavos() {
        assertEquals(1250L, canchita.getPrecioCentavos());
    }

    @Test
    @DisplayName("Precio 0 es válido (producto de cortesía) y se muestra como S/ 0.00")
    void precioCero_esValido() {
        Producto gratis = new Producto(3, "Sorbete", null, "Extras", 0, false, true);
        assertEquals("Sorbete (S/ 0.00)", gratis.toString());
    }

    @Test
    @DisplayName("Precio negativo (-1) lanza IllegalArgumentException")
    void precioNegativo_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new Producto(4, "Error", null, "Extras", -1, false, true));
    }

    // ---------------- Stock ----------------

    @Test
    @DisplayName("Un producto nuevo tiene stock 0 y está agotado en la sede")
    void productoNuevo_estaAgotado() {
        assertEquals(0, canchita.getStock(sede));
        assertTrue(canchita.estaAgotado(sede));
    }

    @Test
    @DisplayName("reponerStock(50) y luego descontarStock(20) deja 30 unidades")
    void reponerYDescontar_calculaElStock() {
        // Arrange
        canchita.reponerStock(sede, 50);
        // Act
        canchita.descontarStock(sede, 20);
        // Assert
        assertEquals(30, canchita.getStock(sede));
    }

    @Test
    @DisplayName("Con 15 unidades hay stock bajo; con 16 no (umbral = 15)")
    void umbralStockBajo_es15() {
        canchita.reponerStock(sede, 15);
        assertTrue(canchita.tieneStockBajo(sede));

        canchita.reponerStock(sede, 1);   // ahora 16
        assertFalse(canchita.tieneStockBajo(sede));
    }

    @Test
    @DisplayName("Con stock 0 no es 'stock bajo' sino 'agotado'")
    void stockCero_esAgotadoNoBajo() {
        assertFalse(canchita.tieneStockBajo(sede));
        assertTrue(canchita.estaAgotado(sede));
    }

    @Test
    @DisplayName("Descontar más de lo que hay lanza IllegalStateException y no cambia el stock")
    void descontarDeMas_lanzaExcepcion() {
        canchita.reponerStock(sede, 5);
        assertThrows(IllegalStateException.class, () -> canchita.descontarStock(sede, 6));
        assertEquals(5, canchita.getStock(sede));
    }

    @Test
    @DisplayName("Reponer 0 unidades o en una sede nula lanza IllegalArgumentException")
    void reponerInvalido_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> canchita.reponerStock(sede, 0));
        assertThrows(IllegalArgumentException.class, () -> canchita.reponerStock(null, 10));
    }

    @Test
    @DisplayName("Un producto desactivado no tiene stock disponible para vender")
    void productoInactivo_noVerificaStock() {
        canchita.reponerStock(sede, 10);
        canchita.desactivar();
        assertFalse(canchita.verificarStock(sede, 1));
    }
}
