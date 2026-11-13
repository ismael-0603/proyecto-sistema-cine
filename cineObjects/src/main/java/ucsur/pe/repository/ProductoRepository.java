package ucsur.pe.repository;

import ucsur.pe.model.Producto;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Producto: define QUÉ se puede guardar y buscar, no CÓMO.
 * Hoy lo cumple ProductoMemoriaRepository; con la base de datos lo cumplirá una versión Jdbc.
 */
public interface ProductoRepository {
    void guardar(Producto producto);
    Optional<Producto> buscarPorId(int idProducto);
    List<Producto> listarTodos();
}
