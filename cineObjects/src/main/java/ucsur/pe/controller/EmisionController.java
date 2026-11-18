package ucsur.pe.controller;

import ucsur.pe.model.Compra;
import ucsur.pe.model.DetalleCompra;
import ucsur.pe.service.EmisionService;

/** Acciones del comprobante: ver el detalle, imprimir, enviar por correo y recuperar entradas (RF-20 a RF-22, RF-33). */
public class EmisionController extends ControllerBase {
    private final EmisionService servicio;

    public EmisionController(EmisionService servicio) {
        this.servicio = servicio;
    }

    public Respuesta<DetalleCompra> verDetalle(Compra compra) {
        return ejecutar("", () -> servicio.verDetalle(compra));
    }

    public Respuesta<String> imprimir(Compra compra) {
        return ejecutar("Imprimiendo tus entradas", () -> servicio.imprimir(compra));
    }

    public Respuesta<Void> enviarPorCorreo(Compra compra, String destinatario) {
        return ejecutar("Te enviamos tus entradas por correo", () -> servicio.enviarPorCorreo(compra, destinatario));
    }

    /** Recuperar entradas en el kiosco: código del comprobante y correo con el que se compró. */
    public Respuesta<DetalleCompra> recuperarPorCodigo(String serieNumero, String correoComprador) {
        return ejecutar("", () -> servicio.recuperarPorCodigo(serieNumero, correoComprador));
    }

    public Respuesta<Integer> recuperarPorCorreo(String correo) {
        Respuesta<Integer> r = ejecutar("", () -> servicio.recuperarPorCorreo(correo));
        return r.ok() ? Respuesta.exito("Enviamos " + r.dato() + " compra(s) a " + correo, r.dato()) : r;
    }
}
