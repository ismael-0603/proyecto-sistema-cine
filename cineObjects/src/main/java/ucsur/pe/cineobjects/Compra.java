package ucsur.pe.cineobjects;

import ucsur.pe.cineInterfaz.Descuento;
import ucsur.pe.cineInterfaz.Totalizable;
import java.util.ArrayList;
import java.util.List;

/**
 * Por qué existe: representa la compra de un cliente en una sede y controla su proceso de principio a fin
 * (4 minutos para completarla). Coordina y delega: asientos a SeleccionAsientos, dinero a CalculadoraImporte,
 * puntos a GestorPuntos y comprobante a Comprobante.
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
    private String estado;
    private Cliente cliente;
    private Sede sede; // Sede donde se hace la compra; en la BD es compra.id_sede NOT NULL.
    /** Cupones y promociones aplicados; Compra los trata a todos como Descuento (la interfaz). */
    private final List<Descuento> descuentos = new ArrayList<>();
    private Pedido pedido;

    // Colaboradores: cada uno se encarga de una parte de la compra.
    private final SeleccionAsientos asientos = new SeleccionAsientos();
    private final CalculadoraImporte calculadora = new CalculadoraImporte();
    private final GestorPuntos gestorPuntos = new GestorPuntos();

    public Compra(int idCompra, DateTime fechaHora, Cliente cliente, Sede sede) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente es obligatorio");
        }
        if (sede == null) {
            throw new IllegalArgumentException("La sede es obligatoria");
        }
        this.idCompra = idCompra;
        this.fechaHora = fechaHora != null ? fechaHora : DateTime.ahora();
        this.cliente = cliente;
        this.sede = sede;
        this.estado = EN_PROCESO;
        this.importe = ResumenImporte.VACIO;

        // El plazo comienza al crear la compra, cuando el cliente entra al mapa de asientos.
        this.expiraCompra = this.fechaHora.sumarMinutos(TIEMPO_MAXIMO_COMPRA_MIN);
    }

    /** Comprueba el plazo y expira la compra si venció; devuelve true si ya expiró. */
    public boolean verificarExpiracion() {
        if (!EN_PROCESO.equals(estado)) {
            return EXPIRADA.equals(estado);
        }
        boolean tiempoVencido = (expiraCompra != null && DateTime.ahora().esDespuesDe(expiraCompra));
        if (asientos.isBloqueados() && asientos.algunaExpirada()) {
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
    /** Preselecciona una entrada; la butaca aún no se bloquea y no se repite la misma butaca. */
    public void agregarEntrada(Entrada entrada) {
        validarEnProceso();
        asientos.agregar(entrada);
        calcularTotal();
    }

    /** Quita una selección mientras los asientos sigan sin bloquearse. */
    public boolean quitarEntrada(Entrada entrada) {
        validarEnProceso();
        boolean removida = asientos.quitar(entrada);
        if (removida) {
            calcularTotal();
        }
        return removida;
    }

    /**
     * Valida que las butacas sigan libres y las bloquea hasta la hora límite de la compra.
     * Si alguna ya no está disponible lanza un error con su código y no bloquea ninguna:
     * el cliente debe quitar esa entrada, elegir otra butaca y volver a confirmar.
     */
    public boolean confirmarSeleccionAsientos() {
        validarEnProceso();
        return asientos.bloquear(this.expiraCompra);
    }

    /** Asigna el pedido de dulcería; rechaza productos de oferta si el cliente no tiene el nivel requerido. */
    public void asignarPedido(Pedido pedido) {
        validarEnProceso();
        gestorPuntos.validarProductosSocio(cliente, pedido);
        this.pedido = pedido;
        calcularTotal();
    }

    // ---------- Totales (delegado en CalculadoraImporte) ----------
    /** Recalcula subtotal, descuentos, IGV y total, y los puntos que cuesta la compra. */
    @Override
    public double calcularTotal() {
        List<Entrada> validas = asientos.getEntradasValidas();
        if (EN_PROCESO.equals(estado)) {
            this.puntosCanjeados = gestorPuntos.calcularPuntosRequeridos(validas);
        }
        this.importe = calculadora.calcular(validas, pedido, descuentos);
        return importe.getTotal();
    }

    // ---------- Puntos (delegado en GestorPuntos) ----------
    /** Puntos base de entradas válidas y combos del pedido (sin multiplicador de nivel). */
    public int calcularPuntos() {
        return gestorPuntos.calcularPuntosBase(asientos.getEntradasValidas(), pedido);
    }

    /**
     * Comprueba que las entradas de socio y los combos de oferta los use un cliente con el nivel requerido,
     * y que el saldo de puntos alcance para los tipos de entrada que cuestan puntos.
     */
    public void validarBeneficiosSocio() {
        gestorPuntos.validarBeneficiosSocio(cliente, asientos.getEntradasValidas(), pedido);
    }

    // ---------- Descuentos (cupones y promociones vistos como Descuento) ----------
    /** Busca el cupón por código en el catálogo y lo aplica si sigue vigente. */
    public boolean aplicarCupon(String codigo, CatalogoCupones catalogo) {
        validarEnProceso();
        if (catalogo == null) return false;
        Cupon encontrado = catalogo.buscarPorCodigo(codigo);
        return encontrado != null && aplicarDescuento(encontrado);
    }

    /**
     * Aplica cualquier descuento (cupón, promoción...) si cumple sus condiciones para esta compra.
     * Como en la BD (columnas id_cupon e id_promocion), hay a lo más uno de cada clase:
     * uno nuevo de la misma clase reemplaza al anterior.
     */
    public boolean aplicarDescuento(Descuento descuento) {
        validarEnProceso();
        if (descuento == null || !descuento.esAplicable(this)) {
            return false;
        }
        descuentos.removeIf(d -> d.getClass() == descuento.getClass());
        descuentos.add(descuento);
        calcularTotal();
        return true;
    }

    // ---------- Confirmación, cancelación y reembolso ----------
    /**
     * Confirma la venta si el plazo sigue vigente, los asientos ya están bloqueados y los datos son válidos.
     * Antes de cambiar estados valida las entradas, los beneficios de socio, los puntos, los descuentos y el pedido.
     */
    public boolean confirmar() {
        if (!EN_PROCESO.equals(estado)) return false;

        // 1. Verificar si los 4 minutos ya expiraron
        if (verificarExpiracion()) {
            return false;
        }

        // Las butacas deben haberse bloqueado antes con confirmarSeleccionAsientos()
        if (!asientos.estaVacia() && !asientos.isBloqueados()) {
            return false;
        }
        if (asientos.algunaExpirada()) {
            cancelarPorTimeout();
            return false;
        }
        if (!asientos.todasConTipo()) {
            return false; // Toda entrada debe tener asignado su TipoEntrada
        }

        // 2. La compra debe tener algo que vender
        if (asientos.contarBloqueadas() == 0 && pedido == null) return false;
        // 3. Cada descuento debe seguir siendo aplicable (por ejemplo, un cupón que agotó sus usos ya no)
        for (Descuento d : descuentos) {
            if (!d.esAplicable(this)) return false;
        }

        calcularTotal();

        // 4. Entradas de socio, combos por nivel y saldo de puntos
        try {
            validarBeneficiosSocio();
        } catch (IllegalStateException ex) {
            return false;
        }
        if (puntosCanjeados > 0 && (!cliente.esSocio() || cliente.consultarPuntos() < puntosCanjeados)) {
            return false;
        }

        // 5. Registrar el pedido de dulcería (verifica y descuenta stock en la sede de la compra)
        if (pedido != null && !pedido.isRegistrado() && !pedido.registrar(sede)) return false;

        // 6. Ya no hay nada que pueda fallar: se confirma todo
        asientos.venderBloqueadas();
        for (Descuento d : descuentos) {
            d.registrarUso(); // cada descuento sabe qué hacer: el cupón suma un uso, la promoción nada
        }
        if (puntosCanjeados > 0) {
            cliente.canjearPuntos(puntosCanjeados);
        }
        this.estado = CONFIRMADA; // la serie la asigna la base al confirmar (trigger trg_compra_serie)

        // Los puntos ganados usan el nivel que tenía el cliente antes de esta compra
        this.puntosOtorgados = gestorPuntos.calcularPuntosGanados(cliente, asientos.getEntradasValidas(), pedido);
        if (puntosOtorgados > 0) {
            cliente.acumularPuntos(puntosOtorgados);
        }
        cliente.actualizarNivel();
        return true;
    }

    /** Cancela una compra en proceso y libera las butacas que estuvieran bloqueadas. */
    public void cancelar() {
        validarEnProceso();
        asientos.anularBloqueadas();
        this.puntosCanjeados = 0; // los puntos nunca se descontaron del saldo
        this.estado = CANCELADA;
    }

    /** Expira la compra por tiempo y libera los bloqueos; no cambia compras ya finalizadas. */
    public void cancelarPorTimeout() {
        if (!EN_PROCESO.equals(estado)) return;
        asientos.liberarPorTimeout();
        this.puntosCanjeados = 0; // los puntos nunca se descontaron del saldo
        this.estado = EXPIRADA;
    }

    /**
     * Reembolsa solo si todas las funciones fueron canceladas y el pedido no fue entregado.
     * Devuelve los puntos canjeados y quita los puntos ganados con esta compra.
     */
    public boolean reembolsar() {
        if (!CONFIRMADA.equals(estado)) return false;
        if (!asientos.todasConFuncionCancelada()) return false;
        if (pedido != null && Pedido.ENTREGADO.equals(pedido.getEstado())) {
            return false;
        }
        asientos.anularVendidas();
        this.estado = REEMBOLSADA;
        cliente.devolverPuntos(puntosCanjeados);
        cliente.revertirPuntos(puntosOtorgados);
        return true;
    }

    // ---------- Comprobante (delegado en Comprobante) ----------
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

    /** Arma el comprobante; exige que la compra ya tenga serie. */
    public Comprobante obtenerComprobante() {
        return new Comprobante(serieNumero, fechaHora, importe);
    }

    /** Texto del comprobante; exige que la compra ya tenga serie. */
    public String generarComprobante() {
        return obtenerComprobante().generarTexto();
    }

    /** Uso interno del paquete: fija el estado si es uno de los cinco válidos. */
    void setEstado(String estado) {
        if (!EN_PROCESO.equals(estado) && !CONFIRMADA.equals(estado) && !CANCELADA.equals(estado)
                && !EXPIRADA.equals(estado) && !REEMBOLSADA.equals(estado)) {
            throw new IllegalArgumentException("Estado de compra inválido: " + estado);
        }
        this.estado = estado;
    }

    public int getIdCompra() { return idCompra; }
    public DateTime getFechaHora() { return fechaHora; }
    public DateTime getExpiraCompra() { return expiraCompra; }
    public boolean isAsientosBloqueados() { return asientos.isBloqueados(); }
    public String getSerieNumero() { return serieNumero; }
    public ResumenImporte getImporte() { return importe; }
    public double getSubtotal() { return importe.getSubtotal(); }
    public double getDescuento() { return importe.getDescuento(); }
    public double getIgv() { return importe.getIgv(); }
    public double getTotal() { return importe.getTotal(); }
    public int getPuntosCanjeados() { return puntosCanjeados; }
    public int getPuntosOtorgados() { return puntosOtorgados; }
    public String getEstado() { return estado; }
    public Cliente getCliente() { return cliente; }
    public Sede getSede() { return sede; }
    public List<Descuento> getDescuentos() { return new ArrayList<>(descuentos); }
    public Pedido getPedido() { return pedido; }
    public List<Entrada> getEntradas() { return asientos.getEntradas(); }

    @Override
    public String toString() {
        return "Compra #" + idCompra + " [" + estado + "] S/ " + String.format("%.2f", getTotal());
    }
}
