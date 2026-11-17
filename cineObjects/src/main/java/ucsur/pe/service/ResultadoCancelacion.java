package ucsur.pe.service;

import ucsur.pe.util.ConversorDinero;
import java.util.List;

/**
 * Por qué existe: resumen que ve el administrador al cancelar una función (RF-24): cuántas compras se
 * reembolsaron solas, cuánto dinero se devolvió, cuántas compras en curso se cancelaron y cuáles
 * no se pudieron reembolsar (por ejemplo, porque la pasarela no respondió) para reintentarlas.
 */
public record ResultadoCancelacion(int comprasReembolsadas, long montoDevueltoCentavos,
                                   int comprasEnProcesoCanceladas, List<String> pendientes) {

    public ResultadoCancelacion {
        pendientes = List.copyOf(pendientes);
    }

    @Override
    public String toString() {
        return comprasReembolsadas + " compras reembolsadas (" + ConversorDinero.formatear(montoDevueltoCentavos)
                + "), " + comprasEnProcesoCanceladas + " compras en curso canceladas"
                + (pendientes.isEmpty() ? "" : ", pendientes: " + String.join("; ", pendientes));
    }
}
