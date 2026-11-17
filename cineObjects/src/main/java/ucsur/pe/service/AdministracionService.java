package ucsur.pe.service;

import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Activable;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Cupon;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Rol;
import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoSala;
import ucsur.pe.repository.CompraRepository;
import ucsur.pe.repository.CuponRepository;
import ucsur.pe.repository.PeliculaRepository;
import ucsur.pe.repository.ProductoRepository;
import ucsur.pe.repository.PromocionRepository;
import ucsur.pe.repository.SalaRepository;
import ucsur.pe.repository.SedeRepository;
import ucsur.pe.tipo.CalculoDescuento;
import java.util.ArrayList;
import java.util.List;

/**
 * Por qué existe: casos de uso del administrador (gestionar catálogo, RF-37 a RF-42 y RF-47).
 * Todas las operaciones exigen el permiso ADMINISTRAR; como el controller solo llama a este
 * servicio, el permiso se verifica siempre.
 * «Eliminar» siempre es una baja lógica (se desactiva o se retira): las compras y pedidos antiguos
 * siguen apuntando a esos datos y el historial no se pierde.
 */
public class AdministracionService {
    private final SedeRepository sedes;
    private final SalaRepository salas;
    private final PeliculaRepository peliculas;
    private final ProductoRepository productos;
    private final CuponRepository cupones;
    private final PromocionRepository promociones;
    private final CompraRepository compras;
    private final PagoService pagos; // para el reembolso automático al cancelar una función

    public AdministracionService(SedeRepository sedes, SalaRepository salas, PeliculaRepository peliculas,
                                 ProductoRepository productos, CuponRepository cupones,
                                 PromocionRepository promociones, CompraRepository compras, PagoService pagos) {
        this.sedes = sedes;
        this.salas = salas;
        this.peliculas = peliculas;
        this.productos = productos;
        this.cupones = cupones;
        this.promociones = promociones;
        this.compras = compras;
        this.pagos = pagos;
    }

    // ---------- Películas (wireframe 17) ----------
    public void registrarPelicula(Empleado admin, Pelicula pelicula) {
        exigirAdmin(admin);
        peliculas.guardar(pelicula);
    }

    public void actualizarPelicula(Empleado admin, Pelicula pelicula, String titulo, String sinopsis,
                                   String clasificacion, String trailer, String poster, DateTime fechaEstreno) {
        exigirAdmin(admin);
        pelicula.actualizarInformacion(titulo, sinopsis, clasificacion, trailer, poster, fechaEstreno);
        peliculas.guardar(pelicula);
    }

    /** «Eliminar» película: pasa a FUERA_DE_CARTELERA si no tiene funciones pendientes en ninguna sede. */
    public void retirarPelicula(Empleado admin, Pelicula pelicula) {
        exigirAdmin(admin);
        if (pelicula == null) throw new IllegalArgumentException("La película es obligatoria");
        for (Sede sede : sedes.listarTodas()) {
            for (Funcion f : sede.getCartelera()) {
                if (f.getPelicula() == pelicula && esPendiente(f)) {
                    throw new ReglaNegocioException("La película tiene funciones pendientes en " + sede.getNombre()
                            + "; cancélelas antes de retirarla");
                }
            }
        }
        pelicula.setEstado(Pelicula.FUERA_DE_CARTELERA);
        peliculas.guardar(pelicula);
    }

    public List<Pelicula> listarPeliculas(Empleado admin) {
        exigirAdmin(admin);
        return peliculas.listarTodas();
    }

    // ---------- Funciones (wireframe 18) ----------
    /** Programa una función en la sede del administrador; la sede rechaza cruces de horario (RF-47). */
    public void programarFuncion(Empleado admin, Funcion funcion) {
        exigirAdmin(admin);
        if (funcion == null) throw new IllegalArgumentException("La función es obligatoria");
        admin.getSede().agregarFuncion(funcion);
        sedes.guardar(admin.getSede());
    }

    /** Cambia fecha, hora e idioma de una función que aún no tiene entradas vendidas ni reservadas. */
    public void editarFuncion(Empleado admin, Funcion funcion, DateTime fecha, DateTime hora, String idioma) {
        exigirFuncionDeSuSede(admin, funcion);
        funcion.reprogramar(fecha, hora, idioma);
        sedes.guardar(admin.getSede());
    }

