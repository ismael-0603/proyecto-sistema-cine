package ucsur.pe.service;

import ucsur.pe.correo.ServicioCorreo;
import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.DetalleCompra;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.TransaccionPago;
import ucsur.pe.repository.CompraRepository;
import ucsur.pe.repository.PagoRepository;
import java.util.List;
import java.util.Optional;

/**
 * Por qué existe: casos de uso de emisión (RF-20 a RF-22 y RF-33): ver el detalle de una compra pagada
 * (pantalla de comprobante o «Ver» en Mis compras), imprimirlo, enviarlo por correo y recuperar entradas
 * con el código de compra o el correo.
 */
public class EmisionService {
    public static final int MAX_INTENTOS_FALLIDOS = 5; // intentos seguidos de recuperar por código antes de bloquear
    public static final int MINUTOS_BLOQUEO = 5;       // cuánto dura el bloqueo

    private final CompraRepository compras;
    private final PagoRepository pagos; // para mostrar con qué medio se pagó
    private final ServicioCorreo correo;
    private final Reloj reloj; // de dónde sale la hora actual (el real o uno fijo en pruebas)

    // Cada kiosco tiene su propio EmisionService, así que el conteo es por kiosco.
    private int intentosFallidos;
    private DateTime bloqueadoHasta; // null si no está bloqueado

    public EmisionService(CompraRepository compras, PagoRepository pagos, ServicioCorreo correo, Reloj reloj) {
        if (reloj == null) throw new IllegalArgumentException("El reloj es obligatorio");
        this.compras = compras;
        this.pagos = pagos;
        this.correo = correo;
        this.reloj = reloj;
    }

    /** Resumen con el QR y el medio de pago: comprobante (wireframe 9) y «Ver» en Mis compras (wireframe 23). */
    public DetalleCompra verDetalle(Compra compra) {
        if (compra == null) throw new IllegalArgumentException("La compra es obligatoria");
        String medio = pagos.listarPorCompra(compra).stream()
                .filter(p -> TransaccionPago.APROBADO.equals(p.getEstadoPago()))
                .map(TransaccionPago::getMedioPago)
                .findFirst()
                .orElse(null);
        return compra.obtenerDetalle(medio);
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

    /**
     * RF-33: con el código del comprobante (B001-000123) y el correo del comprador se vuelve a mostrar
     * la compra y su QR. La serie es correlativa (así la pide SUNAT) y se puede adivinar, por eso no basta sola.
     * Si no existe la serie o el correo no coincide, el mensaje es el mismo: así no se revela qué series existen.
     * Tras MAX_INTENTOS_FALLIDOS seguidos, el kiosco bloquea la recuperación por MINUTOS_BLOQUEO minutos.
     */
    public DetalleCompra recuperarPorCodigo(String serieNumero, String correoComprador) {
        DateTime ahora = reloj.ahora();
        if (bloqueadoHasta != null && ahora.esAntesDe(bloqueadoHasta)) {
            throw new ReglaNegocioException("Demasiados intentos fallidos; vuelve a intentarlo después de las "
                    + bloqueadoHasta.soloHora());
        }
        Optional<Compra> compra = compras.buscarPorSerie(serieNumero)
                .filter(c -> correoComprador != null && c.getCliente().getCorreo() != null
                        && c.getCliente().getCorreo().equalsIgnoreCase(correoComprador.trim()));
        if (compra.isEmpty()) {
            registrarIntentoFallido(ahora);
            throw new ReglaNegocioException("No se encontró una compra con ese código y correo");
        }
        intentosFallidos = 0;
        bloqueadoHasta = null;
        return verDetalle(compra.get());
    }

    /** Cuenta un intento fallido; al llegar al máximo bloquea la recuperación y reinicia la cuenta. */
    private void registrarIntentoFallido(DateTime ahora) {
        intentosFallidos++;
        if (intentosFallidos >= MAX_INTENTOS_FALLIDOS) {
            bloqueadoHasta = ahora.sumarMinutos(MINUTOS_BLOQUEO);
            intentosFallidos = 0;
        }
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
