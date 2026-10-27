package ucsur.pe.cineobjects;

import ucsur.pe.cineInterfaz.CalculoDescuento;

/**
 * Por qué existe: descuenta un porcentaje del total (por ejemplo 10 = 10%).
 */
public class CalculoPorcentaje implements CalculoDescuento {
    // Nombre en la BD (constante: se usa como CalculoPorcentaje.TIPO y su valor no cambia).
    public static final String TIPO = "PORCENTAJE";

    private final double porcentaje; // de 0 a 100

    public CalculoPorcentaje(double porcentaje) {
        if (porcentaje < 0 || porcentaje > 100) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100");
        }
        this.porcentaje = porcentaje;
    }

    @Override
    public double calcular(double total) {
        if (total <= 0) return 0.0;
        return total * (porcentaje / 100.0);
    }

    @Override
    public String getTipo() { return TIPO; }

    @Override
    public double getValor() { return porcentaje; }

    @Override
    public String describir() {
        return String.format("%.0f%%", porcentaje);
    }
}
