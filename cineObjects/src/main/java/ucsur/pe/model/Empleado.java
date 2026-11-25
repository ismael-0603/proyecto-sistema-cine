package ucsur.pe.model;

/**
 * Por qué existe: es el personal de una sede. Según su rol puede preparar y entregar pedidos de dulcería;
 * esas acciones las coordina PedidoService con la cola de su sede, y el pedido recuerda quién lo preparó.
 * Puede tener una cuenta personal (Cliente con su mismo DNI) para comprar entradas; el PIN protege
 * el regreso a su panel desde esa cuenta.
 */
public class Empleado extends Persona {
    private int idEmpleado;
    private String turno; // texto libre, máx. 20 caracteres
    private Rol rol;
    private Sede sede;
    private String pin;              // 4 dígitos
    private Cliente cuentaPersonal;  // null hasta que la vincula

    public Empleado(int idEmpleado, String dni, String nombres, String apellidos, DateTime fechaNac,
                    String correoElectronico, String telefono, String contra,
                    String turno, Rol rol, Sede sede, String pin, Reloj reloj) {
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
        this.pin = validarPin(pin);
    }

    /**
     * Solo para la capa de base de datos (ucsur.pe.db): reconstruye un empleado guardado con su id
     * con su estado y su cuenta personal (null si no tiene), igual que Cliente.restaurar.
     */
    public static Empleado restaurar(int idEmpleado, String dni, String nombres, String apellidos, DateTime fechaNac,
                                     String correo, String telefono, String contra, boolean estado,
                                     String turno, Rol rol, Sede sede, String pin, Cliente cuentaPersonal,
                                     Reloj reloj) {
        Empleado empleado = new Empleado(idEmpleado, dni, nombres, apellidos, fechaNac, correo, telefono, contra,
                turno, rol, sede, pin, reloj); // valida los datos, el rol, la sede y el PIN
        if (!estado) empleado.desactivar();
        if (cuentaPersonal != null) empleado.vincularCuentaPersonal(cuentaPersonal);
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

    // ---------- PIN ----------

    public boolean verificarPin(String pin) {
        return pin != null && this.pin.equals(pin.trim());
    }

    /** Cambia el PIN si el actual es correcto; el nuevo debe tener 4 dígitos. */
    public void cambiarPin(String pinActual, String pinNuevo) {
        if (!verificarPin(pinActual)) throw new IllegalArgumentException("El PIN actual no es correcto");
        this.pin = validarPin(pinNuevo);
    }

    /** Regla del PIN para las pantallas: el motivo del error o null si son exactamente 4 dígitos. */
    public static String errorPin(String pin) {
        if (pin == null || !pin.trim().matches("\\d{4}")) return "El PIN debe tener exactamente 4 dígitos";
        return null;
    }

    private static String validarPin(String pin) {
        String error = errorPin(pin);
        if (error != null) throw new IllegalArgumentException(error);
        return pin.trim();
    }

    /** Solo para la capa de base de datos (ucsur.pe.db), que necesita escribir la columna pin. */
    public String getPin() { return pin; }

    // ---------- Cuenta personal ----------

    /** Enlaza su cuenta de cliente: debe ser una cuenta registrada (no invitada) con su mismo DNI. */
    public void vincularCuentaPersonal(Cliente cliente) {
        if (cuentaPersonal != null) throw new IllegalStateException("El empleado ya tiene una cuenta personal");
        if (cliente == null || cliente.esInvitado() || !getDni().equals(cliente.getDni())) {
            throw new IllegalArgumentException("La cuenta personal debe ser un cliente registrado con el mismo DNI");
        }
        this.cuentaPersonal = cliente;
    }

    public boolean tieneCuentaPersonal() { return cuentaPersonal != null; }
    public Cliente getCuentaPersonal() { return cuentaPersonal; }

    public int getIdEmpleado() { return idEmpleado; }
    public String getTurno() { return turno; }
    public Rol getRol() { return rol; }
    public Sede getSede() { return sede; }

    @Override
    public String toString() {
        return getNombreCompleto() + " - " + rol.getNombre();
    }
}
