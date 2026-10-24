package ucsur.pe.cineobjects.cineInterfaz;

/** Define operaciones para consultar, descontar y reponer existencias de un producto. */
public interface Inventariable {
    /** Indica si hay existencias suficientes para la cantidad solicitada. */
    boolean verificarStock(int cant);
    /** Descuenta del inventario la cantidad indicada. */
    void descontarStock(int cant);
    /** Suma al inventario las unidades repuestas. */
    void reponerStock(int cant);
}
