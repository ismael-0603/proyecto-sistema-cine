package ucsur.pe.controller;

import ucsur.pe.model.Compra;
import ucsur.pe.model.DetalleCompra;
import ucsur.pe.service.EmisionService;
import java.util.List;

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

    /** Paso 1 de recuperación (RF-33): envía un código de 6 dígitos al correo si tiene compras confirmadas. */
    public Respuesta<Void> solicitarCodigoRecuperacion(String correo) {
        Respuesta<String> r = ejecutar("", () -> servicio.solicitarCodigoRecuperacion(correo));
        return r.ok() ? Respuesta.exito(r.dato(), null) : Respuesta.error(r.mensaje());
    }

    /** Paso 2 de recuperación (RF-33): verifica el código y devuelve las compras confirmadas del correo. */
    public Respuesta<List<Compra>> verificarCodigoRecuperacion(String correo, String codigo) {
        return ejecutar("", () -> servicio.verificarCodigoRecuperacion(correo, codigo));
    }
}
