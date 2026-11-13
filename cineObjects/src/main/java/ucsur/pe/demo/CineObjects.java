package ucsur.pe.demo;

import ucsur.pe.App;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Butaca;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Cupon;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.DetallePedido;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Entrada;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Genero;
import ucsur.pe.model.Pedido;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Reclamo;
import ucsur.pe.model.Rol;
import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoEntrada;
import ucsur.pe.model.TipoSala;
import ucsur.pe.model.TransaccionPago;
import ucsur.pe.tipo.CalculoPorcentaje;
import java.util.ArrayList;
import java.util.List;

/**
 * Por qué existe: prueba por consola que recorre el flujo completo del cine (cartelera, compra,
 * dulcería, pago, puntos y postventa). Hace de "vista": solo llama a los controllers de App y
 * muestra lo que responden. Los datos de infraestructura (sede, salas, butacas) se cargan aquí
 * como datos de prueba; con la base de datos vendrían de ella.
 */
public class CineObjects {

    /** Imprime un encabezado para separar visualmente cada ejemplo. */
    private static void titulo(String texto) {
        System.out.println("\n=== " + texto + " ===");
    }

    /** Muestra si la acción salió bien o el motivo por el que se rechazó. */
    private static String resultado(Respuesta<?> r) {
        return r.ok() ? "true" : "false | motivo: " + r.mensaje();
    }

