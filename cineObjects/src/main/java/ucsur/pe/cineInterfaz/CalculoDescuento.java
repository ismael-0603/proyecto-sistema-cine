package ucsur.pe.cineInterfaz;

/**
 * Por qué existe: cada forma de descontar (porcentaje, monto fijo...) calcula a su manera.
 * Así un cupón o una promoción solo dicen "descuéntame" y no necesitan un if por cada forma;
 * una forma nueva se agrega con una clase nueva, sin tocar las existentes.
 */
public interface CalculoDescuento {
    /** Cuánto se descuenta del importe recibido; nunca más que ese importe. */
    double calcular(double total);
    /** Nombre que se guarda en la columna tipo_descuento de la BD, por ejemplo "PORCENTAJE". */
    String getTipo();
    /** Valor configurado: el porcentaje o el monto en soles, según la forma. */
    double getValor();
    /** Texto para mostrar, como "10%" o "S/ 5.00". */
    String describir();
}
