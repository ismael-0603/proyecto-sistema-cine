package ucsur.pe.model;

/**
 * Por qué existe: junta lo común de Cliente y Empleado (datos personales, validaciones e inicio de sesión)
 * para no repetirlo. Es abstracta: no se crean personas sueltas, solo clientes o empleados.
 */
public abstract class Persona implements Activable {
    // private (encapsulamiento): ni las subclases cambian estos datos sin pasar por las validaciones;
    // Cliente y Empleado los leen con los getters y cambian el estado con activar() y desactivar().
    public static final int MIN_CONTRA = 8; // wireframe 5: «Mínimo 8 caracteres»
    public static final int EDAD_MIN = 15; // edad mínima para tener una cuenta
    public static final int EDAD_MAX = 110; // por encima, la fecha de nacimiento es un error de tipeo
    /** Letras (con tildes, ñ y ü), espacios, apóstrofo y guion; al menos una letra. Rechaza "12345" o "@@@". */
    public static final String REGEX_NOMBRE = "(?=.*\\p{L})[\\p{L} '-]+";
    /** usuario@dominio.tld con TLD de 2 letras o más: rechaza "a@..com", "a@.com", "a@b.c" o "user@dom,com". */
    public static final String REGEX_CORREO = "[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}";
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
        this.nombres = validarNombre(nombres, "Los nombres", 80);
        this.apellidos = validarNombre(apellidos, "Los apellidos", 80);
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

    // ---------- Reglas públicas: devuelven el motivo del error o null si el dato es válido ----------
    // Las pantallas las usan para marcar en rojo un campo mientras se escribe, con el mismo texto que
    // lanzaría el modelo. Los obligatorios rechazan null y vacío; el teléfono es opcional.

    /** DNI peruano: 8 dígitos (se ignoran espacios al inicio y al final) y no 00000000. */
    public static String errorDni(String dni) {
        if (dni == null || !esDniValido(dni.trim())) return "El DNI debe tener exactamente 8 dígitos";
        return null;
    }

    public static String errorNombres(String nombres) {
        return errorNombre(nombres, "Los nombres", 80);
    }

    public static String errorApellidos(String apellidos) {
        return errorNombre(apellidos, "Los apellidos", 80);
    }

