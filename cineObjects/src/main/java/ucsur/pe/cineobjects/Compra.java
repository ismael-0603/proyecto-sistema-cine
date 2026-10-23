package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
/**
 *
 * @author user
 */
public class Compra {
    public static final String EN_PROCESO = "EN_PROCESO";
    public static final String CONFIRMADA = "CONFIRMADA";
    public static final String CANCELADA = "CANCELADA";
    public static final String EXPIRADA = "EXPIRADA";
    public static final String REEMBOLSADA = "REEMBOLSADA";

    public static final int TIEMPO_MAXIMO_COMPRA_MIN = 4;
    private static final double TASA_IGV = 0.18;
    private static int contadorSerie = 0;
    private static List<Cupon> catalogoCupones = new ArrayList<>();

    private int idCompra;
    private DateTime fechaHora;
    private DateTime expiraCompra; // Límite de 4 minutos desde que abre el mapa
    private boolean asientosBloqueados;
    private Timer temporizadorExpiracion;
    private String serieNumero;
    private double subtotal;
    private double descuento;
    private double igv;
    private double total;
    private int puntosCanjeados;
    private String estado;
    private Cliente cliente;
    private Cupon cupon;
    private Promocion promocion;
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

        // MODELO A: El reloj de 4 minutos inicia apenas se crea la compra (al entrar al mapa)
        this.expiraCompra = this.fechaHora.sumarMinutos(TIEMPO_MAXIMO_COMPRA_MIN);
        iniciarTemporizador();
    }

    public static void registrarCuponEnCatalogo(Cupon c) {
        if (c != null && !catalogoCupones.contains(c)) catalogoCupones.add(c);
    }

    // Verifica en cualquier operación si ya se cumplieron los 4 minutos desde que inició la compra
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

    // FASE 1: El usuario selecciona asientos en el mapa (siguen DISPONIBLES en la función)
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

    // Permite al usuario desmarcar o cambiar de butaca libremente mientras sigue en el mapa
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

    // FASE 2: Se llama cuando el usuario confirma sus sitios y pasa a elegir Tipos de Entrada / Combos.
    // Bloquea únicamente las butacas seleccionadas por el tiempo restante de los 4 minutos.
    public synchronized boolean confirmarSeleccionAsientos() {
        validarEnProceso();
        if (asientosBloqueados) {
            return true;
        }
        if (entradas.isEmpty()) {
            return false;
        }

        // 1. Verificar que todas las butacas elegidas sigan disponibles en este instante
        for (Entrada e : entradas) {
            if (!e.getFuncion().estaDisponible() || !e.getFuncion().esButacaDisponible(e.getButaca())) {
                return false;
            }
        }

        // 2. Bloquear las butacas seleccionadas heredando la hora límite (expiraCompra)
        for (Entrada e : entradas) {
            if (!e.bloquearTemporalmente(this.expiraCompra)) {
                cancelarPorTimeout();
                return false;
            }
        }

        this.asientosBloqueados = true;
        return true;
    }

    // Inicia el temporizador automático de 4 minutos exactos en segundo plano
    private void iniciarTemporizador() {
        detenerTemporizador();
        temporizadorExpiracion = new Timer(true); // Hilo daemon
        long milisegundos = TIEMPO_MAXIMO_COMPRA_MIN * 60L * 1000L;
        temporizadorExpiracion.schedule(new TimerTask() {
            @Override
            public void run() {
                cancelarPorTimeout();
            }
        }, milisegundos);
    }

    private void detenerTemporizador() {
        if (temporizadorExpiracion != null) {
            temporizadorExpiracion.cancel();
            temporizadorExpiracion = null;
        }
    }

    public void asignarPedido(Pedido pedido) {
        validarEnProceso();
        this.pedido = pedido;
        calcularTotal();
    }

    public double calcularTotal() {
        double sumaEntradas = 0;
        for (Entrada e : entradas) {
            if (!Entrada.ANULADA.equals(e.getEstado()) && !Entrada.EXPIRADA.equals(e.getEstado())) {
                sumaEntradas += e.calculaPrecioFinal();
            }
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

    public boolean aplicarCupon(Cupon cupon) {
        validarEnProceso();
        if (cupon != null && cupon.validarVigencia(DateTime.ahora())) {
            this.cupon = cupon;
            calcularTotal();
            return true;
        }
        return false;
    }

    public boolean aplicarPromocion(Promocion promocion) {
        validarEnProceso();
        if (promocion != null && promocion.esAplicable(this)) {
            this.promocion = promocion;
            calcularTotal();
            return true;
        }
        return false;
    }

    // FASE 3: Confirmación de la compra al procesar el pago dentro de los 4 minutos
    public synchronized boolean confirmar() {
        if (!EN_PROCESO.equals(estado)) return false;

        // 1. Verificar si los 4 minutos ya expiraron
        if (verificarExpiracion()) {
            return false;
        }

        // Si las entradas no habían sido bloqueadas explícitamente, las bloquea ahora
        if (!entradas.isEmpty() && !asientosBloqueados) {
            if (!confirmarSeleccionAsientos()) {
                return false;
            }
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

        // 4. Registrar el pedido de dulcería
        if (pedido != null && !pedido.isRegistrado() && !pedido.registrar()) return false;

        // 5. Detener el temporizador de 4 minutos y confirmar todo
        detenerTemporizador();
        for (Entrada e : porVender) {
            e.confirmarVenta();
        }
        if (cupon != null) {
            cupon.registrarUso();
        }
        this.serieNumero = String.format("B001-%06d", ++contadorSerie);
        this.estado = CONFIRMADA;

        if (cliente.esSocio()) {
            cliente.acumularPuntos(calcularPuntos());
        }
        cliente.actualizarNivel();
        return true;
    }

    public synchronized void cancelar() {
        validarEnProceso();
        detenerTemporizador();
        if (asientosBloqueados) {
            for (Entrada e : entradas) {
                if (Entrada.BLOQUEADA.equals(e.getEstado())) {
                    e.anular();
                }
            }
        }
        this.asientosBloqueados = false;
        this.expiraCompra = null;
        this.estado = CANCELADA;
    }

    // Se ejecuta automáticamente al cumplirse los 4 minutos sin haber confirmado el pago
    public synchronized void cancelarPorTimeout() {
        if (!EN_PROCESO.equals(estado)) return;
        detenerTemporizador();
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
        this.estado = EXPIRADA;
    }

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
        return true;
    }

    public String generarComprobante() {
        if (serieNumero == null) {
            throw new IllegalStateException("La compra aún no tiene comprobante (no está confirmada)");
        }
        return String.format("Comprobante %s | Fecha: %s | Subtotal: S/ %.2f | Desc: S/ %.2f | IGV: S/ %.2f | Total: S/ %.2f",
                serieNumero, fechaHora, subtotal, descuento, igv, total);
    }

    public void setEstado(String estado) {
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
