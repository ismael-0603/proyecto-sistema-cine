package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import ucsur.pe.App;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Promocion;
import java.util.ArrayList;
import java.util.List;

/** 10 · Promociones vigentes hoy, con filtro «Para todos» o «Socios». */
public class PromocionesVista extends Pantalla {

    private enum Filtro { TODAS, PARA_TODOS, SOCIOS }

    @FXML private EncabezadoVista encabezadoController;
    @FXML private HBox filtros;
    @FXML private Label lblMensaje;
    @FXML private FlowPane promociones;

    private List<Promocion> vigentes = new ArrayList<>();
    private final List<Button> botones = new ArrayList<>();
    private Image imagen;

    public PromocionesVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        encabezadoController.marcarPromociones();
        var recurso = App.class.getResource("img/promo-entradas.png");
        if (recurso != null) imagen = new Image(recurso.toExternalForm());
        Respuesta<List<Promocion>> r = app().cartelera().listarPromocionesVigentes();
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        vigentes = r.dato();
        agregarFiltro("Todas", Filtro.TODAS);
        agregarFiltro("Para todos", Filtro.PARA_TODOS);
        agregarFiltro("Socios", Filtro.SOCIOS);
        elegir(Filtro.TODAS, botones.get(0));
    }

    private void agregarFiltro(String texto, Filtro filtro) {
        Button b = new Button(texto);
        b.getStyleClass().addAll("chip-dia", "btn-chico");
        b.setOnAction(e -> elegir(filtro, b));
        botones.add(b);
        filtros.getChildren().add(b);
    }

    private void elegir(Filtro filtro, Button boton) {
        botones.forEach(b -> b.getStyleClass().remove("activo"));
        boton.getStyleClass().add("activo");
        promociones.getChildren().clear();
        for (Promocion p : vigentes) {
            boolean paraSocios = p.isSoloSocios();
            if (filtro == Filtro.TODAS || (filtro == Filtro.SOCIOS) == paraSocios) {
                promociones.getChildren().add(tarjeta(p));
            }
        }
        if (promociones.getChildren().isEmpty()) {
            mostrarError(lblMensaje, vigentes.isEmpty() ? "No hay promociones vigentes por ahora."
                    : "No hay promociones vigentes en este grupo.");
        }
        else limpiar(lblMensaje);
    }

    private VBox tarjeta(Promocion p) {
        VBox t = new VBox(8);
        if (imagen != null) {
            ImageView foto = new ImageView(imagen);
            foto.setFitWidth(398);
            foto.setFitHeight(125);
            Rectangle esquinas = new Rectangle(398, 125);
            esquinas.setArcWidth(24);
            esquinas.setArcHeight(24);
            foto.setClip(esquinas);
            t.getChildren().add(foto);
        }
        Label nombre = new Label(p.getNombre());
        nombre.getStyleClass().add("tarjeta-titulo");
        Label descripcion = new Label(p.getDescripcion());
        descripcion.getStyleClass().add("nota");
        descripcion.setWrapText(true);
        Label dias = new Label(Formato.diasAplicables(p.getDiasAplicables()));
        dias.getStyleClass().add("etiqueta-fuerte");
        Label vigencia = new Label(p.getFechaFin() == null ? "Vigente"
                : "Vigente hasta " + String.format("%02d/%02d", p.getFechaFin().getDia(), p.getFechaFin().getMes()));
        vigencia.getStyleClass().add("nota");
        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);
        t.getChildren().addAll(nombre, descripcion, dias, vigencia, espacio, publico(p));
        t.getStyleClass().add("tarjeta");
        t.setPadding(new javafx.geometry.Insets(22));
        t.setPrefWidth(442);
        t.setMinHeight(302);
        return t;
    }

    /** «Para todos los clientes» o la etiqueta «Socio nivel N+». */
    private static Label publico(Promocion p) {
        if (!p.isSoloSocios()) {
            Label l = new Label("Para todos los clientes");
            l.getStyleClass().add("nota");
            return l;
        }
        int nivel = Math.max(1, p.getNivelSocioMinimo());
        Label l = new Label("Socio nivel " + nivel + "+");
        l.getStyleClass().addAll("estado-chip", "reembolsada");
        return l;
    }
}
