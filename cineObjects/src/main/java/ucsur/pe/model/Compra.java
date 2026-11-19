package ucsur.pe.model;

import ucsur.pe.util.ConversorDinero;
import java.util.ArrayList;
import java.util.List;

/**
 * Por qué existe: representa la compra de un cliente en una sede y controla su proceso de principio a fin
 * (4 minutos para completarla). Coordina y delega: asientos a SeleccionAsientos, dinero a CalculadoraImporte,
 * puntos a GestorPuntos y el comprobante (con sus QR) a DetalleCompra.
 */
public class Compra implements Totalizable{
    // Estados de una compra (constantes: se usan como Compra.NOMBRE y su valor no cambia).
    public static final String EN_PROCESO = "EN_PROCESO"; // el cliente aún está eligiendo y pagando
    public static final String CONFIRMADA = "CONFIRMADA"; // pagada; las entradas quedan vendidas
    public static final String CANCELADA = "CANCELADA"; // el cliente la canceló antes de pagar
    public static final String EXPIRADA = "EXPIRADA"; // se acabaron los 4 minutos sin pagar
    public static final String REEMBOLSADA = "REEMBOLSADA"; // se devolvió el dinero (función cancelada)

    public static final int TIEMPO_MAXIMO_COMPRA_MIN = 4; // minutos para completar toda la compra

    private int idCompra;
    private DateTime fechaHora;
    private DateTime expiraCompra; // Hora límite de todo el proceso (butacas, dulcería y pago); en la BD es compra.expira_compra.
    private String serieNumero;
    private ResumenImporte importe; // subtotal, descuento, IGV y total del último cálculo
    private int puntosCanjeados; // Puntos que cuestan los tipos de entrada elegidos; se descuentan solo al confirmar.
    private int puntosOtorgados; // Puntos ganados al confirmar; se revierten si se reembolsa.
    private long montoReembolsadoCentavos; // Lo devuelto al cliente; si ya recogió su dulcería, solo las entradas.
    private String estado;
    private Cliente cliente;
    private Sede sede; // Sede donde se hace la compra; en la BD es compra.id_sede NOT NULL.
    /**
     * Cupones y promociones aplicados; Compra los trata a todos como Descuento (la interfaz).
     * Los que además cuentan usos (DescuentoConUsos, hoy el cupón) suman o devuelven su uso al confirmar,
     * reembolsar o anular (ver registrarUsos y revertirUsos).
     */
    private final List<Descuento> descuentos = new ArrayList<>();
    private Pedido pedido;

    // Colaboradores: cada uno se encarga de una parte de la compra. Llegan por el constructor
    // (inyección de dependencias), así Compra no decide cómo se crean.
    private final SeleccionAsientos asientos;
    private final CalculadoraImporte calculadora;
    private final GestorPuntos gestorPuntos;
    private final Reloj reloj; // de dónde sale la hora actual (el real o uno fijo en pruebas)

    /**
     * Recibe los colaboradores ya creados (inyección de dependencias): Compra no decide cuáles se usan,
     * así se pueden cambiar (por ejemplo, un reloj fijo en pruebas) sin tocar esta clase.
     */
    public Compra(int idCompra, DateTime fechaHora, Cliente cliente, Sede sede,
                  SeleccionAsientos asientos, CalculadoraImporte calculadora, GestorPuntos gestorPuntos,
                  Reloj reloj) {
        if (asientos == null || calculadora == null || gestorPuntos == null || reloj == null) {
            throw new IllegalArgumentException("Los colaboradores de la compra son obligatorios");
        }
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente es obligatorio");
        }
        if (sede == null) {
            throw new IllegalArgumentException("La sede es obligatoria");
        }
        this.idCompra = idCompra;
        this.reloj = reloj;
        this.fechaHora = fechaHora != null ? fechaHora : reloj.ahora();
        this.cliente = cliente;
        this.sede = sede;
        this.estado = EN_PROCESO;
        this.importe = ResumenImporte.VACIO;
        this.asientos = asientos;
        this.calculadora = calculadora;
        this.gestorPuntos = gestorPuntos;

