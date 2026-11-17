package ucsur.pe.service;

import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.TransaccionPago;
import ucsur.pe.pasarela.PasarelaPago;
import ucsur.pe.pasarela.ResultadoPago;
import ucsur.pe.repository.CompraRepository;
import ucsur.pe.repository.PagoRepository;
import ucsur.pe.repository.PedidoRepository;
import java.util.List;

/**
 * Por qué existe: caso de uso "Realizar pago" (RF-16 a RF-18, RF-45) y el reembolso automático (RF-24).
 * Coordina el modelo, la pasarela de pagos (actor externo) y los repositorios.
 * Orden: validar sin cambiar nada -> cobrar -> confirmar con el mismo instante (todo o nada, RNF-10).
 */
public class PagoService {
    private final PagoRepository pagos;
    private final CompraRepository compras;
    private final PedidoRepository pedidos;
    private final PasarelaPago pasarela;
    private final Reloj reloj;

    public PagoService(PagoRepository pagos, CompraRepository compras, PedidoRepository pedidos,
                       PasarelaPago pasarela, Reloj reloj) {
        this.pagos = pagos;
        this.compras = compras;
        this.pedidos = pedidos;
        this.pasarela = pasarela;
        this.reloj = reloj;
    }

    /** Medios que la vista muestra al cliente. */
    public List<String> mediosDisponibles() {
        return TransaccionPago.MEDIOS_DISPONIBLES;
    }

    /**
     * Intenta pagar la compra y devuelve la transacción: APROBADO, o RECHAZADO con su motivo.
     * Si se rechaza, la compra sigue EN_PROCESO para reintentar mientras dure el plazo (RF-18).
     */
    public TransaccionPago pagar(Compra compra, long montoCentavos, String medioPago) {
        TransaccionPago pago = new TransaccionPago(pagos.siguienteId(), montoCentavos, medioPago, compra);
        procesar(pago);
        pagos.guardar(pago);
        if (TransaccionPago.APROBADO.equals(pago.getEstadoPago())) {
            compras.guardar(compra); // la serie del comprobante se asigna al guardar la compra confirmada
            if (compra.getPedido() != null) {
                pedidos.guardar(compra.getPedido()); // el pedido entra a la cola de la dulcería
            }
        }
        return pago;
    }

    private void procesar(TransaccionPago pago) {
        Compra compra = pago.getCompra();
        DateTime ahora = reloj.ahora(); // un único instante para validar y confirmar

        // 1. Validaciones antes de cobrar (no cambian nada)
        if (compra.verificarExpiracion()) {
            pago.rechazar("La compra expiró: se acabaron los " + Compra.TIEMPO_MAXIMO_COMPRA_MIN + " minutos", ahora);
            return;
        }
        if (pago.getMontoCentavos() <= 0) {
            pago.rechazar("Monto inválido", ahora);
            return;
        }
        // En céntimos la comparación es exacta: ya no hace falta un margen de tolerancia como con double.
        if (pago.getMontoCentavos() != compra.calcularTotalCentavos()) {
            pago.rechazar("El monto no coincide con el total de la compra", ahora);
            return;
        }
        String motivo = compra.motivoNoConfirmable(ahora);
        if (motivo != null) {
            pago.rechazar("La compra no puede pagarse: " + motivo, ahora); // no se llega a cobrar
            return;
        }

        // 2. Cobro en la pasarela
        ResultadoPago resultado = pasarela.cobrar(pago.getMedioPago(), pago.getMontoCentavos(), "COMPRA-" + compra.getIdCompra());
        if (resultado == null || !resultado.aprobado()) {
            pago.rechazar(resultado == null ? "La pasarela no respondió" : resultado.motivo(), ahora);
            return;
        }
        pago.registrarCobro(resultado.referencia(), ahora);

        // 3. Confirmación con el mismo instante del paso 1
        try {
            compra.confirmar(pago, ahora);
        } catch (RuntimeException ex) {
            // No debería pasar porque ya se validó todo; si pasa, no se queda con el dinero del cliente.
            boolean devuelto = pasarela.reembolsar(resultado.referencia(), pago.getMontoCentavos());
            pago.rechazar((devuelto ? "Cobro devuelto" : "Cobro NO devuelto, revisar") + ": " + ex.getMessage(), ahora);
            return;
        }
        pago.aprobar();
    }

    /**
     * Reembolso automático de una compra cuya función canceló el cine (RF-24); no lo pide el cliente.
     * Busca su pago aprobado, pide a la pasarela devolver el monto y recién entonces marca la compra
     * como REEMBOLSADA: si la pasarela no acepta, la compra queda CONFIRMADA para reintentarlo.
     * Devuelve el monto devuelto, en céntimos (si la dulcería ya se entregó, solo el de las entradas).
     */
    public long reembolsar(Compra compra) {
        String motivo = compra.motivoNoReembolsable();
        if (motivo != null) {
            throw new ReglaNegocioException("No se puede reembolsar la compra #" + compra.getIdCompra() + ": " + motivo);
        }
        TransaccionPago pago = pagos.listarPorCompra(compra).stream()
                .filter(p -> TransaccionPago.APROBADO.equals(p.getEstadoPago()))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("La compra #" + compra.getIdCompra() + " no tiene un pago aprobado"));
        long monto = compra.calcularMontoReembolsoCentavos();
        if (!pasarela.reembolsar(pago.getReferenciaPasarela(), monto)) {
            throw new ReglaNegocioException("La pasarela no aceptó la devolución de la compra #" + compra.getIdCompra());
        }
        compra.reembolsar();
        compras.guardar(compra);
        if (compra.getPedido() != null) {
            pedidos.guardar(compra.getPedido());
        }
        return monto;
    }
}
