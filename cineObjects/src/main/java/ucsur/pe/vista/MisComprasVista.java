package ucsur.pe.vista;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Calificacion;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Reclamo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 13 · Mis compras: historial del cliente (lo da PostventaController), cinco por página, con «Ver»,
 * «Calificar» y «Reclamo» (o el estado del reclamo ya presentado).
 */
public class MisComprasVista extends Pantalla {

    private static final int POR_PAGINA = 5;
    // Ancho de cada columna: fecha, película, sede · sala, total, estado (acciones ocupa el resto)
    private static final double[] ANCHOS = {100, 180, 170, 85, 108};

    @FXML private HBox cabecera;
    @FXML private VBox tabla;
    @FXML private VBox filas;
    @FXML private Label lblNota;
    @FXML private Label lblMensaje;
    @FXML private HBox paginacion;
    @FXML private Label lblMostrando;
    @FXML private Button btnAnterior;
    @FXML private HBox paginas;
    @FXML private Button btnSiguiente;
    @FXML private VBox cajaVacia;

    private Cliente cliente;
    private List<Compra> compras = new ArrayList<>();

    public MisComprasVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        cliente = estado().clienteConSesion();
        if (cliente == null) {
            Platform.runLater(() -> nav.ir("iniciar-sesion"));
            return;
        }
        String[] titulos = {"Fecha", "Película", "Sede · Sala", "Total", "Estado", "Acciones"};
        for (int i = 0; i < titulos.length; i++) cabecera.getChildren().add(celda(new Label(titulos[i]), i));

