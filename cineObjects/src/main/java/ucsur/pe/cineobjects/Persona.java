package ucsur.pe.cineobjects;

import ucsur.pe.cineobjects.cineInterfaz.Autenticable;

/**
 * Clase base para clientes y empleados; centraliza sus datos personales y validaciones comunes.
 * El DNI, cuando se proporciona, debe tener ocho dígitos; también se valida el correo, teléfono,
 * fecha de nacimiento y longitud de la contraseña. Los datos obligatorios se exigen según el tipo de cuenta.
 */
public abstract class Persona implements Autenticable {
    protected String dni;
    protected String nombres;
    protected String apellidos;
    protected DateTime fechaNac; // solo fecha: DateTime(dia, mes, anio)
    protected String correo;
    protected String telefono;
    protected String contra;
    protected boolean estado = true;

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
    /** Inicia sesión solo si la cuenta está activa y coinciden correo y contraseña. */
    @Override
    public boolean iniciarSesion(String correo, String contra) {
        if (!estado || this.correo == null || this.contra == null || correo == null || contra == null) {
            return false;
        }
        return this.correo.equalsIgnoreCase(correo) && this.contra.equals(contra);
    }

    /** Valida correo y teléfono antes de guardar; si alguno no sirve, conserva los datos anteriores. */
    @Override
    public boolean modificarDatos(String telefono, String correo) {
        try {
            String nuevoCorreo = validarCorreo(correo);
            if (nuevoCorreo == null) return false;
            this.telefono = validarTelefono(telefono);
            this.correo = nuevoCorreo;
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
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

    // Validaciones comunes del modelo; estos campos son obligatorios para cuentas registradas.
    protected void exigirDatosCompletos() {
        if (dni == null || nombres == null || apellidos == null || correo == null || contra == null) {
            throw new IllegalArgumentException("DNI, nombres, apellidos, correo y contraseña son obligatorios");
        }
    }

    // Un invitado no necesita datos personales, pero sí correo.
    protected void exigirCorreo() {
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

    // El límite de longitud corresponde al campo de contraseña definido para empleados.
    /** Permite contraseña nula para invitados; si se informa, no puede estar vacía ni exceder 40 caracteres. */
    private static String validarContra(String contra) {
        if (contra != null && (contra.isEmpty() || contra.length() > 40)) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía ni superar 40 caracteres");
        }
        return contra;
    }

    // Los setters vuelven a validar cada dato antes de guardarlo.
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = validarDni(dni); }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = validarTexto(nombres, "Los nombres", 80); }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = validarTexto(apellidos, "Los apellidos", 80); }

    public DateTime getFechaNac() { return fechaNac; }
    public void setFechaNac(DateTime fechaNac) { this.fechaNac = validarFechaNac(fechaNac); }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = validarCorreo(correo); }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = validarTelefono(telefono); }

    public void setContra(String contra) { this.contra = validarContra(contra); }

    public boolean isEstado() { return estado; }
    public void setEstado(boolean estado) { this.estado = estado; }

    @Override
    public String toString() {
        return getNombreCompleto();
    }
}
