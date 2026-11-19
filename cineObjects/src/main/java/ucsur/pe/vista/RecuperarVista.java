package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.DetalleCompra;

/** 24 · Recuperar entradas: con el código del comprobante y el correo, o reenviándolas al correo. */
public class RecuperarVista extends Pantalla {

    @FXML private TextField txtCodigo;
    @FXML private TextField txtCorreoCodigo;
    @FXML private Label lblMensajeCodigo;
    @FXML private TextField txtCorreo;
    @FXML private VBox cajaEnviado;
    @FXML private Label lblEnviado;
    @FXML private Label lblMensajeCorreo;

    public RecuperarVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void onVerEntradas() {
        String correo = txtCorreoCodigo.getText();
        Respuesta<DetalleCompra> r = app().emision().recuperarPorCodigo(txtCodigo.getText(), correo);
        if (!r.ok()) {
            mostrarError(lblMensajeCodigo, r.mensaje());
            return;
        }
        estado().setDetalleRecuperado(r.dato(), correo == null ? "" : correo.trim());
        nav.ir("detalle-compra");
    }

    @FXML
    private void onEnviarCorreo() {
        Respuesta<Integer> r = app().emision().recuperarPorCorreo(txtCorreo.getText());
        if (!r.ok()) {
            cajaEnviado.setVisible(false);
            cajaEnviado.setManaged(false);
            mostrarError(lblMensajeCorreo, r.mensaje());
            return;
        }
        limpiar(lblMensajeCorreo);
        int n = r.dato();
        lblEnviado.setText("✓ Te enviamos " + n + (n == 1 ? " compra" : " compras") + " a "
                + Formato.correoOculto(txtCorreo.getText().trim()));
        cajaEnviado.setVisible(true);
        cajaEnviado.setManaged(true);
    }

    @FXML
    private void onVolver() {
        nav.ir(estado().getSede() == null ? "elegir-sede" : "cartelera");
    }
}
