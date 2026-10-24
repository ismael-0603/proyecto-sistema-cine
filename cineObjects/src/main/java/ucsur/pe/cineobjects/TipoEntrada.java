package ucsur.pe.cineobjects;

/**
 * Define una tarifa de entrada, sus requisitos de elegibilidad y los puntos que genera.
 * El nombre es obligatorio, la descripción tiene un máximo de 200 caracteres y la tarifa no puede ser negativa.
 */
public class TipoEntrada {
    // Puntos que se otorgan según la categoría de la entrada.
    public static final int PUNTOS_SOCIO = 3;
    public static final int PUNTOS_GENERAL = 2;
    public static final int PUNTOS_REDUCIDO = 1;
    
    private int idTipoEntrada;
    private String nombre;
    private String descripcion;
    private double tarifa;
    private boolean requiereSocio;
    /** Puntos que cuesta canjear esta tarifa; cero significa que no se descuentan puntos. */
    private int puntosCanje;
    private boolean estado;

    public TipoEntrada(int idTipoEntrada, String nombre, String descripcion,
                       double tarifa, boolean requiereSocio, boolean estado) {
        this(idTipoEntrada, nombre, descripcion, tarifa, requiereSocio, estado, 0);
    }

    public TipoEntrada(int idTipoEntrada, String nombre, String descripcion,
                       double tarifa, boolean requiereSocio, boolean estado, int puntosCanje) {
        if (nombre == null || nombre.trim().isEmpty() || nombre.trim().length() > 40) {
            throw new IllegalArgumentException("El nombre es obligatorio (máx. 40 caracteres)");
        }
        if (descripcion != null && descripcion.length() > 200) {
            throw new IllegalArgumentException("La descripción no puede superar 200 caracteres");
        }
        if (tarifa < 0) {
            throw new IllegalArgumentException("La tarifa no puede ser negativa");
        }
        if (puntosCanje < 0) {
            throw new IllegalArgumentException("Los puntos de canje no pueden ser negativos");
        }
        if (puntosCanje > 0 && !requiereSocio) {
        throw new IllegalArgumentException("Un tipo de entrada que cuesta puntos debe ser exclusivo para socios");
        }
        this.idTipoEntrada = idTipoEntrada;
        this.nombre = nombre.trim();
        this.descripcion = descripcion;
        this.tarifa = tarifa;
        this.requiereSocio = requiereSocio;
        this.estado = estado;
        this.puntosCanje = puntosCanje;
    }

    public int getPuntosCanje() { return puntosCanje; }

        public double getTarifa() {
            return this.tarifa;
        }

    /** Rechaza tipos inactivos y exige que el cliente sea socio cuando la tarifa lo requiere. */
    public boolean validarElegibilidad(Cliente cliente) {
        if (!this.estado) return false;
        if (this.requiereSocio) {
            return cliente != null && cliente.esSocio();
        }
        return true;
    }

    /** Habilita esta tarifa para nuevas compras. */
    public void activar() { 
        this.estado = true; 
    }
    
    /** Deshabilita esta tarifa para nuevas compras. */
    public void desactivar() { 
        this.estado = false; 
    }
    
    /** Devuelve los puntos de la categoría; los tipos inactivos no generan puntos. */
    /** Devuelve los puntos que entrega esta tarifa según su nombre; si está inactiva, devuelve cero. */
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