    /** Solo letras, espacios, apóstrofo o guion (con al menos una letra); no vacío ni más largo que el máximo. */
    private static String errorNombre(String texto, String campo, int max) {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty() || t.length() > max) return campo + " no pueden estar vacíos ni superar " + max + " caracteres";
        if (!t.matches(REGEX_NOMBRE)) return campo + " solo pueden tener letras, espacios, apóstrofo o guion";
        return null;
    }

    /** Ni futura ni con una edad fuera de EDAD_MIN..EDAD_MAX años cumplidos a la fecha 'hoy'. */
    public static String errorFechaNacimiento(DateTime fecha, DateTime hoy) {
        if (fecha == null) return "La fecha de nacimiento es obligatoria";
        if (fecha.esDespuesDe(hoy)) return "La fecha de nacimiento no puede ser futura";
        int edad = edadEn(fecha, hoy);
        if (edad < EDAD_MIN) return "Debes tener al menos " + EDAD_MIN + " años";
        if (edad > EDAD_MAX) return "Revisa la fecha de nacimiento: la edad no puede superar " + EDAD_MAX + " años";
        return null;
    }

    /** Formato usuario@dominio.tld y hasta 100 caracteres. */
    public static String errorCorreo(String correo) {
        String c = correo == null ? "" : correo.trim();
        if (c.isEmpty()) return "El correo es obligatorio";
        if (c.length() > 100 || !c.matches(REGEX_CORREO)) return "Correo inválido";
        return null;
    }

    /** Opcional: vacío es válido; si se escribe, celular de Perú de 9 dígitos que empieza con 9. */
    public static String errorTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) return null;
        if (!telefono.trim().matches("9\\d{8}")) return "El celular debe tener 9 dígitos y empezar con 9";
        return null;
    }

    /** Entre MIN_CONTRA y 255 caracteres y no solo espacios. */
    public static String errorContrasena(String contra) {
        if (contra == null) return "La contraseña es obligatoria";
        if (contra.length() < MIN_CONTRA || contra.length() > 255) {
            return "La contraseña debe tener entre " + MIN_CONTRA + " y 255 caracteres";
        }
        if (contra.isBlank()) return "La contraseña no puede estar formada solo por espacios";
        return null;
    }

    /** Formato de DNI peruano: 8 dígitos y no todos ceros. Lo usa también la pantalla de pago. */
    public static boolean esDniValido(String dni) {
        return dni != null && dni.matches("\\d{8}") && !dni.equals("00000000");
    }

    /** Lanza el motivo como IllegalArgumentException si hay error. */
    private static void exigir(String error) {
        if (error != null) throw new IllegalArgumentException(error);
    }

    // ---------- Validaciones del constructor: null se acepta (cuentas invitadas) ----------

    private static String validarDni(String dni) {
        if (dni == null) return null;
        exigir(errorDni(dni));
        return dni.trim(); // "12345678 " (espacio al copiar) también vale
    }

    private static String validarNombre(String texto, String campo, int max) {
        if (texto == null) return null;
        exigir(errorNombre(texto, campo, max));
        return texto.trim();
    }

    private DateTime validarFechaNac(DateTime fecha) {
        if (fecha == null) return null;
        exigir(errorFechaNacimiento(fecha, reloj.ahora()));
        return fecha;
    }

    /** Años cumplidos a la fecha indicada: resta uno si aún no llega el cumpleaños de ese año. */
    private static int edadEn(DateTime nacimiento, DateTime hoy) {
        int edad = hoy.getAnio() - nacimiento.getAnio();
        boolean faltaCumple = hoy.getMes() < nacimiento.getMes()
                || (hoy.getMes() == nacimiento.getMes() && hoy.getDia() < nacimiento.getDia());
        return faltaCumple ? edad - 1 : edad;
    }

    /**
     * Si hoy es su cumpleaños (mismo día y mes). Quien nació un 29 de febrero lo celebra el 28 en los años
     * que no son bisiestos. Sin fecha de nacimiento (cuenta invitada), nunca.
     */
    public boolean esCumpleanos() {
        if (fechaNac == null) return false;
        DateTime hoy = reloj.ahora();
        int dia = fechaNac.getDia();
        if (fechaNac.getMes() == 2 && dia == 29 && !DateTime.esBisiesto(hoy.getAnio())) dia = 28;
        return hoy.getMes() == fechaNac.getMes() && hoy.getDia() == dia;
    }

    /** Acepta un correo nulo cuando el tipo de persona lo permite; valida formato y longitud si se informa. */
    private static String validarCorreo(String correo) {
        if (correo == null) return null;
        exigir(errorCorreo(correo));
        return correo.trim();
    }

    /**
     * Cambia la contraseña al recuperarla («¿Olvidaste tu contraseña?»); aplica las mismas reglas que al registrarse.
     * Quien verifica el código enviado al correo es AutenticacionService.
     */
    public void cambiarContrasena(String nueva) {
        if (nueva == null) throw new IllegalArgumentException("La contraseña es obligatoria");
        this.contra = validarContra(nueva);
    }

    /**
     * Permite omitir el teléfono: nulo o vacío ("" es lo que devuelve un TextField sin llenar) se guarda como null.
     * Si se informa, debe ser un celular de Perú: 9 dígitos que empiezan con 9.
     */
    private static String validarTelefono(String telefono) {
        exigir(errorTelefono(telefono));
        return telefono == null || telefono.isBlank() ? null : telefono.trim();
    }

    /** Permite contraseña nula para invitados; si se informa, 8 a 255 caracteres y no solo espacios. */
    private static String validarContra(String contra) {
        if (contra == null) return null;
        exigir(errorContrasena(contra));
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
