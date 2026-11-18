package ucsur.pe.repository;

import ucsur.pe.model.Compra;
import ucsur.pe.model.TransaccionPago;
import java.util.List;

/**
 * Contrato de persistencia de TransaccionPago: define QUÉ se puede guardar y buscar, no CÓMO.
 * Lo cumplen PagoMemoriaRepository (demos en memoria) y PagoJdbcRepository (MySQL).
 */
public interface PagoRepository {
    void guardar(TransaccionPago pago);
    /**
     * Guarda un pago APROBADO junto con su compra ya confirmada: quedan los dos o ninguno.
     * Si falla, la compra y el pago siguen sin id nuevo y se puede deshacer el cobro.
     */
    void guardarConCompra(TransaccionPago pago);
    List<TransaccionPago> listarPorCompra(Compra compra);
}
