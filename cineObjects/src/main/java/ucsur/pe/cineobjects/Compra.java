package ucsur.pe.cineobjects;

import ucsur.pe.cineobjects.cineInterfaz.Totalizable;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordina una compra desde la selección de asientos hasta su confirmación, cancelación o reembolso.
 * El constructor exige un cliente y comienza un plazo de cuatro minutos; la clase también calcula
 * descuentos, IGV y puntos, y valida que las entradas, el cupón, los beneficios de socio (incluido el
 * saldo de puntos que cuestan ciertos tipos de entrada) y el pedido puedan confirmarse.
 * El plazo se comprueba con DateTime, sin hilos en segundo plano.
 */
public class Compra implements Totalizable{
    public static final String EN_PROCESO = "EN_PROCESO";
    public static final String CONFIRMADA = "CONFIRMADA";
    public static final String CANCELADA = "CANCELADA";
    public static final String EXPIRADA = "EXPIRADA";
    public static final String REEMBOLSADA = "REEMBOLSADA";

    public static final int TIEMPO_MAXIMO_COMPRA_MIN = 4;
    private static final double TASA_IGV = 0.18;
    private static int contadorSerie = 0;
    /** Cupones disponibles para buscar por código en las compras de esta ejecución. */
    private static List<Cupon> catalogoCupones = new ArrayList<>();

    private int idCompra;
    private DateTime fechaHora;
    private DateTime expiraCompra; // Hora límite para completar la compra.
    private boolean asientosBloqueados; // Indica si las butacas seleccionadas ya se bloquearon en la función.
    private String serieNumero;
    private double subtotal;
    private double descuento;
    private double igv;
    private double total;
    private int puntosCanjeados; // Puntos que cuestan los tipos de entrada elegidos; se descuentan solo al confirmar.
    private int puntosOtorgados; // Puntos ganados al confirmar; se revierten si se reembolsa.
    private String estado;
    private Cliente cliente;
    private Cupon cupon;
    private Promocion promocion;
    /** Entradas seleccionadas en esta compra, incluidas las que luego se bloquean o venden. */
    private List<Entrada> entradas;
    private Pedido pedido;

    public Compra(int idCompra, DateTime fechaHora, Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente es obligatorio");
        }
        this.idCompra = idCompra;
        this.fechaHora = fechaHora != null ? fechaHora : DateTime.ahora();
        this.cliente = cliente;
        this.estado = EN_PROCESO;
        this.entradas = new ArrayList<>();
        this.asientosBloqueados = false;

