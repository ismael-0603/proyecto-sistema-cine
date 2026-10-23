package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Reclamo {
    public static final String RECLAMO = "RECLAMO";
    public static final String QUEJA = "QUEJA";
    public static final String REEMBOLSO = "REEMBOLSO";

    public static final String REGISTRADO = "REGISTRADO";
    public static final String EN_REVISION = "EN_REVISION";
    public static final String RESUELTO = "RESUELTO";
    public static final String RECHAZADO = "RECHAZADO";

    private int idReclamo;
    private String tipo;
    private String descripcion;
    private DateTime fecha;
    private String estado;
    private String respuesta;
    private Compra compra;
    private boolean registrado;

    public Reclamo(int idReclamo, String tipo, String descripcion, Compra compra) {
        if (!RECLAMO.equals(tipo) && !QUEJA.equals(tipo) && !REEMBOLSO.equals(tipo)) {
            throw new IllegalArgumentException("Tipo de reclamo inválido: " + tipo);
        }
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción es obligatoria");
        }
        if (compra == null) {
            throw new IllegalArgumentException("La compra es obligatoria");
        }
        this.idReclamo = idReclamo;
        this.tipo = tipo;
        this.descripcion = descripcion.trim();
        this.fecha = DateTime.ahora();
        this.estado = REGISTRADO;
        this.compra = compra;
        this.registrado = false;
    }

    // Formaliza el reclamo. Solo procede si la compra fue confirmada (o reembolsada) y solo una vez.
    public boolean registrar() {
        if (registrado) return false;
        if (!Compra.CONFIRMADA.equals(compra.getEstado()) && !Compra.REEMBOLSADA.equals(compra.getEstado())) {
            return false;
        }
        this.fecha = DateTime.ahora();
        this.estado = REGISTRADO;
        this.registrado = true;
        return true;
    }

    public String consultarEstado() {
        return this.estado;
    }

    public void iniciarRevision() {
        validarAbierto();
        if (!REGISTRADO.equals(estado)) {
            throw new IllegalStateException("Solo se puede revisar un reclamo REGISTRADO (estado actual: " + estado + ")");
        }
        this.estado = EN_REVISION;
    }

    public void responderReclamo(String respuesta) {
        validarAbierto();
        validarRespuesta(respuesta);
        this.respuesta = respuesta.trim();
        this.estado = RESUELTO;
    }

    public void rechazarReclamo(String motivo) {
        validarAbierto();
        validarRespuesta(motivo);
        this.respuesta = motivo.trim();
        this.estado = RECHAZADO;
    }

    public boolean esReembolso() {
        return REEMBOLSO.equals(this.tipo);
    }

    // Debe estar registrado y aún sin cerrar (ni RESUELTO ni RECHAZADO)
    private void validarAbierto() {
        if (!registrado) {
            throw new IllegalStateException("El reclamo aún no fue registrado");
        }
        if (RESUELTO.equals(estado) || RECHAZADO.equals(estado)) {
            throw new IllegalStateException("El reclamo ya fue cerrado (" + estado + ")");
        }
    }

    private void validarRespuesta(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException("La respuesta es obligatoria");
        }
    }

    public int getIdReclamo() { return idReclamo; }
    public String getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public DateTime getFecha() { return fecha; }
    public String getEstado() { return estado; }
    public String getRespuesta() { return respuesta; }
    public Compra getCompra() { return compra; }

    @Override
    public String toString() {
        return tipo + " #" + idReclamo + " [" + estado + "]";
    }
}