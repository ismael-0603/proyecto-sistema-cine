package ucsur.pe.model;

/**
 * Por qué existe: separa de Descuento lo que solo necesitan los descuentos con límite de usos (hoy, Cupon).
 * Antes registrarUso y revertirUso estaban en Descuento y Promocion los heredaba vacíos; ahora solo los
 * implementa quien los usa (principio de segregación de interfaces, ISP).
 * Compra guarda aparte los descuentos de este tipo para sumarles un uso al confirmar y devolverlo al reembolsar.
 */
public interface DescuentoConUsos extends Descuento {
    /** Suma un uso cuando una compra confirmada usa este descuento; falla si ya alcanzó su límite. */
    void registrarUso();
    /** Devuelve el uso cuando se reembolsa la compra que lo usó. */
    void revertirUso();
}
