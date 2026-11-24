package ucsur.pe.vista;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Sede;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** 20 · Productos de dulcería: catálogo, stock de la sede del administrador, reponer y activar/desactivar. */
public class AdminProductosVista extends PantallaAdmin {

    private static final String TODAS = "Categoría";
    private static final String TODOS = "Estado";

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private Label lblAlerta;
    @FXML private Label lblMensaje;
    @FXML private VBox contenedorTabla;
    @FXML private VBox panelReponer;
    @FXML private Label lblReponer;
    @FXML private TextField txtCantidad;
    @FXML private Label lblNuevoStock;
    @FXML private Label lblMensajeReponer;

    private final Tabla<Producto> tabla = new Tabla<>(
            new String[] {"Producto", "Categoría", "Precio", "Nivel mín.", "Stock", "Estado", "Acciones"},
            new double[] {260, 120, 100, 140, 150, 120}, 8);
    private List<Producto> productos = new ArrayList<>();
    private Producto reponiendo;
    private Sede sede;

    public AdminProductosVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        if (!prepararAdmin("productos")) return;
        sede = admin.getSede();
        contenedorTabla.getChildren().add(tabla.vista());
        cmbEstado.setItems(FXCollections.observableArrayList(TODOS, "Activo", "Inactivo"));
        cmbEstado.getSelectionModel().selectFirst();
        txtBuscar.textProperty().addListener((o, a, b) -> filtrar(false));
        cmbCategoria.valueProperty().addListener((o, a, b) -> filtrar(false));
        cmbEstado.valueProperty().addListener((o, a, b) -> filtrar(false));
        txtCantidad.textProperty().addListener((o, a, b) -> actualizarNuevoStock());
        cargar(false);
    }

    private void cargar(boolean mantenerPagina) {
        Respuesta<List<Producto>> r = app().adminProductos().listarProductos(admin);
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        productos = r.dato();
        String elegida = cmbCategoria.getValue();
        TreeSet<String> categorias = new TreeSet<>();
        productos.forEach(p -> categorias.add(Formato.capitalizar(p.getCategoria())));
        List<String> opciones = new ArrayList<>(List.of(TODAS));
        opciones.addAll(categorias);
        cmbCategoria.setItems(FXCollections.observableArrayList(opciones));
        cmbCategoria.setValue(elegida != null && opciones.contains(elegida) ? elegida : TODAS);

        Respuesta<List<Producto>> bajo = app().adminProductos().listarProductosConStockBajo(admin);
        boolean hay = bajo.ok() && !bajo.dato().isEmpty();
        lblAlerta.setVisible(hay);
        lblAlerta.setManaged(hay);
        if (hay) {
            lblAlerta.setGraphic(Iconos.crear(Iconos.ALERTA, 16, "icono-rojo"));
            lblAlerta.setGraphicTextGap(8);
            lblAlerta.setText(bajo.mensaje() + " en " + sede.getNombre() + ".");
        }
        filtrar(mantenerPagina);
    }

    private void filtrar(boolean mantenerPagina) {
        String buscado = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        String categoria = TODAS.equals(cmbCategoria.getValue()) ? null : cmbCategoria.getValue();
        String est = TODOS.equals(cmbEstado.getValue()) ? null : cmbEstado.getValue();
        tabla.mostrar(productos.stream()
                .filter(p -> buscado.isEmpty() || p.getNombre().toLowerCase().contains(buscado))
                .filter(p -> categoria == null || Formato.capitalizar(p.getCategoria()).equals(categoria))
                .filter(p -> est == null || ("Activo".equals(est) == p.isEstado()))
                .toList(), this::fila, mantenerPagina);
    }

    private List<Node> fila(Producto p) {
        int stock = p.getStock(sede);
        Node celdaStock;
        if (p.estaAgotado(sede)) celdaStock = Tabla.chip(stock + " · Agotado", "agotado");
        else if (p.tieneStockBajo(sede)) celdaStock = Tabla.chip(stock + " · Stock bajo", "inactiva");
        else celdaStock = Tabla.texto(String.valueOf(stock));
        Button estadoBtn = Tabla.boton(p.isEstado() ? "Activo" : "Inactivo", p.isEstado(),
                () -> despues(app().adminProductos().cambiarEstadoProducto(admin, p, !p.isEstado())));
        return List.of(Tabla.texto(p.getNombre().toUpperCase()), Tabla.texto(Formato.capitalizar(p.getCategoria())),
                Tabla.texto(Formato.dinero(p.getPrecioCentavos())),
                Tabla.texto(p.getNivelSocioMinimo() <= 0 ? "—" : Formato.nivelSocio(p.getNivelSocioMinimo())),
                celdaStock, estadoBtn,
                Tabla.acciones(Tabla.boton("Editar", false, () -> {
                    estado().setEnEdicion(p);
                    nav.ir("admin-producto-form");
                }), Tabla.boton("Reponer", p.requiereReposicion(sede), () -> abrirReponer(p))));
    }

    private void despues(Respuesta<?> r) {
        mostrar(lblMensaje, r);
        cargar(true);
    }

    @FXML
    private void onNuevo() {
        estado().setEnEdicion(null);
        nav.ir("admin-producto-form");
    }

    // ---------- Reponer stock ----------

    private void abrirReponer(Producto p) {
        reponiendo = p;
        lblReponer.setText(p.getNombre().toUpperCase() + " · Stock actual en " + sede.getNombre() + ": "
                + p.getStock(sede) + " unidades");
        txtCantidad.setText("20");
        limpiar(lblMensajeReponer);
        panelReponer.setVisible(true);
        panelReponer.setManaged(true);
        actualizarNuevoStock();
    }

    private int cantidad() {
        try {
            return Integer.parseInt(txtCantidad.getText().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void actualizarNuevoStock() {
        if (reponiendo == null) return;
        lblNuevoStock.setText("Nuevo stock: " + (reponiendo.getStock(sede) + Math.max(0, cantidad())));
    }

    @FXML private void onMenos() { txtCantidad.setText(String.valueOf(Math.max(1, cantidad() - 1))); }
    @FXML private void onMas() { txtCantidad.setText(String.valueOf(cantidad() + 1)); }

    @FXML
    private void onCancelarReponer() {
        reponiendo = null;
        panelReponer.setVisible(false);
        panelReponer.setManaged(false);
    }

    @FXML
    private void onConfirmarReponer() {
        int cantidad;
        try {
            cantidad = Formato.leerEntero(txtCantidad.getText(), "La cantidad"); // "abc" avisa que no es un número
        } catch (IllegalArgumentException e) {
            mostrarError(lblMensajeReponer, e.getMessage());
            return;
        }
        Respuesta<Integer> r = app().adminProductos().reponerStock(admin, reponiendo, cantidad);
        if (!r.ok()) {
            mostrarError(lblMensajeReponer, r.mensaje());
            return;
        }
        onCancelarReponer();
        mostrarOk(lblMensaje, r.mensaje());
        cargar(true);
    }
}
