package ucsur.pe.util;

/**
 * Por qué existe: el dinero del sistema se guarda en CÉNTIMOS como long (S/ 4.70 -> 470), no como double.
 * Con double, 0.1 + 0.2 da 0.30000000000000004 y una suma de precios puede no coincidir con el total esperado;
 * entre enteros las sumas y multiplicaciones son exactas.
 *
 * Esta clase solo convierte montos (no tiene reglas de negocio), por eso sus métodos son static, igual que Math:
 *   aCentavos("4.70")          -> 470        al recibir un monto como texto (teclado, BD)
 *   formatear(470)             -> "S/ 4.70"  al mostrar un monto
 *   aplicarPorcentaje(1055, 18) -> 190       cuando un porcentaje deja fracciones de céntimo
 *
 * IMPORTANTE: nunca se pasa por double. (long) (Double.parseDouble("0.29") * 100) da 28 y no 29,
 * porque 0.29 * 100 = 28.999999999999996.
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
}
