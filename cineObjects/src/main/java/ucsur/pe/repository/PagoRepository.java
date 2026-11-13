package ucsur.pe.repository;

import ucsur.pe.model.Compra;
import ucsur.pe.model.TransaccionPago;
import java.util.List;

/**
 * Contrato de persistencia de TransaccionPago: define QUÉ se puede guardar y buscar, no CÓMO.
 * Hoy lo cumple PagoMemoriaRepository; con la base de datos lo cumplirá una versión Jdbc.
 */
public interface PagoRepository {
    void guardar(TransaccionPago pago);
    List<TransaccionPago> listarPorCompra(Compra compra);
    int siguienteId();
}
