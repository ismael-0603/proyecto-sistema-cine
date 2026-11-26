package ucsur.pe.vista;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Butaca;
import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoSala;
import java.util.List;

/**
 * 26 · Salas de una sede: crear, editar (número, formato y distribución de butacas), ver el mapa,
 * poner en mantenimiento y dar de baja. La distribución es «filas · laterales | centrales | laterales».
 */
public class AdminSalasVista extends PantallaAdmin {

    @FXML private ComboBox<Sede> cmbSede;
    @FXML private Label lblMensaje;
    @FXML private VBox contenedorTabla;
    @FXML private VBox panelForm;
    @FXML private Label lblFormTitulo;
    @FXML private TextField txtNumero;
    @FXML private ComboBox<TipoSala> cmbFormato;
    @FXML private TextField txtFilas;
    @FXML private TextField txtLaterales;
    @FXML private TextField txtCentrales;
    @FXML private Label lblCapacidad;
    @FXML private Label lblMensajeForm;
    @FXML private VBox panelMapa;
    @FXML private Label lblMapaTitulo;
    @FXML private GridPane mapa;
    @FXML private Label lblMapaNota;

    private final Tabla<Sala> tabla = new Tabla<>(
            new String[] {"Sala", "Formato", "Capacidad", "Distribución", "Estado", "Acciones"},
            new double[] {80, 150, 110, 200, 160}, 375, 8);
    private Sala editando;

