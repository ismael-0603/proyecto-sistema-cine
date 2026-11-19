package ucsur.pe.service;

import ucsur.pe.model.Compra;
import ucsur.pe.util.ConversorDinero;
import java.util.List;

/**
 * Por qué existe: resumen que ve el administrador al cancelar una función (RF-24): cuántas compras se
 * reembolsaron solas, cuánto dinero se devolvió, cuántas compras en curso se cancelaron y cuáles
 * no se pudieron reembolsar (por ejemplo, porque la pasarela no respondió) para reintentarlas.
 * pendientes trae el motivo de cada una para mostrarlo; comprasPendientes, las compras mismas, para el botón
 * «Reintentar» (AdminController.reintentarReembolso). Las dos listas van en el mismo orden.
 */
public record ResultadoCancelacion(int comprasReembolsadas, long montoDevueltoCentavos,
                                   int comprasEnProcesoCanceladas, List<String> pendientes,
                                   List<Compra> comprasPendientes) {

    public ResultadoCancelacion {
        pendientes = List.copyOf(pendientes);
        comprasPendientes = List.copyOf(comprasPendientes);
    }

    @Override
    public String toString() {
        return comprasReembolsadas + " compras reembolsadas (" + ConversorDinero.formatear(montoDevueltoCentavos)
                + "), " + comprasEnProcesoCanceladas + " compras en curso canceladas"
                + (pendientes.isEmpty() ? "" : ", pendientes: " + String.join("; ", pendientes));
    }
}
