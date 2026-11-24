package ucsur.pe.tipo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongFunction;

/**
 * Por qué existe: convierte lo guardado en la BD (columnas tipo_descuento y valor) en su forma de cálculo.
 * Está fuera del modelo porque traducir columnas de la BD no es trabajo suyo (SRP); la usan el DAO de cupones y promociones (al leer) y el servicio de administración (al registrar o editar).
 * Usa un mapa en lugar de un switch: un tipo nuevo se registra con una línea (OCP).
 */
public final class FabricaCalculoDescuento {
    private static final Map<String, LongFunction<CalculoDescuento>> TIPOS = new LinkedHashMap<>(); // en orden de registro

    static {
        TIPOS.put(CalculoPorcentaje.TIPO, CalculoPorcentaje::new);
        TIPOS.put(CalculoMontoFijo.TIPO, CalculoMontoFijo::new);
    }

    private FabricaCalculoDescuento() { } // solo tiene un método estático

    /**
     * Crea la forma de cálculo según el tipo guardado en la BD; lanza error si el tipo no existe.
     * El valor es el porcentaje (10 = 10%) o el monto en céntimos (500 = S/ 5.00); al leer un monto de la BD
     * se toma como texto (rs.getString) y se convierte con ConversorDinero.aCentavos, nunca con getDouble.
     */
    public static CalculoDescuento crear(String tipoDescuento, long valor) {
        LongFunction<CalculoDescuento> constructor = TIPOS.get(tipoDescuento);
        if (constructor == null) {
            throw new IllegalArgumentException("Tipo de descuento inválido: " + tipoDescuento);
        }
        return constructor.apply(valor);
    }

    /** Los tipos que se pueden elegir (para el ComboBox del formulario, como nombresDeTipos de MVCone). */
    public static List<String> tiposDisponibles() {
        return List.copyOf(TIPOS.keySet());
    }
}