    public static void main(String[] args) {
        App app = new App();

        // ---------- 1. Infraestructura y cartelera ----------
        titulo("1. Infraestructura y cartelera");
        Sede sede = new Sede(1, "Cineplanet Prueba", "Av. Lima 123", "Miraflores", null, null, Sede.ACTIVA);
        TipoSala tipo = new TipoSala(1, "2D", "Sala estandar", 0);
        Sala sala = new Sala(1, 1, 10, Sala.ACTIVA, sede, tipo);
        sede.agregarSala(sala);
        for (int i = 1; i <= 5; i++) {
            sala.agregarButaca(new Butaca(0, "A", i, Butaca.ESTANDAR, sala));
        }
        app.sedes().guardar(sede);

        Rol rolAdmin = new Rol(1, "Administrador", "Gestiona el cine", Rol.NIVEL_ADMIN);
        Empleado admin = new Empleado(1, "11223344", "Carla", "Rios", new DateTime(3, 3, 1990),
                "carla@cine.com", null, "admin123", "Manana", rolAdmin, sede);
        app.empleados().guardar(admin);

        Genero accion = new Genero(1, "Accion");
        Pelicula peli = new Pelicula(1, "Pelicula de prueba", "Sinopsis", 120, "+14",
                null, null, new DateTime(1, 1, 2026), Pelicula.EN_CARTELERA);
        peli.agregarGenero(accion);
        app.admin().registrarPelicula(admin, peli);

        // Funciones de manana, para que no hayan empezado
        DateTime manana = DateTime.ahora().sumarDias(1);
        DateTime fechaFuncion = new DateTime(manana.getDia(), manana.getMes(), manana.getAnio());

        Funcion funcion = new Funcion(1, fechaFuncion, new DateTime(19, 0), "Doblada", null, peli, sala);
        app.admin().programarFuncion(admin, funcion);
        System.out.println("Funcion: " + funcion + " | hora fin: " + funcion.obtenerFechaHoraFin().soloHora());

        // Solapamiento: empieza a las 20:00 y la anterior termina a las 21:00 (+15 min de limpieza)
        Respuesta<Void> cruce = app.admin().programarFuncion(admin,
                new Funcion(2, fechaFuncion, new DateTime(20, 0), "Doblada", null, peli, sala));
        System.out.println("Solapamiento detectado: " + cruce.mensaje());

        // Esta si cabe: empieza a las 21:30, despues de la limpieza (21:15)
        Funcion funcion3 = new Funcion(3, fechaFuncion, new DateTime(21, 30), "Subtitulada", null, peli, sala);
        app.admin().programarFuncion(admin, funcion3);
        System.out.println("Funciones en cartelera: " + app.cartelera().consultarCartelera(sede).dato().size());
        System.out.println("Filtro idioma Doblada + genero Accion: "
                + app.cartelera().filtrarCartelera(sede, "Doblada", null, accion).dato().size());

        // Sin permiso ADMINISTRAR no se programan funciones
        Rol rolDulceria = new Rol(2, "Personal de dulceria", "Prepara y entrega pedidos", Rol.NIVEL_DULCERIA);
        Empleado empleado = new Empleado(2, "87654321", "Ana", "Quispe", new DateTime(5, 5, 1999),
                "ana@mail.com", null, "clave456", "Tarde", rolDulceria, sede);
        app.empleados().guardar(empleado);
        System.out.println("Dulceria programa una funcion: "
                + resultado(app.admin().programarFuncion(empleado, new Funcion(9, fechaFuncion, new DateTime(8, 0), "Doblada", null, peli, sala))));

        // ---------- 2. Compra basica ----------
        titulo("2. Compra basica");
        Cliente cliente = app.login().registrarCliente("12345678", "Ismael", "Chipana",
                new DateTime(1, 1, 2000), "ismael@mail.com", null, "clave123").dato();
        TipoEntrada general = new TipoEntrada(1, "GENERAL", "Entrada general", 20.0, 2, false, true);

        Compra compra = app.compra().iniciarCompra(cliente, sede).dato();
        List<Butaca> recomendados = app.compra().recomendarAsientos(funcion, 2).dato();
        System.out.println("Asientos recomendados: " + recomendados);
        for (Butaca b : recomendados) {
            app.compra().agregarEntrada(compra, funcion, b, general);
        }
        System.out.println("Asientos bloqueados: " + resultado(app.compra().confirmarAsientos(compra)));

        Respuesta<TransaccionPago> pago = app.pago().pagar(compra, compra.getTotal(), TransaccionPago.TARJETA_CREDITO);
        System.out.println("Pago aprobado: " + resultado(pago));
        System.out.println(app.compra().generarComprobante(compra).dato()); // la serie la asigna el repositorio (como el trigger)
        System.out.println("Puntos del cliente: " + cliente.consultarPuntos());
        System.out.println("Butacas libres en funcion 1: " + funcion.consultarDisponibilidad());

        // ---------- 3. Conflicto de asiento ----------
        titulo("3. Dos clientes eligen la misma butaca (A5)");
        Cliente otro = app.login().ingresarComoInvitado("otro@mail.com").dato();
        Compra c1 = app.compra().iniciarCompra(cliente, sede).dato();
        Compra c2 = app.compra().iniciarCompra(otro, sede).dato();
        Butaca a4 = sala.obtenerMapaButacas().get(3);
        Butaca a5 = sala.obtenerMapaButacas().get(4);

        app.compra().agregarEntrada(c1, funcion, a5, general);
        Entrada entradaC2 = app.compra().agregarEntrada(c2, funcion, a5, general).dato(); // todavia libre, se acepta
        app.compra().confirmarAsientos(c1);                                               // c1 bloquea A5

        System.out.println("Error esperado: " + app.compra().confirmarAsientos(c2).mensaje());
        app.compra().quitarEntrada(c2, entradaC2);
        app.compra().agregarEntrada(c2, funcion, a4, general);
        System.out.println("Reintento con A4: " + resultado(app.compra().confirmarAsientos(c2)));

        app.compra().cancelar(c1);
        app.compra().cancelar(c2);
        System.out.println("Estado c1: " + c1.getEstado() + " | estado c2: " + c2.getEstado());
        System.out.println("Butacas libres tras cancelar: " + funcion.consultarDisponibilidad());

        // ---------- 4. Dulceria, cupon y pedido ----------
        titulo("4. Entrada + combo + cupon + pedido");
        Producto combo = new Producto(1, "Combo Mediano", "Canchita y gaseosa", "Combos", 25.0, true, true);
        app.admin().registrarProducto(admin, combo);
        app.admin().reponerStock(admin, combo, 10);
        Cupon cupon = new Cupon(1, "CINE10", new CalculoPorcentaje(10),
                new DateTime(1, 1, 2026), new DateTime(31, 12, 2030), 5, true);
        app.admin().registrarCupon(admin, cupon);
        System.out.println("Menu de dulceria: " + app.dulceria().consultarMenu(sede, cliente).dato());

        Compra c3 = app.compra().iniciarCompra(cliente, sede).dato();
        app.compra().agregarEntrada(c3, funcion3, sala.obtenerMapaButacas().get(0), general); // A1
        Pedido pedido = new Pedido(0);
        pedido.agregarDetalle(new DetallePedido(0, combo, 2, "Sin sal"));
        app.compra().asignarPedido(c3, pedido);
        System.out.println("Cupon aplicado: " + resultado(app.compra().aplicarCupon(c3, "CINE10")));
        System.out.println("Cupon inexistente: " + resultado(app.compra().aplicarCupon(c3, "NOEXISTE")));
        System.out.println("Asientos bloqueados: " + resultado(app.compra().confirmarAsientos(c3)));

        Respuesta<TransaccionPago> pago3 = app.pago().pagar(c3, c3.getTotal(), TransaccionPago.YAPE);
        System.out.println("Pago aprobado: " + resultado(pago3));
        System.out.println(app.compra().generarComprobante(c3).dato());
        System.out.println("Stock del combo: " + combo.getStock(sede));
        System.out.println("Puntos del cliente: " + cliente.consultarPuntos());

        System.out.println("Pedidos pendientes en dulceria: " + app.dulceria().consultarPendientes(empleado).dato());
        app.dulceria().prepararPedido(empleado, pedido);
        System.out.println(pedido);
        System.out.println(app.dulceria().marcarListo(empleado, pedido).dato());
        System.out.println("Entrega con QR incorrecto: " + resultado(app.dulceria().entregarPedido(empleado, pedido, "QR-FALSO")));
        System.out.println("Entrega con QR correcto: " + resultado(app.dulceria().entregarPedido(empleado, pedido, pedido.getCodigoQR())));
        System.out.println(pedido);

        // ---------- 5. Pago rechazado y reembolso ----------
        titulo("5. Pago rechazado y reembolso");
        Compra c5 = app.compra().iniciarCompra(cliente, sede).dato();
        app.compra().agregarEntrada(c5, funcion3, sala.obtenerMapaButacas().get(2), general); // A3
        app.compra().confirmarAsientos(c5);
        System.out.println("Pago con monto incorrecto: " + resultado(app.pago().pagar(c5, 1.0, TransaccionPago.TARJETA_DEBITO)));
        app.compra().cancelar(c5);
        System.out.println("Estado c5: " + c5.getEstado());

        Compra c4 = app.compra().iniciarCompra(cliente, sede).dato();
        app.compra().agregarEntrada(c4, funcion3, sala.obtenerMapaButacas().get(1), general); // A2
        app.compra().confirmarAsientos(c4);
        Respuesta<TransaccionPago> pago4 = app.pago().pagar(c4, c4.getTotal(), TransaccionPago.TARJETA_CREDITO);
        System.out.println("Pago c4 aprobado: " + resultado(pago4));
        System.out.println("Puntos antes del reembolso: " + cliente.consultarPuntos());
        System.out.println("Reembolso con funcion activa: " + resultado(app.pago().reembolsar(pago4.dato())));
        app.admin().cancelarFuncion(admin, funcion3);
        System.out.println("Reembolso con funcion cancelada: " + resultado(app.pago().reembolsar(pago4.dato())));
        System.out.println("Estado c4: " + c4.getEstado());
        System.out.println("Puntos despues del reembolso: " + cliente.consultarPuntos());
        System.out.println("Butacas libres en funcion 3: " + funcion3.consultarDisponibilidad());

        // ---------- 6. Entradas y combos de socio ----------
        titulo("6. Entradas y combos de socio");
        cliente.acumularPuntos(300); // saldo de prueba
        System.out.println("Saldo de puntos de prueba: " + cliente.consultarPuntos());
        TipoEntrada normal = new TipoEntrada(2, "GENERAL", "Entrada general", 30.0, 2, false, true);
        TipoEntrada socio50 = new TipoEntrada(3, "SOCIO", "Entrada socio a mitad de precio", 15.0, 3, true, true, 40);
        Producto comboNivel2 = new Producto(2, "Combo Oferta Nivel 2", "Oferta para socios nivel 2", "Combos", 30.0, true, 2, true);
        Producto comboSocio = new Producto(3, "Combo Socio", "Oferta para socios", "Combos", 25.0, true, 1, true);
        app.admin().registrarProducto(admin, comboNivel2);
        app.admin().reponerStock(admin, comboNivel2, 5);
        app.admin().registrarProducto(admin, comboSocio);
        app.admin().reponerStock(admin, comboSocio, 5);
        System.out.println("Tarifa normal: " + normal.getTarifa());
        System.out.println("Tarifa socio (mitad): " + socio50.getTarifa());
        System.out.println("Puntos que cuesta: " + socio50.getPuntosCanje());

        Compra c6 = app.compra().iniciarCompra(cliente, sede).dato();
        app.compra().agregarEntrada(c6, funcion, sala.obtenerMapaButacas().get(0), socio50); // A1

        // Un combo de nivel 2 no esta disponible para un cliente de nivel 1
        Pedido pedidoFallido = new Pedido(0);
        pedidoFallido.agregarDetalle(new DetallePedido(0, comboNivel2, 1, null));
        System.out.println("Error esperado: " + app.compra().asignarPedido(c6, pedidoFallido).mensaje());

        Pedido pedido6 = new Pedido(0);
        pedido6.agregarDetalle(new DetallePedido(0, comboSocio, 1, null));
        app.compra().asignarPedido(c6, pedido6);
        System.out.println("Asientos bloqueados: " + resultado(app.compra().confirmarAsientos(c6)));

        Respuesta<TransaccionPago> pago6 = app.pago().pagar(c6, c6.getTotal(), TransaccionPago.PLIN);
        System.out.println("Pago aprobado: " + resultado(pago6));
        System.out.println(app.compra().generarComprobante(c6).dato());
        System.out.println("Puntos del cliente: " + cliente.consultarPuntos());
        System.out.println("Stock del Combo Socio: " + comboSocio.getStock(sede));

        // Un invitado no puede usar la entrada de socio
        Compra c7 = app.compra().iniciarCompra(otro, sede).dato();
        app.compra().agregarEntrada(c7, funcion, sala.obtenerMapaButacas().get(3), socio50); // A4
        System.out.println("Invitado - error esperado: " + app.compra().validarBeneficiosSocio(c7).mensaje());
        app.compra().cancelar(c7);

        // Un socio sin puntos suficientes tampoco
        Cliente nuevo = app.login().registrarCliente("11112222", "Luis", "Perez", new DateTime(1, 1, 1995),
                "luis@mail.com", null, "clave789").dato();
        Compra c8 = app.compra().iniciarCompra(nuevo, sede).dato();
        app.compra().agregarEntrada(c8, funcion, sala.obtenerMapaButacas().get(3), socio50); // A4
        System.out.println("Socio sin puntos - error esperado: " + app.compra().validarBeneficiosSocio(c8).mensaje());
        app.compra().cancelar(c8);

        // Reembolso: la funcion se cancela y los puntos vuelven a su saldo
        app.admin().cancelarFuncion(admin, funcion);
        System.out.println("Reembolso c6: " + resultado(app.pago().reembolsar(pago6.dato())));
        System.out.println("Estado c6: " + c6.getEstado() + " | puntos tras reembolso: " + cliente.consultarPuntos());

        // ---------- 7. Expiracion de los 4 minutos y subida de nivel ----------
        titulo("7. Expiracion de los 4 minutos y subida de nivel");
        Sala sala2 = new Sala(2, 2, 10, Sala.ACTIVA, sede, tipo);
        sede.agregarSala(sala2);
        for (int i = 1; i <= 10; i++) {
            sala2.agregarButaca(new Butaca(0, "B", i, Butaca.ESTANDAR, sala2));
        }
        Funcion funcion4 = new Funcion(4, fechaFuncion, new DateTime(10, 0), "Doblada", null, peli, sala2);
        app.admin().programarFuncion(admin, funcion4);

        // Una compra creada hace 10 minutos ya vencio su plazo de 4 (se arma a mano solo para la prueba)
        Compra vencida = new Compra(0, DateTime.ahora().sumarMinutos(-10), cliente, sede);
        System.out.println("Error esperado: "
                + app.compra().agregarEntrada(vencida, funcion4, sala2.obtenerMapaButacas().get(0), general).mensaje());
        System.out.println("Estado de la compra vencida: " + vencida.getEstado());

        // 10 compras confirmadas en el anio suben al nivel 2
        Cliente frecuente = app.login().registrarCliente("33334444", "Rosa", "Diaz", new DateTime(10, 10, 1990),
                "rosa@mail.com", null, "clave000").dato();
        for (int i = 0; i < 10; i++) {
            Compra cv = app.compra().iniciarCompra(frecuente, sede).dato();
            app.compra().agregarEntrada(cv, funcion4, sala2.obtenerMapaButacas().get(i), general);
            app.compra().confirmarAsientos(cv);
            app.pago().pagar(cv, cv.getTotal(), TransaccionPago.PAGO_QR);
        }
        System.out.println("Visitas del cliente frecuente: " + frecuente.contarVisitasAnioActual());
        System.out.println("Nivel de socio: " + frecuente.getNivelSocio()
                + " | multiplicador de puntos: " + frecuente.obtenerMultiplicadorPuntos());
        int base4Entradas = 4 * general.obtenerPuntos();
        System.out.println("4 entradas GENERAL (base " + base4Entradas + " puntos): nivel "
                + cliente.getNivelSocio() + " gana " + cliente.aplicarMultiplicador(base4Entradas)
                + " | nivel " + frecuente.getNivelSocio() + " gana " + frecuente.aplicarMultiplicador(base4Entradas));
        System.out.println("Puntos del cliente frecuente: " + frecuente.consultarPuntos());

        // ---------- 8. Promocion por dia, calificacion, reclamo y validaciones ----------
        titulo("8. Promocion por dia, calificacion, reclamo y validaciones");
        int diaHoy = DateTime.ahora().diaDeLaSemana();
        List<Integer> soloHoy = new ArrayList<>();
        soloHoy.add(diaHoy);
        List<Integer> otroDia = new ArrayList<>();
        otroDia.add(diaHoy % 7 + 1);
        Promocion pHoy = new Promocion(1, "Promo hoy", "20% solo hoy", new CalculoPorcentaje(20),
                new DateTime(1, 1, 2026), new DateTime(31, 12, 2030), false, soloHoy, true);
        Promocion pOtro = new Promocion(2, "Promo otro dia", "20% otro dia", new CalculoPorcentaje(20),
                new DateTime(1, 1, 2026), new DateTime(31, 12, 2030), false, otroDia, true);
        app.admin().registrarPromocion(admin, pHoy);
        app.admin().registrarPromocion(admin, pOtro);
        System.out.println("Promociones vigentes hoy: " + app.cartelera().consultarPromociones().dato());
        Compra cp = app.compra().iniciarCompra(cliente, sede).dato();
        System.out.println("Hoy es dia " + diaHoy
                + " | promo de hoy aplicable: " + app.compra().aplicarPromocion(cp, pHoy).ok()
                + " | promo de otro dia aplicable: " + app.compra().aplicarPromocion(cp, pOtro).ok());
        app.compra().cancelar(cp);

        System.out.println("Calificacion registrada: " + resultado(app.postventa().calificar(compra, 5, "Excelente atencion"))
                + " | repetida: " + resultado(app.postventa().calificar(compra, 4, "Otra vez")));
        Compra compraSinCalificar = c3;
        System.out.println("Calificacion con puntuacion 9 - error esperado: "
                + app.postventa().calificar(compraSinCalificar, 9, null).mensaje());

        Reclamo rec = app.postventa().presentarReclamo(compra, Reclamo.QUEJA, "Demora en la dulceria").dato();
        System.out.println("Responder sin revisar: " + resultado(app.postventa().responderReclamo(rec, "Disculpas")));
        app.postventa().iniciarRevision(rec);
        app.postventa().responderReclamo(rec, "Se ofrecen disculpas");
        System.out.println("Reclamo: " + rec);
        Compra pendiente = app.compra().iniciarCompra(cliente, sede).dato();
        System.out.println("Reclamo sobre compra en proceso: "
                + resultado(app.postventa().presentarReclamo(pendiente, Reclamo.RECLAMO, "Prueba")));
        app.compra().cancelar(pendiente);

        System.out.println("Cliente con DNI de 3 digitos - error esperado: " + app.login().registrarCliente(
                "123", "Ana", "Lopez", new DateTime(1, 1, 2000), "ana.lopez@mail.com", null, "clave").mensaje());
        System.out.println("Correo ya registrado - error esperado: " + app.login().registrarCliente(
                "55556666", "Otro", "Ismael", null, "ismael@mail.com", null, "clave").mensaje());

        titulo("Resumen del cliente");
        System.out.println(cliente + " | puntos: " + cliente.consultarPuntos()
                + " | compras en historial: " + app.postventa().consultarHistorial(cliente).dato().size());
    }
}
