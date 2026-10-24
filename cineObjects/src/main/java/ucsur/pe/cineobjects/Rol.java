package ucsur.pe.cineobjects;

/**
 * Define el nivel de acceso de una persona empleada y los permisos que obtiene con ese nivel.
 * El nombre es obligatorio y el nivel solo puede ser de dulcería o de administración.
 */
public class Rol {
    // Acciones que pueden comprobarse con tienePermiso.
    public static final String ADMINISTRAR = "ADMINISTRAR";       // CRUD de películas, funciones, salas, promociones, etc.
    public static final String ATENDER_PEDIDO = "ATENDER_PEDIDO"; // preparar y entregar pedidos

    // Dulcería puede atender pedidos; administración incluye también las tareas administrativas.
    public static final int NIVEL_DULCERIA = 1; // Puede preparar y entregar pedidos.
    public static final int NIVEL_ADMIN = 2; // Incluye administración y atención de pedidos.

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

    /** Comprueba el permiso solicitado; devuelve false para acciones desconocidas o nulas. */
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
