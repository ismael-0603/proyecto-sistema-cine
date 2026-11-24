package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Aviso que aparece encima de la pantalla (wireframes 8.3 Cancelar compra, 8.4 Tiempo agotado,
 * 8.5 Pago rechazado y 27 Aviso de inactividad): título, texto, contenido opcional y hasta dos botones.
 */
public class DialogoVista extends Pantalla {

    @FXML private Label lblTitulo;
    @FXML private Label lblTexto;
    @FXML private VBox contenido;
    @FXML private Button btnSecundario;
    @FXML private Button btnPrincipal;
    @FXML private Button btnEnlace;

    public DialogoVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        ocultar(btnSecundario);
        ocultar(btnPrincipal);
        ocultar(btnEnlace);
    }

    /** Título y texto; {@code peligro} pinta el título en rojo (errores y tiempo agotado). */
    public void configurar(String titulo, String texto, boolean peligro) {
        lblTitulo.setText(titulo);
        lblTexto.setText(texto);
        if (peligro) lblTitulo.getStyleClass().add("peligro");
    }

    public void agregarContenido(Node... nodos) {
        contenido.getChildren().addAll(nodos);
        contenido.setManaged(true);
        contenido.setVisible(true);
    }

    public void botonSecundario(String texto, Runnable accion) {
        mostrar(btnSecundario, texto, accion);
    }

    public void botonPrincipal(String texto, Runnable accion) {
        mostrar(btnPrincipal, texto, accion);
    }

    /** Enlace rojo debajo de los botones (por ejemplo, «Cancelar compra»). */
    public void enlace(String texto, Runnable accion) {
        mostrar(btnEnlace, texto, accion);
    }

    private void mostrar(Button boton, String texto, Runnable accion) {
        boton.setText(texto);
        boton.setOnAction(e -> accion.run());
        boton.setVisible(true);
        boton.setManaged(true);
    }

    private static void ocultar(Node nodo) {
        nodo.setVisible(false);
        nodo.setManaged(false);
    }
}
