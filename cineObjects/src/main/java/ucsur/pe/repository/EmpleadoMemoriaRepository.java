package ucsur.pe.repository;

import ucsur.pe.model.Empleado;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria de EmpleadoRepository (una lista hace de tabla). */
public class EmpleadoMemoriaRepository extends RepositorioMemoria<Empleado> implements EmpleadoRepository {
    @Override public void guardar(Empleado empleado) { agregar(empleado); }
    @Override public Optional<Empleado> buscarPorCorreo(String correo) {
        return buscarPrimero(e -> e.getCorreo().equalsIgnoreCase(correo));
    }
    @Override public List<Empleado> listarTodos() { return todos(); }
}
