package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public abstract class Persona {
    protected String dni;
    protected String nombres;
    protected String apellidos;
    protected DateTime fechaNac; // solo fecha: DateTime(dia, mes, anio)
    protected String correo;
    protected String telefono;
    protected String contra;
    protected boolean estado = true;

    // Los datos son opcionales aquí (el cliente invitado solo tiene correo);
    // los que lo exigen llaman a exigirDatosCompletos().
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

    // ---------- Comportamientos del diagrama ----------
    public boolean iniciarSesion(String correo, String contra) {
        if (!estado || this.correo == null || this.contra == null || correo == null || contra == null) {
            return false;
        }
        return this.correo.equalsIgnoreCase(correo) && this.contra.equals(contra);
    }

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

    public String getNombreCompleto() {
        String completo = ((nombres != null ? nombres : "") + " " + (apellidos != null ? apellidos : "")).trim();
        return completo.isEmpty() ? "Invitado" : completo;
    }

    public int calcularEdad() {
        if (this.fechaNac == null) return 0;
        return this.fechaNac.calcularAniosHasta(DateTime.ahora());
    }

    // ---------- Validaciones (reglas de la BD) ----------
    // Para clientes registrados y empleados: estos datos son NOT NULL en la BD
    protected void exigirDatosCompletos() {
        if (dni == null || nombres == null || apellidos == null || correo == null || contra == null) {
            throw new IllegalArgumentException("DNI, nombres, apellidos, correo y contraseña son obligatorios");
        }
    }

    // Para el cliente invitado: solo el correo es obligatorio
    protected void exigirCorreo() {
        if (correo == null) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
    }

    private static String validarDni(String dni) {
        if (dni != null && !dni.matches("\\d{8}")) {
            throw new IllegalArgumentException("El DNI debe tener exactamente 8 dígitos");
        }
        return dni;
    }

    private static String validarTexto(String texto, String campo, int max) {
        if (texto == null) return null;
        String t = texto.trim();
        if (t.isEmpty() || t.length() > max) {
            throw new IllegalArgumentException(campo + " no pueden estar vacíos ni superar " + max + " caracteres");
        }
        return t;
    }

    private static DateTime validarFechaNac(DateTime fecha) {
        if (fecha != null && fecha.esDespuesDe(DateTime.ahora())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }
        return fecha;
    }

    private static String validarCorreo(String correo) {
        if (correo == null) return null;
        String c = correo.trim();
        if (c.length() > 100 || !c.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("Correo inválido");
        }
        return c;
    }

    private static String validarTelefono(String telefono) {
        if (telefono != null && telefono.length() > 20) {
            throw new IllegalArgumentException("El teléfono no puede superar 20 caracteres");
        }
        return telefono;
    }

    // Máx. 40 caracteres: es el límite de empleado.contra en la BD
    private static String validarContra(String contra) {
        if (contra != null && (contra.isEmpty() || contra.length() > 40)) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía ni superar 40 caracteres");
        }
        return contra;
    }

    // ---------- Getters y setters ----------
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