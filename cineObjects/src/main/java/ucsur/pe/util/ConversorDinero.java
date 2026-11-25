package ucsur.pe.util;

/**
 * Por qué existe: el dinero se guarda en CÉNTIMOS como long (S/ 4.70 -> 470), no como double, para que
 * las sumas sean exactas (con double, 0.29 * 100 da 28.999999999999996). Nunca se pasa por double.
 * Solo convierte montos (sin reglas de negocio), por eso sus métodos son static, igual que Math:
 *   aCentavos("4.70") -> 470 · formatear(470) -> "S/ 4.70" · aplicarPorcentaje(1055, 18) -> 190
 *   impuestoIncluido(2490, 18) -> 380 (el IGV que ya trae un precio de S/ 24.90)
 */
public final class ConversorDinero {

    /** Parte entera máxima: los montos de la BD son DECIMAL(10,2), es decir, hasta S/ 99 999 999.99. */
    public static final int MAX_DIGITOS_ENTEROS = 8;

    private ConversorDinero() {
        // Constructor privado: no se crean objetos de esta clase.
    }

    /**
     * Convierte un texto como "10", "10.5", "10.50" o "10,50" a céntimos.
     * Separa la parte entera de la decimal y trabaja cada una como entero.
     */
    public static long aCentavos(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }
        String limpio = texto.trim().replace(',', '.');

        // Solo dígitos, y opcionalmente un punto con 1 o 2 decimales.
        if (!limpio.matches("\\d+(\\.\\d{1,2})?")) {
            throw new IllegalArgumentException("Monto inválido: " + texto);
        }

        String[] partes = limpio.split("\\.");
        // Sin este límite, un texto muy largo desbordaría el long (o Long.parseLong fallaría con otro error).
        if (partes[0].replaceFirst("^0+(?=\\d)", "").length() > MAX_DIGITOS_ENTEROS) {
            throw new IllegalArgumentException("Monto demasiado grande (máximo S/ 99999999.99): " + texto);
        }
        long parteEntera = Long.parseLong(partes[0]);
        long parteDecimal = 0;

        if (partes.length == 2) {
            String decimales = partes[1];
            if (decimales.length() == 1) {
                decimales = decimales + "0";   // "10.5" es 10.50, no 10.05
            }
            parteDecimal = Long.parseLong(decimales);
        }

        return parteEntera * 100 + parteDecimal;
    }

    /** Convierte céntimos a texto con dos decimales: 470 -> "S/ 4.70" (y -470 -> "-S/ 4.70"). */
    public static String formatear(long centavos) {
        String signo = (centavos < 0) ? "-" : "";
        long positivo = Math.abs(centavos);
        long soles = positivo / 100;   // división entera: 470 / 100 = 4
        long resto = positivo % 100;   // módulo:          470 % 100 = 70
        return String.format("%sS/ %d.%02d", signo, soles, resto);
    }

    /**
     * Calcula el porcentaje de un monto en céntimos, redondeando al céntimo más cercano (0.5 sube).
     * Ejemplo: 18% de 1055 céntimos = 189.9 -> 190. Sumar 50 antes de dividir entre 100 hace el redondeo
     * usando solo enteros. El monto no puede ser negativo.
     */
    public static long aplicarPorcentaje(long centavos, long porcentaje) {
        if (centavos < 0 || porcentaje < 0) {
            throw new IllegalArgumentException("El monto y el porcentaje no pueden ser negativos");
        }
        return (centavos * porcentaje + 50) / 100;
    }

    /**
     * Separa el impuesto que YA está dentro de un precio (el precio al público incluye el IGV).
     * Primero saca la base: precio * 100 / (100 + porcentaje), redondeada al céntimo más cercano con enteros
     * (sumar la mitad del divisor antes de dividir). El impuesto es lo que falta para llegar al precio, así
     * base + impuesto da siempre el precio exacto. Ejemplo: S/ 24.90 con 18% -> base 2110, impuesto 380.
     */
    public static long impuestoIncluido(long centavosConImpuesto, long porcentaje) {
        if (centavosConImpuesto < 0 || porcentaje < 0) {
            throw new IllegalArgumentException("El monto y el porcentaje no pueden ser negativos");
        }
        long divisor = 100 + porcentaje;
        long base = (centavosConImpuesto * 100 + divisor / 2) / divisor;
        return centavosConImpuesto - base;
    }
}
