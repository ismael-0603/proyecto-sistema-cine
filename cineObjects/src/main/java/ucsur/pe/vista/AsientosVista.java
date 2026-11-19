package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Butaca;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Entrada;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.MapaButacas;
import ucsur.pe.model.TipoEntrada;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 6 · Selección de asientos: cuántas entradas de cada tipo, el mapa de la sala con la sugerencia del sistema
 * y las butacas elegidas. Al continuar se agregan las entradas a la compra y se bloquean las butacas.
 */
public class AsientosVista extends Pantalla {

    @FXML private EncabezadoCompraVista encabezadoController;
    @FXML private GridPane mapa;
    @FXML private HBox cajaSugerencia;
    @FXML private Label lblSugerencia;
    @FXML private Button btnSugerencia;
    @FXML private CheckBox chkAccesible;
    @FXML private Label lblMensajeMapa;
    @FXML private VBox tipos;
    @FXML private Label lblSocio;
    @FXML private Label lblButacas;
    @FXML private Label lblEntradas;
    @FXML private Label lblSubtotal;
    @FXML private Label lblPieSubtotal;
    @FXML private Label lblMensaje;

    private Funcion funcion;
    private Compra compra;
    private MapaButacas mapaActual;
    private final Map<TipoEntrada, Integer> cantidades = new LinkedHashMap<>();
    private final Map<TipoEntrada, Label> valores = new LinkedHashMap<>();
    private final List<Butaca> elegidas = new ArrayList<>();

