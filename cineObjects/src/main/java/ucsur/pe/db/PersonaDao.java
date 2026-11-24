package ucsur.pe.db;

import ucsur.pe.model.Cliente;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Rol;
import ucsur.pe.model.Sede;
import java.util.List;
import java.util.Map;

/** Contrato DAO de las cuentas: cliente, empleado y rol. */
public interface PersonaDao {

    /** Todos los clientes (socios e invitados) con sus puntos, nivel y estado; su historial lo llenan las compras. */
    List<Cliente> listarClientes();
    /** Inserta el cliente (socio o invitado, con id 0) y le asigna el id que genera la base (AUTO_INCREMENT). */
    void insertarCliente(Cliente cliente);
    /** Guarda los datos que cambian: contacto, clave, estado, puntos, nivel y si dejó de ser invitado. */
    void actualizarCliente(Cliente cliente);

    List<Rol> listarRoles();
    /** Todos los empleados; su rol, su sede y su cuenta personal se toman de los mapas (id -> objeto). */
    List<Empleado> listarEmpleados(Map<Integer, Rol> roles, Map<Integer, Sede> sedes, Map<Integer, Cliente> clientes);
    void insertarEmpleado(Empleado empleado);
    void actualizarEmpleado(Empleado empleado);
}
