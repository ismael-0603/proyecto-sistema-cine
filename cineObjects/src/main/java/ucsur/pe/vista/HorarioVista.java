package ucsur.pe.vista;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Genero;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.TipoSala;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 2 · Selección de horario: las funciones de la película en la sede, por día y por formato de sala.
 * Al tocar un horario empieza la compra (si ya hay cliente) o se pide iniciar sesión o seguir como invitado.
 */
public class HorarioVista extends Pantalla {

    @FXML private StackPane poster;
    @FXML private Label lblTitulo;
    @FXML private FlowPane generos;
    @FXML private Label lblDuracion;
    @FXML private Label lblClasificacion;
    @FXML private Button btnTrailer;
    @FXML private HBox cajaSedeFecha;
    @FXML private Label lblSedeFecha;
    @FXML private Label lblResumenDia;
    @FXML private FlowPane dias;
    @FXML private Label lblMensaje;
    @FXML private VBox formatos;

    private final List<Funcion> funciones = new ArrayList<>();
    private final List<Button> botonesDia = new ArrayList<>();

    public HorarioVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        Pelicula p = estado().getPelicula();
        if (p == null || estado().getSede() == null) {
            Platform.runLater(() -> nav.ir("cartelera"));
            return;
        }
        estado().setFuncion(null);
        poster.getChildren().add(Formato.poster(p, 312, 460, 24));
        lblTitulo.setText(p.getTitulo());
        for (Genero g : p.getGeneros()) {
            Label tag = new Label(g.getNombre());
            tag.getStyleClass().add("tag-genero-oscuro");
            generos.getChildren().add(tag);
        }
        lblDuracion.setText(p.getDuracion() + " min");
        lblClasificacion.setText(p.getClasificacionEdad());
        btnTrailer.setGraphic(Iconos.crear(Iconos.PLAY, 15));
        btnTrailer.setGraphicTextGap(6);
        btnTrailer.setDisable(p.getTrailer() == null || p.getTrailer().isBlank());
        cajaSedeFecha.getChildren().add(0, Iconos.crear(Iconos.UBICACION, 15, "icono-dorado"));

