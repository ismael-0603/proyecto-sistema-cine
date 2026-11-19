package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DetallePedido;
import ucsur.pe.model.Entrada;
import ucsur.pe.model.Producto;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 7 · Dulcería: el menú de la sede para este cliente. Cada −/+ cambia el pedido de la compra en el servicio,
 * que valida stock y ofertas de socio y recalcula el total.
 */
public class DulceriaVista extends Pantalla {

    private static final String TODOS = "Todos";

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
        // El modelo no guarda foto del producto: se muestra su categoría en el recuadro de imagen
        Label cat = new Label(Formato.capitalizar(p.getCategoria()));
        cat.getStyleClass().add("producto-categoria");
        StackPane imagen = new StackPane(cat);
        imagen.getStyleClass().add("producto-imagen");

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

    private DetallePedido lineaDe(Producto p) {
        if (compra.getPedido() == null) return null;
        for (DetallePedido d : compra.getPedido().getDetalles()) {
            if (d.getProducto().getIdProducto() == p.getIdProducto()) return d;
        }
        return null;
    }

    private int cantidadEnPedido(Producto p) {
        DetallePedido d = lineaDe(p);
        return d == null ? 0 : d.getCantidad();
    }

    private void cambiar(Producto p, int cambio) {
        DetallePedido linea = lineaDe(p);
        Respuesta<?> r;
        if (linea == null) {
            if (cambio < 0) return;
            r = app().compra().agregarProducto(compra, p, 1, null);
        } else if (linea.getCantidad() + cambio <= 0) {
            r = app().compra().quitarProducto(compra, linea);
        } else {
            r = app().compra().cambiarCantidad(compra, linea, linea.getCantidad() + cambio);
        }
        if (r.ok()) limpiar(lblMensaje);
        else mostrarError(lblMensaje, r.mensaje());
        mostrarProductos();
        actualizarPedido();
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

        Label descripcion = new Label(d.getProducto().getDescripcion());
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
