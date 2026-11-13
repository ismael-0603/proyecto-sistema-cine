package ucsur.pe.model;

/**
 * Por qué existe: junta lo común de Cliente y Empleado (datos personales, validaciones e inicio de sesión)
 * para no repetirlo. Es abstracta: no se crean personas sueltas, solo clientes o empleados.
 */
public abstract class Persona implements Activable {
    // protected: las subclases (Cliente y Empleado) pueden acceder a estos datos directamente.
    protected String dni;
    protected String nombres;
    protected String apellidos;
    protected DateTime fechaNac; // solo fecha: DateTime(dia, mes, anio)
    protected String correo;
    protected String telefono;
    protected String contra;
    protected boolean estado = true; // cuenta activa; si es false no puede iniciar sesión

    // Se permiten datos incompletos para invitados; clientes registrados y empleados validan lo requerido.
    protected Persona(String dni, String nombres, String apellidos, DateTime fechaNac,
                      String correo, String telefono, String contra) {
        this.dni = validarDni(dni);
        this.nombres = validarTexto(nombres, "Los nombres", 80);
        this.apellidos = validarTexto(apellidos, "Los apellidos", 80);
        this.fechaNac = validarFechaNac(fechaNac);
        this.correo = validarCorreo(correo);
        this.telefono = validarTelefono(telefono);
        this.contra = validarContra(contra);
        this.estado = true;
    }

    // Operaciones compartidas de inicio de sesión y datos personales.
    /**
     * Indica si la cuenta está activa y la contraseña coincide. Buscar la cuenta por su correo
     * es trabajo de AutenticacionService (con el repositorio); aquí solo se verifica la clave.
     */
    public boolean verificarContrasena(String contra) {
        return estado && this.contra != null && contra != null && this.contra.equals(contra);
    }

    /**
     * Cambia teléfono y correo. Valida los dos antes de guardar: si alguno no sirve, lanza el error
     * y conserva los datos anteriores. Que el correo no lo use otra cuenta lo revisa el servicio.
     */
    public void modificarDatos(String telefono, String correo) {
        String nuevoCorreo = validarCorreo(correo);
        if (nuevoCorreo == null) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
        String nuevoTelefono = validarTelefono(telefono);
        this.telefono = nuevoTelefono;
        this.correo = nuevoCorreo;
    }

    /** Une nombres y apellidos; devuelve "Invitado" si no hay ninguno informado. */
    public String getNombreCompleto() {
        String completo = ((nombres != null ? nombres : "") + " " + (apellidos != null ? apellidos : "")).trim();
        return completo.isEmpty() ? "Invitado" : completo;
    }

    /** Calcula la edad a la fecha de hoy; si no se conoce el nacimiento, devuelve cero. */
    public int calcularEdad() {
        if (this.fechaNac == null) return 0;
        return this.fechaNac.calcularAniosHasta(DateTime.ahora());
    }

    /** Indica si están los datos obligatorios de una cuenta registrada; la subclase pregunta sin ver la contraseña. */
    protected final boolean tieneDatosCompletos() {
        return dni != null && nombres != null && apellidos != null && correo != null && contra != null;
    }

    // Validaciones comunes del modelo; estos campos son obligatorios para cuentas registradas.
    protected final void exigirDatosCompletos() {
        if (!tieneDatosCompletos()) {
            throw new IllegalArgumentException("DNI, nombres, apellidos, correo y contraseña son obligatorios");
        }
    }

    // Un invitado no necesita datos personales, pero sí correo.
    protected final void exigirCorreo() {
        if (correo == null) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
    }

    /** Acepta un DNI nulo para invitados; si existe, debe contener exactamente ocho dígitos. */
    private static String validarDni(String dni) {
        if (dni != null && !dni.matches("\\d{8}")) {
            throw new IllegalArgumentException("El DNI debe tener exactamente 8 dígitos");
        }
        return dni;
    }

    /** Recorta espacios y rechaza textos vacíos o que superen el máximo indicado. */
    private static String validarTexto(String texto, String campo, int max) {
        if (texto == null) return null;
        String t = texto.trim();
        if (t.isEmpty() || t.length() > max) {
            throw new IllegalArgumentException(campo + " no pueden estar vacíos ni superar " + max + " caracteres");
        }
        return t;
    }

    /** Rechaza fechas de nacimiento posteriores al momento actual. */
    private static DateTime validarFechaNac(DateTime fecha) {
        if (fecha != null && fecha.esDespuesDe(DateTime.ahora())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }
        return fecha;
    }

    /** Acepta un correo nulo cuando el tipo de persona lo permite; valida formato y longitud si se informa. */
    private static String validarCorreo(String correo) {
        if (correo == null) return null;
        String c = correo.trim();
        if (c.length() > 100 || !c.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("Correo inválido");
        }
        return c;
    }

    /** Permite omitir el teléfono y limita a veinte caracteres el valor informado. */
    private static String validarTelefono(String telefono) {
        if (telefono != null && telefono.length() > 20) {
            throw new IllegalArgumentException("El teléfono no puede superar 20 caracteres");
        }
        return telefono;
    }

    /** Permite contraseña nula para invitados; si se informa, no puede estar vacía ni exceder 255 caracteres. */
    private static String validarContra(String contra) {
        if (contra != null && (contra.isEmpty() || contra.length() > 255)) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía ni superar 255 caracteres");
        }
        return contra;
    }

    // Los setters vuelven a validar cada dato antes de guardarlo.
    public String getDni() { return dni; }
    /** Solo para completar el DNI de un invitado que se registra; una vez registrado, el DNI no cambia. */
    public void setDni(String dni) {
        if (this.dni != null) {
            throw new IllegalStateException("El DNI no se puede cambiar una vez registrado");
        }
        this.dni = validarDni(dni);
    }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = validarTexto(nombres, "Los nombres", 80); }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = validarTexto(apellidos, "Los apellidos", 80); }

    public DateTime getFechaNac() { return fechaNac; }
    public void setFechaNac(DateTime fechaNac) { this.fechaNac = validarFechaNac(fechaNac); }

    public String getCorreo() { return correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = validarTelefono(telefono); }

    public void setContra(String contra) { this.contra = validarContra(contra); }

    /** Indica si la cuenta está activa (puede iniciar sesión). */
    @Override
    public boolean isEstado() { return estado; }

    /** Activa la cuenta. */
    @Override
    public void activar() { this.estado = true; }

    /** Desactiva la cuenta: ya no puede iniciar sesión. */
    @Override
    public void desactivar() { this.estado = false; }

    @Override
    public String toString() {
        return getNombreCompleto();
    }
}
