package ucsur.pe.cineobjects;

/**
 * Por qué existe: es el boleto de una butaca para una función.
 * Pasa de BLOQUEADA a VENDIDA, o se libera como EXPIRADA o ANULADA.
 */
public class Entrada {
    // Estados de una entrada (constantes: se usan como Entrada.NOMBRE y su valor no cambia).
    public static final String BLOQUEADA = "BLOQUEADA"; // butaca reservada temporalmente, aún sin pagar
    public static final String VENDIDA = "VENDIDA"; // pagada; ya tiene código QR
    public static final String EXPIRADA = "EXPIRADA"; // el bloqueo venció sin pagar; la butaca se libera
    public static final String ANULADA = "ANULADA"; // cancelada o reembolsada; la butaca se libera

    private int idEntrada;
    private String codigoQR; // se crea al confirmar la venta
    private double precio; // tarifa del tipo de entrada más el recargo del formato de sala
    private DateTime fechaEmision;
    private DateTime expiraBloqueo; // Momento en que debe liberarse el asiento si no se confirma la venta.
    private String estado;
    private Funcion funcion;
    private Butaca butaca;
    private TipoEntrada tipoEntrada;

    /** Crea una preselección sin tipo de entrada; todavía no bloquea la butaca en la función. */
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

    /** Crea una entrada con su tipo, calculando el precio base y validando sus asociaciones. */
    public Entrada(int idEntrada, Funcion funcion, Butaca butaca, TipoEntrada tipoEntrada) {
        this(idEntrada, funcion, butaca);
        asignarTipoEntrada(tipoEntrada);
    }

    /** Asigna un tipo válido y calcula el precio final según tarifa y recargo de sala. */
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

    /** Asigna el id real generado por la base tras el INSERT; el QR se crea después, al confirmar la venta. */
    public void asignarId(int idEntrada) {
        if (this.idEntrada > 0) {
            throw new IllegalStateException("La entrada ya tiene id asignado");
        }
        if (idEntrada <= 0) {
            throw new IllegalArgumentException("El id de la entrada debe ser mayor a 0");
        }
        this.idEntrada = idEntrada;
    }

    /** Bloquea la butaca hasta la hora límite compartida con la compra; solo la usa SeleccionAsientos. */
    boolean bloquearTemporalmente(DateTime ahora, DateTime fechaExpiracion) {
        if (fechaExpiracion == null || !ahora.esAntesDe(fechaExpiracion)) {
            return false; // El tiempo de la compra ya venció
        }
        if (!BLOQUEADA.equals(estado) || expiraBloqueo != null) {
            throw new IllegalStateException("La entrada ya fue bloqueada o no está en estado BLOQUEADA");
        }
        // Si la función no está disponible o la butaca ya está ocupada, Funcion.registrarEntrada lo rechaza.
        this.expiraBloqueo = fechaExpiracion;
        try {
            funcion.registrarEntrada(this);
        } catch (RuntimeException ex) {
            this.expiraBloqueo = null; // si la función la rechaza, la entrada vuelve a ser solo una preselección
            throw ex;
        }
        return true;
    }

    /** Indica, sin cambiar nada, si la entrada está bloqueada y su bloqueo ya venció en el instante indicado. */
    public boolean bloqueoVencido(DateTime ahora) {
        return BLOQUEADA.equals(estado) && expiraBloqueo != null && ahora.esDespuesDe(expiraBloqueo);
    }

    /** Comprueba si venció el bloqueo y, en ese caso, expira la entrada y libera la butaca. */
    boolean haExpirado(DateTime ahora) {
        if (bloqueoVencido(ahora)) {
            expirar();
        }
        return EXPIRADA.equals(estado);
    }

    /** Marca la entrada como expirada y quita su bloqueo de la función. */
    void expirar() {
        if (BLOQUEADA.equals(estado)) {
            this.estado = EXPIRADA;
            this.expiraBloqueo = null;
            this.funcion.actualizarEstado();
        }
    }

    /** Suma la tarifa de entrada y el recargo por formato de sala; sin tipo, el precio es cero. */
    public double calculaPrecioFinal() {
        if (tipoEntrada == null) {
            this.precio = 0.0;
            return 0.0;
        }
        this.precio = tipoEntrada.getTarifa() + funcion.getSala().getTipoSala().obtenerRecargoFormato();
        return this.precio;
    }

    /** Cambia el bloqueo vigente a venta y crea el código QR; solo se vende al confirmar la compra pagada. */
    void confirmarVenta(DateTime ahora) {
        if (!BLOQUEADA.equals(estado)) {
            throw new IllegalStateException("Solo se puede confirmar una entrada BLOQUEADA (estado actual: " + estado + ")");
        }
        if (expiraBloqueo == null) {
            throw new IllegalStateException("Primero se debe bloquear la entrada confirmando la selección de asientos");
        }
        if (bloqueoVencido(ahora)) { // se usa el instante del pago, no se vuelve a consultar el reloj
            throw new IllegalStateException("El bloqueo de la entrada expiró");
        }
        if (tipoEntrada == null) {
            throw new IllegalStateException("Debe asignar un tipo de entrada antes de confirmar la venta");
        }
        this.estado = VENDIDA;
        this.fechaEmision = ahora;
        this.expiraBloqueo = null;
        this.codigoQR = generarCodigoQR();
    }

    /** Anula una entrada bloqueada o vendida; solo al cancelar o reembolsar la compra. */
    void anular() {
        if (!BLOQUEADA.equals(estado) && !VENDIDA.equals(estado)) {
            throw new IllegalStateException("No se puede anular una entrada en estado " + estado);
        }
        this.estado = ANULADA;
        this.expiraBloqueo = null;
        this.funcion.actualizarEstado();
    }

    /** Forma un código único con la entrada, la función, el asiento y, si existe, la fecha de venta. */
    public String generarCodigoQR() {
        String base = "QR-ENT-" + idEntrada + "-F" + funcion.getIdFuncion() + "-" + butaca.getCodigoAsiento();
        return (fechaEmision != null) ? base + "-" + fechaEmision.toValorComparable() : base;
    }
    
    /** Deshace el bloqueo y devuelve la entrada a la preselección. */
    void liberarBloqueo() {
        if (BLOQUEADA.equals(estado) && expiraBloqueo != null) {
            this.expiraBloqueo = null;
            funcion.liberarEntrada(this);
        }
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
