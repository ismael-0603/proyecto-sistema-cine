package ucsur.pe.vista;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DetalleCompra;
import ucsur.pe.model.Entrada;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.ResumenImporte;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Por qué existe: «Resumen y pago», «Datos de tarjeta» y «Pago con billetera» muestran los mismos importes
 * de la compra (entradas, dulcería, descuentos, op. gravada, IGV y total). Arma esas filas en un solo lugar.
 * Los montos salen de la compra (ResumenImporte); aquí solo se les da formato.
 */
final class ResumenCompra {

    private ResumenCompra() { }

    /** «2 × General, 1 × Niño» */
    static String entradasPorTipo(Compra compra) {
        Map<String, Integer> porTipo = new LinkedHashMap<>();
        for (Entrada e : compra.getEntradas()) {
            String nombre = e.getTipoEntrada() == null ? "Entrada" : e.getTipoEntrada().getNombre();
            porTipo.merge(nombre, 1, Integer::sum);
        }
        return String.join(", ", porTipo.entrySet().stream().map(x -> x.getValue() + " × " + x.getKey()).toList());
    }

    static String butacas(Compra compra) {
        return String.join(", ", compra.getEntradas().stream().map(e -> e.getButaca().getCodigoAsiento()).toList());
    }

    static long montoEntradas(Compra compra) {
        long total = 0;
        for (Entrada e : compra.getEntradas()) total += e.getPrecioCentavos();
        return total;
    }

    static long montoDulceria(Compra compra) {
        return compra.getPedido() == null ? 0 : compra.getPedido().getTotalCentavos();
    }

    /** Filas de importes: entradas, dulcería, descuento, op. gravada, IGV (el total va aparte). */
    static List<Node> filasImporte(Compra compra) {
        ResumenImporte imp = compra.getImporte();
        String detalleEntradas = entradasPorTipo(compra);
        return List.of(
                fila("Entradas" + (detalleEntradas.isEmpty() ? "" : " (" + detalleEntradas + ")"),
                        Formato.dinero(montoEntradas(compra)), true),
                fila("Dulcería", Formato.dinero(montoDulceria(compra)), true),
                fila("Descuento (cupón y promoción)", "- " + Formato.dinero(imp.getDescuentoCentavos()), true),
                fila("Op. Gravada", Formato.dinero(imp.getTotalCentavos() - imp.getIgvCentavos()), true),
                fila("IGV (18 %)", Formato.dinero(imp.getIgvCentavos()), true));
    }

    /** Datos de la función para la columna «Resumen de tu compra» (8.1 y 8.2). */
    static List<Node> filasFuncion(Funcion f, Compra compra) {
        return List.of(
                fila("Película", f.getPelicula().getTitulo(), false),
                fila("Sede", f.getSala().getSede().getNombre(), false),
                fila("Sala", String.valueOf(f.getSala().getNumero()), false),
                fila("Función", f.getHoraInicio().soloHora() + " · " + Formato.capitalizar(f.getIdioma()), false),
                fila("Fecha", Formato.fechaConAnio(f.getFecha()), false),
                fila("Butacas", butacas(compra), false));
    }

    /** Filas del comprobante (9) y del detalle de compra (23), a partir del DetalleCompra que arma el modelo. */
    static List<Node> filasDetalle(DetalleCompra d) {
        ResumenImporte imp = d.getImporte();
        List<String> entradas = new java.util.ArrayList<>();
        d.getEntradasPorTipo().forEach((tipo, n) -> entradas.add(n + " × " + tipo));
        return List.of(
                fila("Película", valor(d.getPelicula()), true),
                fila("Sede · Sala", valor(d.getSedeYSala()), true),
                fila("Fecha y hora", valor(d.getFechaHoraFuncion()), true),
                fila("Butacas", d.getButacas().isEmpty() ? "—" : String.join(", ", d.getButacas()), true),
                fila("Entradas", entradas.isEmpty() ? "—" : String.join(", ", entradas), true),
                fila("Dulcería", d.getDulceria().isEmpty() ? "—" : String.join(", ", d.getDulceria()), true),
                fila("Descuento", "- " + Formato.dinero(imp.getDescuentoCentavos()), true),
                fila("IGV (18 %)", Formato.dinero(imp.getIgvCentavos()), true));
    }

    /** «Total pagado  S/ 95.00» (o «Total reembolsado» si la compra se reembolsó). */
    static HBox filaTotalPagado(DetalleCompra d) {
        Label izq = new Label("Total pagado");
        izq.getStyleClass().add("total-grande");
        Label der = new Label(Formato.dinero(d.getImporte().getTotalCentavos()));
        der.getStyleClass().add("total-grande");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox h = new HBox(izq, espacio, der);
        h.setPadding(new Insets(10, 0, 4, 0));
        return h;
    }

    private static String valor(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    static HBox fila(String etiqueta, String valor, boolean valorFuerte) {
        Label izq = new Label(etiqueta);
        izq.getStyleClass().add("etiqueta");
        izq.setWrapText(true);
        Label der = new Label(valor);
        der.getStyleClass().add(valorFuerte ? "importe" : "etiqueta");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox h = new HBox(12, izq, espacio, der);
        h.getStyleClass().add("fila-resumen");
        return h;
    }

    /** Fila final «Total  S/ 95.00» en grande. */
    static HBox filaTotal(Compra compra) {
        Label izq = new Label("Total");
        izq.getStyleClass().add("total-grande");
        Label der = new Label(Formato.dinero(compra.getTotalCentavos()));
        der.getStyleClass().add("total-grande");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox h = new HBox(izq, espacio, der);
        h.setPadding(new Insets(10, 0, 4, 0));
        return h;
    }

    /** Tarjeta completa «Resumen de tu compra» de las pantallas 8.1 y 8.2. */
    static VBox tarjeta(Funcion f, Compra compra, String notaSeguridad) {
        Label titulo = new Label("Resumen de tu compra");
        titulo.getStyleClass().add("tarjeta-titulo");
        VBox v = new VBox(2, titulo);
        v.getChildren().addAll(filasFuncion(f, compra));
        v.getChildren().addAll(filasImporte(compra));
        v.getChildren().add(filaTotal(compra));
        Label seguro = new Label("🛡  Pago 100 % seguro\n" + notaSeguridad);
        seguro.getStyleClass().addAll("caja-dato", "nota");
        seguro.setMaxWidth(Double.MAX_VALUE);
        v.getChildren().add(seguro);
        v.getStyleClass().add("tarjeta");
        return v;
    }
}
