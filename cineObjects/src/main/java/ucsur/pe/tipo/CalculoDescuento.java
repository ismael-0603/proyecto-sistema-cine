package ucsur.pe.tipo;

/**
 * Por qué existe: cada forma de descontar (porcentaje, monto fijo...) calcula a su manera.
 * Así un cupón o una promoción solo dicen "descuéntame" y no necesitan un if por cada forma;
 * una forma nueva se agrega con una clase nueva, sin tocar las existentes.
 */
public interface CalculoDescuento {
    /** Cuántos céntimos se descuentan del importe recibido (en céntimos); nunca más que ese importe. */
    long calcularCentavos(long totalCentavos);
    /** Nombre que se guarda en la columna tipo_descuento de la BD, por ejemplo "PORCENTAJE". */
    String getTipo();
    /** Valor configurado: el porcentaje (10 = 10%) o el monto en céntimos (500 = S/ 5.00), según la forma. */
    long getValor();
    /** Texto para mostrar, como "10%" o "S/ 5.00". */
    String describir();
}
