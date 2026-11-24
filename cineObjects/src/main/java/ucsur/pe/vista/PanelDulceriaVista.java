package ucsur.pe.vista;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.DetallePedido;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Pedido;
import ucsur.pe.model.Producto;
import ucsur.pe.service.Sesion;
import java.util.ArrayList;
import java.util.List;

/**
 * 15 · Panel de dulcería: la cola de pedidos de la sede del empleado en cuatro columnas
 * (pendientes, en preparación, listos y entregados hoy). Cada botón pide la acción a DulceriaController
 * y vuelve a cargar las columnas; además se recargan solas cada 20 segundos.
 */
public class PanelDulceriaVista extends Pantalla {

    private static final int SEGUNDOS_RECARGA = 20;

    @FXML private EncabezadoVista encabezadoController;
    @FXML private TextField txtBuscar;
    @FXML private Label lblMensaje;
    @FXML private Label lblStock;
    @FXML private HBox columnas;

    private Empleado empleado;
    private Timeline recarga;

    public PanelDulceriaVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        Sesion sesion = estado().getSesion();
        empleado = sesion == null ? null : sesion.empleadoEnPanel();
        if (empleado == null) {
            Platform.runLater(() -> nav.ir("iniciar-sesion"));
            return;
        }
        encabezadoController.modoPanel("Panel de dulcería",
                "Sede: " + (empleado.getSede() == null ? "—" : empleado.getSede().getNombre()));
        cargar();
        recarga = new Timeline(new KeyFrame(Duration.seconds(SEGUNDOS_RECARGA), e -> cargar()));
        recarga.setCycleCount(Timeline.INDEFINITE);
        recarga.play();
        // Al salir de la pantalla se detiene la recarga automática
        columnas.sceneProperty().addListener((o, antes, ahora) -> {
            if (ahora == null) recarga.stop();
        });
    }

    /** Vuelve a pedir las tres listas al controller y redibuja las columnas. */
    public void cargar() {
        Respuesta<List<Pedido>> cola = app().dulceria().consultarPendientes(empleado);
        Respuesta<List<Pedido>> listos = app().dulceria().consultarListos(empleado);
        Respuesta<List<Pedido>> entregados = app().dulceria().consultarEntregadosHoy(empleado);
        if (!cola.ok()) {
            mostrarError(lblMensaje, cola.mensaje());
            return;
        }
        // La cola trae PENDIENTE y EN_PREPARACION juntos: la pantalla los separa en dos columnas
        List<Pedido> pendientes = new ArrayList<>();
        List<Pedido> enPreparacion = new ArrayList<>();
        for (Pedido p : cola.dato()) {
            (Pedido.PENDIENTE.equals(p.getEstado()) ? pendientes : enPreparacion).add(p);
        }
        columnas.getChildren().setAll(
                columna("Pendientes", "insignia-roja", pendientes),
                columna("En preparación", "insignia-dorada", enPreparacion),
                columna("Listos para entregar", "insignia-azul", listos.ok() ? listos.dato() : List.of()),
                columna("Entregados hoy", "insignia-gris", entregados.ok() ? entregados.dato() : List.of()));
        avisarStockBajo();
    }

    private void avisarStockBajo() {
        Respuesta<List<Producto>> r = app().dulceria().consultarStockBajo(empleado);
        boolean hay = r.ok() && !r.dato().isEmpty();
        lblStock.setVisible(hay);
        lblStock.setManaged(hay);
        if (hay) {
            List<String> nombres = r.dato().stream()
                    .map(p -> p.getNombre() + " (" + p.getStock(empleado.getSede()) + ")").toList();
            lblStock.setGraphic(Iconos.crear(Iconos.ALERTA, 16, "icono-rojo"));
            lblStock.setGraphicTextGap(8);
            lblStock.setText("Stock bajo en tu sede: " + String.join(", ", nombres));
        }
    }

    private VBox columna(String titulo, String estiloInsignia, List<Pedido> pedidos) {
        Label nombre = new Label(titulo);
        nombre.getStyleClass().add("etiqueta-fuerte");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Label cuenta = new Label(String.valueOf(pedidos.size()));
        cuenta.getStyleClass().addAll("insignia", estiloInsignia);
        HBox cabecera = new HBox(nombre, espacio, cuenta);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        VBox tarjetas = new VBox(12);
        for (Pedido p : pedidos) tarjetas.getChildren().add(tarjeta(p));
        if (pedidos.isEmpty()) {
            Label vacio = new Label("Sin pedidos");
            vacio.getStyleClass().add("nota-suave");
            tarjetas.getChildren().add(vacio);
        }
        ScrollPane desplazable = new ScrollPane(tarjetas);
        desplazable.setFitToWidth(true);
        desplazable.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(desplazable, Priority.ALWAYS);

        VBox col = new VBox(12, cabecera, desplazable);
        col.getStyleClass().add("columna-pedidos");
        HBox.setHgrow(col, Priority.ALWAYS);
        col.setMaxWidth(Double.MAX_VALUE);
        col.setMinWidth(200);
        col.setPrefWidth(320);
        return col;
    }

    private VBox tarjeta(Pedido p) {
        Label numero = new Label(numero(p));
        numero.getStyleClass().add("etiqueta-fuerte");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Label hora = new Label(p.getFechaHora() == null ? "" : p.getFechaHora().soloHora());
        hora.getStyleClass().add("nota");
        HBox arriba = new HBox(numero, espacio, hora);

        VBox lineas = new VBox(3);
        for (DetallePedido d : p.getDetalles()) {
            Label l = new Label(d.getCantidad() + " × " + d.getProducto().getNombre().toUpperCase()
                    + (d.getPersonalizacion() == null || d.getPersonalizacion().isBlank() ? "" : " (" + d.getPersonalizacion() + ")"));
            l.getStyleClass().add("nota");
            l.setWrapText(true);
            lineas.getChildren().add(l);
        }
        if (p.getEmpleado() != null && !Pedido.ENTREGADO.equals(p.getEstado())) {
            Label quien = new Label("Atiende: " + p.getEmpleado().getNombres().split(" ")[0]);
            quien.getStyleClass().add("nota-suave");
            lineas.getChildren().add(quien);
        }

        Button accion = botonDe(p);
        accion.setMaxWidth(Double.MAX_VALUE);
        VBox tarjeta = new VBox(8, arriba, lineas, accion);
        tarjeta.getStyleClass().add("tarjeta-pedido");
        return tarjeta;
    }

    /** Preparar → Marcar listo (solo quien lo prepara) → Entregar → Entregado. */
    private Button botonDe(Pedido p) {
        Button b = new Button();
        b.getStyleClass().add("btn-primario");
        switch (p.getEstado()) {
            case Pedido.PENDIENTE -> {
                b.setText("Preparar");
                b.setOnAction(e -> despues(app().dulceria().prepararPedido(empleado, p)));
            }
            case Pedido.EN_PREPARACION -> {
                b.setText("Marcar listo");
                b.setOnAction(e -> {
                    Respuesta<String> r = app().dulceria().marcarListo(empleado, p);
                    // El aviso para el cliente lo arma el modelo; aquí solo se muestra
                    if (r.ok()) mostrarOk(lblMensaje, r.dato());
                    else mostrarError(lblMensaje, r.mensaje());
                    cargar();
                });
                b.setDisable(p.getEmpleado() != empleado);
            }
            case Pedido.LISTO -> {
                b.setText("Entregar");
                b.setOnAction(e -> abrirEntrega(p));
            }
            default -> {
                b.setText(Formato.capitalizar(p.getEstado()));
                b.getStyleClass().setAll("button", "btn-entregado");
                b.setDisable(true);
            }
        }
        return b;
    }

    private void despues(Respuesta<?> r) {
        mostrar(lblMensaje, r);
        cargar();
    }

    private void abrirEntrega(Pedido p) {
        EntregarPedidoVista entrega = nav.modal("entregar-pedido");
        entrega.mostrar(p, empleado, this);
    }

    @FXML
    private void onBuscar() {
        Respuesta<Pedido> r = app().dulceria().buscarPedido(empleado, txtBuscar.getText());
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        limpiar(lblMensaje);
        abrirEntrega(r.dato()); // la ventana de entrega muestra el detalle y el estado del pedido
    }

    /** Lo usa la ventana de entrega para mostrar el resultado en el panel. */
    void avisar(String texto) {
        mostrarOk(lblMensaje, texto);
        cargar();
    }

    static String numero(Pedido p) {
        return "Pedido #" + String.format("%04d", p.getIdPedido());
    }
}
