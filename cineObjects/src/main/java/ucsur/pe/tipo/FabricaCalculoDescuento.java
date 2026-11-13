package ucsur.pe.tipo;

import ucsur.pe.model.DescuentoBase;
import java.util.Map;
import java.util.function.DoubleFunction;

/**
 * Por qué existe: convierte lo guardado en la BD (columnas tipo_descuento y valor) en su forma de cálculo.
 * Antes estaba dentro de DescuentoBase, pero traducir columnas de la BD no es trabajo del modelo (SRP);
 * cuando exista la capa de base de datos, esta conversión pasará al DAO de cupones y promociones.
 * Usa un mapa en lugar de un switch: un tipo nuevo se registra con una línea (OCP).
 */
public final class FabricaCalculoDescuento {
    private static final Map<String, DoubleFunction<CalculoDescuento>> TIPOS = Map.of(
            CalculoPorcentaje.TIPO, CalculoPorcentaje::new,
            CalculoMontoFijo.TIPO, CalculoMontoFijo::new);

    private FabricaCalculoDescuento() { } // solo tiene un método estático

    /** Crea la forma de cálculo según el tipo guardado en la BD; lanza error si el tipo no existe. */
    public static CalculoDescuento crear(String tipoDescuento, double valor) {
        DoubleFunction<CalculoDescuento> constructor = TIPOS.get(tipoDescuento);
        if (constructor == null) {
            throw new IllegalArgumentException("Tipo de descuento inválido: " + tipoDescuento);
        }
        return constructor.apply(valor);
    }
}
