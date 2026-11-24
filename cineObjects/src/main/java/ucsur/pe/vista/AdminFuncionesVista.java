package ucsur.pe.vista;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Funcion;
import ucsur.pe.service.ResultadoCancelacion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/** 18 · Funciones de la sede del administrador: programar, editar, cancelar (con reembolso) y reactivar. */
public class AdminFuncionesVista extends PantallaAdmin {

    private static final String TODAS = "Película: todas";
    private static final String TODOS_ESTADOS = "Estado";

    @FXML private ComboBox<String> cmbPelicula;
    @FXML private TextField txtFecha;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private Label lblMensaje;
    @FXML private VBox contenedorTabla;

    private final Tabla<Funcion> tabla = new Tabla<>(
            new String[] {"Película", "Sala", "Fecha y hora", "Idioma", "Ocupación", "Estado", "Acciones"},
            new double[] {210, 135, 130, 105, 95, 118}, 8);
    private List<Funcion> funciones = new ArrayList<>();

    public AdminFuncionesVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        if (!prepararAdmin("funciones")) return;
        contenedorTabla.getChildren().add(tabla.vista());
        cmbEstado.setItems(FXCollections.observableArrayList(TODOS_ESTADOS, "Programada", "Agotada", "En curso",
                "Finalizada", "Cancelada"));
        cmbEstado.getSelectionModel().selectFirst();
        cmbPelicula.valueProperty().addListener((o, a, b) -> filtrar(false));
        cmbEstado.valueProperty().addListener((o, a, b) -> filtrar(false));
        txtFecha.textProperty().addListener((o, a, b) -> filtrar(false));
        cargar(false);
    }

    private void cargar(boolean mantenerPagina) {
        Respuesta<List<Funcion>> r = app().adminFunciones().listarFunciones(admin);
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        funciones = new ArrayList<>(r.dato());
        funciones.sort(Comparator.comparingLong(f -> f.obtenerFechaHoraInicio().toValorComparable()));
        String elegida = cmbPelicula.getValue();
        TreeSet<String> titulos = new TreeSet<>();
        funciones.forEach(f -> titulos.add(f.getPelicula().getTitulo()));
        List<String> opciones = new ArrayList<>(List.of(TODAS));
        opciones.addAll(titulos);
        cmbPelicula.setItems(FXCollections.observableArrayList(opciones));
        cmbPelicula.setValue(elegida != null && opciones.contains(elegida) ? elegida : TODAS);
        filtrar(mantenerPagina);
    }

    private void filtrar(boolean mantenerPagina) {
        String pelicula = TODAS.equals(cmbPelicula.getValue()) ? null : cmbPelicula.getValue();
        String estadoElegido = TODOS_ESTADOS.equals(cmbEstado.getValue()) ? null : cmbEstado.getValue();
        DateTime dia = null;
        if (txtFecha.getText() != null && !txtFecha.getText().isBlank()) {
            try {
                dia = Formato.leerFecha(txtFecha.getText(), "Fecha");
            } catch (IllegalArgumentException e) {
                dia = null; // mientras escribe la fecha incompleta, no se filtra por día
            }
        }
        DateTime filtroDia = dia;
        List<Funcion> visibles = funciones.stream()
                .filter(f -> pelicula == null || f.getPelicula().getTitulo().equals(pelicula))
                .filter(f -> estadoElegido == null || estadoDe(f).equals(estadoElegido))
                .filter(f -> filtroDia == null || f.getFecha().equals(filtroDia))
                .toList();
        tabla.mostrar(visibles, this::fila, mantenerPagina);
    }

    private static String estadoDe(Funcion f) {
        return switch (f.getEstado()) {
            case Funcion.DISPONIBLE -> "Programada";
            case Funcion.AGOTADA -> "Agotada";
            case Funcion.EN_CURSO -> "En curso";
            case Funcion.FINALIZADA -> "Finalizada";
            case Funcion.CANCELADA -> "Cancelada";
            default -> Formato.capitalizar(f.getEstado());
        };
    }

    private List<Node> fila(Funcion f) {
        int capacidad = f.getSala().getCapacidad();
        int ocupadas = capacidad - f.consultarDisponibilidad();
        boolean cancelada = Funcion.CANCELADA.equals(f.getEstado());
        boolean pendiente = f.estaPendiente();
        var editar = Tabla.boton("Editar", false, () -> abrirFormulario(f));
        editar.setDisable(cancelada || !pendiente);
        var accion = cancelada
                ? Tabla.boton("Reactivar", true, () -> despues(app().adminFunciones().reactivarFuncion(admin, f)))
                : Tabla.boton("Cancelar", false, () -> confirmarCancelacion(f));
        if (!cancelada) accion.setDisable(!pendiente);
        return List.of(Tabla.texto(f.getPelicula().getTitulo()),
                Tabla.texto("Sala " + f.getSala().getNumero() + " · " + f.getSala().getTipoSala().getFormato()),
                Tabla.texto(Formato.fechaHoraCorta(f.obtenerFechaHoraInicio())),
                Tabla.texto(Formato.capitalizar(f.getIdioma())),
                Tabla.texto(ocupadas + "/" + capacidad),
                Tabla.chip(estadoDe(f), cancelada ? "cancelada" : null),
                Tabla.acciones(editar, accion));
    }

    private void despues(Respuesta<?> r) {
        mostrar(lblMensaje, r);
        cargar(true);
    }

    @FXML
    private void onNueva() {
        abrirFormulario(null);
    }

    private void abrirFormulario(Funcion f) {
        estado().setEnEdicion(f);
        nav.ir("admin-funcion-form");
    }

    /** Confirmación y, al cancelar, el resumen de reembolsos con «Reintentar» si alguno quedó pendiente. */
    private void confirmarCancelacion(Funcion f) {
        DialogoVista d = nav.dialogo();
        d.configurar("Cancelar función", "«" + f.getPelicula().getTitulo() + "» · "
                + Formato.fechaHoraCorta(f.obtenerFechaHoraInicio()) + " · Sala " + f.getSala().getNumero()
                + "\n\nSe devolverá el dinero automáticamente a las compras pagadas. Las compras en curso se cancelan "
                + "y se liberan las butacas. Si el cliente ya recogió su dulcería, solo se le devuelven las entradas.", false);
        d.botonSecundario("Volver", nav::cerrarModal);
        d.botonPrincipal("Cancelar y reembolsar", () -> {
            Respuesta<ResultadoCancelacion> r = app().adminFunciones().cancelarFuncion(admin, f);
            if (!r.ok()) {
                nav.cerrarModal();
                mostrarError(lblMensaje, r.mensaje());
                return;
            }
            mostrarResultado(r.dato());
            cargar(true);
        });
    }

    private void mostrarResultado(ResultadoCancelacion res) {
        DialogoVista d = nav.dialogo();
        StringBuilder texto = new StringBuilder("✓ Función cancelada\n\n")
                .append(res.comprasReembolsadas()).append(" compras reembolsadas: ")
                .append(Formato.dinero(res.montoDevueltoCentavos())).append("\n")
                .append(res.comprasEnProcesoCanceladas()).append(" compras en curso canceladas");
        for (String p : res.pendientes()) texto.append("\nPendiente: ").append(p);
        d.configurar("Resultado", texto.toString(), false);
        if (!res.comprasPendientes().isEmpty()) {
            d.botonSecundario("Reintentar", () -> reintentar(res.comprasPendientes()));
        }
        d.botonPrincipal("Entendido", nav::cerrarModal);
    }

    private void reintentar(List<Compra> pendientes) {
        List<String> mensajes = new ArrayList<>();
        for (Compra c : pendientes) mensajes.add(app().adminFunciones().reintentarReembolso(admin, c).mensaje());
        nav.cerrarModal();
        mostrarOk(lblMensaje, String.join(" · ", mensajes));
        cargar(true);
    }
}