    public AdminSalasVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        if (!prepararAdmin("salas")) return;
        contenedorTabla.getChildren().add(tabla.vista());
        cmbSede.setConverter(convertidor(s -> "Sede: " + s.getNombre()));
        cmbFormato.setConverter(convertidor(TipoSala::getFormato));
        Respuesta<List<Sede>> sedes = app().adminInfraestructura().listarSedes(admin);
        if (sedes.ok()) cmbSede.setItems(FXCollections.observableArrayList(sedes.dato()));
        Respuesta<List<TipoSala>> tipos = app().adminInfraestructura().listarTiposSala(admin);
        if (tipos.ok()) cmbFormato.setItems(FXCollections.observableArrayList(tipos.dato()));
        cmbSede.setValue(admin.getSede());
        cmbSede.valueProperty().addListener((o, a, b) -> {
            onCancelar();
            cargar(false);
        });
        for (TextField t : new TextField[] {txtFilas, txtLaterales, txtCentrales}) {
            t.textProperty().addListener((o, a, b) -> vistaPreviaFormulario());
        }
        cargar(false);
    }

    private static <T> StringConverter<T> convertidor(java.util.function.Function<T, String> texto) {
        return new StringConverter<>() {
            @Override public String toString(T t) { return t == null ? "" : texto.apply(t); }
            @Override public T fromString(String s) { return null; }
        };
    }

    private void cargar(boolean mantenerPagina) {
        if (cmbSede.getValue() == null) return;
        Respuesta<List<Sala>> r = app().adminInfraestructura().listarSalas(admin, cmbSede.getValue());
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        tabla.mostrar(r.dato(), this::fila, mantenerPagina);
    }

    /** {filas, laterales, centrales} contando las butacas reales de la sala. */
    private static int[] distribucion(Sala s) {
        List<String> filas = s.obtenerFilas();
        if (filas.isEmpty()) return new int[] {0, 0, 0};
        String primera = filas.get(0);
        int lateral = 0;
        int central = 0;
        for (Butaca b : s.obtenerMapaButacas()) {
            if (!b.getFila().equals(primera)) continue;
            if (Butaca.CENTRO.equals(b.getBloque())) central++;
            else if (Butaca.IZQUIERDA.equals(b.getBloque())) lateral++;
        }
        return new int[] {filas.size(), lateral, central};
    }

    private List<Node> fila(Sala s) {
        int[] d = distribucion(s);
        boolean activa = Sala.ACTIVA.equals(s.getEstado());
        boolean inactiva = Sala.INACTIVA.equals(s.getEstado());
        Button estadoBtn = inactiva
                ? Tabla.boton("Activar", true, () -> despues(app().adminInfraestructura().cambiarEstadoSala(admin, s, Sala.ACTIVA)))
                : Tabla.boton("Dar de baja", false, () -> confirmarBaja(s));
        Button mant = activa
                ? Tabla.boton("Mantenimiento", false, () -> despues(app().adminInfraestructura().cambiarEstadoSala(admin, s, Sala.EN_MANTENIMIENTO)))
                : Tabla.boton("Reabrir", false, () -> despues(app().adminInfraestructura().cambiarEstadoSala(admin, s, Sala.ACTIVA)));
        mant.setVisible(!inactiva);
        mant.setManaged(!inactiva);
        return List.of(Tabla.texto(String.valueOf(s.getNumero())), Tabla.texto(s.getTipoSala().getFormato()),
                Tabla.texto(String.valueOf(s.getCapacidad())),
                Tabla.texto(d[0] + " filas · " + d[1] + " | " + d[2] + " | " + d[1]),
                Tabla.chip(Formato.capitalizar(s.getEstado()), activa ? "pagada" : "inactiva"),
                Tabla.acciones(Tabla.boton("Editar", false, () -> abrirForm(s)),
                        Tabla.boton("Ver mapa", false, () -> verMapa(s)), estadoBtn, mant));
    }

    private void confirmarBaja(Sala s) {
        DialogoVista d = nav.dialogo();
        d.configurar("¿Dar de baja la sala " + s.getNumero() + "?",
                "La sala queda inactiva y no se le pueden programar funciones. Si tiene funciones pendientes, "
                        + "primero hay que cancelarlas.", false);
        d.botonSecundario("Volver", nav::cerrarModal);
        d.botonPrincipal("Dar de baja", () -> {
            nav.cerrarModal();
            despues(app().adminInfraestructura().darDeBajaSala(admin, s));
        });
    }

    private void despues(Respuesta<?> r) {
        mostrar(lblMensaje, r);
        cargar(true);
    }

    // ---------- Formulario ----------

    @FXML
    private void onNueva() {
        abrirForm(null);
    }

    private void abrirForm(Sala s) {
        editando = s;
        lblFormTitulo.setText(s == null ? "Nueva sala" : "Editar sala " + s.getNumero());
        int[] d = s == null ? new int[] {9, 4, 10} : distribucion(s);
        txtNumero.setText(s == null ? "" : String.valueOf(s.getNumero()));
        cmbFormato.setValue(s == null ? null : s.getTipoSala());
        txtFilas.setText(String.valueOf(d[0]));
        txtLaterales.setText(String.valueOf(d[1]));
        txtCentrales.setText(String.valueOf(d[2]));
        limpiar(lblMensajeForm);
        panelForm.setVisible(true);
        panelForm.setManaged(true);
        vistaPreviaFormulario();
    }

    @FXML
    private void onCancelar() {
        editando = null;
        panelForm.setVisible(false);
        panelForm.setManaged(false);
        panelMapa.setVisible(false);
        panelMapa.setManaged(false);
    }

    @FXML
    private void onGuardar() {
        int numero;
        int filas;
        int lateral;
        int central;
        try {
            numero = Formato.leerEntero(txtNumero.getText(), "El número");
            filas = Formato.leerEntero(txtFilas.getText(), "Las filas");
            lateral = Formato.leerEntero(txtLaterales.getText(), "Las laterales");
            central = Formato.leerEntero(txtCentrales.getText(), "Las centrales");
        } catch (IllegalArgumentException e) {
            mostrarError(lblMensajeForm, e.getMessage());
            return;
        }
        Respuesta<?> r;
        if (editando == null) {
            r = app().adminInfraestructura().registrarSala(admin, cmbSede.getValue(), numero, cmbFormato.getValue(), filas, lateral, central);
        } else {
            r = app().adminInfraestructura().actualizarSala(admin, editando, numero, cmbFormato.getValue());
            int[] actual = distribucion(editando);
            if (r.ok() && (actual[0] != filas || actual[1] != lateral || actual[2] != central)) {
                r = app().adminInfraestructura().redistribuirSala(admin, editando, filas, lateral, central);
                if (!r.ok()) {
                    cargar(true); // número/formato ya quedaron guardados en la sala
                }
            }
        }
        if (!r.ok()) {
            mostrarError(lblMensajeForm, r.mensaje());
            return;
        }
        onCancelar();
        mostrarOk(lblMensaje, r.mensaje());
        cargar(true);
    }

    // ---------- Mapa ----------

    /** Mientras se escribe la distribución: capacidad calculada y mapa de butacas estándar. */
    private void vistaPreviaFormulario() {
        try {
            int f = Integer.parseInt(txtFilas.getText().trim());
            int l = Integer.parseInt(txtLaterales.getText().trim());
            int c = Integer.parseInt(txtCentrales.getText().trim());
            // Mismos topes que Sala: así un "200" no hace dibujar miles de butacas en cada tecla.
            if (f <= 0 || l < 0 || c <= 0 || f > Sala.MAX_FILAS || l > Sala.MAX_LATERAL || c > Sala.MAX_CENTRAL) {
                throw new NumberFormatException();
            }
            lblCapacidad.setText(f + " × (" + l + " + " + c + " + " + l + ") = " + f * (2 * l + c) + " butacas");
            mapa.getChildren().clear();
            for (int fila = 0; fila < f; fila++) {
                mapa.add(letra(fila), 0, fila);
                int col = 1;
                for (int i = 0; i < l; i++) mapa.add(cuadro(null), col++, fila);
                mapa.add(pasillo(), col++, fila);
                for (int i = 0; i < c; i++) mapa.add(cuadro(null), col++, fila);
                mapa.add(pasillo(), col++, fila);
                for (int i = 0; i < l; i++) mapa.add(cuadro(null), col++, fila);
            }
            lblMapaTitulo.setText("Vista previa del mapa");
            lblMapaNota.setText("Al guardar, el sistema ubica las butacas accesibles junto a los pasillos.");
            panelMapa.setVisible(true);
            panelMapa.setManaged(true);
        } catch (NumberFormatException | NullPointerException e) {
            lblCapacidad.setText("Completa filas, laterales y centrales");
        }
    }

    /** «Ver mapa»: las butacas reales de la sala con su tipo. */
    private void verMapa(Sala s) {
        mapa.getChildren().clear();
        List<String> filas = s.obtenerFilas();
        if (filas.isEmpty()) {
            lblMapaTitulo.setText("Mapa de la sala " + s.getNumero() + " · " + s.getCapacidad() + " butacas");
            lblMapaNota.setText("Esta sala aún no tiene butacas configuradas.");
            panelMapa.setVisible(true);
            panelMapa.setManaged(true);
            return;
        }
        int[] d = distribucion(s);
        for (Butaca b : s.obtenerMapaButacas()) {
            int fila = filas.indexOf(b.getFila());
            int corrimiento = Butaca.CENTRO.equals(b.getBloque()) ? 1 : Butaca.DERECHA.equals(b.getBloque()) ? 2 : 0;
            mapa.add(cuadro(b), b.getNumero() + corrimiento, fila);
        }
        for (int f = 0; f < filas.size(); f++) mapa.add(letra(f), 0, f);
        if (d[1] > 0) {
            mapa.add(pasillo(), d[1] + 1, 0);
            mapa.add(pasillo(), d[1] + d[2] + 2, 0);
        }
        lblMapaTitulo.setText("Mapa de la sala " + s.getNumero() + " · " + s.getCapacidad() + " butacas");
        lblMapaNota.setText("Pasillos entre los bloques " + d[1] + " | " + d[2] + " | " + d[1]
                + ". Fila ideal: " + filas.get(Sala.indiceFilaIdeal(filas.size())) + " (la mejor vista a la pantalla).");
        panelMapa.setVisible(true);
        panelMapa.setManaged(true);
    }

    private static Label letra(int fila) {
        Label l = new Label(String.valueOf((char) ('A' + fila)));
        l.getStyleClass().add("nota");
        l.setMinWidth(14);
        return l;
    }

    private static Region cuadro(Butaca b) {
        Region r = new Region();
        r.getStyleClass().add("butaca-mini");
        if (b != null && b.esAccesible()) r.getStyleClass().add("accesible");
        return r;
    }

    private static Region pasillo() {
        Region r = new Region();
        r.setMinWidth(10);
        return r;
    }
}
