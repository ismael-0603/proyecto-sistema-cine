package ucsur.pe.cineobjects;

import ucsur.pe.cineInterfaz.CalculoDescuento;

/**
 * Por qué existe: descuenta una cantidad fija de soles (por ejemplo 5 = S/ 5.00),
 * sin pasar del total para que el importe nunca quede negativo.
 */
public class CalculoMontoFijo implements CalculoDescuento {
    // Nombre en la BD (constante: se usa como CalculoMontoFijo.TIPO y su valor no cambia).
    public static final String TIPO = "MONTO_FIJO";

    private final double monto; // en soles

    public CalculoMontoFijo(double monto) {
        if (monto < 0) {
            throw new IllegalArgumentException("El monto del descuento no puede ser negativo");
        }
        this.monto = monto;
    }

    @Override
    public double calcular(double total) {
        if (total <= 0) return 0.0;
        return Math.min(total, monto);
    }

    @Override
    public String getTipo() { return TIPO; }

    @Override
    public double getValor() { return monto; }

    @Override
    public String describir() {
        return "S/ " + String.format("%.2f", monto);
    }
}