        Respuesta<List<Compra>> r = app().postventa().consultarHistorial(cliente);
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        // Solo las compras que llegaron a pagarse; la más reciente primero
        for (Compra c : r.dato()) {
            if (Compra.CONFIRMADA.equals(c.getEstado()) || Compra.REEMBOLSADA.equals(c.getEstado())) compras.add(c);
        }
        compras.sort(Comparator.comparingLong((Compra c) -> c.getFechaHora().toValorComparable()).reversed());
        boolean vacio = compras.isEmpty();
        tabla.setVisible(!vacio);
        tabla.setManaged(!vacio);
        paginacion.setVisible(!vacio);
        paginacion.setManaged(!vacio);
        cajaVacia.setVisible(vacio);
        cajaVacia.setManaged(vacio);
        mostrarPagina();
    }

    private int totalPaginas() {
        return Math.max(1, (compras.size() + POR_PAGINA - 1) / POR_PAGINA);
    }

    private void mostrarPagina() {
        int pagina = Math.min(estado().getPaginaCompras(), totalPaginas() - 1);
        estado().setPaginaCompras(pagina);
        int desde = pagina * POR_PAGINA;
        int hasta = Math.min(desde + POR_PAGINA, compras.size());
        filas.getChildren().clear();
        long reembolsado = 0;
        for (Compra c : compras.subList(desde, hasta)) {
            filas.getChildren().add(fila(c));
            reembolsado += c.getMontoReembolsadoCentavos();
        }
        lblNota.setText(reembolsado > 0 ? "Función cancelada por el cine: se devolvió " + Formato.dinero(reembolsado)
                + " automáticamente (no hay que pedirlo)." : "");
        lblNota.setManaged(reembolsado > 0);
        lblMostrando.setText(compras.isEmpty() ? "" : "Mostrando " + (desde + 1) + "–" + hasta + " de " + compras.size());
        btnAnterior.setMinWidth(Region.USE_PREF_SIZE);
        btnSiguiente.setMinWidth(Region.USE_PREF_SIZE);
        btnAnterior.setDisable(pagina == 0);
        btnSiguiente.setDisable(pagina >= totalPaginas() - 1);
        paginas.getChildren().clear();
        for (int p = 0; p < totalPaginas(); p++) {
            int destino = p;
            Button b = new Button(String.valueOf(p + 1));
            b.getStyleClass().addAll("btn-secundario", "btn-chico", "pagina");
            b.setMinWidth(Region.USE_PREF_SIZE);
            if (p == pagina) b.getStyleClass().add("activo");
            b.setOnAction(e -> irAPagina(destino));
            paginas.getChildren().add(b);
        }
    }

    private void irAPagina(int pagina) {
        estado().setPaginaCompras(pagina);
        mostrarPagina();
    }

    private HBox fila(Compra c) {
        Funcion f = c.getEntradas().isEmpty() ? null : c.getEntradas().get(0).getFuncion();
        String fecha = Formato.fechaHoraCorta(f != null ? f.obtenerFechaHoraInicio() : c.getFechaHora());
        String pelicula = f != null ? f.getPelicula().getTitulo() : "Solo dulcería";
        String sedeSala = c.getSede().getNombre() + (f != null ? " · Sala " + f.getSala().getNumero()
                + " · " + f.getSala().getTipoSala().getFormato() : "");

        Label estado = new Label(Formato.estadoCompra(c.getEstado()));
        estado.getStyleClass().addAll("estado-chip", Compra.CONFIRMADA.equals(c.getEstado()) ? "pagada" : "reembolsada");

        HBox acciones = new HBox(6);
        acciones.setAlignment(Pos.CENTER_LEFT);
        acciones.getChildren().add(boton("Ver", false, () -> {
            estado().setCompraElegida(c);
            nav.ir("detalle-compra");
        }));
        if (Compra.CONFIRMADA.equals(c.getEstado())) {
            Respuesta<Calificacion> calif = app().postventa().consultarCalificacion(cliente, c);
            if (calif.ok() && calif.dato() != null) {
                Button calificado = boton("Calificado ★ " + calif.dato().getPuntuacion(), false, () -> { });
                calificado.setDisable(true);
                acciones.getChildren().add(calificado);
            } else {
                acciones.getChildren().add(boton("Calificar", true, () -> abrirPostventa(c)));
            }
            acciones.getChildren().add(botonReclamo(c));
        }

        HBox h = new HBox(celda(texto(fecha), 0), celda(texto(pelicula), 1), celda(texto(sedeSala), 2),
                celda(texto(Formato.dinero(c.getTotalCentavos())), 3), celda(estado, 4), celda(acciones, 5));
        h.getStyleClass().add("tabla-fila");
        return h;
    }

    /** «Reclamo» si aún no presentó uno; si ya lo hizo, su estado, que abre el detalle (13.1). */
    private Button botonReclamo(Compra c) {
        Respuesta<List<Reclamo>> r = app().postventa().consultarReclamos(cliente, c);
        if (r.ok() && !r.dato().isEmpty()) {
            Reclamo ultimo = r.dato().get(r.dato().size() - 1);
            return boton("Reclamo " + Formato.estadoReclamo(ultimo.getEstado()).toLowerCase(), false, () -> {
                VerReclamoVista ver = nav.modal("ver-reclamo");
                ver.mostrar(ultimo);
            });
        }
        return boton("Reclamo", false, () -> abrirPostventa(c));
    }

    private void abrirPostventa(Compra c) {
        estado().setCompraElegida(c);
        nav.ir("calificar-reclamo");
    }

    private static Button boton(String texto, boolean principal, Runnable accion) {
        Button b = new Button(texto);
        b.getStyleClass().addAll(principal ? "btn-primario" : "btn-secundario", "btn-chico");
        b.setMinWidth(Region.USE_PREF_SIZE);
        b.setOnAction(e -> accion.run());
        return b;
    }

    private static Label texto(String s) {
        Label l = new Label(s);
        l.getStyleClass().add("nota");
        l.setWrapText(true);
        return l;
    }

    private static Node celda(Node contenido, int columna) {
        if (columna >= ANCHOS.length) {
            HBox.setHgrow(contenido, Priority.ALWAYS);
            if (contenido instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
            return contenido;
        }
        if (contenido instanceof Label l) l.setMaxWidth(ANCHOS[columna] - 14); // así el texto largo baja de línea
        HBox caja = new HBox(contenido);
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.setMinWidth(ANCHOS[columna]);
        caja.setPrefWidth(ANCHOS[columna]);
        caja.setMaxWidth(ANCHOS[columna]);
        return caja;
    }

    @FXML private void onAnterior() { irAPagina(estado().getPaginaCompras() - 1); }
    @FXML private void onSiguiente() { irAPagina(estado().getPaginaCompras() + 1); }
    @FXML private void onMisDatos() { nav.ir("mi-cuenta"); }
    @FXML private void onCartelera() { nav.ir(estado().getSede() == null ? "elegir-sede" : "cartelera"); }
}
