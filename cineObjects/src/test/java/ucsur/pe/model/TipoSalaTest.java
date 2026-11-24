package ucsur.pe.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de TipoSala: el formato de una sala (2D, 3D...) y su recargo en céntimos.
 *
 * Se verifica:
 *   - El constructor guarda los datos (formato recortado) y los getters los devuelven.
 *   - Valores límite aceptados: recargo 0, formato de 30 caracteres, descripción de 150 o null.
 *   - Valores rechazados: recargo negativo, formato vacío/null/de 31, descripción de 151.
 *
 * Cada prueba sigue el patrón AAA: Arrange (preparar), Act (ejecutar), Assert (verificar).
 * No necesita base de datos ni JavaFX: es puro modelo.
 */
@DisplayName("Pruebas unitarias: TipoSala")
class TipoSalaTest {

    private static final int    ID          = 2;
    private static final String FORMATO     = "3D";
    private static final String DESCRIPCION = "Proyección con lentes 3D";
    private static final long   RECARGO     = 500;   // S/ 5.00 en céntimos

    private TipoSala tipoSala;

    // Se ejecuta antes de CADA prueba: crea un TipoSala nuevo para que ninguna prueba afecte a otra.
    @BeforeEach
    void setUp() {
        tipoSala = new TipoSala(ID, FORMATO, DESCRIPCION, RECARGO);
    }

    // ---------------- Casos normales ----------------

    @Test
    @DisplayName("getIdTipoSala() devuelve el id entregado (2)")
    void getId_devuelveElIdEntregado() {
        // Act
        int id = tipoSala.getIdTipoSala();
        // Assert
        assertEquals(2, id);
    }

    @Test
    @DisplayName("getFormato() y getDescripcion() devuelven lo entregado")
    void getters_devuelvenLoEntregado() {
        assertEquals("3D", tipoSala.getFormato());
        assertEquals("Proyección con lentes 3D", tipoSala.getDescripcion());
    }

    @Test
    @DisplayName("obtenerRecargoFormatoCentavos() devuelve 500 céntimos (S/ 5.00)")
    void recargo_devuelveLosCentavosEntregados() {
        assertEquals(500L, tipoSala.obtenerRecargoFormatoCentavos());
    }

    @Test
    @DisplayName("El formato se guarda sin espacios al inicio ni al final: '  IMAX ' -> 'IMAX'")
    void formato_seGuardaRecortado() {
        // Arrange
        TipoSala imax = new TipoSala(3, "  IMAX ", null, 1000);
        // Act
        String formato = imax.getFormato();
        // Assert
        assertEquals("IMAX", formato);
    }

    @Test
    @DisplayName("toString() devuelve el formato (es lo que muestran los combos de la vista)")
    void toString_devuelveElFormato() {
        assertEquals("3D", tipoSala.toString());
    }

    // ---------------- Valores límite aceptados ----------------

    @Test
    @DisplayName("Recargo 0 (sala 2D sin recargo) es válido")
    void recargoCero_esValido() {
        TipoSala sala2D = new TipoSala(1, "2D", "Proyección normal", 0);
        assertEquals(0L, sala2D.obtenerRecargoFormatoCentavos());
    }

    @Test
    @DisplayName("Formato de exactamente 30 caracteres es válido (máximo de la BD)")
    void formatoDe30Caracteres_esValido() {
        String treinta = "A".repeat(30);
        TipoSala t = new TipoSala(4, treinta, null, 0);
        assertEquals(30, t.getFormato().length());
    }

    @Test
    @DisplayName("Descripción de 150 caracteres o null es válida")
    void descripcion150oNull_esValida() {
        assertDoesNotThrow(() -> new TipoSala(5, "4DX", "d".repeat(150), 0));
        assertNull(new TipoSala(6, "4DX", null, 0).getDescripcion());
    }

    // ---------------- Valores rechazados ----------------

    @Test
    @DisplayName("Recargo negativo (-1) lanza IllegalArgumentException")
    void recargoNegativo_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new TipoSala(7, "2D", null, -1));
    }

    @Test
    @DisplayName("Formato null, vacío o solo espacios lanza IllegalArgumentException")
    void formatoVacioONull_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new TipoSala(8, null, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new TipoSala(8, "", null, 0));
        assertThrows(IllegalArgumentException.class, () -> new TipoSala(8, "   ", null, 0));
    }

    @Test
    @DisplayName("Formato de 31 caracteres lanza IllegalArgumentException")
    void formatoDe31Caracteres_lanzaExcepcion() {
        String treintaYUno = "A".repeat(31);
        assertThrows(IllegalArgumentException.class, () -> new TipoSala(9, treintaYUno, null, 0));
    }

    @Test
    @DisplayName("Descripción de 151 caracteres lanza IllegalArgumentException")
    void descripcionDe151_lanzaExcepcion() {
        String cientoCincuentaYUno = "d".repeat(151);
        assertThrows(IllegalArgumentException.class, () -> new TipoSala(10, "2D", cientoCincuentaYUno, 0));
    }
}
