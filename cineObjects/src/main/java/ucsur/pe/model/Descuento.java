package ucsur.pe.model;

/**
 * Por qué existe: para tratar igual a cupones y promociones: todos dicen si están vigentes,
 * cuánto descuentan y si aplican a una compra.
 * Compra trabaja solo con esta interfaz; no sabe si tiene un cupón, una promoción u otro descuento.
 * Ya no depende de Compra: recibe solo los datos que necesita (ContextoDescuento).
 * Activar y desactivar están en la interfaz Activable, porque los usa el administrador, no la compra.
 */
public interface Descuento {
    /** Comprueba si el descuento puede usarse en la fecha indicada. */
    boolean validarVigencia(DateTime fecha);
    /** Calcula cuántos céntimos se descuentan del importe recibido (también en céntimos). */
    long calcularDescuentoCentavos(long totalCentavos);
    /** Indica si se puede usar con esos datos de la compra (fecha, socio...: cada descuento pone sus reglas). */
    boolean esAplicable(ContextoDescuento contexto);
    /**
     * Orden al combinar descuentos: el menor se aplica primero sobre el subtotal
     * y el siguiente sobre lo que queda.
     */
    int getOrdenAplicacion();
    /**
     * Categoría del descuento ("CUPON" o "PROMOCION"). Una compra admite a lo más uno de cada categoría
     * (en la BD, columnas id_cupon e id_promocion); así la compra pregunta al descuento en vez de mirar su clase.
     */
    String getCategoria();
    // registrarUso y revertirUso ya no están aquí: solo los necesitan los descuentos con límite de usos,
    // y esos implementan DescuentoConUsos (ISP: nadie depende de métodos que no usa).
}
