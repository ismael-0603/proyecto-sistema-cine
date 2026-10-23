package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public abstract class Persona {
    protected String dni;
    protected String nombres;
    protected String apellidos;
    protected DateTime fechaNac;
    protected String correo;
    protected String telefono;
    protected String contra;
    protected boolean estado = true; 
    
    public Persona() {
    }
    
    public Persona(String dni, String nombres, String apellidos, DateTime fechaNac, String correo, String telefono, String contra, boolean estado) {
        this.dni = dni;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.fechaNac = fechaNac;
        this.correo = correo;
        this.telefono = telefono;
        this.contra = contra;
        this.estado = estado;
    }
   
    // GET --> Obtener | SET --> Establecer | viene a ser encapsulamiento
    // Aun faltaria asignar los comportamientos

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public DateTime getFechaNac() {
        return fechaNac;
    }

    public void setFechaNac(DateTime fechaNac) {
        this.fechaNac = fechaNac;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getContra() {
        return contra;
    }

    public void setContra(String contra) {
        this.contra = contra;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }
    
}
