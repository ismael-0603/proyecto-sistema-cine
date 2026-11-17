package ucsur.pe.service;

import ucsur.pe.correo.ServicioCorreo;
import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DetalleCompra;
import ucsur.pe.repository.CompraRepository;
import java.util.List;

/**
 * Por qué existe: casos de uso de emisión (RF-20 a RF-22 y RF-33): ver el detalle de una compra pagada
 * (pantalla de comprobante o «Ver» en Mis compras), imprimirlo, enviarlo por correo y recuperar entradas
 * con el código de compra o el correo.
 */
public class EmisionService {
    private final CompraRepository compras;
    private final ServicioCorreo correo;

    public EmisionService(CompraRepository compras, ServicioCorreo correo) {
        this.compras = compras;
        this.correo = correo;
    }

    /** Resumen con el QR que se muestra al tocar «Ver» en Mis compras. */
    public DetalleCompra verDetalle(Compra compra) {
        if (compra == null) throw new IllegalArgumentException("La compra es obligatoria");
        return compra.obtenerDetalle();
    }

    /** Texto del ticket que se manda a la impresora del kiosco (RF-21); la impresión real es de la vista. */
    public String imprimir(Compra compra) {
        return verDetalle(compra).generarTexto();
    }

    /**
     * Envía el comprobante con los QR al correo indicado o, si viene vacío, al del cliente (RF-22).
     * Lo usan el botón «Enviar al correo» de la confirmación y el de «Ver» en Mis compras.
     */
    public void enviarPorCorreo(Compra compra, String destinatario) {
        DetalleCompra detalle = verDetalle(compra);
        String para = (destinatario == null || destinatario.isBlank()) ? compra.getCliente().getCorreo() : destinatario.trim();
        if (!correo.enviar(para, "Tu compra " + detalle.getSerieNumero(), detalle.generarTexto())) {
            throw new ReglaNegocioException("No se pudo enviar el correo a " + para);
        }
    }

    /** RF-33: con el código del comprobante (B001-000123) se vuelve a mostrar la compra y su QR. */
    public DetalleCompra recuperarPorCodigo(String serieNumero) {
        Compra compra = compras.buscarPorSerie(serieNumero)
                .orElseThrow(() -> new ReglaNegocioException("No existe una compra con el código " + serieNumero));
        return verDetalle(compra);
    }

    /**
     * RF-33: con el correo no se muestran las entradas en el kiosco (cualquiera podría escribir un correo ajeno);
     * se reenvían a ese correo las compras confirmadas. Devuelve cuántas se enviaron.
     */
    public int recuperarPorCorreo(String correoCliente) {
        List<Compra> confirmadas = compras.listarPorCorreo(correoCliente).stream()
                .filter(c -> Compra.CONFIRMADA.equals(c.getEstado()))
                .toList();
        if (confirmadas.isEmpty()) {
            throw new ReglaNegocioException("No hay compras confirmadas con el correo " + correoCliente);
        }
        for (Compra c : confirmadas) {
            enviarPorCorreo(c, correoCliente);
        }
        return confirmadas.size();
    }
}
