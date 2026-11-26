package ucsur.pe.service;

import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.exception.RepositoryException;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.Sala;
import ucsur.pe.repository.CompraRepository;
import ucsur.pe.repository.SedeRepository;
import java.util.ArrayList;
import java.util.List;

import static ucsur.pe.service.PermisosAdmin.exigirAdmin;
import static ucsur.pe.service.PermisosAdmin.exigirFuncionDeSuSede;

/**
 * Por qué existe: casos de uso del administrador sobre las funciones de su sede (wireframe 18, RF-47 y RF-24):
 * programar, reprogramar, cancelar con reembolso automático y reactivar. Todas exigen el permiso ADMINISTRAR
 * y que la función sea de la sede donde trabaja el administrador.
 */
public class FuncionAdminService {
    private final SedeRepository sedes;
    private final CompraRepository compras;
    private final PagoService pagos; // para el reembolso automático al cancelar una función
    private final Reloj reloj;       // las funciones nuevas lo necesitan para saber su estado

    public FuncionAdminService(SedeRepository sedes, CompraRepository compras, PagoService pagos, Reloj reloj) {
        this.sedes = sedes;
        this.compras = compras;
        this.pagos = pagos;
        this.reloj = reloj;
    }

    /**
     * Programa una función en una sala de la sede del administrador; la sede rechaza una sala de otra sede
     * y la sala, los cruces de horario (RF-47). Solo se programan películas en cartelera, en preestreno
     * o reestrenadas (wireframe 18.1). La base le genera el id.
     */
    public Funcion programarFuncion(Empleado admin, Pelicula pelicula, Sala sala, DateTime fecha, DateTime hora,
                                    String idioma) {
        exigirAdmin(admin);
        if (pelicula != null && !pelicula.estaEnCartelera()) {
            throw new ReglaNegocioException("Solo se programan películas en cartelera, en preestreno o reestrenadas");
        }
        if (sala == null) {
            throw new IllegalArgumentException("La sala es obligatoria");
        }
        if (!sala.estaOperativa()) {
            throw new ReglaNegocioException("La sala " + sala.getNumero() + " no está activa");
        }
        Funcion funcion = new Funcion(0, fecha, hora, idioma, Funcion.DISPONIBLE, pelicula, sala, reloj);
        if (!reloj.ahora().esAntesDe(funcion.obtenerFechaHoraInicio())) {
            throw new IllegalArgumentException("La fecha y hora de la función deben ser futuras");
        }
        admin.getSede().agregarFuncion(funcion);
        try {
            sedes.guardar(admin.getSede());
        } catch (RuntimeException e) {
            admin.getSede().quitarFuncion(funcion);
            throw e;
        }
        return funcion;
    }

    /** Cambia fecha, hora e idioma de una función que aún no tiene entradas vendidas ni reservadas. */
    public void editarFuncion(Empleado admin, Funcion funcion, DateTime fecha, DateTime hora, String idioma) {
        exigirFuncionDeSuSede(admin, funcion);
        DateTime fechaAntes = funcion.getFecha();
        DateTime horaAntes = funcion.getHoraInicio();
        String idiomaAntes = funcion.getIdioma();
        funcion.reprogramar(fecha, hora, idioma);
        try {
            sedes.guardar(admin.getSede());
        } catch (RuntimeException e) {
            funcion.restaurarHorario(fechaAntes, horaAntes, idiomaAntes);
            throw e;
        }
    }

    /**
     * Cancela una función de la sede del administrador y devuelve el dinero sin que el cliente lo pida (RF-24):
     * las compras pagadas se reembolsan por la pasarela y las que estaban en curso se cancelan, liberando
     * sus butacas. Si un reembolso falla, la compra queda en la lista de pendientes y las demás siguen.
     * Orden: primero se guarda la función cancelada y recién después se devuelve el dinero. Un reembolso de la
     * pasarela no se puede deshacer con un rollback, así que si la BD no acepta la cancelación, no se devuelve nada.
     */
    public ResultadoCancelacion cancelarFuncion(Empleado admin, Funcion funcion) {
        exigirFuncionDeSuSede(admin, funcion);
        funcion.cancelar();
        try {
            sedes.guardar(admin.getSede());
        } catch (RepositoryException e) {
            deshacerCancelacion(funcion);
            throw e;
        }
        int reembolsadas = 0;
        int enProcesoCanceladas = 0;
        long devuelto = 0;
        List<String> pendientes = new ArrayList<>();
        List<Compra> comprasPendientes = new ArrayList<>();
        for (Compra compra : compras.listarPorFuncion(funcion)) {
            try {
                if (Compra.CONFIRMADA.equals(compra.getEstado())) {
                    devuelto += pagos.reembolsar(compra);
                    reembolsadas++;
                } else if (Compra.EN_PROCESO.equals(compra.getEstado()) && !compra.verificarExpiracion()) {
                    compra.cancelar();
                    compras.guardar(compra);
                    enProcesoCanceladas++;
                }
            } catch (IllegalStateException | IllegalArgumentException | RepositoryException e) {
                pendientes.add("Compra #" + compra.getIdCompra() + ": " + e.getMessage());
                comprasPendientes.add(compra);
            }
        }
        return new ResultadoCancelacion(reembolsadas, devuelto, enProcesoCanceladas, pendientes, comprasPendientes);
    }

    /**
     * «Reintentar» del aviso de cancelación (wireframe 18): vuelve a pedir a la pasarela el reembolso de una
     * compra que quedó pendiente. Solo para compras de la sede del administrador; devuelve el monto en céntimos.
     */
    public long reintentarReembolso(Empleado admin, Compra compra) {
        exigirAdmin(admin);
        if (compra == null) throw new IllegalArgumentException("La compra es obligatoria");
        if (!compra.getSede().equals(admin.getSede())) {
            throw new ReglaNegocioException("La compra es de otra sede; el administrador gestiona " + admin.getSede().getNombre());
        }
        return pagos.reembolsar(compra); // valida que esté CONFIRMADA y que su función esté cancelada
    }

    /** Vuelve a poner a la venta una función cancelada que aún no empieza (wireframe 18, «Reactivar»). */
    public void reactivarFuncion(Empleado admin, Funcion funcion) {
        exigirFuncionDeSuSede(admin, funcion);
        funcion.reactivar();
        sedes.guardar(admin.getSede());
    }

    public List<Funcion> listarFunciones(Empleado admin) {
        exigirAdmin(admin);
        return admin.getSede().getCartelera();
    }

    /** La BD no guardó la cancelación: la función vuelve a estar a la venta también en memoria, si aún se puede. */
    private void deshacerCancelacion(Funcion funcion) {
        try {
            funcion.reactivar();
        } catch (IllegalStateException | IllegalArgumentException e) {
            // ya empezó o su horario se ocupó: queda cancelada solo en memoria hasta reiniciar el kiosco
        }
    }
}
