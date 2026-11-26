package ucsur.pe.tipo;

import ucsur.pe.util.ConversorDinero;

/**
 * Por qué existe: descuenta un porcentaje del total (por ejemplo 10 = 10%).
 */
public class CalculoPorcentaje implements CalculoDescuento {
    // Nombre en la BD (constante: se usa como CalculoPorcentaje.TIPO y su valor no cambia).
    public static final String TIPO = "PORCENTAJE";

    public static final long MAX_PORCENTAJE = 90; // tope máximo de descuento: no existen compras de S/ 0.00

    private final long porcentaje; // entero de 1 a 90

    public CalculoPorcentaje(long porcentaje) {
        if (porcentaje < 1 || porcentaje > MAX_PORCENTAJE) { // un 0 % no descuenta nada y > 90 % se prohíbe
            throw new IllegalArgumentException("El porcentaje debe estar entre 1 y " + MAX_PORCENTAJE);
        }
        this.porcentaje = porcentaje;
    }

    @Override
    public long calcularCentavos(long totalCentavos) {
        if (totalCentavos <= 0) return 0;
        // Redondea al céntimo más cercano; como porcentaje <= 100, nunca supera el total.
        return ConversorDinero.aplicarPorcentaje(totalCentavos, porcentaje);
    }

    @Override
    public String getTipo() { return TIPO; }

    @Override
    public long getValor() { return porcentaje; }

    @Override
    public String describir() {
        return porcentaje + "%";
    }
}
