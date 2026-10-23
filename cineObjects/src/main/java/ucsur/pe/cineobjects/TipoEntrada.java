package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class TipoEntrada {
    // Escala de puntos por tipo de entrada comprada
    public static final int PUNTOS_SOCIO = 3;
    public static final int PUNTOS_GENERAL = 2;
    public static final int PUNTOS_REDUCIDO = 1;
    
    private int idTipoEntrada;
    private String nombre;
    private String descripcion;
    private double tarifa;
    private boolean requiereSocio;
    private boolean estado;

    public TipoEntrada(int idTipoEntrada, String nombre, String descripcion,
                       double tarifa, boolean requiereSocio, boolean estado) {
        if (nombre == null || nombre.trim().isEmpty() || nombre.trim().length() > 40) {
            throw new IllegalArgumentException("El nombre es obligatorio (máx. 40 caracteres)");
        }
        if (descripcion != null && descripcion.length() > 200) {
            throw new IllegalArgumentException("La descripción no puede superar 200 caracteres");
        }
        if (tarifa < 0) {
            throw new IllegalArgumentException("La tarifa no puede ser negativa");
        }
        this.idTipoEntrada = idTipoEntrada;
        this.nombre = nombre.trim();
        this.descripcion = descripcion;
        this.tarifa = tarifa;
        this.requiereSocio = requiereSocio;
        this.estado = estado;
    }

    public double getTarifa() {
        return this.tarifa;
    }

    // Un tipo inactivo no se vende; si requiere socio, el cliente debe serlo
    public boolean validarElegibilidad(Cliente cliente) {
        if (!this.estado) return false;
        if (this.requiereSocio) {
            return cliente != null && cliente.esSocio();
        }
        return true;
    }

    public void activar() { 
        this.estado = true; 
    }
    
    public void desactivar() { 
        this.estado = false; 
    }
    
    // Asigna los puntos según el tipo de entrada si se encuentra activa
    public int obtenerPuntos() {
        if (!this.estado) return 0;
        return switch (this.nombre.toUpperCase()) {
            case "SOCIO", "ENTRADA SOCIO" -> PUNTOS_SOCIO;
            case "GENERAL", "ADULTO", "ENTRADA GENERAL" -> PUNTOS_GENERAL;
            case "NIÑO", "NINO", "ADULTO MAYOR", "MAYOR", "CONADIS", "DISCAPACIDAD" -> PUNTOS_REDUCIDO;
            default -> PUNTOS_REDUCIDO;
        };
    }

    public int getIdTipoEntrada() { return idTipoEntrada; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public boolean isRequiereSocio() { return requiereSocio; }
    public boolean isEstado() { return estado; }

    @Override
    public String toString() {
        return nombre + " (S/ " + String.format("%.2f", tarifa) + ")";
    }
}