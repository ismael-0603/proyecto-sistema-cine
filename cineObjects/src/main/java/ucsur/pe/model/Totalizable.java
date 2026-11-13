package ucsur.pe.model;

/**
 * Por qué existe: para pedirle el total a un pedido o a una compra de la misma forma.
 */
public interface Totalizable {
    /** Calcula y devuelve el importe total actual. */
    double calcularTotal();
}
