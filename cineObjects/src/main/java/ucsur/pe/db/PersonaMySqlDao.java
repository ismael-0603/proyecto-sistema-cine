package ucsur.pe.db;

import ucsur.pe.exception.RepositoryException;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.Rol;
import ucsur.pe.model.Sede;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Por qué existe: traduce entre las tablas cliente, empleado y rol y los objetos del modelo.
 * No aplica reglas (que el correo no lo use otra cuenta lo revisa AutenticacionService), pero si la BD
 * rechaza un DNI o correo repetido, lo explica en español en vez de mostrar el error de MySQL.
 */
public class PersonaMySqlDao implements PersonaDao {

    private final Connection conexion;
    private final Reloj reloj; // las cuentas lo necesitan para su fecha de registro y sus visitas del año

    public PersonaMySqlDao(Connection conexion, Reloj reloj) {
        this.conexion = conexion;
        this.reloj = reloj;
    }

    // ---------- Clientes ----------

    @Override
    public List<Cliente> listarClientes() {
        String sql = """
                SELECT id_cliente, dni, nombres, apellidos, fecha_nac, correo, telefono, contra, estado,
                       fecha_registro, puntos, nivel_socio, es_invitado
                FROM cliente
                ORDER BY id_cliente
                """;
        List<Cliente> clientes = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clientes.add(Cliente.restaurar(rs.getInt("id_cliente"), rs.getString("dni"), rs.getString("nombres"),
                        rs.getString("apellidos"), Fechas.desdeDate(rs.getDate("fecha_nac")), rs.getString("correo"),
                        rs.getString("telefono"), rs.getString("contra"), rs.getBoolean("estado"),
                        Fechas.desdeTimestamp(rs.getTimestamp("fecha_registro")), rs.getInt("puntos"),
                        rs.getInt("nivel_socio"), rs.getBoolean("es_invitado"), reloj));
            }
        } catch (SQLException e) {
            throw new RepositoryException("No se pudieron cargar los clientes.", e);
        }
        return clientes;
    }

    @Override
    public void insertarCliente(Cliente cliente) {
        String sql = """
                INSERT INTO cliente (dni, nombres, apellidos, fecha_nac, correo, telefono, contra, estado,
                                     fecha_registro, puntos, nivel_socio, es_invitado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            llenarCliente(ps, cliente);
            ps.executeUpdate();
            cliente.asignarId(Claves.generada(ps));
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new RepositoryException("Ya existe una cuenta con ese DNI o correo.", e);
        } catch (SQLException e) {
            throw new RepositoryException("No se pudo registrar el cliente " + cliente.getCorreo() + ".", e);
        }
    }

    @Override
    public void actualizarCliente(Cliente cliente) {
        String sql = """
                UPDATE cliente
                SET dni = ?, nombres = ?, apellidos = ?, fecha_nac = ?, correo = ?, telefono = ?, contra = ?,
                    estado = ?, fecha_registro = ?, puntos = ?, nivel_socio = ?, es_invitado = ?
                WHERE id_cliente = ?
                """;
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            llenarCliente(ps, cliente);
            ps.setInt(13, cliente.getIdCliente());
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new RepositoryException("Ya existe una cuenta con ese DNI o correo.", e);
        } catch (SQLException e) {
            throw new RepositoryException("No se pudo actualizar el cliente " + cliente.getCorreo() + ".", e);
        }
    }

    /** Los 12 datos del cliente en el orden del INSERT y del UPDATE (el id va después). */
    private void llenarCliente(PreparedStatement ps, Cliente c) throws SQLException {
        ps.setString(1, c.getDni());
        ps.setString(2, c.getNombres());
        ps.setString(3, c.getApellidos());
        ps.setDate(4, Fechas.aDate(c.getFechaNac()));
        ps.setString(5, c.getCorreo());
        ps.setString(6, c.getTelefono());
        ps.setString(7, c.getContra());
        ps.setBoolean(8, c.isEstado());
        ps.setTimestamp(9, Fechas.aTimestamp(c.getFechaRegistro()));
        ps.setInt(10, c.consultarPuntos());
        ps.setInt(11, c.getNivelSocio());
        ps.setBoolean(12, c.esInvitado());
    }

    // ---------- Roles y empleados ----------

    @Override
    public List<Rol> listarRoles() {
        String sql = "SELECT id_rol, nombre, descripcion, nivel_acceso FROM rol ORDER BY id_rol";
        List<Rol> roles = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                roles.add(new Rol(rs.getInt("id_rol"), rs.getString("nombre"), rs.getString("descripcion"),
                        rs.getInt("nivel_acceso")));
            }
        } catch (SQLException e) {
            throw new RepositoryException("No se pudieron cargar los roles.", e);
        }
        return roles;
    }

    @Override
    public List<Empleado> listarEmpleados(Map<Integer, Rol> roles, Map<Integer, Sede> sedes,
                                          Map<Integer, Cliente> clientes) {
        String sql = """
                SELECT id_empleado, dni, nombres, apellidos, fecha_nac, correo, telefono, contra, estado,
                       turno, id_rol, id_sede, pin, id_cliente
                FROM empleado
                ORDER BY id_empleado
                """;
        List<Empleado> empleados = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id_empleado");
                Rol rol = roles.get(rs.getInt("id_rol"));
                Sede sede = sedes.get(rs.getInt("id_sede"));
                if (rol == null || sede == null) {
                    throw new RepositoryException("El empleado #" + id + " apunta a un rol o sede que no existe.");
                }
                int idCliente = rs.getInt("id_cliente"); // 0 si es NULL (sin cuenta personal)
                Cliente cuentaPersonal = idCliente == 0 ? null : clientes.get(idCliente);
                if (idCliente != 0 && cuentaPersonal == null) {
                    throw new RepositoryException("El empleado #" + id + " apunta a un cliente que no existe.");
                }
                Empleado e = Empleado.restaurar(id, rs.getString("dni"), rs.getString("nombres"), rs.getString("apellidos"),
                        Fechas.desdeDate(rs.getDate("fecha_nac")), rs.getString("correo"), rs.getString("telefono"),
                        rs.getString("contra"), rs.getBoolean("estado"), rs.getString("turno"), rol, sede,
                        rs.getString("pin"), cuentaPersonal, reloj);
                empleados.add(e);
            }
        } catch (SQLException e) {
            throw new RepositoryException("No se pudieron cargar los empleados.", e);
        }
        return empleados;
    }

    @Override
    public void insertarEmpleado(Empleado empleado) {
        String sql = """
                INSERT INTO empleado (dni, nombres, apellidos, fecha_nac, correo, telefono, contra, estado,
                                      turno, id_rol, id_sede, pin, id_cliente)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            llenarEmpleado(ps, empleado);
            ps.executeUpdate();
            empleado.asignarId(Claves.generada(ps));
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new RepositoryException("Ya existe un empleado con ese DNI o correo.", ex);
        } catch (SQLException ex) {
            throw new RepositoryException("No se pudo registrar al empleado " + empleado.getCorreo() + ".", ex);
        }
    }

    @Override
    public void actualizarEmpleado(Empleado empleado) {
        String sql = """
                UPDATE empleado
                SET dni = ?, nombres = ?, apellidos = ?, fecha_nac = ?, correo = ?, telefono = ?, contra = ?,
                    estado = ?, turno = ?, id_rol = ?, id_sede = ?, pin = ?, id_cliente = ?
                WHERE id_empleado = ?
                """;
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            llenarEmpleado(ps, empleado);
            ps.setInt(14, empleado.getIdEmpleado());
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new RepositoryException("Ya existe un empleado con ese DNI o correo.", ex);
        } catch (SQLException ex) {
            throw new RepositoryException("No se pudo actualizar al empleado " + empleado.getCorreo() + ".", ex);
        }
    }

    /** Los 13 datos del empleado en el orden del INSERT y del UPDATE (el id va después). */
    private void llenarEmpleado(PreparedStatement ps, Empleado e) throws SQLException {
        ps.setString(1, e.getDni());
        ps.setString(2, e.getNombres());
        ps.setString(3, e.getApellidos());
        ps.setDate(4, Fechas.aDate(e.getFechaNac()));
        ps.setString(5, e.getCorreo());
        ps.setString(6, e.getTelefono());
        ps.setString(7, e.getContra());
        ps.setBoolean(8, e.isEstado());
        ps.setString(9, e.getTurno());
        ps.setInt(10, e.getRol().getIdRol());
        ps.setInt(11, e.getSede().getIdSede());
        ps.setString(12, e.getPin());
        if (e.tieneCuentaPersonal()) {
            ps.setInt(13, e.getCuentaPersonal().getIdCliente());
        } else {
            ps.setNull(13, Types.INTEGER);
        }
    }
}
