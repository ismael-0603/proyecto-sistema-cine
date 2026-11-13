package ucsur.pe.repository;

import ucsur.pe.model.Empleado;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Empleado: define QUÉ se puede guardar y buscar, no CÓMO.
 * Hoy lo cumple EmpleadoMemoriaRepository; con la base de datos lo cumplirá una versión Jdbc.
 */
public interface EmpleadoRepository {
    void guardar(Empleado empleado);
    Optional<Empleado> buscarPorCorreo(String correo);
    List<Empleado> listarTodos();
}