    public AsientosVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        encabezadoController.setPaso(1);
        funcion = estado().getFuncion();
        compra = estado().getCompra();
        if (funcion == null || compra == null) {
            javafx.application.Platform.runLater(() -> nav.ir("cartelera"));
            return;
        }
        volverAElegir();
        cargarTipos();
        chkAccesible.selectedProperty().addListener((o, a, b) -> recargarMapa());
        recargarMapa();
    }

    /** Si vuelve desde la dulcería, suelta las butacas bloqueadas para poder cambiarlas (el plazo sigue). */
    private void volverAElegir() {
        List<Entrada> entradas = compra.getEntradas();
        if (entradas.isEmpty()) return;
        app().compra().liberarAsientos(compra);
        for (Entrada e : entradas) app().compra().quitarEntrada(compra, e);
        elegidas.addAll(estado().getButacas());
    }

    private void cargarTipos() {
        Respuesta<List<TipoEntrada>> r = app().compra().listarTiposEntrada(estado().cliente());
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        long recargo = funcion.getSala().getTipoSala().obtenerRecargoFormatoCentavos();
        for (TipoEntrada t : r.dato()) {
            cantidades.put(t, estado().getCantidades().getOrDefault(t, 0));
            Label nombre = new Label(t.getNombre());
            nombre.getStyleClass().add("etiqueta");
            Label precio = new Label(Formato.dinero(t.getTarifaCentavos() + recargo));
            precio.getStyleClass().add("nota");
            VBox info = new VBox(1, nombre, precio);
            Region espacio = new Region();
            HBox.setHgrow(espacio, Priority.ALWAYS);

            Button menos = new Button("−");
            menos.getStyleClass().add("contador-menos");
            Label valor = new Label(String.valueOf(cantidades.get(t)));
            valor.getStyleClass().add("contador-valor");
            Button mas = new Button("+");
            mas.getStyleClass().add("contador-mas");
            menos.setOnAction(e -> cambiarCantidad(t, -1));
            mas.setOnAction(e -> cambiarCantidad(t, +1));
            valores.put(t, valor);
            HBox contador = new HBox(menos, valor, mas);
            contador.getStyleClass().add("contador");

            HBox fila = new HBox(info, espacio, contador);
            fila.setAlignment(Pos.CENTER_LEFT);
            tipos.getChildren().add(fila);
        }
        boolean socio = estado().cliente() != null && estado().cliente().esSocio();
        lblSocio.setText(socio ? "" : "Socio (usa puntos): inicia sesión con tu cuenta de socio para verlo.");
        lblSocio.setManaged(!socio);
    }

    private void cambiarCantidad(TipoEntrada tipo, int cambio) {
        int nuevo = Math.max(0, cantidades.get(tipo) + cambio);
        cantidades.put(tipo, nuevo);
        valores.get(tipo).setText(String.valueOf(nuevo));
        // Si ahora pide menos entradas que butacas elegidas, se sueltan las últimas
        while (elegidas.size() > totalEntradas()) elegidas.remove(elegidas.size() - 1);
        limpiar(lblMensaje);
        recargarMapa();
    }

    private int totalEntradas() {
        int total = 0;
        for (int c : cantidades.values()) total += c;
        return total;
    }

    /** Pide al servicio el mapa con las butacas libres y la sugerencia para la cantidad actual. */
    private void recargarMapa() {
        int cantidad = Math.max(1, totalEntradas());
        Respuesta<MapaButacas> r = app().compra().consultarMapa(compra, funcion, cantidad, chkAccesible.isSelected());
        if (!r.ok()) {
            mostrarError(lblMensajeMapa, r.mensaje());
            return;
        }
        mapaActual = r.dato();
        if (r.mensaje() != null && !r.mensaje().isEmpty() && totalEntradas() > 0) mostrarError(lblMensajeMapa, r.mensaje());
        else limpiar(lblMensajeMapa);
        dibujarMapa();
        actualizarResumen();
    }

    private void dibujarMapa() {
        mapa.getChildren().clear();
        // Filas en orden alfabético; dentro de cada fila, la columna depende del número y del bloque
        Map<String, List<Butaca>> porFila = new TreeMap<>();
        int maxIzquierda = 0;
        int maxCentro = 0;
        for (Butaca b : mapaActual.todas()) {
            porFila.computeIfAbsent(b.getFila(), k -> new ArrayList<>()).add(b);
            if (Butaca.IZQUIERDA.equals(b.getBloque())) maxIzquierda = Math.max(maxIzquierda, b.getNumero());
            if (Butaca.CENTRO.equals(b.getBloque())) maxCentro = Math.max(maxCentro, b.getNumero());
        }
        int fila = 0;
        for (var entrada : porFila.entrySet()) {
            Label letra = new Label(entrada.getKey());
            letra.getStyleClass().add("fila-letra");
            mapa.add(letra, 0, fila);
            entrada.getValue().sort(Comparator.comparingInt(Butaca::getNumero));
            for (Butaca b : entrada.getValue()) {
                mapa.add(botonButaca(b), columna(b), fila);
            }
            fila++;
        }
        // Pasillos: columnas vacías con ancho fijo entre los bloques
        if (maxIzquierda > 0) mapa.add(pasillo(), maxIzquierda + 1, 0);
        if (maxCentro > 0) mapa.add(pasillo(), maxCentro + 2, 0);
    }

    /** Columna 0 = letra; los bloques centro y derecha se corren 1 y 2 columnas para dejar los pasillos. */
    private static int columna(Butaca b) {
        int corrimiento = Butaca.CENTRO.equals(b.getBloque()) ? 1 : Butaca.DERECHA.equals(b.getBloque()) ? 2 : 0;
        return b.getNumero() + corrimiento;
    }

    private static Region pasillo() {
        Region r = new Region();
        r.setMinWidth(22);
        return r;
    }

    private Button botonButaca(Butaca b) {
        Button boton = new Button(b.esAccesible() ? "D" : String.valueOf(b.getNumero()));
        boton.getStyleClass().add("butaca");
        boolean elegida = elegidas.contains(b);
        if (elegida) {
            boton.getStyleClass().add("elegida");
        } else if (!mapaActual.estaLibre(b)) {
            boton.setText("X");
            boton.getStyleClass().add("ocupada");
            return boton; // ocupada: no se puede tocar
        } else if (elegidas.isEmpty() && totalEntradas() > 0 && mapaActual.esSugerida(b)) {
            boton.getStyleClass().add("sugerida");
        } else if (Butaca.PREFERENCIAL.equals(b.getTipo())) {
            boton.getStyleClass().add("preferencial");
        }
        boton.setOnAction(e -> tocarButaca(b));
        return boton;
    }

    private void tocarButaca(Butaca b) {
        if (elegidas.contains(b)) {
            elegidas.remove(b);
        } else if (totalEntradas() == 0) {
            mostrarError(lblMensaje, "Primero elige cuántas entradas quieres.");
            return;
        } else if (elegidas.size() >= totalEntradas()) {
            mostrarError(lblMensaje, "Ya elegiste " + totalEntradas() + " butaca(s). Toca una elegida para soltarla.");
            return;
        } else {
            elegidas.add(b);
        }
        limpiar(lblMensaje);
        dibujarMapa();
        actualizarResumen();
    }

    @FXML
    private void onUsarSugerencia() {
        elegidas.clear();
        elegidas.addAll(mapaActual.sugeridas());
        limpiar(lblMensaje);
        dibujarMapa();
        actualizarResumen();
    }

    private void actualizarResumen() {
        // Sugerencia
        List<Butaca> sugeridas = mapaActual.sugeridas();
        boolean haySugerencia = totalEntradas() > 0 && !sugeridas.isEmpty();
        cajaSugerencia.setVisible(haySugerencia);
        cajaSugerencia.setManaged(haySugerencia);
        if (haySugerencia) {
            lblSugerencia.setText("Te sugerimos " + codigos(sugeridas) + ": juntas y al centro.");
        }
        // Butacas y entradas
        lblButacas.setText(elegidas.isEmpty() ? "—" : codigos(elegidas));
        List<String> partes = new ArrayList<>();
        long subtotal = 0;
        long recargo = funcion.getSala().getTipoSala().obtenerRecargoFormatoCentavos();
        for (var e : cantidades.entrySet()) {
            if (e.getValue() > 0) {
                partes.add(e.getValue() + " × " + e.getKey().getNombre());
                subtotal += e.getValue() * (e.getKey().getTarifaCentavos() + recargo);
            }
        }
        lblEntradas.setText(partes.isEmpty() ? "—" : String.join(", ", partes));
        lblSubtotal.setText(Formato.dinero(subtotal));
        lblPieSubtotal.setText(Formato.dinero(subtotal));
    }

    private static String codigos(List<Butaca> butacas) {
        return String.join(", ", butacas.stream().map(Butaca::getCodigoAsiento).toList());
    }

    @FXML
    private void onVolver() {
        nav.volverAHorarios();
    }

    /** Agrega una entrada por butaca (los tipos en el orden de la lista) y bloquea las butacas. */
    @FXML
    private void onContinuar() {
        int total = totalEntradas();
        if (total == 0) {
            mostrarError(lblMensaje, "Elige al menos una entrada.");
            return;
        }
        if (elegidas.size() != total) {
            mostrarError(lblMensaje, "Elige " + total + " butaca(s); llevas " + elegidas.size() + ".");
            return;
        }
        List<TipoEntrada> tiposPorButaca = new ArrayList<>();
        for (var e : cantidades.entrySet()) {
            for (int i = 0; i < e.getValue(); i++) tiposPorButaca.add(e.getKey());
        }
        List<Entrada> agregadas = new ArrayList<>();
        for (int i = 0; i < elegidas.size(); i++) {
            Respuesta<Entrada> r = app().compra().agregarEntrada(compra, funcion, elegidas.get(i), tiposPorButaca.get(i));
            if (!r.ok()) {
                deshacer(agregadas);
                mostrarError(lblMensaje, r.mensaje());
                return;
            }
            agregadas.add(r.dato());
        }
        Respuesta<Void> bloqueo = app().compra().confirmarAsientos(compra);
        if (!bloqueo.ok()) {
            deshacer(agregadas);
            mostrarError(lblMensaje, bloqueo.mensaje());
            recargarMapa(); // alguien pudo haber tomado una butaca: se vuelve a mostrar el mapa
            return;
        }
        estado().getCantidades().clear();
        estado().getCantidades().putAll(cantidades);
        estado().getButacas().clear();
        estado().getButacas().addAll(elegidas);
        nav.ir("dulceria");
    }

    private void deshacer(List<Entrada> agregadas) {
        for (Entrada e : agregadas) app().compra().quitarEntrada(compra, e);
    }
}
