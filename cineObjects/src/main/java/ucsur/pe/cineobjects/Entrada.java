package ucsur.pe.cineobjects;

/**
 * Representa una entrada para una butaca y función, desde su selección hasta su venta o liberación.
 * La función y la butaca son obligatorias, y la butaca debe pertenecer a la sala de esa función.
 */
public class Entrada {
    public static final String BLOQUEADA = "BLOQUEADA";
    public static final String VENDIDA = "VENDIDA";
    public static final String EXPIRADA = "EXPIRADA";
    public static final String ANULADA = "ANULADA";

    public static final int TIEMPO_BLOQUEO_MIN = 4; // máximo de minutos que una butaca puede quedar bloqueada

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

    /** Bloquea la butaca hasta la hora límite compartida con la compra. */
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

    /** Bloquea la butaca durante la cantidad de minutos indicada, hasta un máximo de cuatro. */
    public boolean bloquearTemporalmente(int minutos) {
        if (minutos <= 0 || minutos > TIEMPO_BLOQUEO_MIN) {
            throw new IllegalArgumentException("El tiempo de bloqueo debe estar entre 1 y " + TIEMPO_BLOQUEO_MIN + " minutos");
        }
        return bloquearTemporalmente(DateTime.ahora().sumarMinutos(minutos));
    }

    /** Comprueba si venció el bloqueo y, en ese caso, expira la entrada y libera la butaca. */
    public boolean haExpirado() {
        if (BLOQUEADA.equals(estado) && expiraBloqueo != null
                && DateTime.ahora().esDespuesDe(expiraBloqueo)) {
            expirar();
        }
        return EXPIRADA.equals(estado);
    }

    /** Marca la entrada como expirada y quita su bloqueo de la función. */
    public void expirar() {
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

    /** Cambia el bloqueo vigente a venta y crea el código QR de la entrada. */
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

    /** Anula una entrada bloqueada o vendida y actualiza la disponibilidad de la función. */
    public void anular() {
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
    public void liberarBloqueo() {
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
