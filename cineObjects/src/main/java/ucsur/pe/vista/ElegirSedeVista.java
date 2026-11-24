package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 22 · Elegir sede: la cartelera, los horarios y las butacas dependen de la sede elegida. */
public class ElegirSedeVista extends Pantalla {

    @FXML private EncabezadoVista encabezadoController;
    @FXML private TextField txtBuscar;
    @FXML private FlowPane sedes;
    @FXML private Label lblMensaje;

    private List<Sede> todas = new ArrayList<>();
    private boolean navegando;

    public ElegirSedeVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        encabezadoController.sinSede();
        Respuesta<List<Sede>> r = app().cartelera().listarSedes();
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        todas = r.dato();
        txtBuscar.textProperty().addListener((obs, antes, texto) -> mostrarSedes());
        mostrarSedes();
    }

    /** Filtra por nombre o distrito lo que ya se cargó (buscar es trabajo de la pantalla). */
    private void mostrarSedes() {
        String buscado = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        sedes.getChildren().clear();
        for (Sede s : todas) {
            if (buscado.isEmpty() || s.getNombre().toLowerCase().contains(buscado)
                    || s.getDistrito().toLowerCase().contains(buscado)) {
                sedes.getChildren().add(tarjeta(s));
            }
        }
        if (sedes.getChildren().isEmpty()) {
            mostrarError(lblMensaje, todas.isEmpty() ? "No hay sedes registradas." : "Ninguna sede coincide con «" + buscado + "».");
        } else {
            limpiar(lblMensaje);
        }
    }

    private VBox tarjeta(Sede sede) {
        boolean operativa = sede.estaOperativa();

        Label nombre = new Label(sede.getNombre());
        nombre.getStyleClass().add("tarjeta-titulo");
        nombre.setWrapText(true);
        Region empuje = new Region();
        HBox.setHgrow(empuje, Priority.ALWAYS);
        Label estadoChip = new Label(operativa ? "Abierta" : "Cerrada");
        estadoChip.getStyleClass().addAll("estado-chip", operativa ? "pagada" : "anulada");
        estadoChip.setMinWidth(Region.USE_PREF_SIZE);
        HBox cabecera = new HBox(10, nombre, empuje, estadoChip);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        Label distrito = new Label(sede.getDistrito());
        distrito.getStyleClass().add("etiqueta-fuerte");
        HBox ubicacion = new HBox(6, Iconos.crear(Iconos.UBICACION, 15, "icono-rojo"), distrito);
        ubicacion.setAlignment(Pos.CENTER_LEFT);

        Label direccion = new Label(sede.getDireccion());
        direccion.getStyleClass().add("nota-suave");
        direccion.setWrapText(true);

        FlowPane chips = chipsSalas(sede);
        chips.setPadding(new Insets(6, 0, 0, 0));

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);

        Button elegir = new Button(operativa ? "Elegir esta sede" : "Sede cerrada");
        elegir.getStyleClass().add(operativa ? "btn-primario" : "btn-secundario");
        elegir.setMaxWidth(Double.MAX_VALUE);
        elegir.setDisable(!operativa);
        elegir.setOnAction(e -> seleccionar(sede));

        VBox tarjeta = new VBox(8, cabecera, ubicacion, direccion, chips, espacio, elegir);
        tarjeta.getStyleClass().addAll("tarjeta", "tarjeta-sede");
        if (operativa) {
            tarjeta.setOnMouseClicked(e -> seleccionar(sede));
        } else {
            tarjeta.getStyleClass().add("cerrada");
        }
        tarjeta.setPrefSize(435, 276);
        tarjeta.setMinHeight(276);
        return tarjeta;
    }

    private void seleccionar(Sede sede) {
        if (navegando) return;
        navegando = true;
        estado().setSede(sede);
        nav.ir("cartelera");
    }

    /** Píldoras con la cantidad de salas operativas y sus formatos («5 salas», «2D», «Prime 2D»...). */
    private static FlowPane chipsSalas(Sede sede) {
        int cantidad = 0;
        Set<String> formatos = new LinkedHashSet<>();
        for (Sala sala : sede.getSalas()) {
            if (sala.estaOperativa()) {
                cantidad++;
                formatos.add(sala.getTipoSala().getFormato());
            }
        }
        FlowPane fp = new FlowPane(6, 6);
        if (cantidad == 0) {
            Label sin = new Label("Sin salas disponibles");
            sin.getStyleClass().add("nota-suave");
            fp.getChildren().add(sin);
            return fp;
        }
        Label total = new Label(cantidad + (cantidad == 1 ? " sala" : " salas"));
        total.getStyleClass().add("recargo");
        fp.getChildren().add(total);
        for (String f : formatos) {
            Label tag = new Label(f);
            tag.getStyleClass().add("tag-genero");
            fp.getChildren().add(tag);
        }
        return fp;
    }
}
