package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ucsur.pe.model.TransaccionPago;

/** 8.2 · Pago con billetera digital (Yape, Plin u otra app con QR). */
public class PagoBilleteraVista extends PantallaPago {

    @FXML private EncabezadoCompraVista encabezadoController;
    @FXML private Button btnYape;
    @FXML private Button btnPlin;
    @FXML private Button btnQr;
    @FXML private StackPane marcoQr;
    @FXML private Label lblMonto;
    @FXML private Label lblMensaje;
    @FXML private VBox resumen;
    @FXML private Label lblPieTotal;

    private String medio = TransaccionPago.YAPE;

    public PagoBilleteraVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        encabezadoController.setPaso(3);
        if (compra() == null) {
            javafx.application.Platform.runLater(() -> nav.ir("cartelera"));
            return;
        }
        String total = Formato.dinero(compra().getTotalCentavos());
        lblMonto.setText("Monto a pagar: " + total);
        lblPieTotal.setText(total);
        resumen.getChildren().add(ResumenCompra.tarjeta(estado().getFuncion(), compra(),
                "El QR lo genera la pasarela de pago certificada."));
        elegir(TransaccionPago.YAPE, btnYape);
    }

    @FXML private void onYape() { elegir(TransaccionPago.YAPE, btnYape); }
    @FXML private void onPlin() { elegir(TransaccionPago.PLIN, btnPlin); }
    @FXML private void onQr() { elegir(TransaccionPago.PAGO_QR, btnQr); }

    private void elegir(String medio, Button boton) {
        this.medio = medio;
        for (Button b : new Button[] {btnYape, btnPlin, btnQr}) b.getStyleClass().remove("activo");
        boton.getStyleClass().add("activo");
        StackPane fondo = new StackPane(CodigoVisual.dibujar(medio + "-" + compra().getIdCompra(), 170));
        fondo.getStyleClass().add("qr-fondo");
        marcoQr.getChildren().setAll(fondo);
    }

    @FXML
    private void onVerificar() {
        pagar(medio);
    }

    @FXML
    private void onVolver() {
        nav.ir("resumen-pago");
    }
}
