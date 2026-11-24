package ucsur.pe.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Qué productos de la dulcería abren la ventana de personalización (RF-13) y qué texto se guarda. */
@DisplayName("Personalización de canchitas, gaseosas y combos")
class OpcionesComboTest {

    private static Producto suelto(String nombre, String descripcion, String categoria) {
        return new Producto(1, nombre, descripcion, categoria, 990, false, true);
    }

    private static Producto combo(String descripcion) {
        return new Producto(1, "Combo", descripcion, "Combos", 2190, true, true);
    }

    @Test
    @DisplayName("La canchita por tamaño pide sabor y no marca")
    void canchitaPorTamano() {
        Producto p = suelto("Canchita Pequeña", "Canchita pequeña: elige salada, dulce o mixta", "Canchita");
        assertTrue(OpcionesCombo.pideSabor(p));
        assertFalse(OpcionesCombo.pideGaseosa(p));
        assertEquals("Canchita: Dulce", OpcionesCombo.describir(p, "Dulce", null));
    }

    @Test
    @DisplayName("Las canchitas Gourmet ya traen su sabor")
    void gourmetNoPide() {
        Producto p = suelto("Canchita Gourmet Queso", "Canchita con polvo de queso", "Canchita");
        assertFalse(OpcionesCombo.esPersonalizable(p));
        assertNull(OpcionesCombo.describir(p, null, null));
    }

    @Test
    @DisplayName("La gaseosa por tamaño pide la marca, incluidas las Zero")
    void gaseosaPorTamano() {
        Producto p = suelto("Gaseosa Mediana", "Gaseosa de 16 oz: elige la marca", "Bebidas");
        assertTrue(OpcionesCombo.pideGaseosa(p));
        assertFalse(OpcionesCombo.pideSabor(p));
        assertEquals("Gaseosa: Coca-Cola Zero", OpcionesCombo.describir(p, null, "Coca-Cola Zero"));
    }

    @Test
    @DisplayName("Otras bebidas no se personalizan")
    void otrasBebidas() {
        assertFalse(OpcionesCombo.esPersonalizable(suelto("Chicha morada", "Vaso de 500 ml", "Bebidas")));
    }

    @Test
    @DisplayName("Caramelo y mantequilla ya no son sabores válidos")
    void saboresEliminados() {
        Producto p = suelto("Canchita Grande", "Canchita grande: elige salada, dulce o mixta", "Canchita");
        assertThrows(IllegalArgumentException.class, () -> OpcionesCombo.describir(p, "Caramelo", null));
        assertThrows(IllegalArgumentException.class, () -> OpcionesCombo.describir(p, "Mantequilla", null));
    }

    @Test
    @DisplayName("Los combos siguen igual: con sabor fijo solo piden la gaseosa")
    void combos() {
        Producto individual = combo("Canchita mediana y gaseosa mediana");
        assertEquals("Canchita: Mixta · Gaseosa: Sprite", OpcionesCombo.describir(individual, "Mixta", "Sprite"));
        Producto dulce = combo("Canchita dulce, gomitas y gaseosa");
        assertFalse(OpcionesCombo.pideSabor(dulce));
        assertTrue(OpcionesCombo.pideGaseosa(dulce));
    }
}
