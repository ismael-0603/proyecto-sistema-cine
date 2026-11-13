package ucsur.pe.repository;

import ucsur.pe.model.Pedido;
import ucsur.pe.model.Sede;
import java.util.List;

/**
 * Contrato de persistencia de Pedido: define QUÉ se puede guardar y buscar, no CÓMO.
 * Hoy lo cumple PedidoMemoriaRepository; con la base de datos lo cumplirá una versión Jdbc.
 */
public interface PedidoRepository {
    /** Guarda el pedido; si aún no tiene id, le asigna uno (como el AUTO_INCREMENT). */
    void guardar(Pedido pedido);
    List<Pedido> listarPorSede(Sede sede);
}
