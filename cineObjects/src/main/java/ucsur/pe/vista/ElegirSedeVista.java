package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
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
        Label nombre = new Label(sede.getNombre());
        nombre.getStyleClass().add("tarjeta-titulo");
        Label distrito = etiqueta(sede.getDistrito());
        Label direccion = etiqueta(sede.getDireccion());
        Label salas = etiqueta(resumenSalas(sede));
        salas.setPadding(new javafx.geometry.Insets(8, 0, 0, 0));

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);

        Button elegir = new Button(sede.estaOperativa() ? "Elegir esta sede" : "Sede cerrada");
        elegir.getStyleClass().add("btn-secundario");
        elegir.setPrefWidth(280);
        elegir.setDisable(!sede.estaOperativa());
        elegir.setOnAction(e -> {
            estado().setSede(sede);
            nav.ir("cartelera");
        });
        VBox fila = new VBox(elegir);
        fila.setAlignment(Pos.CENTER);

        VBox tarjeta = new VBox(8, nombre, distrito, direccion, salas, espacio, fila);
        tarjeta.getStyleClass().add("tarjeta");
        tarjeta.setPrefSize(445, 273);
        tarjeta.setMinHeight(273);
        return tarjeta;
    }

    /** «5 salas · 2D, 3D» con las salas operativas y sus formatos. */
    private static String resumenSalas(Sede sede) {
        int cantidad = 0;
        Set<String> formatos = new LinkedHashSet<>();
        for (Sala sala : sede.getSalas()) {
            if (sala.estaOperativa()) {
                cantidad++;
                formatos.add(sala.getTipoSala().getFormato());
            }
        }
        if (cantidad == 0) return "Sin salas disponibles";
        return cantidad + (cantidad == 1 ? " sala · " : " salas · ") + String.join(", ", formatos);
    }

    private static Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("etiqueta");
        return l;
    }
}