        // El plazo comienza al crear la compra, cuando el cliente entra al mapa de asientos.
        this.expiraCompra = this.fechaHora.sumarMinutos(TIEMPO_MAXIMO_COMPRA_MIN);
    }

    public static void registrarCuponEnCatalogo(Cupon c) {
        if (c == null) return;
        for (Cupon existente : catalogoCupones) {
            if (existente.getCodigo().equalsIgnoreCase(c.getCodigo()) && existente != c) {
                throw new IllegalArgumentException("Ya existe un cupón con el código " + c.getCodigo());
            }
        }
        if (!catalogoCupones.contains(c)) catalogoCupones.add(c);
    }

    /** Comprueba el plazo y expira la compra si venció; devuelve true si ya expiró. */
    public boolean verificarExpiracion() {
        if (!EN_PROCESO.equals(estado)) {
            return EXPIRADA.equals(estado);
        }
        boolean tiempoVencido = (expiraCompra != null && DateTime.ahora().esDespuesDe(expiraCompra));
        if (asientosBloqueados) {
            for (Entrada e : entradas) {
                if (e.haExpirado()) {
                    tiempoVencido = true;
                }
            }
        }
        if (tiempoVencido) {
            cancelarPorTimeout();
            return true;
        }
        return false;
    }

    private void validarEnProceso() {
        verificarExpiracion();
        if (!EN_PROCESO.equals(estado)) {
            throw new IllegalStateException("La compra ya no está EN_PROCESO (estado actual: " + estado + ")");
        }
    }

    // En esta primera fase se guardan selecciones, pero las butacas aún no quedan bloqueadas.
    public void agregarEntrada(Entrada entrada) {
        validarEnProceso();
        if (asientosBloqueados) {
            throw new IllegalStateException("Los asientos ya fueron bloqueados; no se pueden agregar más");
        }
        if (entrada == null) {
            throw new IllegalArgumentException("La entrada no puede ser nula");
        }
        if (!entrada.getButaca().estaDisponible(entrada.getFuncion())) {
            throw new IllegalStateException("La butaca " + entrada.getButaca().getCodigoAsiento() + " no está disponible");
        }
        for (Entrada existente : entradas) {
            if (existente.getButaca() == entrada.getButaca() && existente.getFuncion() == entrada.getFuncion()) {
                return;
            }
        }
        entradas.add(entrada);
        calcularTotal();
    }

    /** Quita una selección mientras los asientos sigan sin bloquearse. */
    public boolean quitarEntrada(Entrada entrada) {
        validarEnProceso();
        if (asientosBloqueados) {
            throw new IllegalStateException("No se pueden modificar los asientos una vez bloqueados");
        }
        boolean removida = entradas.remove(entrada);
        if (removida) {
            calcularTotal();
        }
        return removida;
    }

    /**
     * Valida que las butacas sigan libres y las bloquea hasta la hora límite de la compra.
     * Si alguna ya no está disponible lanza un error con su código y no bloquea ninguna:
     * el cliente debe quitar esa entrada, elegir otra butaca y volver a confirmar.
     * Si ya están bloqueadas, la operación es idempotente y devuelve true.
     */
    public boolean confirmarSeleccionAsientos() {
        validarEnProceso();
        if (asientosBloqueados) {
            return true;
        }
        if (entradas.isEmpty()) {
            return false;
        }

        // 1. Se revisan todas las butacas antes de bloquear alguna.
        List<String> noDisponibles = new ArrayList<>();
        for (Entrada e : entradas) {
            if (!e.getFuncion().estaDisponible()) {
                throw new IllegalStateException("La función ya no está disponible para la venta (" + e.getFuncion().getEstado() + ")");
            }
            if (!e.getFuncion().esButacaDisponible(e.getButaca())) {
                noDisponibles.add(e.getButaca().getCodigoAsiento());
            }
        }
        if (!noDisponibles.isEmpty()) {
            throw new IllegalStateException("Butacas no disponibles: " + String.join(", ", noDisponibles)
                    + ". Quita esas entradas y elige otras butacas.");
        }

        // 2. Se bloquean todas hasta el límite de la compra; si algo falla, se revierte lo ya bloqueado.
        List<Entrada> bloqueadas = new ArrayList<>();
        try {
            for (Entrada e : entradas) {
                if (!e.bloquearTemporalmente(this.expiraCompra)) {
                    throw new IllegalStateException("No se pudo bloquear la butaca " + e.getButaca().getCodigoAsiento());
                }
                bloqueadas.add(e);
            }
        } catch (RuntimeException ex) {
            for (Entrada e : bloqueadas) {
                e.liberarBloqueo();
            }
            throw ex;
        }

        this.asientosBloqueados = true;
        return true;
    }

    /** Asigna el pedido de dulcería; rechaza productos de oferta si el cliente no tiene el nivel requerido. */
    public void asignarPedido(Pedido pedido) {
        validarEnProceso();
        validarProductosSocio(pedido);
        this.pedido = pedido;
        calcularTotal();
    }

    // ---------- Totales ----------
    // Puntos que cuestan los tipos de entrada elegidos (solo entradas válidas con tipo asignado).
    private int calcularPuntosRequeridos() {
        int requeridos = 0;
        for (Entrada e : entradas) {
            if (!Entrada.ANULADA.equals(e.getEstado()) && !Entrada.EXPIRADA.equals(e.getEstado())
                    && e.getTipoEntrada() != null) {
                requeridos += e.getTipoEntrada().getPuntosCanje();
            }
        }
        return requeridos;
    }

    /** Recalcula subtotal, promoción, cupón, IGV y total, y los puntos que cuesta la compra. */
    @Override
    public double calcularTotal() {
        double sumaEntradas = 0;
        for (Entrada e : entradas) {
            if (!Entrada.ANULADA.equals(e.getEstado()) && !Entrada.EXPIRADA.equals(e.getEstado())) {
                sumaEntradas += e.calculaPrecioFinal();
            }
        }
        if (EN_PROCESO.equals(estado)) {
            this.puntosCanjeados = calcularPuntosRequeridos();
        }
        double sumaPedido = (pedido != null) ? pedido.calcularTotal() : 0.0;
        this.subtotal = sumaEntradas + sumaPedido;

        double descPromo = (promocion != null) ? promocion.calcularDescuento(this.subtotal) : 0.0;
        double descCupon = (cupon != null) ? cupon.calcularDescuento(this.subtotal - descPromo) : 0.0;
        this.descuento = Math.min(this.subtotal, descPromo + descCupon);

        double baseImponible = this.subtotal - this.descuento;
        this.igv = baseImponible * TASA_IGV;
        this.total = baseImponible + this.igv;
        return this.total;
    }

    /** Suma los puntos base de entradas válidas y combos incluidos en el pedido (sin multiplicador de nivel). */
    public int calcularPuntos() {
        int puntosGenerados = 0;
        for (Entrada e : entradas) {
            if (!Entrada.ANULADA.equals(e.getEstado()) && !Entrada.EXPIRADA.equals(e.getEstado())
                    && e.getTipoEntrada() != null) {
                puntosGenerados += e.getTipoEntrada().obtenerPuntos();
            }
        }
        if (pedido != null) {
            for (DetallePedido d : pedido.getDetalles()) {
                if (d.getProducto().isEsCombo()) {
                    puntosGenerados += d.getCantidad() * d.getProducto().obtenerPuntos();
                }
            }
        }
        return puntosGenerados;
    }

    // ---------- Cupón y promoción ----------
    /** Busca el cupón por código sin distinguir mayúsculas y lo aplica si sigue vigente. */
    public boolean aplicarCupon(String codigo) {
        validarEnProceso();
        if (codigo == null) return false;
        for (Cupon c : catalogoCupones) {
            if (codigo.equalsIgnoreCase(c.getCodigo())) {
                return aplicarCupon(c);
            }
        }
        return false;
    }

    /** Asigna un cupón vigente a la compra y actualiza el total. */
    public boolean aplicarCupon(Cupon cupon) {
        validarEnProceso();
        if (cupon != null && cupon.validarVigencia(DateTime.ahora())) {
            this.cupon = cupon;
            calcularTotal();
            return true;
        }
        return false;
    }

    /** Asigna una promoción aplicable a la compra y actualiza el total. */
    public boolean aplicarPromocion(Promocion promocion) {
        validarEnProceso();
        if (promocion != null && promocion.esAplicable(this)) {
            this.promocion = promocion;
            calcularTotal();
            return true;
        }
        return false;
    }

    // ---------- Beneficios de socio ----------
    /**
     * Comprueba que las entradas de socio y los combos de oferta los use un cliente con el nivel requerido,
     * y que el saldo de puntos alcance para los tipos de entrada que cuestan puntos.
     */
    public void validarBeneficiosSocio() {
        for (Entrada e : entradas) {
            if (Entrada.ANULADA.equals(e.getEstado()) || Entrada.EXPIRADA.equals(e.getEstado())) continue;
            TipoEntrada t = e.getTipoEntrada();
            if (t != null && !t.validarElegibilidad(cliente)) {
                throw new IllegalStateException("El cliente no puede usar la entrada '" + t.getNombre()
                        + "' (es exclusiva para socios o está inactiva)");
            }
        }
        int requeridos = calcularPuntosRequeridos();
        if (requeridos > cliente.consultarPuntos()) {
            throw new IllegalStateException("Puntos insuficientes: la compra requiere " + requeridos
                    + " y el saldo es " + cliente.consultarPuntos());
        }
        validarProductosSocio(this.pedido);
    }

    private void validarProductosSocio(Pedido p) {
        if (p == null) return;
        for (DetallePedido d : p.getDetalles()) {
            Producto prod = d.getProducto();
            if (!prod.validarElegibilidad(cliente)) {
                throw new IllegalStateException("El producto '" + prod.getNombre()
                        + "' no está disponible para este cliente (nivel de socio requerido: "
                        + prod.getNivelSocioMinimo() + ", o producto inactivo)");
            }
        }
    }

    // ---------- Confirmación, cancelación y reembolso ----------
    /**
     * Confirma la venta si el plazo sigue vigente, los asientos ya están bloqueados y los datos son válidos.
     * Antes de cambiar estados valida las entradas, los beneficios de socio, los puntos, el cupón y el pedido.
     */
    public boolean confirmar() {
        if (!EN_PROCESO.equals(estado)) return false;

        // 1. Verificar si los 4 minutos ya expiraron
        if (verificarExpiracion()) {
            return false;
        }

        // Las butacas deben haberse bloqueado antes con confirmarSeleccionAsientos()
        if (!entradas.isEmpty() && !asientosBloqueados) {
            return false;
        }

        List<Entrada> porVender = new ArrayList<>();
        for (Entrada e : entradas) {
            if (e.haExpirado()) {
                cancelarPorTimeout();
                return false;
            }
            if (e.getTipoEntrada() == null) {
                return false; // Toda entrada debe tener asignado su TipoEntrada
            }
            if (Entrada.BLOQUEADA.equals(e.getEstado())) {
                porVender.add(e);
            }
        }

        // 2. La compra debe tener algo que vender
        if (porVender.isEmpty() && pedido == null) return false;
        // 3. El cupón debe seguir vigente
        if (cupon != null && !cupon.validarVigencia(DateTime.ahora())) return false;

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

        // 5. Registrar el pedido de dulcería (verifica y descuenta stock)
        if (pedido != null && !pedido.isRegistrado() && !pedido.registrar()) return false;

        // 6. Ya no hay nada que pueda fallar: se confirma todo
        for (Entrada e : porVender) {
            e.confirmarVenta();
        }
        if (cupon != null) {
            cupon.registrarUso();
        }
        if (puntosCanjeados > 0) {
            cliente.canjearPuntos(puntosCanjeados);
        }
        this.serieNumero = String.format("B001-%06d", ++contadorSerie);
        this.estado = CONFIRMADA;

        // Los puntos ganados usan el nivel que tenía el cliente antes de esta compra
        if (cliente.esSocio()) {
            this.puntosOtorgados = (int) Math.floor(calcularPuntos() * cliente.obtenerMultiplicadorPuntos());
            cliente.acumularPuntos(this.puntosOtorgados);
        }
        cliente.actualizarNivel();
        return true;
    }

    /** Cancela una compra en proceso y libera las butacas que estuvieran bloqueadas. */
    public void cancelar() {
        validarEnProceso();
        for (Entrada e : entradas) {
            if (Entrada.BLOQUEADA.equals(e.getEstado())) {
                e.anular();
            }
        }
        this.asientosBloqueados = false;
        this.expiraCompra = null;
        this.puntosCanjeados = 0; // los puntos nunca se descontaron del saldo
        this.estado = CANCELADA;
    }

    /** Expira la compra por tiempo y libera los bloqueos; no cambia compras ya finalizadas. */
    public void cancelarPorTimeout() {
        if (!EN_PROCESO.equals(estado)) return;
        for (Entrada e : entradas) {
            if (Entrada.BLOQUEADA.equals(e.getEstado())) {
                if (asientosBloqueados) {
                    e.expirar(); // Libera la butaca en la función y pasa la entrada a EXPIRADA
                } else {
                    e.anular();  // Si nunca salió del mapa, solo marca la preselección como no válida
                }
            }
        }
        this.asientosBloqueados = false;
        this.expiraCompra = null;
        this.puntosCanjeados = 0; // los puntos nunca se descontaron del saldo
        this.estado = EXPIRADA;
    }

    /**
     * Reembolsa solo si todas las funciones fueron canceladas y el pedido no fue entregado.
     * Devuelve los puntos canjeados y quita los puntos ganados con esta compra.
     */
    public boolean reembolsar() {
        if (!CONFIRMADA.equals(estado)) return false;
        if (entradas.isEmpty()) return false;
        for (Entrada e : entradas) {
            if (!Funcion.CANCELADA.equals(e.getFuncion().getEstado())) {
                return false;
            }
        }
        if (pedido != null && Pedido.ENTREGADO.equals(pedido.getEstado())) {
            return false;
        }
        for (Entrada e : entradas) {
            if (Entrada.VENDIDA.equals(e.getEstado())) {
                e.anular();
            }
        }
        this.estado = REEMBOLSADA;
        cliente.devolverPuntos(puntosCanjeados);
        cliente.revertirPuntos(puntosOtorgados);
        return true;
    }

    public String generarComprobante() {
        if (serieNumero == null) {
            throw new IllegalStateException("La compra aún no tiene comprobante (no está confirmada)");
        }
        return String.format("Comprobante %s | Fecha: %s | Subtotal: S/ %.2f | Desc: S/ %.2f | IGV: S/ %.2f | Total: S/ %.2f",
                serieNumero, fechaHora, subtotal, descuento, igv, total);
    }

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
    public boolean isAsientosBloqueados() { return asientosBloqueados; }
    public String getSerieNumero() { return serieNumero; }
    public double getSubtotal() { return subtotal; }
    public double getDescuento() { return descuento; }
    public double getIgv() { return igv; }
    public double getTotal() { return total; }
    public int getPuntosCanjeados() { return puntosCanjeados; }
    public int getPuntosOtorgados() { return puntosOtorgados; }
    public String getEstado() { return estado; }
    public Cliente getCliente() { return cliente; }
    public Cupon getCupon() { return cupon; }
    public Promocion getPromocion() { return promocion; }
    public Pedido getPedido() { return pedido; }
    public List<Entrada> getEntradas() { return new ArrayList<>(entradas); }

    @Override
    public String toString() {
        return "Compra #" + idCompra + " [" + estado + "] S/ " + String.format("%.2f", total);
    }
}