package ucsur.pe.tipo;

import ucsur.pe.util.ConversorDinero;

/**
 * Por qué existe: descuenta una cantidad fija en céntimos (por ejemplo 500 = S/ 5.00),
 * sin pasar del total para que el importe nunca quede negativo.
 */
public class CalculoMontoFijo implements CalculoDescuento {
    // Nombre en la BD (constante: se usa como CalculoMontoFijo.TIPO y su valor no cambia).
    public static final String TIPO = "MONTO_FIJO";

    private final long montoCentavos; // en céntimos

    public CalculoMontoFijo(long montoCentavos) {
        if (montoCentavos <= 0) { // S/ 0.00 crearía una promoción que no descuenta nada
            throw new IllegalArgumentException("El monto del descuento debe ser mayor a 0");
        }
        this.montoCentavos = montoCentavos;
    }

    @Override
    public long calcularCentavos(long totalCentavos) {
        if (totalCentavos <= 0) return 0;
        return Math.min(totalCentavos, montoCentavos);
    }

    @Override
    public String getTipo() { return TIPO; }

    @Override
    public long getValor() { return montoCentavos; }

    @Override
    public String describir() {
        return ConversorDinero.formatear(montoCentavos);
    }
}
