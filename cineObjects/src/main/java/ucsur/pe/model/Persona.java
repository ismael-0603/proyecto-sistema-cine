package ucsur.pe.model;

/**
 * Por qué existe: junta lo común de Cliente y Empleado (datos personales, validaciones e inicio de sesión)
 * para no repetirlo. Es abstracta: no se crean personas sueltas, solo clientes o empleados.
 */
public abstract class Persona implements Activable {
    // private (encapsulamiento): ni las subclases cambian estos datos sin pasar por las validaciones;
    // Cliente y Empleado los leen con los getters y cambian el estado con activar() y desactivar().
    private String dni;
    private String nombres;
    private String apellidos;
    private DateTime fechaNac; // solo fecha: DateTime(dia, mes, anio)
    private String correo;
    private String telefono;
    private String contra;
    private boolean estado = true; // cuenta activa; si es false no puede iniciar sesión
    private final Reloj reloj; // de dónde sale la fecha de hoy (el real o uno fijo en pruebas)

    // Se permiten datos incompletos para invitados; clientes registrados y empleados validan lo requerido.
    protected Persona(String dni, String nombres, String apellidos, DateTime fechaNac,
                      String correo, String telefono, String contra, Reloj reloj) {
        if (reloj == null) throw new IllegalArgumentException("El reloj es obligatorio");
        this.reloj = reloj; // va primero: validarFechaNac lo necesita
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
    private DateTime validarFechaNac(DateTime fecha) {
        if (fecha != null && fecha.esDespuesDe(reloj.ahora())) {
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

    // Sin setters: los datos se validan en el constructor y el teléfono y el correo cambian con modificarDatos.
    public String getDni() { return dni; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public DateTime getFechaNac() { return fechaNac; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }

    /**
     * Solo para la capa de base de datos (ucsur.pe.db), que necesita escribir la columna contra.
     * Las demás capas verifican la clave con verificarContrasena, sin leerla.
     * Hoy se guarda en texto plano (alcance del curso); si el sistema se publica, aquí iría el hash.
     */
    public String getContra() { return contra; }

    /** Reloj de la cuenta; Cliente lo usa para su fecha de registro y sus visitas del año. */
    protected Reloj getReloj() { return reloj; }

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
