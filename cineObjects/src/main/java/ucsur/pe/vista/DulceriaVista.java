package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.text.TextAlignment;

/**
 * 7 · Dulcería: el menú de la sede para este cliente. Cada −/+ cambia el pedido de la compra en el servicio,
 * que valida stock y ofertas de socio y recalcula el total. En los combos con canchita o gaseosa, el «+»
 * abre primero la ventana para elegir sabor y gaseosa (RF-13); cada combinación es una línea del pedido.
 */
public class DulceriaVista extends Pantalla {

    private static final String TODOS = "Todos";
    // Tarjeta de 280 px menos el borde (2 × 2); imagen al ras superior en coordenadas enteras
    private static final double ANCHO_IMAGEN = 276;
    private static final double ALTO_IMAGEN = 166;
    private static final double ANCHO_OPCION_FOTO = 136;
    private static final double ALTO_OPCION_FOTO = 85;
    private static final Map<String, String> FOTOS_OPCIONES = Map.of(
            "Salada", "productos/canchita-salada.jpg",
            "Dulce", "productos/canchita-dulce.jpg",
            "Mixta", "productos/canchita-mixta.jpg",
            "Inca Kola", "productos/inca-kola.jpg",
            "Coca-Cola", "productos/coca-cola.jpg",
            "Sprite", "productos/sprite.jpg",
            "Fanta", "productos/fanta.jpg",
            "Inca Kola Zero", "productos/inca-kola-zero.jpg",
            "Coca-Cola Zero", "productos/coca-cola-zero.jpg"
    );

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

    private record TarjetaProducto(Producto producto, VBox tarjeta, Label valor, Label badge) { }

    private Compra compra;
    private List<Producto> menu = new ArrayList<>();
    private String categoria = TODOS;
    private final List<Button> chips = new ArrayList<>();
    private final List<TarjetaProducto> tarjetasVisibles = new ArrayList<>();

    public DulceriaVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        encabezadoController.setPaso(2);
        compra = estado().getCompra();
        if (compra == null) {
            Platform.runLater(() -> nav.ir("cartelera"));
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
        tarjetasVisibles.clear();
        for (Producto p : menu) {
            if (TODOS.equals(categoria) || Formato.capitalizar(p.getCategoria()).equals(categoria)) {
                productos.getChildren().add(tarjeta(p));
            }
        }
        if (menu.isEmpty()) mostrarError(lblMensaje, "No hay productos disponibles en esta sede.");
    }

    /** Actualiza contadores, badges y borde dorado en sitio, sin reconstruir las tarjetas ni imágenes. */
    private void actualizarTarjetas() {
        for (TarjetaProducto t : tarjetasVisibles) {
            int cant = cantidadEnPedido(t.producto());
            if (t.valor() != null) t.valor().setText(String.valueOf(cant));
            if (t.badge() != null) {
                t.badge().setText("×" + cant);
                t.badge().setVisible(cant > 0);
                t.badge().setManaged(cant > 0);
            }
            if (cant > 0) {
                if (!t.tarjeta().getStyleClass().contains("en-pedido")) t.tarjeta().getStyleClass().add("en-pedido");
            } else {
                t.tarjeta().getStyleClass().remove("en-pedido");
            }
        }
    }

    private VBox tarjeta(Producto p) {
        int cantidad = cantidadEnPedido(p);
        boolean agotado = p.estaAgotado(estado().getSede());

        // Foto del producto al ras superior; sin foto, el recuadro muestra su categoría
        Node imagen = Formato.imagenProductoSuperior(p, ANCHO_IMAGEN, ALTO_IMAGEN);
        StackPane portada = new StackPane(imagen);
        portada.setPrefSize(ANCHO_IMAGEN, ALTO_IMAGEN);
        portada.setMinSize(ANCHO_IMAGEN, ALTO_IMAGEN);
        portada.setMaxSize(ANCHO_IMAGEN, ALTO_IMAGEN);
        portada.setClip(Formato.clipSuperior(ANCHO_IMAGEN, ALTO_IMAGEN, 20));

        Label badge = null;
        if (agotado) {
            Label sinStock = new Label("Sin stock");
            sinStock.getStyleClass().addAll("estado-chip", "agotado");
            StackPane.setAlignment(sinStock, Pos.TOP_RIGHT);
            StackPane.setMargin(sinStock, new Insets(10));
            portada.getChildren().add(sinStock);
        } else {
            badge = new Label("×" + cantidad);
            badge.getStyleClass().add("badge-cantidad");
            badge.setVisible(cantidad > 0);
            badge.setManaged(cantidad > 0);
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(10));
            portada.getChildren().add(badge);
        }

        Label nombre = new Label(p.getNombre());
        nombre.getStyleClass().add("producto-nombre");
        nombre.setWrapText(true);
        Label descripcion = new Label(p.getDescripcion() == null ? "" : p.getDescripcion());
        descripcion.getStyleClass().add("nota-suave");
        descripcion.setWrapText(true);
        descripcion.setMinHeight(36);
        descripcion.setAlignment(Pos.TOP_LEFT);