        Respuesta<List<Funcion>> r = app().cartelera().consultarCartelera(estado().getSede());
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        for (Funcion f : r.dato()) {
            if (f.getPelicula().getIdPelicula() == p.getIdPelicula()) funciones.add(f);
        }
        funciones.sort(Comparator.comparingLong(f -> f.obtenerFechaHoraInicio().toValorComparable()));
        cargarDias();
    }

    /** Un botón por día con funciones; se elige el día filtrado en la cartelera o, si no hay, el primero. */
    private void cargarDias() {
        Map<Long, DateTime> unicos = new LinkedHashMap<>();
        for (Funcion f : funciones) unicos.putIfAbsent(f.getFecha().toValorComparable(), f.getFecha());
        if (unicos.isEmpty()) {
            lblSedeFecha.setText(estado().getSede().getNombre());
            mostrarError(lblMensaje, "Esta película ya no tiene funciones disponibles en esta sede.");
            return;
        }
        for (DateTime dia : unicos.values()) {
            Button b = botonDia(dia);
            b.setOnAction(e -> elegirDia(dia, b));
            botonesDia.add(b);
            dias.getChildren().add(b);
        }
        List<DateTime> lista = new ArrayList<>(unicos.values());
        DateTime filtrado = estado().getDiaElegido();
        int elegido = 0;
        for (int i = 0; filtrado != null && i < lista.size(); i++) {
            if (lista.get(i).toValorSoloFecha() == filtrado.toValorSoloFecha()) elegido = i;
        }
        elegirDia(lista.get(elegido), botonesDia.get(elegido));
    }

    private static Button botonDia(DateTime dia) {
        String[] partes = Formato.diaCorto(dia).split(" ");
        Label semana = new Label(partes[0].toUpperCase());
        semana.getStyleClass().add("dia-semana");
        Label numero = new Label(partes.length > 1 ? partes[1] : String.valueOf(dia.getDia()));
        numero.getStyleClass().add("dia-numero");
        Label mes = new Label(Formato.mesCorto(dia).toUpperCase());
        mes.getStyleClass().add("dia-mes");
        VBox contenido = new VBox(-1, semana, numero, mes);
        contenido.setAlignment(Pos.CENTER);
        Button b = new Button();
        b.setGraphic(contenido);
        b.getStyleClass().add("dia-tarjeta");
        return b;
    }

    private void elegirDia(DateTime dia, Button boton) {
        for (Button b : botonesDia) b.getStyleClass().remove("activo");
        boton.getStyleClass().add("activo");
        lblSedeFecha.setText(estado().getSede().getNombre() + " · " + Formato.fechaLarga(dia));

        // Agrupa las funciones del día por formato de sala (2D, 3D, Prime...)
        Map<Integer, List<Funcion>> porFormato = new LinkedHashMap<>();
        Map<Integer, TipoSala> tipos = new LinkedHashMap<>();
        int totalDia = 0;
        for (Funcion f : funciones) {
            if (!f.getFecha().equals(dia)) continue;
            TipoSala tipo = f.getSala().getTipoSala();
            porFormato.computeIfAbsent(tipo.getIdTipoSala(), k -> new ArrayList<>()).add(f);
            tipos.putIfAbsent(tipo.getIdTipoSala(), tipo);
            totalDia++;
        }
        lblResumenDia.setText(totalDia + (totalDia == 1 ? " horario disponible" : " horarios disponibles"));
        lblResumenDia.setVisible(totalDia > 0);
        lblResumenDia.setManaged(totalDia > 0);

        formatos.getChildren().clear();
        for (var entrada : porFormato.entrySet()) {
            formatos.getChildren().add(tarjetaFormato(tipos.get(entrada.getKey()), entrada.getValue()));
        }
    }

    private VBox tarjetaFormato(TipoSala tipo, List<Funcion> delFormato) {
        boolean esPremium = tipo.obtenerRecargoFormatoCentavos() > 0;

        StackPane cajaIcono = new StackPane(Iconos.crear(esPremium ? Iconos.ESTRELLA : Iconos.SALA, 18));
        cajaIcono.getStyleClass().add("tarjeta-formato-icono");

        Label nombre = new Label(tipo.getFormato());
        nombre.getStyleClass().add("titulo-seccion");

        VBox textos = new VBox(3, nombre);
        if (tipo.getDescripcion() != null && !tipo.getDescripcion().isBlank()) {
            Label descripcion = new Label(tipo.getDescripcion());
            descripcion.getStyleClass().add("nota-suave");
            descripcion.setWrapText(true);
            textos.getChildren().add(descripcion);
        }
        HBox.setHgrow(textos, Priority.ALWAYS);

        int cantidad = delFormato.size();
        Label cuenta = new Label(cantidad + (cantidad == 1 ? " función" : " funciones"));
        cuenta.getStyleClass().add("tarjeta-formato-cuenta");
        cuenta.setMinWidth(Region.USE_PREF_SIZE);

        HBox cabecera = new HBox(14, cajaIcono, textos, cuenta);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.getStyleClass().add("tarjeta-formato-cabecera");

        FlowPane horas = new FlowPane(12, 12);
        horas.setAlignment(Pos.CENTER_LEFT);
        for (Funcion f : delFormato) horas.getChildren().add(botonHorario(f));

        VBox cuerpo = new VBox(horas);
        cuerpo.getStyleClass().add("tarjeta-formato-cuerpo");

        VBox tarjeta = new VBox(cabecera, cuerpo);
        tarjeta.getStyleClass().add("tarjeta-formato");
        if (esPremium) tarjeta.getStyleClass().add("premium");
        return tarjeta;
    }

    private Button botonHorario(Funcion f) {
        boolean disponible = f.estaDisponible();
        Label hora = new Label(f.getHoraInicio().soloHora());
        hora.getStyleClass().add("horario-hora");
        hora.setMinWidth(Region.USE_PREF_SIZE);

        String textoIdioma = disponible ? Formato.capitalizar(f.getIdioma()) : "Agotado";
        Label idioma = new Label(textoIdioma);
        idioma.getStyleClass().add("horario-idioma");
        idioma.setMinWidth(Region.USE_PREF_SIZE);
        if (!disponible) {
            idioma.getStyleClass().add("agotada");
        } else if (textoIdioma.toLowerCase().contains("sub")) {
            idioma.getStyleClass().add("subtitulada");
        }

        HBox filaSuperior = new HBox(8, hora, idioma);
        filaSuperior.setAlignment(Pos.CENTER_LEFT);

        int libres = disponible ? f.consultarDisponibilidad() : 0;
        String textoDetalle = !disponible
                ? "Sala " + f.getSala().getNumero() + " · Sin butacas"
                : libres <= 10
                        ? "Sala " + f.getSala().getNumero() + " · ¡Últimas " + libres + "!"
                        : "Sala " + f.getSala().getNumero() + " · " + libres + " libres";
        Label detalle = new Label(textoDetalle);
        detalle.getStyleClass().add("horario-detalle");
        if (disponible && libres <= 10) detalle.getStyleClass().add("pocos");

        VBox contenido = new VBox(6, filaSuperior, detalle);
        contenido.setAlignment(Pos.CENTER_LEFT);
        Button b = new Button();
        b.setGraphic(contenido);
        b.getStyleClass().add("horario");
        b.setDisable(!disponible);
        b.setOnAction(e -> elegirFuncion(f));
        return b;
    }

    /** Si la función es de otro día, pide confirmación antes de iniciar la compra. */
    private void elegirFuncion(Funcion f) {
        if (f.getFecha().toValorSoloFecha() != DateTime.ahora().toValorSoloFecha()) {
            DialogoVista d = nav.dialogo();
            d.configurar("IMPORTANTE",
                    "La fecha seleccionada para la función es " + Formato.fechaLarga(f.getFecha())
                            + " (" + Formato.fecha(f.getFecha()) + ").",
                    true);
            d.centrar();
            d.botonSecundario("Cancelar compra", nav::cerrarModal);
            d.botonPrincipal("Continuar", () -> {
                nav.cerrarModal();
                continuarConFuncion(f);
            });
            return;
        }
        continuarConFuncion(f);
    }

    /** Con cliente ya identificado empieza la compra; si no, pregunta si inicia sesión o sigue como invitado. */
    private void continuarConFuncion(Funcion f) {
        estado().setFuncion(f);
        if (estado().empleadoEnPanel()) {
            nav.irACuentaPersonalConHorario(); // el empleado compra con su cuenta personal
            return;
        }
        if (!estado().hayCliente()) {
            nav.ir("login-invitado");
            return;
        }
        Respuesta<Compra> r = nav.iniciarCompra();
        if (!r.ok()) mostrarError(lblMensaje, r.mensaje());
    }

    @FXML
    private void onVolver() {
        nav.ir("cartelera");
    }

    @FXML
    private void onSinopsis() {
        SinopsisVista sinopsis = nav.modal("sinopsis");
        sinopsis.mostrar(estado().getPelicula(), funciones);
    }

    @FXML
    private void onTrailer() {
        nav.abrirEnlace(estado().getPelicula().getTrailer());
    }
}
