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

    private int idEntrada;
    private String codigoQR;
    private double precio;
    private DateTime fechaEmision;
    private DateTime expiraBloqueo;
    private String estado;
    private Funcion funcion;
    private Butaca butaca;
    private TipoEntrada tipoEntrada;

    public Entrada(int idEntrada, Funcion funcion, Butaca butaca, TipoEntrada tipoEntrada) {
        if (funcion == null) throw new IllegalArgumentException("La función es obligatoria");
        if (butaca == null) throw new IllegalArgumentException("La butaca es obligatoria");
        if (tipoEntrada == null) throw new IllegalArgumentException("El tipo de entrada es obligatorio");
        // Regla del trigger trg_entrada_coherencia_sala (se compara por referencia, no por id)
        if (butaca.getSala() != funcion.getSala()) {
            throw new IllegalArgumentException("La butaca no pertenece a la sala de la función");
        }
        this.idEntrada = idEntrada;
        this.funcion = funcion;
        this.butaca = butaca;
        this.tipoEntrada = tipoEntrada;
        this.estado = BLOQUEADA;
        calculaPrecioFinal();
    }

    // Bloquea la butaca por "minutos" y registra la entrada en la función.
    // Devuelve false si la función no está disponible o la butaca ya está ocupada.
    public boolean bloquearTemporalmente(int minutos) {
        if (minutos <= 0) {
            throw new IllegalArgumentException("Los minutos deben ser mayores a 0");
        }
        if (!BLOQUEADA.equals(estado) || expiraBloqueo != null) {
            throw new IllegalStateException("La entrada ya fue bloqueada o no está en estado BLOQUEADA");
        }
        if (!funcion.estaDisponible() || !funcion.esButacaDisponible(butaca)) {
            return false;
        }
        this.expiraBloqueo = DateTime.ahora().sumarMinutos(minutos);
        funcion.registrarEntrada(this);
        return true;
    }

    // Si el bloqueo venció, pasa a EXPIRADA y libera la butaca
    public boolean haExpirado() {
        if (BLOQUEADA.equals(estado) && expiraBloqueo != null
                && DateTime.ahora().esDespuesDe(expiraBloqueo)) {
            this.estado = EXPIRADA;
        }
        return EXPIRADA.equals(estado);
    }

    // Precio = tarifa del tipo de entrada + recargo del formato de sala
    public double calculaPrecioFinal() {
        this.precio = tipoEntrada.getTarifa() + funcion.getSala().getTipoSala().obtenerRecargoFormato();
        return this.precio;
    }

    // Solo se confirma una entrada BLOQUEADA, ya registrada en la función y con el bloqueo vigente
    public void confirmarVenta() {
        if (!BLOQUEADA.equals(estado)) {
            throw new IllegalStateException("Solo se puede confirmar una entrada BLOQUEADA (estado actual: " + estado + ")");
        }
        if (expiraBloqueo == null) {
            throw new IllegalStateException("Primero se debe bloquear la entrada");
        }
        if (haExpirado()) {
            throw new IllegalStateException("El bloqueo de la entrada expiró");
        }
        this.estado = VENDIDA;
        this.fechaEmision = DateTime.ahora();
        this.expiraBloqueo = null;
        this.codigoQR = generarCodigoQR();
    }

    // Se puede anular una entrada BLOQUEADA o VENDIDA; libera la butaca
    public void anular() {
        if (!BLOQUEADA.equals(estado) && !VENDIDA.equals(estado)) {
            throw new IllegalStateException("No se puede anular una entrada en estado " + estado);
        }
        this.estado = ANULADA;
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