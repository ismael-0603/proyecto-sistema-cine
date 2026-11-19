package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.DetallePedido;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Pedido;

/**
 * 16 · Entregar pedido: el empleado escanea el QR o escribe el código (QR del pedido o código del
 * comprobante) y confirma; quien comprueba que el código corresponde es el servicio. También sirve para ver
 * el detalle y el estado de un pedido encontrado con «Buscar pedido».
 */
public class EntregarPedidoVista extends Pantalla {

    private static final String[][] ESTADOS = {
            {Pedido.PENDIENTE, "Pendiente"}, {Pedido.EN_PREPARACION, "En preparación"}, {Pedido.LISTO, "Listo"}};

    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;
    @FXML private TextField txtCodigo;
    @FXML private Label lblMensaje;
    @FXML private VBox lineas;
    @FXML private HBox filaTotal;
    @FXML private HBox estados;
    @FXML private Button btnConfirmar;

    private Pedido pedido;
    private Empleado empleado;
    private PanelDulceriaVista panel;

    public EntregarPedidoVista(Navegador nav) {
        super(nav);
    }

    public void mostrar(Pedido pedido, Empleado empleado, PanelDulceriaVista panel) {
        this.pedido = pedido;
        this.empleado = empleado;
        this.panel = panel;
        boolean listo = Pedido.LISTO.equals(pedido.getEstado());
        lblTitulo.setText((listo ? "Entregar " : "") + PanelDulceriaVista.numero(pedido).replace("Pedido", listo ? "pedido" : "Pedido"));
        lblSubtitulo.setText("Registrado a las " + (pedido.getFechaHora() == null ? "—" : pedido.getFechaHora().soloHora())
                + (pedido.getEmpleado() == null ? "" : " · Atiende " + pedido.getEmpleado().getNombreCompleto()));

        for (DetallePedido d : pedido.getDetalles()) {
            lineas.getChildren().add(ResumenCompra.fila(d.getCantidad() + " × " + d.getProducto().getNombre().toUpperCase(),
                    Formato.dinero(d.getSubtotalCentavos()), true));
        }
        filaTotal.getChildren().setAll(ResumenCompra.fila("Total", Formato.dinero(pedido.getTotalCentavos()), true).getChildren());

        for (String[] e : ESTADOS) {
            Label chip = new Label(e[1]);
            chip.getStyleClass().add("estado-chip");
            if (e[0].equals(pedido.getEstado())) chip.getStyleClass().add("respondido");
            estados.getChildren().add(chip);
        }
        if (!listo && !Pedido.PENDIENTE.equals(pedido.getEstado()) && !Pedido.EN_PREPARACION.equals(pedido.getEstado())) {
            Label otro = new Label(Formato.capitalizar(pedido.getEstado()));
            otro.getStyleClass().addAll("estado-chip", "respondido");
            estados.getChildren().add(otro);
        }
        // Solo un pedido LISTO se entrega; en otro estado la ventana es solo de consulta
        btnConfirmar.setDisable(!listo);
        txtCodigo.setDisable(!listo);
        if (!listo) mostrarError(lblMensaje, "Este pedido está " + Formato.capitalizar(pedido.getEstado()).toLowerCase()
                + ": se entrega cuando esté listo.");
        else txtCodigo.requestFocus();
    }

    @FXML
    private void onConfirmar() {
        if (btnConfirmar.isDisabled()) return;
        Respuesta<Void> r = app().dulceria().entregarPedido(empleado, pedido, txtCodigo.getText());
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        nav.cerrarModal();
        panel.avisar(PanelDulceriaVista.numero(pedido) + " entregado.");
    }

    @FXML
    private void onCerrar() {
        nav.cerrarModal();
    }
}
