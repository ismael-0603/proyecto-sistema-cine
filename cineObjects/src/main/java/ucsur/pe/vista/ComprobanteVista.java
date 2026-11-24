package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DetalleCompra;
import ucsur.pe.model.TransaccionPago;

/** 9 · Comprobante: el detalle de la compra pagada, el código para entrar y recoger, y los puntos ganados. */
public class ComprobanteVista extends Pantalla {

    @FXML private EncabezadoVista encabezadoController;
    @FXML private Label lblSerie;
    @FXML private VBox filas;
    @FXML private HBox filaTotal;
    @FXML private Label lblMedio;
    @FXML private StackPane marcoQr;
    @FXML private Label lblCodigo;
    @FXML private VBox cajaPuntos;
    @FXML private Label lblPuntos;
    @FXML private Label lblSaldo;
    @FXML private Button btnEnviar;
    @FXML private Button btnImprimir;
    @FXML private Label lblMensaje;

    private Compra compra;

    public ComprobanteVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        encabezadoController.soloVolver("Volver al inicio", nav::volverAlInicio);
        btnEnviar.setGraphic(Iconos.crear(Iconos.CORREO, 16));
        btnEnviar.setGraphicTextGap(8);
        btnImprimir.setGraphic(Iconos.crear(Iconos.TICKET, 16));
        btnImprimir.setGraphicTextGap(8);
        compra = estado().getCompra();
        if (compra == null) {
            javafx.application.Platform.runLater(nav::volverAlInicio);
            return;
        }
        Respuesta<DetalleCompra> r = app().emision().verDetalle(compra);
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        DetalleCompra d = r.dato();
        lblSerie.setText("Comprobante " + d.getSerieNumero());

        filas.getChildren().addAll(ResumenCompra.filasDetalle(d));
        filaTotal.getChildren().setAll(ResumenCompra.filaTotalPagado(d).getChildren());
        String medio = d.getMedioPago() != null ? d.getMedioPago()
                : TransaccionPago.describirMedio(estado().getMedioPago());
        lblMedio.setText("Pagado con " + medio);

        String codigo = d.getCodigosQR().isEmpty() ? d.getSerieNumero() : d.getCodigosQR().get(0);
        StackPane fondo = new StackPane(CodigoVisual.dibujar(codigo, 170));
        fondo.getStyleClass().add("qr-fondo");
        marcoQr.getChildren().setAll(fondo);
        lblCodigo.setText(d.getCodigosQR().size() > 1
                ? d.getCodigosQR().size() + " códigos: entradas y pedido de dulcería" : "Código " + codigo);

        boolean hayPuntos = d.getPuntosGanados() > 0 || d.getSaldoPuntos() != null;
        cajaPuntos.setVisible(hayPuntos);
        cajaPuntos.setManaged(hayPuntos);
        if (hayPuntos) {
            lblPuntos.setText("Puntos ganados: +" + d.getPuntosGanados());
            lblSaldo.setText(d.getSaldoPuntos() == null ? ""
                    : "Saldo: " + d.getSaldoPuntos() + " pts" + (d.getNivelSocio() == null ? "" : " · Socio nivel " + d.getNivelSocio()));
        }
    }

    @FXML
    private void onEnviar() {
        String correo = compra.getCliente().getCorreo();
        Respuesta<Void> r = app().emision().enviarPorCorreo(compra, correo);
        if (r.ok()) mostrarOk(lblMensaje, "Enviamos el comprobante a " + correo);
        else mostrarError(lblMensaje, r.mensaje());
    }

    /** Muestra el texto que se imprimiría (la impresora del kiosco se simula en pantalla). */
    @FXML
    private void onImprimir() {
        Respuesta<String> r = app().emision().imprimir(compra);
        if (!mostrar(lblMensaje, r)) return;
        nav.mostrarTicket(r.dato());
    }

    @FXML
    private void onInicio() {
        nav.volverAlInicio();
    }
}