    /**
     * Cancela una función de la sede del administrador y devuelve el dinero sin que el cliente lo pida (RF-24):
     * las compras pagadas se reembolsan por la pasarela y las que estaban en curso se cancelan, liberando
     * sus butacas. Si un reembolso falla, la compra queda en la lista de pendientes y las demás siguen.
     */
    public ResultadoCancelacion cancelarFuncion(Empleado admin, Funcion funcion) {
        exigirFuncionDeSuSede(admin, funcion);
        funcion.cancelar();
        int reembolsadas = 0;
        int enProcesoCanceladas = 0;
        long devuelto = 0;
        List<String> pendientes = new ArrayList<>();
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
            } catch (IllegalStateException | IllegalArgumentException e) {
                pendientes.add("Compra #" + compra.getIdCompra() + ": " + e.getMessage());
            }
        }
        sedes.guardar(admin.getSede());
        return new ResultadoCancelacion(reembolsadas, devuelto, enProcesoCanceladas, pendientes);
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

    // ---------- Productos de dulcería (wireframe 20) ----------
    public void registrarProducto(Empleado admin, Producto producto) {
        exigirAdmin(admin);
        productos.guardar(producto);
    }

    public void actualizarProducto(Empleado admin, Producto producto, String nombre, String descripcion,
                                   String categoria, long precioCentavos, boolean esCombo, int nivelSocioMinimo) {
        exigirAdmin(admin);
        if (producto == null) throw new IllegalArgumentException("El producto es obligatorio");
        producto.actualizar(nombre, descripcion, categoria, precioCentavos, esCombo, nivelSocioMinimo);
        productos.guardar(producto);
    }

    /** Repone stock del producto en la sede del administrador. */
    public void reponerStock(Empleado admin, Producto producto, int cantidad) {
        exigirAdmin(admin);
        producto.reponerStock(admin.getSede(), cantidad);
        productos.guardar(producto);
    }

    public List<Producto> listarProductos(Empleado admin) {
        exigirAdmin(admin);
        return productos.listarTodos();
    }

    // ---------- Cupones y promociones (wireframe 19) ----------
    /** Registra un cupón; el código no puede repetirse (uq_cupon_codigo). */
    public void registrarCupon(Empleado admin, Cupon cupon) {
        exigirAdmin(admin);
        exigirCodigoLibre(cupon.getCodigo(), cupon);
        cupones.guardar(cupon);
    }

    public void actualizarCupon(Empleado admin, Cupon cupon, String codigo, CalculoDescuento calculo,
                                DateTime fechaInicio, DateTime fechaFin, int limiteUso) {
        exigirAdmin(admin);
        if (cupon == null) throw new IllegalArgumentException("El cupón es obligatorio");
        exigirCodigoLibre(codigo, cupon);
        cupon.actualizar(codigo, calculo, fechaInicio, fechaFin, limiteUso);
        cupones.guardar(cupon);
    }

    public List<Cupon> listarCupones(Empleado admin) {
        exigirAdmin(admin);
        return cupones.listarTodos();
    }

    public void registrarPromocion(Empleado admin, Promocion promocion) {
        exigirAdmin(admin);
        promociones.guardar(promocion);
    }

    public void actualizarPromocion(Empleado admin, Promocion promocion, String nombre, String descripcion,
                                    CalculoDescuento calculo, DateTime fechaInicio, DateTime fechaFin,
                                    boolean soloSocios, List<Integer> diasAplicables) {
        exigirAdmin(admin);
        if (promocion == null) throw new IllegalArgumentException("La promoción es obligatoria");
        promocion.actualizar(nombre, descripcion, calculo, fechaInicio, fechaFin, soloSocios, diasAplicables);
        promociones.guardar(promocion);
    }

    public List<Promocion> listarPromociones(Empleado admin) {
        exigirAdmin(admin);
        return promociones.listarTodas();
    }

    /** Habilita o retira cualquier elemento activable (descuento, producto, tipo de entrada o cuenta). */
    public void cambiarEstado(Empleado admin, Activable elemento, boolean activo) {
        exigirAdmin(admin);
        if (activo) {
            elemento.activar();
        } else {
            elemento.desactivar();
        }
    }

    // ---------- Sedes (RF-42) ----------
    public Sede registrarSede(Empleado admin, String nombre, String direccion, String distrito,
                              String telefono, String horarioAtencion) {
        exigirAdmin(admin);
        Sede sede = new Sede(sedes.siguienteId(), nombre, direccion, distrito, telefono, horarioAtencion, Sede.ACTIVA);
        sedes.guardar(sede);
        return sede;
    }

    public void actualizarSede(Empleado admin, Sede sede, String nombre, String direccion, String distrito,
                               String telefono, String horarioAtencion) {
        exigirAdmin(admin);
        if (sede == null) throw new IllegalArgumentException("La sede es obligatoria");
        sede.actualizarDatos(nombre, direccion, distrito, telefono, horarioAtencion);
        sedes.guardar(sede);
    }

    /** Abre, cierra o pone en mantenimiento una sede; para cerrarla no debe tener funciones pendientes. */
    public void cambiarEstadoSede(Empleado admin, Sede sede, String estado) {
        exigirAdmin(admin);
        if (sede == null) throw new IllegalArgumentException("La sede es obligatoria");
        if (!Sede.ACTIVA.equals(estado) && sede.tieneFuncionesPendientes()) {
            throw new ReglaNegocioException("La sede tiene funciones pendientes; cancélelas antes de cerrarla");
        }
        sede.setEstado(estado);
        sedes.guardar(sede);
    }

    /** «Eliminar» sede: baja lógica (INACTIVA). */
    public void darDeBajaSede(Empleado admin, Sede sede) {
        cambiarEstadoSede(admin, sede, Sede.INACTIVA);
    }

    public List<Sede> listarSedes(Empleado admin) {
        exigirAdmin(admin);
        return sedes.listarTodas();
    }

    // ---------- Salas (RF-39) ----------
    /**
     * Crea una sala con su mapa de butacas: {@code filas} filas de {@code lateral} | {@code central} | {@code lateral}
     * butacas separadas por dos pasillos. La capacidad es el total de butacas.
     */
    public Sala registrarSala(Empleado admin, Sede sede, int numero, TipoSala tipoSala,
                              int filas, int lateral, int central) {
        exigirAdmin(admin);
        if (sede == null) throw new IllegalArgumentException("La sede es obligatoria");
        if (filas <= 0 || lateral < 0 || central <= 0) {
            throw new IllegalArgumentException("Indique filas, butacas laterales y butacas centrales válidas");
        }
        Sala sala = new Sala(salas.siguienteId(), numero, filas * (2 * lateral + central), Sala.ACTIVA, sede, tipoSala);
        sala.generarButacas(filas, lateral, central);
        sede.agregarSala(sala); // rechaza un número de sala repetido en la sede
        salas.guardar(sala);
        sedes.guardar(sede);
        return sala;
    }

    public void actualizarSala(Empleado admin, Sala sala, int numero, TipoSala tipoSala) {
        exigirAdmin(admin);
        if (sala == null) throw new IllegalArgumentException("La sala es obligatoria");
        sala.getSede().actualizarSala(sala, numero, tipoSala);
        salas.guardar(sala);
    }

    /** Cambia filas y anchos de bloque (y la capacidad) de una sala que todavía no tiene funciones. */
    public void redistribuirSala(Empleado admin, Sala sala, int filas, int lateral, int central) {
        exigirAdmin(admin);
        if (sala == null) throw new IllegalArgumentException("La sala es obligatoria");
        sala.redistribuir(filas, lateral, central);
        salas.guardar(sala);
    }

    /** Activa, desactiva o pone en mantenimiento una sala; para sacarla de uso no debe tener funciones pendientes. */
    public void cambiarEstadoSala(Empleado admin, Sala sala, String estado) {
        exigirAdmin(admin);
        if (sala == null) throw new IllegalArgumentException("La sala es obligatoria");
        if (!Sala.ACTIVA.equals(estado) && sala.tieneFuncionesPendientes()) {
            throw new ReglaNegocioException("La sala tiene funciones pendientes; cancélelas antes de sacarla de uso");
        }
        sala.setEstado(estado);
        salas.guardar(sala);
    }

    /** «Eliminar» sala: baja lógica (INACTIVA). */
    public void darDeBajaSala(Empleado admin, Sala sala) {
        cambiarEstadoSala(admin, sala, Sala.INACTIVA);
    }

    public List<Sala> listarSalas(Empleado admin, Sede sede) {
        exigirAdmin(admin);
        if (sede == null) throw new IllegalArgumentException("La sede es obligatoria");
        return sede.getSalas();
    }

    // ---------- Validaciones comunes ----------
    private void exigirAdmin(Empleado admin) {
        if (admin == null || !admin.getRol().tienePermiso(Rol.ADMINISTRAR)) {
            throw new ReglaNegocioException("Solo un administrador puede realizar esta operación");
        }
    }

    private void exigirFuncionDeSuSede(Empleado admin, Funcion funcion) {
        exigirAdmin(admin);
        if (funcion == null) throw new IllegalArgumentException("La función es obligatoria");
        if (!funcion.getSala().getSede().equals(admin.getSede())) {
            throw new ReglaNegocioException("La función es de otra sede; el administrador gestiona " + admin.getSede().getNombre());
        }
    }

    private void exigirCodigoLibre(String codigo, Cupon propio) {
        cupones.buscarPorCodigo(codigo).ifPresent(existente -> {
            if (existente != propio) {
                throw new ReglaNegocioException("Ya existe un cupón con el código " + codigo);
            }
        });
    }

    /** Una función pendiente es la que aún no terminó y no fue cancelada. */
    private static boolean esPendiente(Funcion f) {
        return !Funcion.CANCELADA.equals(f.getEstado()) && !Funcion.FINALIZADA.equals(f.getEstado());
    }
}
