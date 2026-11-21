package ucsur.pe.model;

import ucsur.pe.util.ConversorDinero;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Por qué existe: es lo que se ve en el comprobante (wireframe 9), al tocar «Ver» en Mis compras (wireframe 23)
 * y lo que se imprime o envía por correo: el resumen de una compra ya pagada con sus códigos QR, el medio
 * de pago y, si el cliente es socio, los puntos ganados, su saldo y su nivel.
 * No guarda datos de la tarjeta: el pago pasa por la pasarela y el kiosco solo conoce el medio.
 * Es una foto de la compra en ese momento; no cambia nada.
 */
public final class DetalleCompra {
    private final String serieNumero;
    private final String estado;
    private final DateTime fechaCompra;
    private final String pelicula;      // null si la compra fue solo de dulcería
    private final String sedeYSala;
    private final String fechaHoraFuncion;
    private final List<String> butacas = new ArrayList<>();
    private final Map<String, Integer> entradasPorTipo = new LinkedHashMap<>(); // "General" -> 2
    private final List<String> dulceria = new ArrayList<>();                    // "1 x Combo pareja"
    private final ResumenImporte importe;
    private final long montoReembolsadoCentavos; // 0 si la compra no se reembolsó
    private final List<String> codigosQR = new ArrayList<>(); // de las entradas vendidas y del pedido
    private final String medioPago;     // "Tarjeta de crédito", "Yape"...; null si no se conoce
    private final int puntosGanados;    // 0 para invitados
    private final Integer saldoPuntos;  // null si el cliente no es socio
    private final Integer nivelSocio;   // null si el cliente no es socio

    DetalleCompra(Compra compra, String medioPago) {
        this.medioPago = TransaccionPago.describirMedio(medioPago);
        this.puntosGanados = compra.getPuntosOtorgados();
        Cliente cliente = compra.getCliente();
        boolean socio = cliente != null && cliente.esSocio();
        this.saldoPuntos = socio ? cliente.consultarPuntos() : null;
        this.nivelSocio = socio ? cliente.getNivelSocio() : null;
        this.serieNumero = compra.getSerieNumero();
        this.estado = compra.getEstado();
        this.fechaCompra = compra.getFechaHora();
        this.importe = compra.getImporte();
        this.montoReembolsadoCentavos = compra.getMontoReembolsadoCentavos();
        Funcion funcion = null;
        for (Entrada e : compra.getEntradas()) {
            if (Entrada.EXPIRADA.equals(e.getEstado())) continue;
            funcion = e.getFuncion();
            butacas.add(e.getButaca().getCodigoAsiento());
            String tipo = e.getTipoEntrada() != null ? e.getTipoEntrada().getNombre() : "Sin tipo";
            entradasPorTipo.merge(tipo, 1, Integer::sum);
            if (Entrada.VENDIDA.equals(e.getEstado()) && e.getCodigoQR() != null) {
                codigosQR.add(e.getCodigoQR());
            }
        }
        this.pelicula = funcion != null ? funcion.getPelicula().getTitulo() : null;
        this.sedeYSala = compra.getSede().getNombre() + (funcion != null ? " · Sala " + funcion.getSala().getNumero() : "");
        this.fechaHoraFuncion = funcion != null
                ? funcion.getFecha().soloFecha() + " " + funcion.getHoraInicio().soloHora() + " · " + funcion.getIdioma()
                : null;
        Pedido pedido = compra.getPedido();
        if (pedido != null) {
            for (DetallePedido d : pedido.getDetalles()) {
                String eleccion = d.getPersonalizacion() == null || d.getPersonalizacion().isBlank()
                        ? "" : " (" + d.getPersonalizacion() + ")"; // RF-13: sabor y gaseosa del combo
                dulceria.add(d.getCantidad() + " x " + d.getProducto().getNombre() + eleccion);
            }
            if (pedido.getCodigoQR() != null && !Pedido.ANULADO.equals(pedido.getEstado())) {
                codigosQR.add(pedido.getCodigoQR());
            }
        }
    }

    /** Texto del ticket para imprimir (RF-21) o para el cuerpo del correo (RF-22). */
    public String generarTexto() {
        List<String> lineas = new ArrayList<>();
        lineas.add("Comprobante " + serieNumero + " [" + estado + "]");
        lineas.add("Fecha de compra: " + fechaCompra);
        if (pelicula != null) {
            lineas.add("Película: " + pelicula);
            lineas.add("Sede · Sala: " + sedeYSala);
            lineas.add("Fecha y hora: " + fechaHoraFuncion);
            lineas.add("Butacas: " + String.join(", ", butacas));
            List<String> tipos = new ArrayList<>();
            entradasPorTipo.forEach((tipo, cant) -> tipos.add(cant + " x " + tipo));
            lineas.add("Entradas: " + String.join(", ", tipos));
        } else {
            lineas.add("Sede: " + sedeYSala);
        }
        if (!dulceria.isEmpty()) {
            lineas.add("Dulcería: " + String.join(", ", dulceria));
        }
        lineas.add("Descuento: -" + ConversorDinero.formatear(importe.getDescuentoCentavos()));
        lineas.add("IGV: " + ConversorDinero.formatear(importe.getIgvCentavos()));
        lineas.add("Total pagado: " + ConversorDinero.formatear(importe.getTotalCentavos()));
        if (medioPago != null) {
            lineas.add("Pagado con: " + medioPago);
        }
        if (puntosGanados > 0) {
            lineas.add("Puntos ganados: +" + puntosGanados);
        }
        if (Compra.REEMBOLSADA.equals(estado)) {
            lineas.add("Devuelto (función cancelada): " + ConversorDinero.formatear(montoReembolsadoCentavos));
        }
        for (String qr : codigosQR) {
            lineas.add("QR: " + qr);
        }
        return String.join(System.lineSeparator(), lineas);
    }

    public String getSerieNumero() { return serieNumero; }
    public String getEstado() { return estado; }
    public DateTime getFechaCompra() { return fechaCompra; }
    public String getPelicula() { return pelicula; }
    public String getSedeYSala() { return sedeYSala; }
    public String getFechaHoraFuncion() { return fechaHoraFuncion; }
    public List<String> getButacas() { return List.copyOf(butacas); }
    public Map<String, Integer> getEntradasPorTipo() { return Map.copyOf(entradasPorTipo); }
    public List<String> getDulceria() { return List.copyOf(dulceria); }
    public ResumenImporte getImporte() { return importe; }
    public long getMontoReembolsadoCentavos() { return montoReembolsadoCentavos; }
    public List<String> getCodigosQR() { return List.copyOf(codigosQR); }
    public String getMedioPago() { return medioPago; }
    public int getPuntosGanados() { return puntosGanados; }
    /** Saldo de puntos del socio al ver el comprobante; null para invitados. */
    public Integer getSaldoPuntos() { return saldoPuntos; }
    /** Nivel de socio (1 a 4); null para invitados. */
    public Integer getNivelSocio() { return nivelSocio; }

    @Override
    public String toString() {
        return generarTexto();
    }
}
