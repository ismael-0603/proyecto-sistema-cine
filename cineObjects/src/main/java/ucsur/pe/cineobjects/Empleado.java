package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Empleado extends Persona{
    private int idEmpleado;
    private String turno;
    private Rol rol;
    private Sede sede;

    public Empleado(){
        super();
    }

    public Empleado(int idEmpleado, String turno, Rol rol, Sede sede, String dni, String nombres, String apellidos, DateTime fechaNac, String correo, String telefono, String contra, boolean estado) {
        super(dni, nombres, apellidos, fechaNac, correo, telefono, contra, true);
        this.idEmpleado = idEmpleado;
        this.turno = turno;
        this.rol = rol;
        this.sede = sede;
    }

    public int getIdEmpleado() {
        return idEmpleado;
    }

    public void setIdEmpleado(int idEmpleado) {
        this.idEmpleado = idEmpleado;
    }

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Sede getSede() {
        return sede;
    }

    public void setSede(Sede sede) {
        this.sede = sede;
    }
    
}
