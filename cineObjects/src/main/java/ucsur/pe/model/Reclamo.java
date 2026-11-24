package ucsur.pe.model;

import java.util.List;

/**
 * Por qué existe: registra un reclamo sobre una compra (atención, dulcería o sala) y su atención
 * hasta que se cierra. No hay reclamos de reembolso: si el cine cancela una función,
 * el dinero se devuelve automáticamente.
 */
public class Reclamo {
    // Tipos de reclamo (constantes: se usan como Reclamo.NOMBRE y su valor no cambia).
    public static final String ATENCION = "ATENCION"; // trato del personal o del servicio
    public static final String PRODUCTO_DULCERIA = "PRODUCTO_DULCERIA"; // producto incompleto, frío o equivocado
    public static final String SALA_PROYECCION = "SALA_PROYECCION"; // imagen, sonido, limpieza o butaca de la sala
    public static final List<String> TIPOS = List.of(ATENCION, PRODUCTO_DULCERIA, SALA_PROYECCION);
    public static final int MAX_DESCRIPCION = 500; // tope de texto, como Calificacion.MAX_COMENTARIO
    public static final int MAX_RESPUESTA = 500;

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

    public Reclamo(int idReclamo, String tipo, String descripcion, Compra compra) {
        if (!TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de reclamo inválido: " + tipo);
        }
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción es obligatoria");
        }
        if (descripcion.trim().length() > MAX_DESCRIPCION) {
            throw new IllegalArgumentException("La descripción no puede superar " + MAX_DESCRIPCION + " caracteres");
        }
        if (compra == null) {
            throw new IllegalArgumentException("La compra es obligatoria");
        }
        this.idReclamo = idReclamo;
        this.tipo = tipo;
        this.descripcion = descripcion.trim();
        this.fecha = compra.getReloj().ahora(); // el mismo reloj de la compra reclamada
        this.estado = BORRADOR;
        this.compra = compra;
    }

    /**
     * Solo para la capa de base de datos (ucsur.pe.db): reconstruye un reclamo ya guardado con su estado
     * y su respuesta. En la BD no existe BORRADOR: un reclamo guardado ya fue registrado.
     */
    public static Reclamo restaurar(int idReclamo, String tipo, String descripcion, DateTime fecha,
                                    String estado, String respuesta, Compra compra) {
        Reclamo reclamo = new Reclamo(idReclamo, tipo, descripcion, compra); // valida tipo, descripción y compra
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha del reclamo es obligatoria");
        }
        if (!REGISTRADO.equals(estado) && !EN_REVISION.equals(estado)
                && !RESUELTO.equals(estado) && !RECHAZADO.equals(estado)) {
            throw new IllegalArgumentException("Estado de reclamo inválido: " + estado);
        }
        reclamo.fecha = fecha;
        reclamo.estado = estado;
        reclamo.respuesta = respuesta;
        return reclamo;
    }

    /** Asigna el id real generado por la base tras el INSERT. */
    public void asignarId(int idReclamo) {
        this.idReclamo = Ids.asignar(this.idReclamo, idReclamo, "el reclamo");
    }

    /** Formaliza el reclamo una sola vez y solo para compras confirmadas o reembolsadas. */
    public void registrar() {
        if (!BORRADOR.equals(estado)) {
            throw new IllegalStateException("El reclamo ya fue registrado");
        }
        if (!Compra.CONFIRMADA.equals(compra.getEstado()) && !Compra.REEMBOLSADA.equals(compra.getEstado())) {
            throw new IllegalStateException("Solo se reclama sobre una compra confirmada o reembolsada (estado: "
                    + compra.getEstado() + ")");
        }
        this.fecha = compra.getReloj().ahora();
        this.estado = REGISTRADO;
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
        exigirEnRevision();
        validarRespuesta(respuesta);
        this.respuesta = respuesta.trim();
        this.estado = RESUELTO;
    }

    /** Cierra el reclamo como rechazado y guarda el motivo indicado. */
    public void rechazarReclamo(String motivo) {
        exigirEnRevision();
        validarRespuesta(motivo);
        this.respuesta = motivo.trim();
        this.estado = RECHAZADO;
    }

    /** Un reclamo se cierra (respondido o rechazado) solo después de pasar por EN_REVISION. */
    private void exigirEnRevision() {
        validarAbierto();
        if (!EN_REVISION.equals(estado)) {
            throw new IllegalStateException("Primero se debe iniciar la revisión del reclamo (estado actual: " + estado + ")");
        }
    }

    /** Impide revisar o responder reclamos que no se registraron o que ya fueron cerrados. */
    private void validarAbierto() {
        if (BORRADOR.equals(estado)) {
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
        if (texto.trim().length() > MAX_RESPUESTA) {
            throw new IllegalArgumentException("La respuesta no puede superar " + MAX_RESPUESTA + " caracteres");
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
