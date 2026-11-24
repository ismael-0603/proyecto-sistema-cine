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
 * 6 · Selección de asientos: el cliente toca las butacas en el mapa y cada butaca es una entrada. Todas
 * empiezan como el primer tipo de la lista («General», se calcula solo) y los demás tipos se reparten entre
 * las butacas elegidas. La sugerencia del sistema (RF-29) tiene su propio selector de grupo. Al continuar se
 * agregan las entradas a la compra y se bloquean las butacas.
 */
public class AsientosVista extends Pantalla {

    @FXML private EncabezadoCompraVista encabezadoController;
    @FXML private GridPane mapa;
    @FXML private HBox cajaSugerencia;
    @FXML private Label lblSugerencia;
    @FXML private Button btnSugerencia;
    @FXML private Label lblGrupo;
    @FXML private Button btnGrupoMenos;
    @FXML private Button btnGrupoMas;
    @FXML private CheckBox chkAccesible;
    @FXML private Label lblMensajeMapa;
    @FXML private Label lblGuia;
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
    private int grupoSugerencia = 2;
    private TipoEntrada tipoBase; // el primero de la lista: recibe las butacas que no tienen otro tipo
    /** Cantidad por tipo, el tipo base primero; la del tipo base se calcula a partir de las butacas elegidas. */
    private final Map<TipoEntrada, Integer> cantidades = new LinkedHashMap<>();
    private final Map<TipoEntrada, Label> valores = new LinkedHashMap<>();
    private final Map<TipoEntrada, Button> botonesMas = new LinkedHashMap<>();
    private final Map<TipoEntrada, Button> botonesMenos = new LinkedHashMap<>();
    private final List<Butaca> elegidas = new ArrayList<>();
    private final Map<Butaca, Button> botonesButaca = new LinkedHashMap<>();

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
        for (TipoEntrada t : r.dato()) {
            boolean base = tipoBase == null;
            if (base) tipoBase = t;
            cantidades.put(t, base ? 0 : Math.max(0, estado().getCantidades().getOrDefault(t, 0)));
            Label nombre = new Label(t.getNombre());
            nombre.getStyleClass().add("etiqueta");
            Label precio = new Label(Formato.dinero(funcion.precioEntrada(t)));
            precio.getStyleClass().add("nota");
            VBox info = new VBox(1, nombre, precio);
            if (base) {
                Label automatico = new Label("Automático (resto de tus butacas)");
                automatico.getStyleClass().add("nota");
                info.getChildren().add(automatico);
            }
            Region espacio = new Region();
            HBox.setHgrow(espacio, Priority.ALWAYS);

            Label valor = new Label("0");
            valor.getStyleClass().add("contador-valor");
            valores.put(t, valor);
            HBox fila;
            if (base) {
                fila = new HBox(info, espacio, valor);
            } else {
                Button menos = new Button("−");
                menos.getStyleClass().add("contador-menos");
                Button mas = new Button("+");
                mas.getStyleClass().add("contador-mas");
                menos.setOnAction(e -> cambiarCantidad(t, -1));
                mas.setOnAction(e -> cambiarCantidad(t, +1));
                botonesMenos.put(t, menos);
                botonesMas.put(t, mas);
                HBox contador = new HBox(menos, valor, mas);
                contador.getStyleClass().add("contador");
                fila = new HBox(info, espacio, contador);
            }
            fila.setAlignment(Pos.CENTER_LEFT);
            tipos.getChildren().add(fila);
        }
        ajustarTipos();
        boolean socio = estado().cliente() != null && estado().cliente().esSocio();
        lblSocio.setText(socio ? "" : "Socio (usa puntos): inicia sesión con tu cuenta de socio para verlo.");
        lblSocio.setManaged(!socio);
    }

    /** Suma de las entradas que no son del tipo base. */
    private int totalOtrosTipos() {
        int total = 0;
        for (var e : cantidades.entrySet()) {
            if (e.getKey() != tipoBase) total += e.getValue();
        }
        return total;
    }

    /**
     * Cuadra los tipos con las butacas elegidas: el tipo base absorbe lo que sobra; si hay más entradas de otros
     * tipos que butacas, se quitan primero las del último tipo con cantidad.
     */
    private void ajustarTipos() {
        List<TipoEntrada> orden = new ArrayList<>(cantidades.keySet());
        while (totalOtrosTipos() > elegidas.size()) {
            for (int i = orden.size() - 1; i >= 0; i--) {
                TipoEntrada t = orden.get(i);
                if (t != tipoBase && cantidades.get(t) > 0) {
                    cantidades.put(t, cantidades.get(t) - 1);
                    break;
                }
            }
        }
        if (tipoBase != null) cantidades.put(tipoBase, elegidas.size() - totalOtrosTipos());
        boolean libre = totalOtrosTipos() < elegidas.size();
        for (var e : valores.entrySet()) e.getValue().setText(String.valueOf(cantidades.get(e.getKey())));
        for (var e : botonesMas.entrySet()) e.getValue().setDisable(!libre);
        for (var e : botonesMenos.entrySet()) e.getValue().setDisable(cantidades.get(e.getKey()) == 0);
    }

    /** Pasa una butaca del tipo base a este tipo (+) o la devuelve al tipo base (−). */
    private void cambiarCantidad(TipoEntrada tipo, int cambio) {
        if (cambio > 0 && totalOtrosTipos() >= elegidas.size()) return;
        cantidades.put(tipo, Math.max(0, cantidades.get(tipo) + cambio));
        limpiar(lblMensaje);
        ajustarTipos();
        actualizarResumen();
    }

    /** Cambia el tamaño del grupo para el que el sistema sugiere butacas (no toca la selección actual). */
    @FXML
    private void onGrupoMenos() {
        cambiarGrupo(-1);
    }

    @FXML
    private void onGrupoMas() {
        cambiarGrupo(+1);
    }

    private void cambiarGrupo(int cambio) {
        int maximo = mapaActual == null ? 1 : Math.max(1, mapaActual.libres().size());
        int nuevo = Math.max(1, Math.min(maximo, grupoSugerencia + cambio));
        if (nuevo == grupoSugerencia) return;
        grupoSugerencia = nuevo;
        recargarMapa();
    }

    /** Pide al servicio el mapa con las butacas libres y la sugerencia para el grupo elegido. */
    private void recargarMapa() {
        Respuesta<MapaButacas> r = app().compra().consultarMapa(compra, funcion, grupoSugerencia, chkAccesible.isSelected());
        if (!r.ok()) {
            mostrarError(lblMensajeMapa, r.mensaje());
            return;
        }
        mapaActual = r.dato();
        int libres = mapaActual.libres().size();
        if (libres > 0 && grupoSugerencia > libres) { // el grupo no puede pasar de las butacas libres
            grupoSugerencia = libres;
            recargarMapa();
            return;
        }
        if (r.mensaje() != null && !r.mensaje().isEmpty()) mostrarError(lblMensajeMapa, r.mensaje());
        else limpiar(lblMensajeMapa);
        dibujarMapa();
        actualizarResumen();
    }

    private void dibujarMapa() {
        if (!botonesButaca.isEmpty() && botonesButaca.size() == mapaActual.todas().size()) {
            actualizarBotonesMapa();
            return;
        }
        mapa.getChildren().clear();
        botonesButaca.clear();
        // Filas en orden alfabético; dentro de cada fila, la columna depende del número y del bloque
        Map<String, List<Butaca>> porFila = new TreeMap<>();
        int maxIzquierda = 0;
        int maxCentro = 0;
        int maxColumna = 0;
        for (Butaca b : mapaActual.todas()) {
            porFila.computeIfAbsent(b.getFila(), k -> new ArrayList<>()).add(b);
            if (Butaca.IZQUIERDA.equals(b.getBloque())) maxIzquierda = Math.max(maxIzquierda, b.getNumero());
            if (Butaca.CENTRO.equals(b.getBloque())) maxCentro = Math.max(maxCentro, b.getNumero());
            maxColumna = Math.max(maxColumna, columna(b));
        }
        int fila = 0;
        for (var entrada : porFila.entrySet()) {
            Label letraIzq = new Label(entrada.getKey());
            letraIzq.getStyleClass().add("fila-letra");
            mapa.add(letraIzq, 0, fila);
            entrada.getValue().sort(Comparator.comparingInt(Butaca::getNumero));
            for (Butaca b : entrada.getValue()) {
                Button btn = new Button();
                btn.getStyleClass().add("butaca");
                aplicarEstadoButaca(b, btn);
                botonesButaca.put(b, btn);
                mapa.add(btn, columna(b), fila);
            }
            Label letraDer = new Label(entrada.getKey());
            letraDer.getStyleClass().add("fila-letra");
            mapa.add(letraDer, maxColumna + 1, fila);
            fila++;
        }
        // Pasillos: columnas vacías con ancho fijo entre los bloques
        if (maxIzquierda > 0) mapa.add(pasillo(), maxIzquierda + 1, 0);
        if (maxCentro > 0) mapa.add(pasillo(), maxCentro + 2, 0);
    }

    /** Actualiza el estado visual de las butacas existentes sin destruir ni reconstruir el GridPane. */
    private void actualizarBotonesMapa() {
        for (var e : botonesButaca.entrySet()) {
            aplicarEstadoButaca(e.getKey(), e.getValue());
        }
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

    private void aplicarEstadoButaca(Butaca b, Button boton) {
        boton.getStyleClass().removeAll("elegida", "ocupada", "sugerida", "preferencial");
        boolean elegida = elegidas.contains(b);
        if (elegida) {
            boton.setText(b.esAccesible() ? "D" : String.valueOf(b.getNumero()));
            boton.getStyleClass().add("elegida");
            boton.setOnAction(e -> tocarButaca(b));
        } else if (!mapaActual.estaLibre(b)) {
            boton.setText("X");
            boton.getStyleClass().add("ocupada");
            boton.setOnAction(null); // ocupada: no se puede tocar
        } else {
            boton.setText(b.esAccesible() ? "D" : String.valueOf(b.getNumero()));
            if (elegidas.isEmpty() && mapaActual.esSugerida(b)) {
                boton.getStyleClass().add("sugerida");
            } else if (Butaca.PREFERENCIAL.equals(b.getTipo())) {
                boton.getStyleClass().add("preferencial");
            }
            boton.setOnAction(e -> tocarButaca(b));
        }
    }

    /** Elige o suelta una butaca; cada butaca elegida es una entrada. */
    private void tocarButaca(Butaca b) {
        if (!elegidas.remove(b)) elegidas.add(b);
        limpiar(lblMensaje);
        ajustarTipos();
        dibujarMapa();
        actualizarResumen();
    }

    /** Reemplaza la selección por las butacas sugeridas; todas quedan como tipo base, listas para repartir tipos. */
    @FXML
    private void onUsarSugerencia() {
        if (mapaActual.sugeridas().isEmpty()) return;
        elegidas.clear();
        elegidas.addAll(mapaActual.sugeridas());
        for (TipoEntrada t : cantidades.keySet()) cantidades.put(t, 0);
        limpiar(lblMensaje);
        ajustarTipos();
        dibujarMapa();
        actualizarResumen();
    }

    private void actualizarResumen() {
        // Sugerencia para el grupo
        List<Butaca> sugeridas = mapaActual.sugeridas();
        lblGrupo.setText(String.valueOf(grupoSugerencia));
        btnGrupoMenos.setDisable(grupoSugerencia <= 1);
        btnGrupoMas.setDisable(grupoSugerencia >= mapaActual.libres().size());
        btnSugerencia.setDisable(sugeridas.isEmpty());
        lblSugerencia.setText(sugeridas.isEmpty()
                ? "No hay butacas juntas para " + grupoSugerencia + "."
                : "Te sugerimos " + codigos(sugeridas) + ": juntas y al centro.");
        // Butacas y entradas
        boolean vacio = elegidas.isEmpty();
        lblGuia.setVisible(vacio);
        lblGuia.setManaged(vacio);
        lblButacas.setText(vacio ? "—" : codigos(elegidas));
        List<String> partes = new ArrayList<>();
        long subtotal = 0;
        for (var e : cantidades.entrySet()) {
            if (e.getValue() > 0) {
                partes.add(e.getValue() + " × " + e.getKey().getNombre());
                subtotal += e.getValue() * funcion.precioEntrada(e.getKey());
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
        if (elegidas.isEmpty()) {
            mostrarError(lblMensaje, "Elige al menos una butaca.");
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
