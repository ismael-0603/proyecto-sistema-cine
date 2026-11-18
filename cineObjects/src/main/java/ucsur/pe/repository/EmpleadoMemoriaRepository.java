package ucsur.pe.repository;

import ucsur.pe.model.Empleado;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria de EmpleadoRepository (una lista hace de tabla). */
public class EmpleadoMemoriaRepository extends RepositorioMemoria<Empleado> implements EmpleadoRepository {
    public EmpleadoMemoriaRepository() { super(Empleado::getIdEmpleado, Empleado::asignarId); }

    @Override public void guardar(Empleado empleado) { agregar(empleado); }
    @Override public Optional<Empleado> buscarPorId(int idEmpleado) { return porId(idEmpleado); }
    @Override public List<Empleado> listarTodos() { return todos(); }
}
