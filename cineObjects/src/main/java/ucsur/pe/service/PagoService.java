package ucsur.pe.service;

import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.exception.RepositoryException;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.TransaccionPago;
import ucsur.pe.pasarela.PasarelaPago;
import ucsur.pe.pasarela.ResultadoPago;
import ucsur.pe.repository.CompraRepository;
import ucsur.pe.repository.PagoRepository;

/**
 * Por qué existe: caso de uso "Realizar pago" (RF-16 a RF-18, RF-45) y el reembolso automático (RF-24).
 * Coordina el modelo, la pasarela de pagos (actor externo) y los repositorios.
 * Orden: verificar expiración y validar -> cobrar -> confirmar con el mismo instante (todo o nada, RNF-10).
 */
public class PagoService {
    private final PagoRepository pagos;
    private final CompraRepository compras;
    private final PasarelaPago pasarela;
    private final Reloj reloj;

    public PagoService(PagoRepository pagos, CompraRepository compras, PasarelaPago pasarela, Reloj reloj) {
        this.pagos = pagos;
        this.compras = compras;
        this.pasarela = pasarela;
        this.reloj = reloj;
    }

    /**
     * Intenta pagar la compra y devuelve la transacción: APROBADO, o RECHAZADO con su motivo.
     * Si se rechaza, la compra sigue EN_PROCESO para reintentar mientras dure el plazo (RF-18).
     * Un pago APROBADO se guarda junto con su compra confirmada, en una sola transacción: nunca queda una venta
     * confirmada sin el pago que permite reembolsarla, ni un pago aprobado de una compra que no se guardó.
     * Si ese guardado falla después de cobrar, se devuelve el cobro y la compra se anula (ver deshacerCobro);
     * el pago se guarda RECHAZADO con el motivo.
     * El pedido de dulcería se guarda en esa misma transacción, junto con la compra, y desde ahí entra a la cola.
     */
    public TransaccionPago pagar(Compra compra, long montoCentavos, String medioPago) {
        boolean estabaEnProceso = Compra.EN_PROCESO.equals(compra.getEstado());
        TransaccionPago pago = new TransaccionPago(0, montoCentavos, medioPago, compra);
        procesar(pago);
        boolean guardado = false;
        if (TransaccionPago.APROBADO.equals(pago.getEstadoPago())) {
            try {
                pagos.guardarConCompra(pago); // la serie del comprobante se asigna al guardar la compra confirmada
                guardado = true;
            } catch (RepositoryException ex) {
                deshacerCobro(pago);
            }
        }
        if (!guardado) {
            pagos.guardar(pago); // rechazado (o cobro devuelto): queda registrado con su motivo
            if (estabaEnProceso && Compra.EXPIRADA.equals(compra.getEstado())) {
                try {
                    compras.guardar(compra);
                } catch (RepositoryException ignored) {
                    // Si la BD no responde, ev_expirar_compras la expirará al vencer su plazo.
                }
            }
        }
        return pago;
    }

    /**
     * El cobro se aprobó pero la compra no se pudo guardar (por ejemplo, la base rechazó la venta de una butaca):
     * se pide a la pasarela devolver el dinero, se deshace la confirmación en memoria (butacas, puntos, cupón y
     * stock vuelven a como estaban) y el pago pasa a RECHAZADO con el motivo.
     * Luego se intenta guardar la compra como CANCELADA; si la base sigue fallando, en la BD queda EN_PROCESO
     * y el evento ev_expirar_compras la expira al vencer sus 4 minutos.
     * El motivo lo ve el cliente, así que no lleva el detalle técnico del error de la base.
     */
    private void deshacerCobro(TransaccionPago pago) {
        Compra compra = pago.getCompra();
        boolean devuelto = pasarela.reembolsar(pago.getReferenciaPasarela(), pago.getMontoCentavos());
        compra.anularConfirmacion();
        pago.anularCobro("No se pudo guardar la compra; " + (devuelto ? "cobro devuelto" : "cobro NO devuelto, revisar"),
                reloj.ahora());
        try {
            compras.guardar(compra);
        } catch (RepositoryException otra) {
            // Se deja como está: el pago RECHAZADO ya explica el problema y la BD expirará la compra sola.
        }
    }

    private void procesar(TransaccionPago pago) {
        Compra compra = pago.getCompra();
        DateTime ahora = reloj.ahora(); // un único instante para validar y confirmar

        // 1. Validaciones antes de cobrar (si venció el plazo, verificarExpiracion() la marca EXPIRADA)
        if (compra.verificarExpiracion()) {
            pago.rechazar("La compra expiró: se acabaron los " + Compra.TIEMPO_MAXIMO_COMPRA_MIN + " minutos", ahora);
            return;
        }
        if (pago.getMontoCentavos() <= 0) {
            pago.rechazar("Monto inválido", ahora);
            return;
        }
        // En céntimos la comparación es exacta, sin margen de tolerancia.
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
        compras.guardar(compra); // una sola transacción: la compra, su pedido anulado y el stock devuelto
        return monto;
    }
}
