package ucsur.pe.model;

/**
 * Por qué existe: es el personal de una sede. Según su rol puede preparar y entregar pedidos de dulcería;
 * esas acciones las coordina PedidoService con la cola de su sede, y el pedido recuerda quién lo preparó.
 */
public class Empleado extends Persona {
    private int idEmpleado;
    private String turno; // texto libre, máx. 20 caracteres
    private Rol rol;
    private Sede sede;

    public Empleado(int idEmpleado, String dni, String nombres, String apellidos, DateTime fechaNac,
                    String correoElectronico, String telefono, String contra,
                    String turno, Rol rol, Sede sede, Reloj reloj) {
        super(dni, nombres, apellidos, fechaNac, correoElectronico, telefono, contra, reloj);
        exigirDatosCompletos(); // en la BD dni, nombres, apellidos, correo y contra del empleado son NOT NULL
        if (rol == null) throw new IllegalArgumentException("El rol es obligatorio");
        if (sede == null) throw new IllegalArgumentException("La sede es obligatoria");
        if (turno != null && turno.length() > 20) {
            throw new IllegalArgumentException("El turno no puede superar 20 caracteres");
        }
        this.idEmpleado = idEmpleado;
        this.turno = turno;
        this.rol = rol;
        this.sede = sede;
    }

    /**
     * Solo para la capa de base de datos (ucsur.pe.db): reconstruye un empleado guardado con su id
     * y si su cuenta está activa, igual que Cliente.restaurar.
     */
    public static Empleado restaurar(int idEmpleado, String dni, String nombres, String apellidos, DateTime fechaNac,
                                     String correo, String telefono, String contra, boolean estado,
                                     String turno, Rol rol, Sede sede, Reloj reloj) {
        Empleado empleado = new Empleado(idEmpleado, dni, nombres, apellidos, fechaNac, correo, telefono, contra,
                turno, rol, sede, reloj); // valida los datos, el rol y la sede
        if (!estado) empleado.desactivar();
        return empleado;
    }

    /** Asigna el id que generó la base (AUTO_INCREMENT) tras el INSERT; un empleado nuevo nace con id 0. */
    public void asignarId(int idEmpleado) {
        this.idEmpleado = Ids.asignar(this.idEmpleado, idEmpleado, "el empleado");
    }

    /** Indica si su rol le permite administrar el cine (no mira si la cuenta está activa). */
    public boolean esAdministrador() {
        return rol.tienePermiso(Rol.ADMINISTRAR);
    }

    /** Cuenta activa y rol de administrador: lo que exigen los servicios antes de una operación de administración. */
    public boolean esAdministradorActivo() {
        return isEstado() && esAdministrador();
    }

    public int getIdEmpleado() { return idEmpleado; }
    public String getTurno() { return turno; }
    public Rol getRol() { return rol; }
    public Sede getSede() { return sede; }

    @Override
    public String toString() {
        return getNombreCompleto() + " - " + rol.getNombre();
    }
}
