package ucsur.pe.repository;

import ucsur.pe.model.Cupon;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Cupon: define QUÉ se puede guardar y buscar, no CÓMO.
 * Lo cumple CuponJdbcRepository (MySQL).
 * La búsqueda por código solo filtra listarTodos(): está aquí como método default para no repetirla.
 */
public interface CuponRepository {
    void guardar(Cupon cupon);
    Optional<Cupon> buscarPorId(int idCupon);
    List<Cupon> listarTodos();

    /** Busca por el código que escribe el cliente; no distingue mayúsculas ni espacios alrededor. */
    default Optional<Cupon> buscarPorCodigo(String codigo) {
        if (codigo == null) return Optional.empty();
        String buscado = codigo.trim();
        return listarTodos().stream().filter(c -> c.getCodigo().equalsIgnoreCase(buscado)).findFirst();
    }
}
