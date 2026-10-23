package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Rol {
    // Acciones que el sistema reconoce
    public static final String ADMINISTRAR = "ADMINISTRAR";       // CRUD de películas, funciones, salas, promociones, etc.
    public static final String ATENDER_PEDIDO = "ATENDER_PEDIDO"; // preparar y entregar pedidos

    // Niveles de acceso
    public static final int NIVEL_DULCERIA = 1;
    public static final int NIVEL_ADMIN = 2;

    private int idRol;
    private String nombre;
    private String descripcion;
    private int nivelAcceso;

    public Rol(int idRol, String nombre, String descripcion, int nivelAcceso) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio");
        }
        if (nivelAcceso != NIVEL_DULCERIA && nivelAcceso != NIVEL_ADMIN) {
            throw new IllegalArgumentException("Nivel de acceso inválido (1 = dulcería, 2 = admin)");
        }
        this.idRol = idRol;
        this.nombre = nombre.trim();
        this.descripcion = descripcion;
        this.nivelAcceso = nivelAcceso;
    }

    public boolean tienePermiso(String accion) {
        if (accion == null) return false;
        switch (accion.toUpperCase()) {
            case ADMINISTRAR:
                return nivelAcceso >= NIVEL_ADMIN;
            case ATENDER_PEDIDO:
                return nivelAcceso >= NIVEL_DULCERIA; // dulcería y admin
            default:
                return false;
        }
    }

    public int getIdRol() { return idRol; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public int getNivelAcceso() { return nivelAcceso; }

    @Override
    public String toString() {
        return nombre + " (nivel " + nivelAcceso + ")";
    }
}
