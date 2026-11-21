package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DetallePedido;
import ucsur.pe.model.Entrada;
import ucsur.pe.model.OpcionesCombo;
import ucsur.pe.model.Producto;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 7 · Dulcería: el menú de la sede para este cliente. Cada −/+ cambia el pedido de la compra en el servicio,
 * que valida stock y ofertas de socio y recalcula el total. En los combos con canchita o gaseosa, el «+»
 * abre primero la ventana para elegir sabor y gaseosa (RF-13); cada combinación es una línea del pedido.
 */
public class DulceriaVista extends Pantalla {

    private static final String TODOS = "Todos";
    // Tarjeta de 280 px menos el relleno lateral (14 + 14); las fotos se guardan en 480×300 (misma proporción)
    private static final double ANCHO_IMAGEN = 252;
    private static final double ALTO_IMAGEN = 158;

    @FXML private EncabezadoCompraVista encabezadoController;
    @FXML private HBox categorias;
    @FXML private Label lblMensaje;
    @FXML private FlowPane productos;
    @FXML private Label lblEntradas;
    @FXML private Label lblMontoEntradas;
    @FXML private VBox lineas;
    @FXML private Label lblSubtotalDulceria;
    @FXML private Label lblTotal;
    @FXML private Label lblPieTotal;

    private Compra compra;
    private List<Producto> menu = new ArrayList<>();
    private String categoria = TODOS;
    private final List<Button> chips = new ArrayList<>();

