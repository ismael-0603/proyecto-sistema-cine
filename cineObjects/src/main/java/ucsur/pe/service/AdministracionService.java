package ucsur.pe.service;

import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Activable;
import ucsur.pe.model.Cupon;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Rol;
import ucsur.pe.repository.CuponRepository;
import ucsur.pe.repository.PeliculaRepository;
import ucsur.pe.repository.ProductoRepository;
import ucsur.pe.repository.PromocionRepository;
import ucsur.pe.repository.SedeRepository;

/**
 * Por qué existe: casos de uso del administrador (gestionar catálogo, RF-37 a RF-42).
 * Todas las operaciones exigen el permiso ADMINISTRAR; como el controller solo llama a este
 * servicio, el permiso se verifica siempre.
 */
public class AdministracionService {
    private final SedeRepository sedes;
    private final PeliculaRepository peliculas;
    private final ProductoRepository productos;
    private final CuponRepository cupones;
    private final PromocionRepository promociones;

    public AdministracionService(SedeRepository sedes, PeliculaRepository peliculas, ProductoRepository productos,
                                 CuponRepository cupones, PromocionRepository promociones) {
        this.sedes = sedes;
        this.peliculas = peliculas;
        this.productos = productos;
        this.cupones = cupones;
        this.promociones = promociones;
    }

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

    /** Programa una función en la sede del administrador; la sede rechaza cruces de horario (RF-47). */
    public void programarFuncion(Empleado admin, Funcion funcion) {
        exigirAdmin(admin);
        if (funcion == null) throw new IllegalArgumentException("La función es obligatoria");
        admin.getSede().agregarFuncion(funcion);
        sedes.guardar(admin.getSede());
    }

    /** Cancela una función de la sede del administrador; sus entradas quedan listas para reembolso. */
    public void cancelarFuncion(Empleado admin, Funcion funcion) {
        exigirAdmin(admin);
        if (funcion == null) throw new IllegalArgumentException("La función es obligatoria");
        if (!funcion.getSala().getSede().equals(admin.getSede())) {
            throw new ReglaNegocioException("La función es de otra sede; el administrador gestiona " + admin.getSede().getNombre());
        }
        funcion.cancelar();
    }

    public void registrarProducto(Empleado admin, Producto producto) {
        exigirAdmin(admin);
        productos.guardar(producto);
    }

    /** Repone stock del producto en la sede del administrador. */
    public void reponerStock(Empleado admin, Producto producto, int cantidad) {
        exigirAdmin(admin);
        producto.reponerStock(admin.getSede(), cantidad);
        productos.guardar(producto);
    }

    /** Registra un cupón; el código no puede repetirse (uq_cupon_codigo). */
    public void registrarCupon(Empleado admin, Cupon cupon) {
        exigirAdmin(admin);
        cupones.buscarPorCodigo(cupon.getCodigo()).ifPresent(existente -> {
            if (existente != cupon) {
                throw new ReglaNegocioException("Ya existe un cupón con el código " + cupon.getCodigo());
            }
        });
        cupones.guardar(cupon);
    }

    public void registrarPromocion(Empleado admin, Promocion promocion) {
        exigirAdmin(admin);
        promociones.guardar(promocion);
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

    private void exigirAdmin(Empleado admin) {
        if (admin == null || !admin.getRol().tienePermiso(Rol.ADMINISTRAR)) {
            throw new ReglaNegocioException("Solo un administrador puede realizar esta operación");
        }
    }
}
