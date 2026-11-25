package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Pelicula;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 2.1 · Sinopsis y tráiler, con «ver más» para la sinopsis completa (2.2). */
public class SinopsisVista extends Pantalla {

    /** Largo de la sinopsis antes de «ver más». */
    private static final int LARGO_RESUMEN = 220;
    /** Alto máximo de la caja de la sinopsis; si el texto completo es más alto, se desplaza. */
    private static final double ALTO_MAXIMO_SINOPSIS = 220;

    @FXML private Label lblTitulo;
    @FXML private Button btnTrailer;
    @FXML private ScrollPane scrollSinopsis;
    @FXML private Label lblSinopsis;
    @FXML private Button btnVerMas;
    @FXML private Button btnVolver;
    @FXML private Label lblIdioma;
    @FXML private Label lblFormatos;
    @FXML private Label lblDuracion;
    @FXML private Label lblEstreno;

    private Pelicula pelicula;
    private boolean completa;

    public SinopsisVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        // La caja mide lo que el texto, hasta ALTO_MAXIMO_SINOPSIS (desde ahí aparece la barra de desplazamiento)
        lblSinopsis.heightProperty().addListener((o, a, alto) -> {
            double visible = Math.min(alto.doubleValue(), ALTO_MAXIMO_SINOPSIS);
            scrollSinopsis.setMinViewportHeight(visible);
            scrollSinopsis.setPrefViewportHeight(visible);
        });
    }

    public void mostrar(Pelicula p, List<Funcion> funciones) {
        this.pelicula = p;
        lblTitulo.setText(p.getTitulo());
        btnTrailer.setDisable(p.getTrailer() == null || p.getTrailer().isBlank());
        Set<String> idiomas = new LinkedHashSet<>();
        Set<String> formatos = new LinkedHashSet<>();
        for (Funcion f : funciones) {
            idiomas.add(f.getIdioma().toUpperCase());
            formatos.add(f.getSala().getTipoSala().getFormato().toUpperCase());
        }
        lblIdioma.setText(idiomas.isEmpty() ? "—" : String.join(", ", idiomas));
        lblFormatos.setText(formatos.isEmpty() ? "—" : String.join(", ", formatos));
        lblDuracion.setText(Formato.duracion(p.getDuracion()));
        lblEstreno.setText(p.getFechaEstreno() == null ? "—" : Formato.fechaConAnio(p.getFechaEstreno()));
        // Desde «Próximamente» (cartelera) la película aún no tiene horarios a los que volver
        btnVolver.setText(funciones.isEmpty() ? "Cerrar" : "Volver a los horarios");
        completa = false;
        actualizarSinopsis();
    }

    private void actualizarSinopsis() {
        String texto = pelicula.getSinopsis() == null ? "" : pelicula.getSinopsis();
        boolean larga = texto.length() > LARGO_RESUMEN;
        lblSinopsis.setText(completa || !larga ? texto : texto.substring(0, LARGO_RESUMEN).trim() + "…");
        btnVerMas.setText(completa ? "ver menos" : "ver más");
        btnVerMas.setVisible(larga);
        btnVerMas.setManaged(larga);
        scrollSinopsis.setVvalue(0);
    }

    @FXML
    private void onVerMas() {
        completa = !completa;
        actualizarSinopsis();
    }

    @FXML
    private void onTrailer() {
        nav.abrirEnlace(pelicula.getTrailer());
    }

    @FXML
    private void onCerrar() {
        nav.cerrarModal();
    }
}
