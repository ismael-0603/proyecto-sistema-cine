package ucsur.pe.vista;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Por qué existe: las listas de administración (películas, funciones, productos, sedes, salas, cupones y promociones)
 * comparten tabla blanca con cabecera, filas y paginación. Cada pantalla solo define columnas, pesos y ancho de Acciones.
 */
final class Tabla<T> {

    private static final double ANCHO_ACCIONES_DEFECTO = 160.0;
    private static final double ESPACIO_COLUMNAS = 12.0;
    private static final double RELLENO_HORIZONTAL = 30.0;

    private final double[] anchos;
    private final double[] proporciones;
    private final double anchoAcciones;
    private final int porPagina;
    private final DoubleBinding anchoDatos;
    private final VBox filas = new VBox();
    private final VBox raiz;
    private final Label mostrando = new Label();
    private final HBox paginas = new HBox(8);
    private final Button anterior = new Button("Anterior");
    private final Button siguiente = new Button("Siguiente");

    private List<T> datos = new ArrayList<>();
    private Function<T, List<Node>> filaDe;
    private int pagina = 0;

    Tabla(String[] titulos, double[] anchos, int porPagina) {
        this(titulos, anchos, ANCHO_ACCIONES_DEFECTO, porPagina);
    }

    Tabla(String[] titulos, double[] anchos, double anchoAcciones, int porPagina) {
        this.anchos = anchos;
        this.anchoAcciones = anchoAcciones;
        this.porPagina = porPagina;
        this.proporciones = new double[anchos.length];
        double sumaAnchos = 0;
        for (double a : anchos) sumaAnchos += a;
        for (int i = 0; i < anchos.length; i++) {
            proporciones[i] = sumaAnchos > 0 ? anchos[i] / sumaAnchos : 1.0 / anchos.length;
        }

        HBox cabecera = new HBox(ESPACIO_COLUMNAS);
        cabecera.getStyleClass().add("tabla-cabecera");
        VBox tabla = new VBox(cabecera, filas);
        tabla.getStyleClass().add("tabla");
        this.anchoDatos = Bindings.max(0.0,
                tabla.widthProperty().subtract(anchoAcciones + RELLENO_HORIZONTAL + ESPACIO_COLUMNAS * anchos.length));
        for (int i = 0; i < titulos.length; i++) cabecera.getChildren().add(celda(new Label(titulos[i]), i));

        mostrando.getStyleClass().add("nota-clara");
        for (Button b : new Button[] {anterior, siguiente}) b.getStyleClass().addAll("btn-secundario", "btn-chico");
        anterior.setOnAction(e -> irA(pagina - 1));
        siguiente.setOnAction(e -> irA(pagina + 1));
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox pie = new HBox(10, mostrando, espacio, anterior, paginas, siguiente);
        pie.setAlignment(Pos.CENTER_LEFT);
        pie.setPadding(new Insets(4, 0, 0, 0));
        raiz = new VBox(14, tabla, pie);
    }

    Node vista() {
        return raiz;
    }

    /** Cambia los datos; {@code mantenerPagina} conserva la página actual (por ejemplo, tras editar una fila). */
    void mostrar(List<T> nuevos, Function<T, List<Node>> filaDe, boolean mantenerPagina) {
        this.datos = new ArrayList<>(nuevos);
        this.filaDe = filaDe;
        irA(mantenerPagina ? pagina : 0);
    }

    private int totalPaginas() {
        return Math.max(1, (datos.size() + porPagina - 1) / porPagina);
    }

    private void irA(int nueva) {
        pagina = Math.max(0, Math.min(nueva, totalPaginas() - 1));
        int desde = pagina * porPagina;
        int hasta = Math.min(desde + porPagina, datos.size());
        filas.getChildren().clear();
        for (T d : datos.subList(desde, hasta)) {
            List<Node> celdas = filaDe.apply(d);
            HBox fila = new HBox(ESPACIO_COLUMNAS);
            fila.getStyleClass().add("tabla-fila");
            for (int i = 0; i < celdas.size(); i++) fila.getChildren().add(celda(celdas.get(i), i));
            filas.getChildren().add(fila);
        }
        if (datos.isEmpty()) {
            Label vacio = new Label("No hay registros que coincidan.");
            vacio.getStyleClass().add("nota-suave");
            HBox fila = new HBox(vacio);
            fila.getStyleClass().add("tabla-fila");
            filas.getChildren().add(fila);
        }
        mostrando.setText(datos.isEmpty() ? "" : "Mostrando " + (desde + 1) + "–" + hasta + " de " + datos.size());
        anterior.setDisable(pagina == 0);
        siguiente.setDisable(pagina >= totalPaginas() - 1);
        paginas.getChildren().clear();
        // Como mucho 7 números, alrededor de la página actual
        int desdePag = Math.max(0, Math.min(pagina - 3, totalPaginas() - 7));
        int hastaPag = Math.min(totalPaginas(), desdePag + 7);
        for (int p = desdePag; p < hastaPag; p++) {
            int destino = p;
            Button b = new Button(String.valueOf(p + 1));
            b.getStyleClass().addAll("btn-secundario", "btn-chico", "pagina");
            if (p == pagina) b.getStyleClass().add("activo");
            b.setOnAction(e -> irA(destino));
            paginas.getChildren().add(b);
        }
    }

    private Node celda(Node contenido, int columna) {
        if (columna >= anchos.length) {
            HBox caja = new HBox(contenido);
            caja.setAlignment(Pos.CENTER_RIGHT);
            caja.setMinWidth(anchoAcciones);
            caja.setPrefWidth(anchoAcciones);
            caja.setMaxWidth(anchoAcciones);
            return caja;
        }
        if (contenido instanceof Label l && !l.getStyleClass().contains("estado-chip")) {
            l.setWrapText(true);
            l.setMinWidth(0);
        }
        HBox caja = new HBox(contenido);
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.setMinWidth(0);
        caja.prefWidthProperty().bind(anchoDatos.multiply(proporciones[columna]));
        caja.maxWidthProperty().bind(anchoDatos.multiply(proporciones[columna]));
        return caja;
    }

    // ---------- Piezas que usan las filas ----------

    static Label texto(String s) {
        Label l = new Label(s == null ? "—" : s);
        l.getStyleClass().add("nota");
        return l;
    }

    static Label chip(String texto, String estilo) {
        Label l = new Label(texto);
        l.getStyleClass().add("estado-chip");
        if (estilo != null) l.getStyleClass().add(estilo);
        return l;
    }

    static Button boton(String texto, boolean principal, Runnable accion) {
        Button b = new Button(texto);
        b.getStyleClass().addAll(principal ? "btn-primario" : "btn-secundario", "btn-chico");
        // Sin esto la fila los encoge y el texto se corta («Ed…»): el botón nunca es más angosto que su texto
        b.setMinWidth(Region.USE_PREF_SIZE);
        b.setOnAction(e -> accion.run());
        return b;
    }

    static HBox acciones(Node... botones) {
        HBox h = new HBox(10, botones);
        h.setAlignment(Pos.CENTER_RIGHT);
        return h;
    }
}