        // El plazo comienza al crear la compra, cuando el cliente entra al mapa de asientos.
        this.expiraCompra = this.fechaHora.sumarMinutos(TIEMPO_MAXIMO_COMPRA_MIN);
    }

    /**
     * Solo para la capa de base de datos (ucsur.pe.db): reconstruye una compra tal como quedó guardada.
     * No aplica reglas (no cobra, no descuenta stock, no mueve puntos ni usos del cupón, no recalcula el importe):
     * todo eso ya pasó cuando la compra cambió de estado. La agrega al historial de su cliente,
     * como hace CompraService al crearla.
     * El monto reembolsado llega de la columna compra.monto_reembolsado: el total, o solo las entradas
     * si la dulcería ya se había entregado (ver CompraMySqlDao).
     */
    public static Compra restaurar(int idCompra, DateTime fechaHora, DateTime expiraCompra, String serieNumero,
                                   String estado, ResumenImporte importe, int puntosCanjeados, int puntosOtorgados,
                                   long montoReembolsadoCentavos, Cliente cliente, Sede sede, List<Entrada> entradas,
                                   Cupon cupon, Promocion promocion, Pedido pedido,
                                   SeleccionAsientos asientos, CalculadoraImporte calculadora,
                                   GestorPuntos gestorPuntos, Reloj reloj) {
        if (fechaHora == null || expiraCompra == null) {
            throw new IllegalArgumentException("La fecha y la hora límite de la compra son obligatorias");
        }
        if (!esEstadoValido(estado)) {
            throw new IllegalArgumentException("Estado de compra inválido: " + estado);
        }
        if (importe == null) {
            throw new IllegalArgumentException("El importe es obligatorio");
        }
        if (puntosCanjeados < 0 || puntosOtorgados < 0 || montoReembolsadoCentavos < 0) {
            throw new IllegalArgumentException("Los puntos y el monto reembolsado no pueden ser negativos");
        }
        // El constructor valida cliente, sede y colaboradores
        Compra compra = new Compra(idCompra, fechaHora, cliente, sede, asientos, calculadora, gestorPuntos, reloj);
        compra.expiraCompra = expiraCompra;
        compra.serieNumero = serieNumero;
        compra.estado = estado;
        compra.importe = importe; // lo que se guardó (y se cobró), no un cálculo nuevo
        compra.puntosCanjeados = puntosCanjeados;
        compra.puntosOtorgados = puntosOtorgados;
        compra.montoReembolsadoCentavos = montoReembolsadoCentavos;
        compra.pedido = pedido;
        if (promocion != null) {
            compra.descuentos.add(promocion);
        }
        if (cupon != null) {
            compra.descuentos.add(cupon); // al estar en la lista, un reembolso le devuelve el uso
        }
        compra.asientos.restaurar(entradas != null ? entradas : List.of());
        cliente.agregarCompra(compra);
        return compra;
    }

    /** Los cinco estados posibles (en la BD, ck_compra_estado). */
    private static boolean esEstadoValido(String estado) {
        return EN_PROCESO.equals(estado) || CONFIRMADA.equals(estado) || CANCELADA.equals(estado)
                || EXPIRADA.equals(estado) || REEMBOLSADA.equals(estado);
    }

    /** Comprueba el plazo y expira la compra si venció; devuelve true si ya expiró. */
    public boolean verificarExpiracion() {
        if (!EN_PROCESO.equals(estado)) {
            return EXPIRADA.equals(estado);
        }
        DateTime ahora = reloj.ahora();
        // Vence al LLEGAR a la hora límite (no un instante después): el plazo es de 4 minutos como máximo.
        boolean tiempoVencido = (expiraCompra != null && !ahora.esAntesDe(expiraCompra));
        if (asientos.isBloqueados() && asientos.algunaExpirada(ahora)) {
            tiempoVencido = true;
        }
        if (tiempoVencido) {
            cancelarPorTimeout();
            return true;
        }
        return false;
    }

    /** Expira la compra si venció su plazo y lanza error si ya no está EN_PROCESO. */
    private void validarEnProceso() {
        verificarExpiracion();
        if (!EN_PROCESO.equals(estado)) {
            throw new IllegalStateException("La compra ya no está EN_PROCESO (estado actual: " + estado + ")");
        }
    }

    // ---------- Asientos (delegado en SeleccionAsientos) ----------
    /**
     * Preselecciona una entrada de esta sede; la butaca aún no se bloquea y no se repite la misma butaca.
     * Una compra es para UNA sola función (el cliente entra a una función y paga esa): el comprobante
     * y el reembolso se basan en esa función. Para otra función se inicia una compra nueva.
     */
    public void agregarEntrada(Entrada entrada) {
        validarEnProceso();
        if (entrada != null && !entrada.getFuncion().getSala().getSede().equals(this.sede)) {
            throw new IllegalArgumentException("La función pertenece a otra sede; la compra es de " + sede.getNombre());
        }
        List<Entrada> elegidas = asientos.getEntradas();
        if (entrada != null && !elegidas.isEmpty() && elegidas.get(0).getFuncion() != entrada.getFuncion()) {
            throw new IllegalArgumentException("Esta compra es para la función " + elegidas.get(0).getFuncion()
                    + "; para otra función inicia una compra nueva");
        }
        asientos.agregar(entrada);
        recalcularImporte();
    }

    /** Quita una selección mientras los asientos sigan sin bloquearse. */
    public boolean quitarEntrada(Entrada entrada) {
        validarEnProceso();
        boolean removida = asientos.quitar(entrada);
        if (removida) {
            recalcularImporte();
        }
        return removida;
    }

    /**
     * Valida que las butacas sigan libres y las bloquea hasta la hora límite de la compra.
     * Si alguna ya no está disponible lanza un error con su código y no bloquea ninguna:
     * el cliente debe quitar esa entrada, elegir otra butaca y volver a confirmar.
     */
    public void confirmarSeleccionAsientos() {
        validarEnProceso();
        asientos.bloquear(reloj.ahora(), this.expiraCompra, this.fechaHora);
    }

    /**
     * El cliente vuelve al mapa de butacas para cambiarlas: se liberan los bloqueos y las entradas
     * vuelven a ser una preselección que se puede quitar o cambiar. El plazo de 4 minutos sigue corriendo.
     */
    public void liberarSeleccionAsientos() {
        validarEnProceso();
        asientos.liberarBloqueo();
    }

    /** Butacas que esta compra ya eligió en una función (para no recomendarlas otra vez). */
    public List<Butaca> butacasElegidas(Funcion funcion) {
        List<Butaca> elegidas = new ArrayList<>();
        for (Entrada e : asientos.getEntradasValidas()) {
            if (e.getFuncion() == funcion) elegidas.add(e.getButaca());
        }
        return elegidas;
    }

    // ---------- Dulcería: la compra valida primero y luego delega el cambio en su Pedido ----------
    /**
     * Agrega una línea al pedido de dulcería; si la compra aún no tiene pedido, lo crea (id 0: lo genera la base).
     * Si la compra ya no está EN_PROCESO o el producto es de oferta para socios sin el nivel requerido,
     * lanza el error y el pedido queda como estaba.
     */
    public void agregarProducto(DetallePedido detalle) {
        validarEnProceso();
        Pedido destino = (pedido != null) ? pedido : new Pedido(0, reloj);
        destino.agregarDetalle(detalle);
        try {
            gestorPuntos.validarProductosSocio(cliente, destino);
        } catch (RuntimeException e) {
            destino.quitarDetalle(detalle); // la línea rechazada no se queda en el pedido
            throw e;
        }
        this.pedido = destino;
        recalcularImporte();
    }

    /** Cambia la cantidad de una línea del pedido y recalcula el total. */
    public void cambiarCantidad(DetallePedido detalle, int cantidad) {
        validarEnProceso();
        exigirPedido().modificarCantidad(detalle, cantidad);
        recalcularImporte();
    }

    /** Quita una línea del pedido; si el pedido queda vacío, la compra sigue sin dulcería. */
    public void quitarProducto(DetallePedido detalle) {
        validarEnProceso();
        Pedido actual = exigirPedido();
        if (!actual.quitarDetalle(detalle)) {
            throw new IllegalArgumentException("El producto no forma parte del pedido");
        }
        if (actual.getDetalles().isEmpty()) {
            this.pedido = null;
        }
        recalcularImporte();
    }

    private Pedido exigirPedido() {
        if (pedido == null) {
            throw new IllegalStateException("La compra no tiene pedido de dulcería");
        }
        return pedido;
    }

    // ---------- Totales (delegado en CalculadoraImporte) ----------
    /** Recalcula subtotal, descuentos, IGV y total, y los puntos que cuesta la compra. */
    @Override
    public long calcularTotalCentavos() {
        return calculadora.calcular(asientos.getEntradasValidas(), pedido, descuentos).getTotalCentavos();
    }

    /** Guarda el importe y los puntos que cuesta la compra; se llama cada vez que cambia algo. */
    private void recalcularImporte() {
        List<Entrada> validas = asientos.getEntradasValidas();
        if (EN_PROCESO.equals(estado)) {
            this.puntosCanjeados = gestorPuntos.calcularPuntosRequeridos(validas);
        }
        this.importe = calculadora.calcular(validas, pedido, descuentos);
    }

    // ---------- Descuentos (cupones y promociones vistos como Descuento) ----------
    /**
     * Aplica cualquier descuento (cupón, promoción...) si cumple sus condiciones para esta compra.
     * Como en la BD (columnas id_cupon e id_promocion), hay a lo más uno de cada categoría:
     * uno nuevo de la misma categoría reemplaza al anterior.
     * Es el único camino para aplicar un descuento: un cupón cuenta su uso al confirmar la compra
     * sin importar el tipo de la variable con que llegue.
     */
    public void aplicarDescuento(Descuento descuento) {
        validarEnProceso();
        if (descuento == null) {
            throw new IllegalArgumentException("El descuento es obligatorio");
        }
        if (!descuento.esAplicable(contextoDescuento())) {
            throw new IllegalStateException("El descuento " + descuento + " no aplica a esta compra (fechas, día, usos o nivel de socio)");
        }
        descuentos.removeIf(d -> d.getCategoria().equals(descuento.getCategoria()));
        descuentos.add(descuento);
        recalcularImporte();
    }

    /** Al confirmar: los descuentos con límite de usos (el cupón) suman uno. */
    private void registrarUsos() {
        for (Descuento d : descuentos) {
            if (d instanceof DescuentoConUsos conUsos) conUsos.registrarUso();
        }
    }

    /** Al reembolsar o anular: los descuentos con límite de usos recuperan el uso. */
    private void revertirUsos() {
        for (Descuento d : descuentos) {
            if (d instanceof DescuentoConUsos conUsos) conUsos.revertirUso();
        }
    }

    /** Datos de esta compra que un descuento necesita para decidir si aplica. */
    private ContextoDescuento contextoDescuento() {
        return new ContextoDescuento(fechaHora, cliente.esSocio() ? cliente.getNivelSocio() : 0);
    }

    // ---------- Confirmación, cancelación y reembolso ----------
    /**
     * Revisa, SIN CAMBIAR NADA, si la compra se puede confirmar en el instante indicado.
     * Devuelve null si todo está bien, o el motivo por el que no se puede (para mostrárselo al cliente).
     * TransaccionPago la llama ANTES de cobrar: si hay un motivo, no se cobra.
     */
    public String motivoNoConfirmable(DateTime ahora) {
        if (!EN_PROCESO.equals(estado)) return "la compra está " + estado;
        if (ahora == null) return "falta la fecha y hora de la operación";
        if (!ahora.esAntesDe(expiraCompra)) { // al llegar a la hora límite ya venció
            return "venció el plazo de " + TIEMPO_MAXIMO_COMPRA_MIN + " minutos";
        }
        // Las butacas deben haberse bloqueado antes con confirmarSeleccionAsientos()
        if (!asientos.estaVacia() && !asientos.isBloqueados()) return "falta confirmar la selección de asientos";
        if (asientos.algunBloqueoVencido(ahora)) return "venció el bloqueo de alguna butaca";
        if (!asientos.todasConTipo()) return "hay entradas sin tipo de entrada";
        // La función pudo cancelarse mientras el cliente pagaba. El cierre de venta se mide con la hora en que
        // EMPEZÓ la compra: si empezó antes del cierre, puede terminar de pagar aunque el cierre ya pasó.
        for (Entrada e : asientos.getEntradasValidas()) {
            String motivoFuncion = e.getFuncion().motivoVentaCerrada(fechaHora);
            if (motivoFuncion != null) return motivoFuncion;
        }
        if (asientos.contarBloqueadas() == 0 && pedido == null) return "no hay entradas ni pedido que vender";
        // Cada descuento debe seguir siendo aplicable (por ejemplo, un cupón que agotó sus usos ya no)
        for (Descuento d : descuentos) {
            if (!d.esAplicable(contextoDescuento())) return "el descuento " + d + " ya no es válido";
        }
        String motivoSocio = gestorPuntos.motivoBeneficiosSocio(cliente, asientos.getEntradasValidas(), pedido);
        if (motivoSocio != null) return motivoSocio;
        if (pedido != null && !pedido.isRegistrado() && !pedido.hayStock(sede)) {
            return "no hay stock suficiente en la sede para el pedido de dulcería";
        }
        return null;
    }

    /**
     * Confirma la venta con el cobro ya aprobado por la pasarela (lo llama PagoService).
     * Exige una transacción de esta compra, todavía pendiente y con la referencia del cobro:
     * así no se confirma una compra sin pago.
     * Todo o nada (RNF-10): primero vuelve a revisar con el MISMO instante con el que se validó antes de cobrar,
     * y recién después hace los cambios. Como ya se comprobó que todo procede, ningún cambio puede fallar
     * a mitad de camino (por ejemplo, dejar el stock descontado con entradas sin vender).
     */
    public void confirmar(TransaccionPago pago, DateTime ahora) {
        if (pago == null || pago.getCompra() != this || pago.getReferenciaPasarela() == null
                || !TransaccionPago.PENDIENTE.equals(pago.getEstadoPago())) {
            throw new IllegalStateException("Se requiere un cobro aprobado de esta compra para confirmarla");
        }
        String motivo = motivoNoConfirmable(ahora);
        if (motivo != null) {
            throw new IllegalStateException("No se puede confirmar la compra: " + motivo);
        }
        recalcularImporte();

        // Desde aquí solo se aplican cambios que ya se sabe que proceden.
        // Los puntos van primero: si el canje fallara, se corta antes de tocar el stock o las entradas.
        if (puntosCanjeados > 0 && !cliente.canjearPuntos(puntosCanjeados)) {
            throw new IllegalStateException("No se pudieron canjear " + puntosCanjeados + " puntos");
        }
        if (pedido != null && !pedido.isRegistrado()) {
            pedido.registrar(sede); // descuenta el stock de la sede (ya se verificó que alcanza)
        }
        asientos.venderBloqueadas(ahora);
        registrarUsos(); // solo los que tienen límite de usos (el cupón suma uno)
        this.estado = CONFIRMADA; // la serie la asigna la base al confirmar (trigger trg_compra_serie)

        // Los puntos ganados usan el nivel que tenía el cliente antes de esta compra
        this.puntosOtorgados = gestorPuntos.calcularPuntosGanados(cliente, asientos.getEntradasValidas(), pedido);
        if (puntosOtorgados > 0) {
            cliente.acumularPuntos(puntosOtorgados);
        }
        cliente.actualizarNivel();
    }

    /** Cancela una compra en proceso y libera las butacas que estuvieran bloqueadas. */
    public void cancelar() {
        validarEnProceso();
        asientos.anularNoVendidas();
        anularPedidoSinConfirmar();
        this.puntosCanjeados = 0; // los puntos nunca se descontaron del saldo
        this.estado = CANCELADA;
    }

    /**
     * Si la compra no llega a confirmarse, su pedido de dulcería tampoco se prepara: pasa a ANULADO
     * (igual que el evento ev_expirar_compras de la BD). No vuelve stock porque aún no se había descontado.
     */
    private void anularPedidoSinConfirmar() {
        if (pedido != null) {
            pedido.anular();
        }
    }

    /**
     * Expira la compra por tiempo y libera los bloqueos; no cambia compras ya finalizadas.
     * Es privada: solo se expira cuando la propia compra comprueba que el plazo venció.
     */
    private void cancelarPorTimeout() {
        if (!EN_PROCESO.equals(estado)) return;
        asientos.liberarPorTimeout();
        anularPedidoSinConfirmar();
        this.puntosCanjeados = 0; // los puntos nunca se descontaron del saldo
        this.estado = EXPIRADA;
    }

    /**
     * Revisa, sin cambiar nada, si la compra se puede reembolsar (RF-24): debe estar CONFIRMADA y todas
     * sus funciones canceladas por el cine. Devuelve null si procede o el motivo si no.
     */
    public String motivoNoReembolsable() {
        if (!CONFIRMADA.equals(estado)) return "solo se reembolsa una compra CONFIRMADA (estado actual: " + estado + ")";
        if (!asientos.todasConFuncionCancelada()) return "solo se reembolsa si el cine canceló la función";
        return null;
    }

    /** Indica si el cliente ya recogió su pedido de dulcería: esa parte ya se consumió y no se devuelve. */
    private boolean dulceriaEntregada() {
        return pedido != null && Pedido.ENTREGADO.equals(pedido.getEstado());
    }

    /**
     * Monto a devolver, sin cambiar nada: el total pagado o, si la dulcería ya se entregó, solo lo que
     * costaron las entradas (con sus descuentos e IGV), sin pasar nunca del total pagado.
     */
    public long calcularMontoReembolsoCentavos() {
        if (!dulceriaEntregada()) return importe.getTotalCentavos();
        long soloEntradas = calculadora.calcular(asientos.getEntradasValidas(), null, descuentos).getTotalCentavos();
        return Math.min(soloEntradas, importe.getTotalCentavos());
    }

    /**
     * Reembolsa la compra cuando el cine cancela la función (lo llama PagoService después de que la
     * pasarela devolvió el dinero). Anula las entradas, devuelve los puntos canjeados y el uso del cupón,
     * y quita los puntos ganados. Si la dulcería ya se entregó, esa parte no se devuelve: el pedido se queda
     * como está y el cliente conserva los puntos que ganó por ella.
     */
    public void reembolsar() {
        String motivo = motivoNoReembolsable();
        if (motivo != null) {
            throw new IllegalStateException("No se puede reembolsar: " + motivo);
        }
        this.montoReembolsadoCentavos = calcularMontoReembolsoCentavos();
        int puntosARevertir = puntosOtorgados;
        if (dulceriaEntregada()) {
            int baseTotal = gestorPuntos.calcularPuntosBase(asientos.getEntradasValidas(), pedido);
            int baseEntradas = gestorPuntos.calcularPuntosBase(asientos.getEntradasValidas(), null);
            puntosARevertir = (baseTotal == 0) ? 0 : puntosOtorgados * baseEntradas / baseTotal;
        } else if (pedido != null && pedido.isRegistrado()) {
            pedido.anular(); // el stock vuelve solo si nadie lo había empezado a preparar
        }
        asientos.anularVendidas();
        revertirUsos(); // el cupón recupera el uso
        this.estado = REEMBOLSADA;
        cliente.devolverPuntos(puntosCanjeados);
        cliente.revertirPuntos(puntosARevertir);
    }

    /**
     * Deshace una confirmación que no se pudo guardar (por ejemplo, la base rechazó la venta de una butaca).
     * Lo llama PagoService después de pedir a la pasarela que devuelva el cobro. Revierte lo mismo que un
     * reembolso (libera las butacas, devuelve los puntos canjeados, quita los ganados, devuelve el uso del cupón
     * y repone el stock del pedido), pero sin exigir que la función esté cancelada, y deja la compra CANCELADA:
     * para volver a intentarlo el cliente inicia una compra nueva.
     * Solo vale para una compra CONFIRMADA sin serie: si ya tiene serie, la confirmación sí quedó guardada.
     * Como en el reembolso, el nivel de socio no baja (Cliente.actualizarNivel nunca lo reduce).
     */
    public void anularConfirmacion() {
        if (!CONFIRMADA.equals(estado) || serieNumero != null) {
            throw new IllegalStateException("Solo se anula una confirmación que no llegó a guardarse (estado: "
                    + estado + (serieNumero != null ? ", serie " + serieNumero : "") + ")");
        }
        if (pedido != null && pedido.isRegistrado()) {
            pedido.anular(); // recién registrado y PENDIENTE: el stock vuelve a la sede
        }
        asientos.anularVendidas();
        revertirUsos();
        cliente.devolverPuntos(puntosCanjeados);
        cliente.revertirPuntos(puntosOtorgados);
        this.puntosCanjeados = 0;
        this.puntosOtorgados = 0;
        this.estado = CANCELADA;
    }

    // ---------- Comprobante (delegado en DetalleCompra) ----------
    /** Guarda la serie que la base asignó al confirmar la compra (por ejemplo B001-000123); solo se fija una vez. */
    public void asignarSerie(String serieNumero) {
        if (!CONFIRMADA.equals(estado) && !REEMBOLSADA.equals(estado)) {
            throw new IllegalStateException("Solo una compra confirmada puede tener serie");
        }
        if (this.serieNumero != null) {
            throw new IllegalStateException("La compra ya tiene serie asignada");
        }
        if (serieNumero == null || serieNumero.trim().isEmpty() || serieNumero.trim().length() > 20) {
            throw new IllegalArgumentException("La serie es obligatoria (máx. 20 caracteres)");
        }
        this.serieNumero = serieNumero.trim();
    }

    /**
     * Resumen para «Ver» en Mis compras, imprimir o enviar por correo (RF-20 a RF-22, RF-33).
     * Solo existe para compras pagadas: confirmadas o reembolsadas (estas ya sin QR válidos).
     */
    public DetalleCompra obtenerDetalle() {
        return obtenerDetalle(null);
    }

    /**
     * Igual, indicando con qué medio se pagó (TransaccionPago.TARJETA_CREDITO, YAPE...): la compra no guarda
     * su pago, así que quien lo conoce (EmisionService) lo pasa. null si no se sabe.
     */
    public DetalleCompra obtenerDetalle(String medioPago) {
        if (serieNumero == null || (!CONFIRMADA.equals(estado) && !REEMBOLSADA.equals(estado))) {
            throw new IllegalStateException("Solo se puede ver el detalle de una compra pagada");
        }
        return new DetalleCompra(this, medioPago);
    }

    /** Reloj de la compra; TransaccionPago lo usa para que el pago y la compra miren la misma hora. */
    Reloj getReloj() { return reloj; }

    /** Asigna el id que generó la base (AUTO_INCREMENT) tras el INSERT; una compra nueva nace con id 0. */
    public void asignarId(int idCompra) {
        this.idCompra = Ids.asignar(this.idCompra, idCompra, "la compra");
    }

    public int getIdCompra() { return idCompra; }
    public DateTime getFechaHora() { return fechaHora; }
    public DateTime getExpiraCompra() { return expiraCompra; }
    public String getSerieNumero() { return serieNumero; }
    public ResumenImporte getImporte() { return importe; }
    public long getTotalCentavos() { return importe.getTotalCentavos(); }
    public int getPuntosCanjeados() { return puntosCanjeados; }
    public int getPuntosOtorgados() { return puntosOtorgados; }
    public long getMontoReembolsadoCentavos() { return montoReembolsadoCentavos; }
    public String getEstado() { return estado; }
    public Cliente getCliente() { return cliente; }
    public Sede getSede() { return sede; }
    public List<Descuento> getDescuentos() { return new ArrayList<>(descuentos); }
    public Pedido getPedido() { return pedido; }
    public List<Entrada> getEntradas() { return asientos.getEntradas(); }

    @Override
    public String toString() {
        return "Compra #" + idCompra + " [" + estado + "] " + ConversorDinero.formatear(getTotalCentavos());
    }
}
