package ucsur.pe.cineobjects;

/**
 * Define una tarifa de entrada, sus requisitos de elegibilidad y los puntos que genera.
 * El nombre es obligatorio, la descripción tiene un máximo de 200 caracteres y la tarifa no puede ser negativa.
 */
public abstract class TipoEntrada {
    private int idTipoEntrada;
    private String nombre;
    private String descripcion;
    private double tarifa;
    private boolean requiereSocio; // si es true, solo la compra un cliente socio
    /** Puntos que cuesta canjear esta tarifa; cero significa que no se descuentan puntos. */
    private int puntosCanje;
    private boolean estado;

    protected TipoEntrada(int idTipoEntrada, String nombre, String descripcion,
                          double tarifa, boolean requiereSocio, boolean estado) {
        this(idTipoEntrada, nombre, descripcion, tarifa, requiereSocio, estado, 0);
    }

    protected TipoEntrada(int idTipoEntrada, String nombre, String descripcion,
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

    /** Puntos que se descuentan del saldo del cliente al usar esta tarifa. */
    public int getPuntosCanje() { return puntosCanje; }

    /** Precio base de la entrada, sin el recargo del formato de sala. */
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
    
    /** Devuelve los puntos que entrega esta tarifa; si está inactiva, devuelve cero. */
    public int obtenerPuntos() {
        return this.estado ? puntosBase() : 0;
    }

    /** Cada categoría de entrada define cuántos puntos otorga. */
    protected abstract int puntosBase();

    /**
     * Crea la categoría adecuada a partir del nombre guardado en la tabla tipo_entrada:
     * SOCIO, GENERAL/ADULTO o, en cualquier otro caso, entrada reducida.
     */
    public static TipoEntrada desdeNombre(int idTipoEntrada, String nombre, String descripcion,
                                          double tarifa, boolean requiereSocio, boolean estado, int puntosCanje) {
        String n = nombre == null ? "" : nombre.trim().toUpperCase();
        return switch (n) {
            case "SOCIO", "ENTRADA SOCIO" ->
                    new TipoEntradaSocio(idTipoEntrada, nombre, descripcion, tarifa, requiereSocio, estado, puntosCanje);
            case "GENERAL", "ADULTO", "ENTRADA GENERAL" ->
                    new TipoEntradaGeneral(idTipoEntrada, nombre, descripcion, tarifa, requiereSocio, estado, puntosCanje);
            default ->
                    new TipoEntradaReducida(idTipoEntrada, nombre, descripcion, tarifa, requiereSocio, estado, puntosCanje);
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
