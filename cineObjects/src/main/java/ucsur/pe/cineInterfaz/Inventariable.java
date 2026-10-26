package ucsur.pe.cineInterfaz;

import ucsur.pe.cineobjects.Sede;

/** Define operaciones para consultar, descontar y reponer las existencias de un producto en una sede. */
public interface Inventariable {
    /** Indica si la sede tiene existencias suficientes para la cantidad solicitada. */
    boolean verificarStock(Sede sede, int cant);
    /** Descuenta del inventario de la sede la cantidad indicada. */
    void descontarStock(Sede sede, int cant);
    /** Suma al inventario de la sede las unidades repuestas. */
    void reponerStock(Sede sede, int cant);
}
