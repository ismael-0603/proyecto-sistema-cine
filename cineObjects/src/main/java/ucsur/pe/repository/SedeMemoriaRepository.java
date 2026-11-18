package ucsur.pe.repository;

import ucsur.pe.model.Funcion;
import ucsur.pe.model.Sede;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria de SedeRepository (una lista hace de tabla). */
public class SedeMemoriaRepository extends RepositorioMemoria<Sede> implements SedeRepository {
    public SedeMemoriaRepository() { super(Sede::getIdSede, Sede::asignarId); }

    private int ultimaFuncion = 0;

    /** Guarda la sede y, como haría la BD, les da id a las funciones nuevas de su cartelera (AUTO_INCREMENT). */
    @Override
    public void guardar(Sede sede) {
        agregar(sede);
        for (Funcion f : sede.getCartelera()) {
            if (f.getIdFuncion() <= 0) {
                f.asignarId(++ultimaFuncion);
            } else {
                ultimaFuncion = Math.max(ultimaFuncion, f.getIdFuncion());
            }
        }
    }
    @Override public Optional<Sede> buscarPorId(int idSede) { return porId(idSede); }
    @Override public List<Sede> listarTodas() { return todos(); }
}
