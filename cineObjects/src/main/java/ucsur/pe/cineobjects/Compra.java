package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.List;
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

    private static final double TASA_IGV = 0.18;
    private static final int SOLES_POR_PUNTO = 10; // 1 punto por cada S/ 10 de compra
    private static int contadorSerie = 0;
    private static List<Cupon> catalogoCupones = new ArrayList<>();

    private int idCompra;
    private DateTime fechaHora;
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
    }

    public static void registrarCuponEnCatalogo(Cupon c) {
        if (c != null && !catalogoCupones.contains(c)) catalogoCupones.add(c);
    }

    private void validarEnProceso() {
        if (!EN_PROCESO.equals(estado)) {
            throw new IllegalStateException("La compra ya no está EN_PROCESO (estado actual: " + estado + ")");
        }
    }

    public void agregarEntrada(Entrada entrada) {
        validarEnProceso();
        if (entrada == null) {
            throw new IllegalArgumentException("La entrada no puede ser nula");
        }
        if (!entradas.contains(entrada)) {
            entradas.add(entrada);
            calcularTotal();
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

    // Firma del diagrama: + aplicarCupon(codigo: String): boolean
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

    // Sobrecarga pasando el objeto Cupon directamente
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
    
    public int calcularPuntos() {
    int puntosGenerados = 0;

    // Puntos por cada entrada válida en la compra
    for (Entrada e : entradas) {
        if (!Entrada.ANULADA.equals(e.getEstado()) && !Entrada.EXPIRADA.equals(e.getEstado())) {
            puntosGenerados += e.getTipoEntrada().obtenerPuntos();
        }
    }

    // Puntos por cada combo incluido en el pedido de dulcería
    if (pedido != null) {
        for (DetallePedido d : pedido.getDetalles()) {
            if (d.getProducto().isEsCombo()) {
                puntosGenerados += d.getCantidad() * d.getProducto().obtenerPuntos();
            }
        }
    }

    return puntosGenerados;
}
    // Primero revisa todo y recién después cambia estados, para no dejar la compra a medias.
    // Devuelve false si no se pudo confirmar.
    public boolean confirmar() {
    if (!EN_PROCESO.equals(estado)) return false;

    // 1. Entradas: si alguna expiró, se cancela toda la compra
    List<Entrada> porVender = new ArrayList<>();
    for (Entrada e : entradas) {
        if (e.haExpirado()) {
            cancelarPorTimeout();
            return false;
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

    // 4. Registrar el pedido (verifica y descuenta stock; si falla, no se ha cambiado nada)
    if (pedido != null && !pedido.isRegistrado() && !pedido.registrar()) return false;

    // 5. Ya no hay nada que pueda fallar: se confirma todo
    for (Entrada e : porVender) {
        e.confirmarVenta();
    }
    if (cupon != null) {
        cupon.registrarUso();
    }
    this.serieNumero = String.format("B001-%06d", ++contadorSerie);
    this.estado = CONFIRMADA;

    // 6. Acumulación de puntos por entradas/combos y actualización de nivel de socio
    if (cliente.esSocio()) {
        cliente.acumularPuntos(calcularPuntos());
    }
    cliente.actualizarNivel(); // el estado ya es CONFIRMADA, así que cuenta como visita
    return true;
}

    // Solo se cancela una compra EN_PROCESO (una CONFIRMADA se reembolsa)
    public void cancelar() {
        validarEnProceso();
        liberarEntradas();
        this.estado = CANCELADA;
    }

    public void cancelarPorTimeout() {
        if (!EN_PROCESO.equals(estado)) return;
        liberarEntradas();
        this.estado = EXPIRADA;
    }

    // Libera las butacas: solo se anulan las entradas que siguen BLOQUEADAS
    private void liberarEntradas() {
        for (Entrada e : entradas) {
            if (Entrada.BLOQUEADA.equals(e.getEstado())) {
                e.anular();
            }
        }
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
    
    // Solo se reembolsa si una funcion se CANCELA
    // En Compra.java: reemplaza el método reembolsar() existente por este
    public boolean reembolsar() {
        if (!CONFIRMADA.equals(estado)) return false;

        // Regla de negocio: no procede si no hay entradas o si la función no está CANCELADA
        if (entradas.isEmpty()) return false;
        for (Entrada e : entradas) {
            if (!Funcion.CANCELADA.equals(e.getFuncion().getEstado())) {
                return false; 
            }
        }

        if (pedido != null && Pedido.ENTREGADO.equals(pedido.getEstado())) {
            return false; // el pedido de dulcería ya fue consumido
        }

        for (Entrada e : entradas) {
            if (Entrada.VENDIDA.equals(e.getEstado())) {
                e.anular();
            }
        }
        this.estado = REEMBOLSADA;
        return true;
    }

    public int getIdCompra() { return idCompra; }
    public DateTime getFechaHora() { return fechaHora; }
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
