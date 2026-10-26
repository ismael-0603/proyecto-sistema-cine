package ucsur.pe.cineobjects;
import ucsur.pe.cineInterfaz.Registrable;

/**
 * Por qué existe: registra un reclamo, queja o pedido de reembolso sobre una compra
 * y su atención hasta que se cierra.
 */
public class Reclamo implements Registrable {
    // Tipos de reclamo (constantes: se usan como Reclamo.NOMBRE y su valor no cambia).
    public static final String RECLAMO = "RECLAMO"; // problema con el servicio o la compra
    public static final String QUEJA = "QUEJA"; // malestar o inconformidad con la atención
    public static final String REEMBOLSO = "REEMBOLSO"; // pide que se devuelva el dinero

    // Estados de un reclamo (constantes: se usan como Reclamo.NOMBRE y su valor no cambia).
    public static final String BORRADOR = "BORRADOR"; // solo en memoria hasta registrar(); no existe en la BD
    public static final String REGISTRADO = "REGISTRADO"; // formalizado, esperando revisión
    public static final String EN_REVISION = "EN_REVISION"; // el cine lo está atendiendo
    public static final String RESUELTO = "RESUELTO"; // cerrado con una respuesta
    public static final String RECHAZADO = "RECHAZADO"; // cerrado sin aceptarse, con un motivo

    private int idReclamo;
    private String tipo;
    private String descripcion;
    private DateTime fecha;
    private String estado;
    private String respuesta; // texto con el que se cierra; si se rechaza, es el motivo
    private Compra compra;
    private boolean registrado; // evita registrar el mismo reclamo dos veces

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
        this.estado = BORRADOR;
        this.compra = compra;
        this.registrado = false;
    }

    /** Formaliza el reclamo una sola vez y solo para compras confirmadas o reembolsadas. */
    @Override
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

    /** Pasa de REGISTRADO a EN_REVISION. */
    public void iniciarRevision() {
        validarAbierto();
        if (!REGISTRADO.equals(estado)) {
            throw new IllegalStateException("Solo se puede revisar un reclamo REGISTRADO (estado actual: " + estado + ")");
        }
        this.estado = EN_REVISION;
    }

    /** Cierra el reclamo como resuelto y guarda la respuesta no vacía. */
    public void responderReclamo(String respuesta) {
        validarAbierto();
        validarRespuesta(respuesta);
        this.respuesta = respuesta.trim();
        this.estado = RESUELTO;
    }

    /** Cierra el reclamo como rechazado y guarda el motivo indicado. */
    public void rechazarReclamo(String motivo) {
        validarAbierto();
        validarRespuesta(motivo);
        this.respuesta = motivo.trim();
        this.estado = RECHAZADO;
    }

    /** Indica si el reclamo pide un reembolso. */
    public boolean esReembolso() {
        return REEMBOLSO.equals(this.tipo);
    }

    /** Impide revisar o responder reclamos que no se registraron o que ya fueron cerrados. */
    private void validarAbierto() {
        if (!registrado) {
            throw new IllegalStateException("El reclamo aún no fue registrado");
        }
        if (RESUELTO.equals(estado) || RECHAZADO.equals(estado)) {
            throw new IllegalStateException("El reclamo ya fue cerrado (" + estado + ")");
        }
    }

    /** Evita cerrar el reclamo sin explicar la respuesta o el motivo. */
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