    public DulceriaVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        encabezadoController.setPaso(2);
        compra = estado().getCompra();
        if (compra == null) {
            javafx.application.Platform.runLater(() -> nav.ir("cartelera"));
            return;
        }
        Respuesta<List<Producto>> r = app().dulceria().consultarMenu(estado().getSede(), estado().cliente());
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
        } else {
            menu = r.dato();
        }
        cargarCategorias();
        mostrarProductos();
        actualizarPedido();
    }

    private void cargarCategorias() {
        Set<String> nombres = new LinkedHashSet<>();
        nombres.add(TODOS);
        for (Producto p : menu) nombres.add(Formato.capitalizar(p.getCategoria()));
        for (String nombre : nombres) {
            Button b = new Button(nombre);
            b.getStyleClass().add("chip");
            b.setOnAction(e -> {
                categoria = nombre;
                chips.forEach(c -> c.getStyleClass().remove("activo"));
                b.getStyleClass().add("activo");
                mostrarProductos();
            });
            chips.add(b);
            categorias.getChildren().add(b);
        }
        chips.get(0).getStyleClass().add("activo");
    }

    private void mostrarProductos() {
        productos.getChildren().clear();
        for (Producto p : menu) {
            if (TODOS.equals(categoria) || Formato.capitalizar(p.getCategoria()).equals(categoria)) {
                productos.getChildren().add(tarjeta(p));
            }
        }
        if (menu.isEmpty()) mostrarError(lblMensaje, "No hay productos disponibles en esta sede.");
    }

    private VBox tarjeta(Producto p) {
        // Foto del producto; sin foto, el recuadro muestra su categoría
        Node imagen = Formato.imagenProducto(p, ANCHO_IMAGEN, ALTO_IMAGEN);

        Label nombre = new Label(p.getNombre().toUpperCase());
        nombre.getStyleClass().add("producto-nombre");
        nombre.setWrapText(true);
        Label descripcion = new Label(p.getDescripcion());
        descripcion.getStyleClass().add("nota-suave");
        descripcion.setWrapText(true);
        descripcion.setMinHeight(36);
        descripcion.setAlignment(Pos.TOP_LEFT);

        Label precio = new Label(Formato.dinero(p.getPrecioCentavos()));
        precio.getStyleClass().add("producto-precio");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        boolean agotado = p.estaAgotado(estado().getSede());
        HBox pie;
        if (agotado) {
            Label sinStock = new Label("Agotado");
            sinStock.getStyleClass().add("tag-genero");
            pie = new HBox(precio, espacio, sinStock);
        } else {
            Button menos = new Button("−");
            menos.getStyleClass().add("contador-menos");
            Label valor = new Label(String.valueOf(cantidadEnPedido(p)));
            valor.getStyleClass().add("contador-valor");
            Button mas = new Button("+");
            mas.getStyleClass().add("contador-mas");
            menos.setOnAction(e -> cambiar(p, -1));
            mas.setOnAction(e -> cambiar(p, +1));
            HBox contador = new HBox(menos, valor, mas);
            contador.getStyleClass().add("contador");
            pie = new HBox(precio, espacio, contador);
        }
        pie.setAlignment(Pos.CENTER_LEFT);

        VBox tarjeta = new VBox(6, imagen, nombre, descripcion, pie);
        tarjeta.getStyleClass().add("producto");
        tarjeta.setPrefWidth(280);
        tarjeta.setMaxWidth(280);
        return tarjeta;
    }

    /**
     * La línea del pedido con ese producto y esa personalización. Un combo puede estar en varias líneas
     * (uno con Inca Kola y otro con Coca-Cola); un producto sin personalizar, en una sola.
     */
    private DetallePedido lineaDe(Producto p, String personalizacion) {
        if (compra.getPedido() == null) return null;
        for (DetallePedido d : compra.getPedido().getDetalles()) {
            if (d.getProducto().getIdProducto() == p.getIdProducto()
                    && Objects.equals(d.getPersonalizacion(), personalizacion)) return d;
        }
        return null;
    }

    /** La última línea agregada de ese producto, sea cual sea su personalización: la que quita el «−». */
    private DetallePedido ultimaLineaDe(Producto p) {
        if (compra.getPedido() == null) return null;
        DetallePedido ultima = null;
        for (DetallePedido d : compra.getPedido().getDetalles()) {
            if (d.getProducto().getIdProducto() == p.getIdProducto()) ultima = d;
        }
        return ultima;
    }

    /** Cuántas unidades del producto hay en el pedido, sumando todas sus líneas. */
    private int cantidadEnPedido(Producto p) {
        if (compra.getPedido() == null) return 0;
        int total = 0;
        for (DetallePedido d : compra.getPedido().getDetalles()) {
            if (d.getProducto().getIdProducto() == p.getIdProducto()) total += d.getCantidad();
        }
        return total;
    }

    private void cambiar(Producto p, int cambio) {
        if (cambio > 0 && OpcionesCombo.esPersonalizable(p)) {
            personalizar(p); // RF-13: primero se elige el sabor y la gaseosa
            return;
        }
        DetallePedido linea = cambio > 0 ? lineaDe(p, null) : ultimaLineaDe(p);
        if (linea == null) {
            if (cambio < 0) return;
            agregar(p, null);
            return;
        }
        sumar(linea, cambio);
    }

    /** Suma una unidad a la línea con esa personalización, o crea la línea si no existe. */
    private void agregar(Producto p, String personalizacion) {
        DetallePedido linea = lineaDe(p, personalizacion);
        if (linea != null) {
            sumar(linea, +1);
            return;
        }
        terminar(app().compra().agregarProducto(compra, p, 1, personalizacion));
    }

    private void sumar(DetallePedido linea, int cambio) {
        terminar(linea.getCantidad() + cambio <= 0
                ? app().compra().quitarProducto(compra, linea)
                : app().compra().cambiarCantidad(compra, linea, linea.getCantidad() + cambio));
    }

    private void terminar(Respuesta<?> r) {
        if (r.ok()) limpiar(lblMensaje);
        else mostrarError(lblMensaje, r.mensaje());
        mostrarProductos();
        actualizarPedido();
    }

    // ---------- Personalización de combos (RF-13) ----------

    /** Ventana para elegir el sabor de la canchita y la gaseosa del combo antes de agregarlo. */
    private void personalizar(Producto p) {
        DialogoVista d = nav.dialogo();
        d.configurar("Personaliza tu " + Formato.capitalizar(p.getNombre().toLowerCase()), p.getDescripcion(), false);
        String[] sabor = {OpcionesCombo.SABORES_CANCHITA.get(0)};
        String[] gaseosa = {OpcionesCombo.GASEOSAS.get(0)};
        if (OpcionesCombo.pideSabor(p)) {
            d.agregarContenido(grupo("Sabor de la canchita", OpcionesCombo.SABORES_CANCHITA, v -> sabor[0] = v));
        }
        if (OpcionesCombo.pideGaseosa(p)) {
            String titulo = p.getDescripcion().toLowerCase().contains("gaseosas") ? "Gaseosas" : "Gaseosa";
            d.agregarContenido(grupo(titulo, OpcionesCombo.GASEOSAS, v -> gaseosa[0] = v));
        }
        d.botonSecundario("Cancelar", nav::cerrarModal);
        d.botonPrincipal("Agregar al pedido", () -> {
            nav.cerrarModal();
            agregar(p, OpcionesCombo.describir(p, sabor[0], gaseosa[0]));
        });
    }

    /** Un título y sus opciones como botones; la primera viene elegida y se marca en dorado. */
    private static VBox grupo(String titulo, List<String> opciones, Consumer<String> alElegir) {
        Label etiqueta = new Label(titulo);
        etiqueta.getStyleClass().add("etiqueta-fuerte");
        FlowPane botones = new FlowPane(10, 10);
        List<Button> todos = new ArrayList<>();
        for (String opcion : opciones) {
            Button b = new Button(opcion);
            b.getStyleClass().addAll("opcion", "opcion-centro");
            b.setMinHeight(48);
            b.setMinWidth(116); // crece si el texto es más largo (Coca-Cola)
            b.setOnAction(e -> {
                todos.forEach(o -> o.getStyleClass().remove("activo"));
                b.getStyleClass().add("activo");
                alElegir.accept(opcion);
            });
            todos.add(b);
            botones.getChildren().add(b);
        }
        todos.get(0).getStyleClass().add("activo");
        VBox caja = new VBox(8, etiqueta, botones);
        caja.setMaxWidth(Double.MAX_VALUE);
        return caja;
    }

    private void actualizarPedido() {
        List<Entrada> entradas = compra.getEntradas();
        long montoEntradas = 0;
        for (Entrada e : entradas) montoEntradas += e.getPrecioCentavos();
        lblEntradas.setText("Entradas (" + entradas.size() + ")");
        lblMontoEntradas.setText(Formato.dinero(montoEntradas));

        lineas.getChildren().clear();
        long subtotal = 0;
        if (compra.getPedido() != null) {
            for (DetallePedido d : compra.getPedido().getDetalles()) {
                subtotal += d.getSubtotalCentavos();
                lineas.getChildren().add(lineaPedido(d));
            }
        }
        if (lineas.getChildren().isEmpty()) {
            Label vacio = new Label("Aún no agregas productos.");
            vacio.getStyleClass().add("nota-suave");
            lineas.getChildren().add(vacio);
        }
        lblSubtotalDulceria.setText(Formato.dinero(subtotal));
        String total = Formato.dinero(compra.getTotalCentavos());
        lblTotal.setText(total);
        lblPieTotal.setText(total);
    }

    private VBox lineaPedido(DetallePedido d) {
        Label nombre = new Label(d.getProducto().getNombre().toUpperCase() + "  ×" + d.getCantidad());
        nombre.getStyleClass().add("etiqueta-fuerte");
        nombre.setWrapText(true);
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Label monto = new Label(Formato.dinero(d.getSubtotalCentavos()));
        monto.getStyleClass().add("etiqueta-fuerte");
        HBox arriba = new HBox(nombre, espacio, monto);

        // Un combo personalizado muestra lo elegido en vez de su descripción genérica
        boolean personalizado = d.getPersonalizacion() != null && !d.getPersonalizacion().isBlank();
        Label descripcion = new Label(personalizado ? d.getPersonalizacion() : d.getProducto().getDescripcion());
        descripcion.getStyleClass().add("nota-suave");
        descripcion.setWrapText(true);
        Button quitar = new Button("Quitar");
        quitar.getStyleClass().addAll("btn-link");
        quitar.setOnAction(e -> {
            Respuesta<Void> r = app().compra().quitarProducto(compra, d);
            if (!r.ok()) mostrarError(lblMensaje, r.mensaje());
            mostrarProductos();
            actualizarPedido();
        });
        Region espacio2 = new Region();
        HBox.setHgrow(espacio2, Priority.ALWAYS);
        HBox abajo = new HBox(descripcion, espacio2, quitar);
        abajo.setAlignment(Pos.CENTER_LEFT);

        VBox caja = new VBox(4, arriba, abajo);
        caja.getStyleClass().add("item-pedido");
        return caja;
    }

    @FXML
    private void onCambiarButacas() {
        nav.ir("asientos");
    }

    @FXML
    private void onSinDulceria() {
        if (compra.getPedido() != null) {
            for (DetallePedido d : compra.getPedido().getDetalles()) app().compra().quitarProducto(compra, d);
        }
        nav.ir("resumen-pago");
    }

    @FXML
    private void onContinuar() {
        nav.ir("resumen-pago");
    }
}
