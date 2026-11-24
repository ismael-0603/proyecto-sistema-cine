package ucsur.pe.vista;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Calificacion;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Reclamo;
import java.util.ArrayList;
import java.util.List;

/** 14 · Calificar la experiencia (1 a 5 estrellas) y presentar un reclamo sobre una compra. */
public class CalificarReclamoVista extends Pantalla {

    @FXML private Label lblCompraCalif;
    @FXML private HBox estrellas;
    @FXML private Label lblPuntaje;
    @FXML private TextArea txtComentario;
    @FXML private Label lblContador;
    @FXML private Label lblMensajeCalif;
    @FXML private Button btnCalificar;
    @FXML private Label lblCompraReclamo;
    @FXML private FlowPane tipos;
    @FXML private TextArea txtDescripcion;
    @FXML private Label lblContadorReclamo;
    @FXML private Label lblMensajeReclamo;
    @FXML private Button btnReclamo;

    private Cliente cliente;
    private Compra compra;
    private int puntuacion = 0;
    private String tipoElegido;
    private final List<Button> botonesEstrella = new ArrayList<>();
    private final List<Button> botonesTipo = new ArrayList<>();

    public CalificarReclamoVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        cliente = estado().clienteConSesion();
        compra = estado().getCompraElegida();
        if (cliente == null || compra == null) {
            Platform.runLater(() -> nav.ir("mis-compras"));
            return;
        }
        Funcion f = compra.getEntradas().isEmpty() ? null : compra.getEntradas().get(0).getFuncion();
        lblCompraCalif.setText(f == null ? "Compra " + compra.getSerieNumero()
                : f.getPelicula().getTitulo() + " · " + Formato.fechaHoraCorta(f.obtenerFechaHoraInicio())
                + " · Sala " + f.getSala().getNumero());
        lblCompraReclamo.setText("Compra " + compra.getSerieNumero() + " · " + Formato.fechaHoraCorta(compra.getFechaHora()));

        for (int i = 1; i <= 5; i++) {
            int valor = i;
            Button b = new Button("☆");
            b.getStyleClass().add("estrella");
            b.setOnAction(e -> elegirPuntuacion(valor));
            botonesEstrella.add(b);
            estrellas.getChildren().add(b);
        }
        elegirPuntuacion(0);
        txtComentario.textProperty().addListener((o, a, texto) ->
                lblContador.setText(texto.length() + " de " + Calificacion.MAX_COMENTARIO + " caracteres"));
        txtDescripcion.textProperty().addListener((o, a, texto) ->
                lblContadorReclamo.setText(texto.length() + " de " + Reclamo.MAX_DESCRIPCION + " caracteres"));

        for (String tipo : Reclamo.TIPOS) {
            Button b = new Button("◯  " + Formato.tipoReclamo(tipo));
            b.getStyleClass().addAll("opcion", "btn-chico");
            b.setOnAction(e -> elegirTipo(tipo, b));
            botonesTipo.add(b);
            tipos.getChildren().add(b);
        }
        Respuesta<Calificacion> previa = app().postventa().consultarCalificacion(cliente, compra);
        if (previa.ok() && previa.dato() != null) {
            marcarCalificada(previa.dato().getPuntuacion());
            txtComentario.setText(previa.dato().getComentario() == null ? "" : previa.dato().getComentario());
        }
        // Un reclamo a la vez: si hay uno sin cerrar, se avisa en vez de dejar presentar otro igual
        Respuesta<List<Reclamo>> previos = app().postventa().consultarReclamos(cliente, compra);
        if (previos.ok()) {
            for (Reclamo r : previos.dato()) {
                if (Reclamo.REGISTRADO.equals(r.getEstado()) || Reclamo.EN_REVISION.equals(r.getEstado())) {
                    bloquearReclamo("Ya presentaste el reclamo #R-" + String.format("%04d", r.getIdReclamo())
                            + " (" + Formato.estadoReclamo(r.getEstado()).toLowerCase() + "). Te avisaremos cuando lo respondamos.");
                    break;
                }
            }
        }
    }

    private void bloquearReclamo(String mensaje) {
        mostrarOk(lblMensajeReclamo, mensaje);
        txtDescripcion.setDisable(true);
        botonesTipo.forEach(b -> b.setDisable(true));
        btnReclamo.setDisable(true);
    }

    private void elegirPuntuacion(int valor) {
        puntuacion = valor;
        for (int i = 0; i < botonesEstrella.size(); i++) botonesEstrella.get(i).setText(i < valor ? "★" : "☆");
        lblPuntaje.setText(valor == 0 ? "Toca las estrellas para calificar" : valor + " de 5");
    }

    private void elegirTipo(String tipo, Button boton) {
        tipoElegido = tipo;
        for (int i = 0; i < botonesTipo.size(); i++) {
            Button b = botonesTipo.get(i);
            b.getStyleClass().remove("activo");
            b.setText("◯  " + Formato.tipoReclamo(Reclamo.TIPOS.get(i)));
        }
        boton.getStyleClass().add("activo");
        boton.setText("◉  " + Formato.tipoReclamo(tipo));
    }

    @FXML
    private void onCalificar() {
        if (puntuacion == 0) {
            mostrarError(lblMensajeCalif, "Elige de 1 a 5 estrellas.");
            return;
        }
        Respuesta<Calificacion> r = app().postventa().calificar(cliente, compra, puntuacion, txtComentario.getText());
        if (!mostrar(lblMensajeCalif, r)) return;
        marcarCalificada(puntuacion);
    }

    /** Ya calificada: se deja ver el puntaje pero no se puede volver a enviar (una calificación por compra). */
    private void marcarCalificada(int estrellasDadas) {
        elegirPuntuacion(estrellasDadas);
        botonesEstrella.forEach(b -> b.setDisable(true));
        txtComentario.setDisable(true);
        btnCalificar.setDisable(true);
        btnCalificar.setText("Calificado ★ " + estrellasDadas);
    }

    @FXML
    private void onReclamo() {
        if (tipoElegido == null) {
            mostrarError(lblMensajeReclamo, "Elige el tipo de reclamo.");
            return;
        }
        Respuesta<Reclamo> r = app().postventa().presentarReclamo(cliente, compra, tipoElegido, txtDescripcion.getText());
        if (!r.ok()) {
            mostrarError(lblMensajeReclamo, r.mensaje());
            return;
        }
        bloquearReclamo("Reclamo #R-" + String.format("%04d", r.dato().getIdReclamo())
                + " registrado. Te avisaremos por correo cuando lo respondamos.");
    }

    @FXML
    private void onVolver() {
        nav.ir("mis-compras");
    }
}
