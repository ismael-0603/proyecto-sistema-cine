package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Entrada {
    public static final String BLOQUEADA = "BLOQUEADA";
    public static final String VENDIDA = "VENDIDA";
    public static final String EXPIRADA = "EXPIRADA";
    public static final String ANULADA = "ANULADA";

    public static final int TIEMPO_BLOQUEO_MIN = 4;

    private int idEntrada;
    private String codigoQR;
    private double precio;
    private DateTime fechaEmision;
    private DateTime expiraBloqueo;
    private String estado;
    private Funcion funcion;
    private Butaca butaca;
    private TipoEntrada tipoEntrada;

    // Constructor para Fase 1 (selección en el mapa: aún no tiene TipoEntrada ni ocupa la butaca en Funcion)
    public Entrada(int idEntrada, Funcion funcion, Butaca butaca) {
        if (funcion == null) throw new IllegalArgumentException("La función es obligatoria");
        if (butaca == null) throw new IllegalArgumentException("La butaca es obligatoria");
        if (butaca.getSala() != funcion.getSala()) {
            throw new IllegalArgumentException("La butaca no pertenece a la sala de la función");
        }
        this.idEntrada = idEntrada;
        this.funcion = funcion;
        this.butaca = butaca;
        this.estado = BLOQUEADA;
    }

    // Constructor completo
    public Entrada(int idEntrada, Funcion funcion, Butaca butaca, TipoEntrada tipoEntrada) {
        this(idEntrada, funcion, butaca);
        asignarTipoEntrada(tipoEntrada);
    }

    // Se llama en la Fase 2 cuando el usuario elige el tipo de entrada para cada asiento bloqueado
    public void asignarTipoEntrada(TipoEntrada tipoEntrada) {
        if (tipoEntrada == null) {
            throw new IllegalArgumentException("El tipo de entrada es obligatorio");
        }
        if (VENDIDA.equals(estado)) {
            throw new IllegalStateException("No se puede cambiar el tipo de una entrada ya vendida");
        }
        this.tipoEntrada = tipoEntrada;
        calculaPrecioFinal();
    }

    // Bloquea la butaca heredando la hora exacta de expiración de la Compra (Modelo A)
    public boolean bloquearTemporalmente(DateTime fechaExpiracion) {
        if (fechaExpiracion == null || !DateTime.ahora().esAntesDe(fechaExpiracion)) {
            return false; // El tiempo de la compra ya venció
        }
        if (!BLOQUEADA.equals(estado) || expiraBloqueo != null) {
            throw new IllegalStateException("La entrada ya fue bloqueada o no está en estado BLOQUEADA");
        }
        if (!funcion.estaDisponible() || !funcion.esButacaDisponible(butaca)) {
            return false;
        }
        this.expiraBloqueo = fechaExpiracion;
        funcion.registrarEntrada(this);
        return true;
    }

    // Sobrecarga por minutos (máx. 4 minutos)
    public boolean bloquearTemporalmente(int minutos) {
        if (minutos <= 0 || minutos > TIEMPO_BLOQUEO_MIN) {
            throw new IllegalArgumentException("El tiempo de bloqueo debe estar entre 1 y " + TIEMPO_BLOQUEO_MIN + " minutos");
        }
        return bloquearTemporalmente(DateTime.ahora().sumarMinutos(minutos));
    }

    // Si el bloqueo venció, pasa a EXPIRADA y libera la butaca
    public boolean haExpirado() {
        if (BLOQUEADA.equals(estado) && expiraBloqueo != null
                && DateTime.ahora().esDespuesDe(expiraBloqueo)) {
            expirar();
        }
        return EXPIRADA.equals(estado);
    }

    // Libera el asiento cuando se agotan los 4 minutos
    public void expirar() {
        if (BLOQUEADA.equals(estado)) {
            this.estado = EXPIRADA;
            this.expiraBloqueo = null;
            this.funcion.actualizarEstado();
        }
    }

    public double calculaPrecioFinal() {
        if (tipoEntrada == null) {
            this.precio = 0.0;
            return 0.0;
        }
        this.precio = tipoEntrada.getTarifa() + funcion.getSala().getTipoSala().obtenerRecargoFormato();
        return this.precio;
    }

    public void confirmarVenta() {
        if (!BLOQUEADA.equals(estado)) {
            throw new IllegalStateException("Solo se puede confirmar una entrada BLOQUEADA (estado actual: " + estado + ")");
        }
        if (expiraBloqueo == null) {
            throw new IllegalStateException("Primero se debe bloquear la entrada confirmando la selección de asientos");
        }
        if (haExpirado()) {
            throw new IllegalStateException("El bloqueo de la entrada expiró");
        }
        if (tipoEntrada == null) {
            throw new IllegalStateException("Debe asignar un tipo de entrada antes de confirmar la venta");
        }
        this.estado = VENDIDA;
        this.fechaEmision = DateTime.ahora();
        this.expiraBloqueo = null;
        this.codigoQR = generarCodigoQR();
    }

    public void anular() {
        if (!BLOQUEADA.equals(estado) && !VENDIDA.equals(estado)) {
            throw new IllegalStateException("No se puede anular una entrada en estado " + estado);
        }
        this.estado = ANULADA;
        this.expiraBloqueo = null;
        this.funcion.actualizarEstado();
    }

    public String generarCodigoQR() {
        String base = "QR-ENT-" + idEntrada + "-F" + funcion.getIdFuncion() + "-" + butaca.getCodigoAsiento();
        return (fechaEmision != null) ? base + "-" + fechaEmision.toValorComparable() : base;
    }

    public int getIdEntrada() { return idEntrada; }
    public String getCodigoQR() { return codigoQR; }
    public double getPrecio() { return precio; }
    public DateTime getFechaEmision() { return fechaEmision; }
    public DateTime getExpiraBloqueo() { return expiraBloqueo; }
    public String getEstado() { return estado; }
    public Funcion getFuncion() { return funcion; }
    public Butaca getButaca() { return butaca; }
    public TipoEntrada getTipoEntrada() { return tipoEntrada; }

    @Override
    public String toString() {
        return funcion.getPelicula() + " | " + butaca.getCodigoAsiento() + " | " + estado;
    }
}