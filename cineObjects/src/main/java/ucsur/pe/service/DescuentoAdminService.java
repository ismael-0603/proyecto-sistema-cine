package ucsur.pe.service;

import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Cupon;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Promocion;
import ucsur.pe.repository.CuponRepository;
import ucsur.pe.repository.PromocionRepository;
import ucsur.pe.tipo.CalculoDescuento;
import ucsur.pe.tipo.FabricaCalculoDescuento;
import java.util.List;

import static ucsur.pe.service.PermisosAdmin.exigirAdmin;

/**
 * Por qué existe: casos de uso del administrador sobre cupones y promociones (wireframe 19, RF-40 y RF-41).
 * El descuento llega como tipo (uno de tiposDescuento(), por ejemplo "PORCENTAJE") y valor: el porcentaje
 * (10 = 10%) o el monto en céntimos (500 = S/ 5.00); FabricaCalculoDescuento arma el cálculo.
 * nivelSocioMinimo: 0 = todos, 1 = cualquier socio, 2 a 4 = socios de ese nivel o más.
 */
public class DescuentoAdminService {
    private final CuponRepository cupones;
    private final PromocionRepository promociones;

    public DescuentoAdminService(CuponRepository cupones, PromocionRepository promociones) {
        this.cupones = cupones;
        this.promociones = promociones;
    }

    // ---------- Cupones ----------
    /** Crea un cupón activo; el código no puede repetirse (uq_cupon_codigo). */
    public Cupon registrarCupon(Empleado admin, String codigo, String tipoDescuento, long valor,
                                DateTime fechaInicio, DateTime fechaFin, int limiteUso, int nivelSocioMinimo) {
        exigirAdmin(admin);
        exigirCodigoLibre(codigo, null);
        Cupon cupon = new Cupon(0, codigo, FabricaCalculoDescuento.crear(tipoDescuento, valor),
                fechaInicio, fechaFin, limiteUso, nivelSocioMinimo, true);
        cupones.guardar(cupon);
        return cupon;
    }

    public void actualizarCupon(Empleado admin, Cupon cupon, String codigo, String tipoDescuento, long valor,
                                DateTime fechaInicio, DateTime fechaFin, int limiteUso, int nivelSocioMinimo) {
        exigirAdmin(admin);
        if (cupon == null) throw new IllegalArgumentException("El cupón es obligatorio");
        exigirCodigoLibre(codigo, cupon);
        cupon.actualizar(codigo, FabricaCalculoDescuento.crear(tipoDescuento, valor), fechaInicio, fechaFin,
                limiteUso, nivelSocioMinimo);
        cupones.guardar(cupon);
    }

    /** Habilita o retira un cupón y lo guarda. */
    public void cambiarEstadoCupon(Empleado admin, Cupon cupon, boolean activo) {
        exigirAdmin(admin);
        if (cupon == null) throw new IllegalArgumentException("El cupón es obligatorio");
        cupon.cambiarEstado(activo);
        cupones.guardar(cupon);
    }

    public List<Cupon> listarCupones(Empleado admin) {
        exigirAdmin(admin);
        return cupones.listarTodos();
    }

    // ---------- Promociones ----------
    /** Crea una promoción activa, con la misma forma de descuento y escala de nivel que el cupón (wireframe 10). */
    public Promocion registrarPromocion(Empleado admin, String nombre, String descripcion, String tipoDescuento,
                                        long valor, DateTime fechaInicio, DateTime fechaFin, int nivelSocioMinimo,
                                        List<Integer> diasAplicables) {
        exigirAdmin(admin);
        Promocion promocion = new Promocion(0, nombre, descripcion, FabricaCalculoDescuento.crear(tipoDescuento, valor),
                fechaInicio, fechaFin, nivelSocioMinimo, diasAplicables, true);
        promociones.guardar(promocion);
        return promocion;
    }

    public void actualizarPromocion(Empleado admin, Promocion promocion, String nombre, String descripcion,
                                    String tipoDescuento, long valor, DateTime fechaInicio, DateTime fechaFin,
                                    int nivelSocioMinimo, List<Integer> diasAplicables) {
        exigirAdmin(admin);
        if (promocion == null) throw new IllegalArgumentException("La promoción es obligatoria");
        CalculoDescuento calculo = FabricaCalculoDescuento.crear(tipoDescuento, valor);
        promocion.actualizar(nombre, descripcion, calculo, fechaInicio, fechaFin, nivelSocioMinimo, diasAplicables);
        promociones.guardar(promocion);
    }

    /** Habilita o retira una promoción y la guarda. */
    public void cambiarEstadoPromocion(Empleado admin, Promocion promocion, boolean activo) {
        exigirAdmin(admin);
        if (promocion == null) throw new IllegalArgumentException("La promoción es obligatoria");
        promocion.cambiarEstado(activo);
        promociones.guardar(promocion);
    }

    public List<Promocion> listarPromociones(Empleado admin) {
        exigirAdmin(admin);
        return promociones.listarTodas();
    }

    /** Formas de descuento para el ComboBox del formulario ("PORCENTAJE", "MONTO_FIJO"), sin escribirlas en la vista. */
    public List<String> tiposDescuento() {
        return FabricaCalculoDescuento.tiposDisponibles();
    }

    /** El código no lo puede usar otro cupón; {@code propio} es null al registrar uno nuevo. */
    private void exigirCodigoLibre(String codigo, Cupon propio) {
        cupones.buscarPorCodigo(codigo).ifPresent(existente -> {
            if (existente != propio) {
                throw new ReglaNegocioException("Ya existe un cupón con el código " + codigo);
            }
        });
    }
}
