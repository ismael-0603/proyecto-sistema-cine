package ucsur.pe.repository;

import ucsur.pe.model.Compra;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Compra: define QUÉ se puede guardar y buscar, no CÓMO.
 * Hoy lo cumple CompraMemoriaRepository; con la base de datos lo cumplirá una versión Jdbc.
 */
public interface CompraRepository {
    /** Guarda la compra; al guardar una compra CONFIRMADA sin serie le asigna la siguiente (como trg_compra_serie). */
    void guardar(Compra compra);
    Optional<Compra> buscarPorId(int idCompra);
    List<Compra> listarTodas();
    int siguienteId();
}
