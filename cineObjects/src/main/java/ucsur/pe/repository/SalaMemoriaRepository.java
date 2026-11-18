package ucsur.pe.repository;

import ucsur.pe.model.Sala;
import ucsur.pe.model.TipoSala;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria de SalaRepository (una lista hace de tabla). */
public class SalaMemoriaRepository extends RepositorioMemoria<Sala> implements SalaRepository {
    public SalaMemoriaRepository() { super(Sala::getIdSala, Sala::asignarId); }

    @Override public void guardar(Sala sala) { agregar(sala); }
    @Override public Optional<Sala> buscarPorId(int idSala) { return porId(idSala); }

    /** En memoria no hay tabla tipo_sala: son los formatos de las salas guardadas, sin repetir. */
    @Override
    public List<TipoSala> listarTiposSala() {
        List<TipoSala> tipos = new ArrayList<>();
        for (Sala s : todos()) {
            if (!tipos.contains(s.getTipoSala())) tipos.add(s.getTipoSala());
        }
        return tipos;
    }
}