        Label precio = new Label(Formato.dinero(p.getPrecioCentavos()));
        precio.getStyleClass().add("producto-precio");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        Label valor = null;
        HBox pie;
        if (agotado) {
            Label sinStock = new Label("Sin stock");
            sinStock.getStyleClass().add("nota-suave");
            pie = new HBox(precio, espacio, sinStock);
        } else {
            Button menos = new Button("−");
            menos.getStyleClass().add("contador-menos");
            valor = new Label(String.valueOf(cantidad));
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

        VBox cuerpo = new VBox(6, nombre, descripcion, pie);
        cuerpo.getStyleClass().add("producto-cuerpo");

        VBox tarjeta = new VBox(portada, cuerpo);
        tarjeta.getStyleClass().add("producto");
        if (agotado) tarjeta.getStyleClass().add("sin-stock");
        if (cantidad > 0) tarjeta.getStyleClass().add("en-pedido");
        tarjeta.setPrefWidth(280);
        tarjeta.setMaxWidth(280);
        tarjetasVisibles.add(new TarjetaProducto(p, tarjeta, valor, badge));
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
        actualizarTarjetas();
        actualizarPedido();
    }

    // ---------- Personalización de combos, canchitas y gaseosas (RF-13) ----------

    /** Ventana para elegir el sabor de la canchita y/o la marca de la gaseosa antes de agregar el producto. */
    private void personalizar(Producto p) {
        String descripcion = p.getDescripcion() == null ? "" : p.getDescripcion();
        DialogoVista d = nav.dialogo();
        d.configurar("Personaliza tu " + Formato.capitalizar(p.getNombre()), descripcion, false);
        String[] sabor = {OpcionesCombo.SABORES_CANCHITA.get(0)};
        String[] gaseosa = {OpcionesCombo.GASEOSAS.get(0)};
        if (OpcionesCombo.pideSabor(p)) {
            d.agregarContenido(grupo("Sabor de la canchita", OpcionesCombo.SABORES_CANCHITA, v -> sabor[0] = v));
        }
        if (OpcionesCombo.pideGaseosa(p)) {
            String titulo = descripcion.toLowerCase().contains("gaseosas") ? "Gaseosas" : "Gaseosa";
            d.agregarContenido(grupo(titulo, OpcionesCombo.GASEOSAS, v -> gaseosa[0] = v));
        }
        d.botonSecundario("Cancelar", nav::cerrarModal);
        d.botonPrincipal("Agregar al pedido", () -> {
            nav.cerrarModal();
            agregar(p, OpcionesCombo.describir(p, sabor[0], gaseosa[0]));
        });
    }

    /** Un título y sus opciones con foto en 3 columnas; la primera viene elegida y se marca en dorado. */
    private static VBox grupo(String titulo, List<String> opciones, Consumer<String> alElegir) {
        Label etiqueta = new Label(titulo);
        etiqueta.getStyleClass().add("etiqueta-fuerte");
        GridPane grilla = new GridPane();
        grilla.setHgap(10);
        grilla.setVgap(10);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 3.0);
            grilla.getColumnConstraints().add(col);
        }
        List<Button> todos = new ArrayList<>();
        for (int i = 0; i < opciones.size(); i++) {
            String opcion = opciones.get(i);
            Button b = botonOpcion(opcion);
            b.setOnAction(e -> {
                todos.forEach(o -> o.getStyleClass().remove("activo"));
                b.getStyleClass().add("activo");
                alElegir.accept(opcion);
            });
            todos.add(b);
            GridPane.setFillWidth(b, true);
            GridPane.setFillHeight(b, true);
            grilla.add(b, i % 3, i / 3);
        }
        todos.get(0).getStyleClass().add("activo");
        VBox caja = new VBox(8, etiqueta, grilla);
        caja.setMaxWidth(Double.MAX_VALUE);
        return caja;
    }

    private static Button botonOpcion(String opcion) {
        String ruta = FOTOS_OPCIONES.get(opcion);
        Button b = new Button();
        b.setMaxWidth(Double.MAX_VALUE);
        b.setMaxHeight(Double.MAX_VALUE);
        if (ruta != null) {
            Node foto = Formato.imagenProducto(ruta, opcion, ANCHO_OPCION_FOTO, ALTO_OPCION_FOTO);
            Label texto = new Label(opcion);
            texto.getStyleClass().add("opcion-visual-nombre");
            texto.setWrapText(true);
            texto.setAlignment(Pos.CENTER);
            texto.setTextAlignment(TextAlignment.CENTER);
            texto.setMinHeight(34);
            texto.setPrefHeight(34);
            texto.setMaxWidth(ANCHO_OPCION_FOTO);
            VBox contenido = new VBox(6, foto, texto);
            contenido.setAlignment(Pos.CENTER);
            b.setGraphic(contenido);
            b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            b.setMinHeight(144);
            b.setPrefHeight(144);
            b.getStyleClass().addAll("opcion", "opcion-visual");
        } else {
            b.setText(opcion);
            b.getStyleClass().addAll("opcion", "opcion-centro");
            b.setMinHeight(48);
        }
        return b;
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
            actualizarTarjetas();
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
