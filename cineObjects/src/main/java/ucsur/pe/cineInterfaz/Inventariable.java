package ucsur.pe.cineInterfaz;

import ucsur.pe.cineobjects.Sede;

/**
 * Por qué existe: para manejar el stock de un producto en cada sede (consultar, descontar, reponer)
 * sin depender de cómo lo guarda el producto.
 */
public interface Inventariable {
    /** Indica si la sede tiene existencias suficientes para la cantidad solicitada. */
    boolean verificarStock(Sede sede, int cant);
    /** Descuenta del inventario de la sede la cantidad indicada. */
    void descontarStock(Sede sede, int cant);
    /** Suma al inventario de la sede las unidades repuestas. */
    void reponerStock(Sede sede, int cant);
}
