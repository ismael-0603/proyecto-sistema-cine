package ucsur.pe.repository;

import ucsur.pe.model.Empleado;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Empleado: define QUÉ se puede guardar y buscar, no CÓMO.
 * Lo cumplen EmpleadoMemoriaRepository (demos en memoria) y EmpleadoJdbcRepository (MySQL).
 * La búsqueda por correo solo filtra listarTodos(): está aquí como método default para no repetirla.
 */
public interface EmpleadoRepository {
    void guardar(Empleado empleado);
    Optional<Empleado> buscarPorId(int idEmpleado);
    List<Empleado> listarTodos();

    /** Busca por correo, sin distinguir mayúsculas ni espacios alrededor. */
    default Optional<Empleado> buscarPorCorreo(String correo) {
        if (correo == null) return Optional.empty();
        String buscado = correo.trim();
        return listarTodos().stream().filter(e -> e.getCorreo().equalsIgnoreCase(buscado)).findFirst();
    }
}
