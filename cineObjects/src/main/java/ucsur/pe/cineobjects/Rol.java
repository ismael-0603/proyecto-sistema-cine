package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Rol {
    private int idRol;
    private String nombre; // nombre del cargo
    private String descripcion; // las funciones o responsabilidades del puesto
    private int nivelAcceso; // 0 = admin, 1 = empleado (ejempo)
    
    public Rol() {
    }
    
    public Rol(int idRol, String nombre, String descripcion, int nivelAcceso) {
        this.idRol = idRol;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.nivelAcceso = nivelAcceso;
    }

    public int getIdRol() {
        return idRol;
    }

    public void setIdRol(int idRol) {
        this.idRol = idRol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getNivelAcceso() {
        return nivelAcceso;
    }

    public void setNivelAcceso(int nivelAcceso) {
        this.nivelAcceso = nivelAcceso;
    }
    
}
