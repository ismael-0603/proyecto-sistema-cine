package ucsur.pe.vista;

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
 * Por qué existe: las listas de administración (películas, funciones, productos, sedes, salas, cupones)
 * son la misma tabla blanca con cabecera, filas y paginación «Anterior · 1 2 · Siguiente». Esta clase la arma;
 * cada pantalla solo dice las columnas y cómo se ve una fila. La última columna (acciones) ocupa el resto.
 */
final class Tabla<T> {

    private final double[] anchos;
    private final int porPagina;
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
        this.anchos = anchos;
        this.porPagina = porPagina;
        HBox cabecera = new HBox();
        cabecera.getStyleClass().add("tabla-cabecera");
        for (int i = 0; i < titulos.length; i++) cabecera.getChildren().add(celda(new Label(titulos[i]), i));
        VBox tabla = new VBox(cabecera, filas);
        tabla.getStyleClass().add("tabla");

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
            HBox fila = new HBox();
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
            HBox.setHgrow(contenido, Priority.ALWAYS);
            if (contenido instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
            return contenido;
        }
        if (contenido instanceof Label l) {
            l.setWrapText(true);
            l.setMaxWidth(anchos[columna] - 14);
        }
        HBox caja = new HBox(contenido);
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.setMinWidth(anchos[columna]);
        caja.setPrefWidth(anchos[columna]);
        caja.setMaxWidth(anchos[columna]);
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
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }
}
