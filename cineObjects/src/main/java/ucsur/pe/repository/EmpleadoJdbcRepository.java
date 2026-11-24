package ucsur.pe.repository;

import ucsur.pe.db.PersonaDao;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Rol;
import ucsur.pe.model.Sede;
import java.util.List;
import java.util.Optional;

/**
 * Implementa EmpleadoRepository con MySQL. Carga roles y empleados una vez; la sede y la cuenta personal
 * de cada empleado son los mismos objetos que usa el resto de la aplicación (de SedeRepository y ClienteRepository).
 */
public class EmpleadoJdbcRepository extends RepositorioJdbc<Empleado> implements EmpleadoRepository {
    private final PersonaDao dao;

    public EmpleadoJdbcRepository(PersonaDao dao, SedeRepository sedes, ClienteRepository clientes) {
        super("el empleado", Empleado::getIdEmpleado);
        this.dao = dao;
        cargar(dao.listarEmpleados(porIdDe(dao.listarRoles(), Rol::getIdRol),
                porIdDe(sedes.listarTodas(), Sede::getIdSede),
                porIdDe(clientes.listarTodos(), Cliente::getIdCliente)));
    }

    @Override public void guardar(Empleado empleado) { guardarCon(empleado, dao::insertarEmpleado, dao::actualizarEmpleado); }

    @Override public List<Empleado> listarTodos() { return todos(); }

    @Override public Optional<Empleado> buscarPorId(int idEmpleado) { return porId(idEmpleado); }
}
