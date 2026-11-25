package ucsur.pe.service;

import ucsur.pe.correo.ServicioCorreo;
import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.DetalleCompra;
import ucsur.pe.model.Persona;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.TransaccionPago;
import ucsur.pe.repository.CompraRepository;
import ucsur.pe.repository.PagoRepository;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Por qué existe: casos de uso de emisión (RF-20 a RF-22 y RF-33): ver el detalle de una compra pagada
 * (pantalla de comprobante o «Ver» en Mis compras), imprimirlo, enviarlo por correo y recuperar entradas
 * verificando la titularidad del correo con un código temporal de 6 dígitos.
 */
public class EmisionService {
    public static final int MINUTOS_VIGENCIA_CODIGO = 10; // el código de recuperación vence a los 10 minutos
    public static final int MAX_INTENTOS_CODIGO = 5;      // tras 5 códigos equivocados hay que pedir otro
    /** Mismo mensaje haya o no compras: así no se revela qué correos compraron. */
    public static final String MENSAJE_CODIGO_ENVIADO =
            "Si hay compras confirmadas con ese correo, te enviamos un código de 6 dígitos";

    private final CompraRepository compras;
    private final PagoRepository pagos; // para mostrar con qué medio se pagó
    private final ServicioCorreo correo;
    private final Reloj reloj; // de dónde sale la hora actual (el real o uno fijo en pruebas)
    private final SecureRandom azar = new SecureRandom();
    /** Códigos pendientes por correo (en minúsculas). Viven en memoria: si el kiosco se reinicia, se pide otro. */
    private final Map<String, CodigoRecuperacion> codigos = new HashMap<>();

    private static final class CodigoRecuperacion {
        private final String codigo;
        private final DateTime vence;
        private int intentosFallidos;

        private CodigoRecuperacion(String codigo, DateTime vence) {
            this.codigo = codigo;
            this.vence = vence;
        }
    }

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
     * RF-33 (paso 1): si el correo tiene compras confirmadas, envía un código temporal de 6 dígitos que vence
     * en MINUTOS_VIGENCIA_CODIGO minutos. Siempre responde lo mismo para no revelar qué correos tienen compras.
     */
    public String solicitarCodigoRecuperacion(String correoCliente) {
        String error = Persona.errorCorreo(correoCliente);
        if (error != null) {
            throw new ReglaNegocioException(error);
        }
        String limpio = correoCliente.trim();
        List<Compra> confirmadas = comprasConfirmadasDe(limpio);
        if (!confirmadas.isEmpty()) {
            String codigo = String.format("%06d", azar.nextInt(1_000_000));
            codigos.put(clave(limpio),
                    new CodigoRecuperacion(codigo, reloj.ahora().sumarMinutos(MINUTOS_VIGENCIA_CODIGO)));
            String cuerpo = "Tu código para ver y recuperar tus entradas es " + codigo
                    + ". Vence en " + MINUTOS_VIGENCIA_CODIGO + " minutos.\n"
                    + "Si no lo pediste, ignora este mensaje.";
            if (!correo.enviar(limpio, "Código para recuperar tus entradas", cuerpo)) {
                codigos.remove(clave(limpio));
                throw new ReglaNegocioException("No se pudo enviar el código; inténtalo de nuevo");
            }
        }
        return MENSAJE_CODIGO_ENVIADO;
    }

    /**
     * RF-33 (paso 2): valida el código de 6 dígitos enviado al correo y devuelve sus compras confirmadas
     * (de la más reciente a la más antigua). El código es de un solo uso y se anula tras MAX_INTENTOS_CODIGO fallos.
     */
    public List<Compra> verificarCodigoRecuperacion(String correoCliente, String codigo) {
        String error = Persona.errorCorreo(correoCliente);
        if (error != null) {
            throw new ReglaNegocioException(error);
        }
        String limpio = correoCliente.trim();
        String key = clave(limpio);
        CodigoRecuperacion pendiente = codigos.get(key);
        if (pendiente == null) {
            throw new ReglaNegocioException("El código no es válido; solicita uno nuevo");
        }
        if (reloj.ahora().esDespuesDe(pendiente.vence)) {
            codigos.remove(key);
            throw new ReglaNegocioException("El código venció; solicita uno nuevo");
        }
        if (codigo == null || !pendiente.codigo.equals(codigo.trim())) {
            pendiente.intentosFallidos++;
            if (pendiente.intentosFallidos >= MAX_INTENTOS_CODIGO) {
                codigos.remove(key);
                throw new ReglaNegocioException("Demasiados intentos con un código incorrecto; solicita uno nuevo");
            }
            throw new ReglaNegocioException("El código no es correcto (te quedan "
                    + (MAX_INTENTOS_CODIGO - pendiente.intentosFallidos) + " intentos)");
        }
        codigos.remove(key);
        List<Compra> confirmadas = comprasConfirmadasDe(limpio);
        if (confirmadas.isEmpty()) {
            throw new ReglaNegocioException("No hay compras confirmadas con ese correo");
        }
        return confirmadas;
    }

    private List<Compra> comprasConfirmadasDe(String correoCliente) {
        return compras.listarPorCorreo(correoCliente).stream()
                .filter(c -> Compra.CONFIRMADA.equals(c.getEstado()))
                .sorted(Comparator.comparingLong((Compra c) -> c.getFechaHora().toValorComparable()).reversed())
                .toList();
    }

    private static String clave(String correoCliente) {
        return correoCliente.trim().toLowerCase();
    }
}
