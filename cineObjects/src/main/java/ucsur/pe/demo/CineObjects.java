package ucsur.pe.demo;

import ucsur.pe.util.ConversorDinero;
import ucsur.pe.App;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.correo.CorreoSimulado;
import ucsur.pe.model.Butaca;
import ucsur.pe.model.CalculadoraImporte;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Cupon;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Entrada;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Genero;
import ucsur.pe.model.GestorPuntos;
import ucsur.pe.model.Pedido;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Reclamo;
import ucsur.pe.model.Rol;
import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import ucsur.pe.model.SeleccionAsientos;
import ucsur.pe.model.TipoEntrada;
import ucsur.pe.model.TipoSala;
import ucsur.pe.model.TransaccionPago;
import ucsur.pe.repository.ClienteMemoriaRepository;
import ucsur.pe.repository.EmpleadoMemoriaRepository;
import ucsur.pe.service.AutenticacionService;
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

    /** Una línea con la serie y los importes (el ticket completo, con QR, lo da app.emision().imprimir). */
    private static String comprobante(Compra compra) {
        return "Comprobante " + compra.getSerieNumero() + " | " + compra.getImporte();
    }

    private static String resultado(Respuesta<?> r) {
        return r.ok() ? "true" : "false | motivo: " + r.mensaje();
    }

    /** Ultimo correo que "envio" el proveedor simulado. */
    /** El código de 6 dígitos del último correo enviado (la vista real lo escribe el usuario). */
    private static String codigoDelUltimoCorreo(App app) {
        List<CorreoSimulado.Mensaje> enviados = ((CorreoSimulado) app.correo()).getEnviados();
        return enviados.get(enviados.size() - 1).cuerpo().replaceAll("(?s).*?(\\d{6}).*", "$1");
    }

    private static String ultimoCorreo(App app) {
        List<CorreoSimulado.Mensaje> enviados = ((CorreoSimulado) app.correo()).getEnviados();
        if (enviados.isEmpty()) return "(ninguno)";
        CorreoSimulado.Mensaje m = enviados.get(enviados.size() - 1);
        return m.destinatario() + " | " + m.asunto();
    }

    /** Dibuja una fila: los pasillos se ven como "||", D = discapacidad, P = preferencial. */
    private static String filaComoTexto(Sala sala, String fila) {
        StringBuilder sb = new StringBuilder();
        String bloqueAnterior = null;
        for (Butaca b : sala.obtenerMapaButacas()) {
            if (!b.getFila().equals(fila)) continue;
            if (bloqueAnterior != null && !bloqueAnterior.equals(b.getBloque())) sb.append("|| ");
            bloqueAnterior = b.getBloque();
            String marca = b.esAccesible() ? "D" : Butaca.PREFERENCIAL.equals(b.getTipo()) ? "P" : "";
            sb.append(b.getNumero()).append(marca).append(' ');
        }
        return sb.toString().trim();
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
                "carla@cine.com", null, "admin123", "Manana", rolAdmin, sede, app.reloj());
        app.empleados().guardar(admin);

        Genero accion = new Genero(1, "Accion");
        // El administrador envía los datos; el servicio crea la película (la vista no arma modelos ni ids)
        Pelicula peli = app.admin().registrarPelicula(admin, "Pelicula de prueba", "Sinopsis", 120, "+14",
                null, null, new DateTime(1, 1, 2026), Pelicula.EN_CARTELERA, List.of(accion)).dato();

        // Funciones de manana, para que no hayan empezado
        DateTime manana = DateTime.ahora().sumarDias(1);
        DateTime fechaFuncion = new DateTime(manana.getDia(), manana.getMes(), manana.getAnio());

        Funcion funcion = app.admin().programarFuncion(admin, peli, sala, fechaFuncion, new DateTime(19, 0), "Doblada").dato();
        System.out.println("Funcion: " + funcion + " | hora fin: " + funcion.obtenerFechaHoraFin().soloHora());

        // Solapamiento: empieza a las 20:00 y la anterior termina a las 21:00 (+15 min de limpieza)
        Respuesta<Funcion> cruce = app.admin().programarFuncion(admin, peli, sala, fechaFuncion, new DateTime(20, 0), "Doblada");
        System.out.println("Solapamiento detectado: " + cruce.mensaje());

        // Esta si cabe: empieza a las 21:30, despues de la limpieza (21:15)
        Funcion funcion3 = app.admin().programarFuncion(admin, peli, sala, fechaFuncion, new DateTime(21, 30), "Subtitulada").dato();
        System.out.println("Funciones en cartelera: " + app.cartelera().consultarCartelera(sede).dato().size());
        System.out.println("Filtro idioma Doblada + genero Accion: "
                + app.cartelera().filtrarCartelera(sede, "Doblada", null, accion).dato().size());

        // Sin permiso ADMINISTRAR no se programan funciones
        Rol rolDulceria = new Rol(2, "Personal de dulceria", "Prepara y entrega pedidos", Rol.NIVEL_DULCERIA);
        Empleado empleado = new Empleado(2, "87654321", "Ana", "Quispe", new DateTime(5, 5, 1999),
                "ana@mail.com", null, "clave456", "Tarde", rolDulceria, sede, app.reloj());
        app.empleados().guardar(empleado);
        System.out.println("Dulceria programa una funcion: "
                + resultado(app.admin().programarFuncion(empleado, peli, sala, fechaFuncion, new DateTime(8, 0), "Doblada")));

        // ---------- 2. Compra basica ----------
        titulo("2. Compra basica");
        Cliente cliente = app.login().registrarCliente("12345678", "Ismael", "Chipana",
                new DateTime(1, 1, 2000), "ismael@mail.com", null, "clave123", "clave123").dato();
        TipoEntrada general = new TipoEntrada(1, "GENERAL", "Entrada general", ConversorDinero.aCentavos("20.00"), 2, false, true);

        Compra compra = app.compra().iniciarCompra(cliente, sede).dato();
        // Al entrar al mapa ya viene la sugerencia marcada (RF-29)
        List<Butaca> recomendados = app.compra().consultarMapa(compra, funcion, 2, false).dato().sugeridas();
        System.out.println("Asientos recomendados: " + recomendados);
        for (Butaca b : recomendados) {
            app.compra().agregarEntrada(compra, funcion, b, general);
        }
        System.out.println("Asientos bloqueados: " + resultado(app.compra().confirmarAsientos(compra)));

        Respuesta<TransaccionPago> pago = app.pago().pagar(compra, compra.getTotalCentavos(), TransaccionPago.TARJETA_CREDITO);
        System.out.println("Pago aprobado: " + resultado(pago));
        System.out.println(comprobante(compra)); // la serie la asigna el repositorio (como el trigger)
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
        Producto combo = app.admin().registrarProducto(admin, "Combo Mediano", "Canchita y gaseosa", "Combos",
                ConversorDinero.aCentavos("25.00"), true, 0, 0).dato();
        app.admin().reponerStock(admin, combo, 10);
        Cupon cupon = app.admin().registrarCupon(admin, "CINE10", CalculoPorcentaje.TIPO, 10,
                new DateTime(1, 1, 2026), new DateTime(31, 12, 2030), 5, 0).dato();
        System.out.println("Menu de dulceria: " + app.dulceria().consultarMenu(sede, cliente).dato());

        Compra c3 = app.compra().iniciarCompra(cliente, sede).dato();
        app.compra().agregarEntrada(c3, funcion3, sala.obtenerMapaButacas().get(0), general); // A1
        System.out.println("Agregar entrada de otra funcion a la misma compra - error esperado: "
                + app.compra().agregarEntrada(c3, funcion, sala.obtenerMapaButacas().get(4), general).mensaje());
        // El servicio arma el pedido: la vista solo dice qué producto y cuántos
        app.compra().agregarProducto(c3, combo, 2, "Sin sal");
        Pedido pedido = c3.getPedido();
        System.out.println("Cupon aplicado: " + resultado(app.compra().aplicarCupon(c3, "CINE10")));
        System.out.println("Cupon inexistente: " + resultado(app.compra().aplicarCupon(c3, "NOEXISTE")));
        System.out.println("Asientos bloqueados: " + resultado(app.compra().confirmarAsientos(c3)));

        Respuesta<TransaccionPago> pago3 = app.pago().pagar(c3, c3.getTotalCentavos(), TransaccionPago.YAPE);
        System.out.println("Pago aprobado: " + resultado(pago3));
        System.out.println(comprobante(c3));
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
        titulo("5. Pago rechazado y reembolso automatico al cancelar la funcion");
        Compra c5 = app.compra().iniciarCompra(cliente, sede).dato();
        app.compra().agregarEntrada(c5, funcion3, sala.obtenerMapaButacas().get(2), general); // A3
        app.compra().confirmarAsientos(c5);
        System.out.println("Pago con monto incorrecto: " + resultado(app.pago().pagar(c5, ConversorDinero.aCentavos("1.00"), TransaccionPago.TARJETA_DEBITO)));
        app.compra().cancelar(c5);
        System.out.println("Estado c5: " + c5.getEstado());

        Compra c4 = app.compra().iniciarCompra(cliente, sede).dato();
        app.compra().agregarEntrada(c4, funcion3, sala.obtenerMapaButacas().get(1), general); // A2
        app.compra().confirmarAsientos(c4);
        Respuesta<TransaccionPago> pago4 = app.pago().pagar(c4, c4.getTotalCentavos(), TransaccionPago.TARJETA_CREDITO);
        System.out.println("Pago c4 aprobado: " + resultado(pago4));
        System.out.println("Puntos antes del reembolso: " + cliente.consultarPuntos());
        // El cliente no pide nada: al cancelar la funcion se reembolsan solas c3 y c4.
        // c3 ya recogio su combo, asi que solo se le devuelven las entradas.
        System.out.println(app.admin().cancelarFuncion(admin, funcion3).mensaje());
        System.out.println("Estado c4: " + c4.getEstado() + " | devuelto: " + ConversorDinero.formatear(c4.getMontoReembolsadoCentavos()));
        System.out.println("Estado c3: " + c3.getEstado() + " | pagado: " + ConversorDinero.formatear(c3.getTotalCentavos())
                + " | devuelto (sin la dulceria entregada): " + ConversorDinero.formatear(c3.getMontoReembolsadoCentavos()));
        System.out.println("Puntos despues del reembolso: " + cliente.consultarPuntos());
        System.out.println("Butacas libres en funcion 3: " + funcion3.consultarDisponibilidad());

        // ---------- 6. Entradas y combos de socio ----------
        titulo("6. Entradas y combos de socio");
        cliente.acumularPuntos(300); // saldo de prueba
        System.out.println("Saldo de puntos de prueba: " + cliente.consultarPuntos());
        TipoEntrada normal = new TipoEntrada(2, "GENERAL", "Entrada general", ConversorDinero.aCentavos("30.00"), 2, false, true);
        TipoEntrada socio50 = new TipoEntrada(3, "SOCIO", "Entrada socio a mitad de precio", ConversorDinero.aCentavos("15.00"), 3, true, true, 40);
        Producto comboNivel2 = app.admin().registrarProducto(admin, "Combo Oferta Nivel 2", "Oferta para socios nivel 2",
                "Combos", ConversorDinero.aCentavos("30.00"), true, 2, 0).dato();
        Producto comboSocio = app.admin().registrarProducto(admin, "Combo Socio", "Oferta para socios", "Combos",
                ConversorDinero.aCentavos("25.00"), true, 1, 0).dato();
        app.admin().reponerStock(admin, comboNivel2, 5);
        app.admin().reponerStock(admin, comboSocio, 5);
        System.out.println("Tarifa normal: " + ConversorDinero.formatear(normal.getTarifaCentavos()));
        System.out.println("Tarifa socio (mitad): " + ConversorDinero.formatear(socio50.getTarifaCentavos()));
        System.out.println("Puntos que cuesta: " + socio50.getPuntosCanje());

        Compra c6 = app.compra().iniciarCompra(cliente, sede).dato();
        app.compra().agregarEntrada(c6, funcion, sala.obtenerMapaButacas().get(0), socio50); // A1

        // Un combo de nivel 2 no esta disponible para un cliente de nivel 1
        System.out.println("Error esperado: " + app.compra().agregarProducto(c6, comboNivel2, 1, null).mensaje());

        app.compra().agregarProducto(c6, comboSocio, 1, null);
        System.out.println("Asientos bloqueados: " + resultado(app.compra().confirmarAsientos(c6)));

        Respuesta<TransaccionPago> pago6 = app.pago().pagar(c6, c6.getTotalCentavos(), TransaccionPago.PLIN);
        System.out.println("Pago aprobado: " + resultado(pago6));
        System.out.println(comprobante(c6));
        System.out.println("Puntos del cliente: " + cliente.consultarPuntos());
        System.out.println("Stock del Combo Socio: " + comboSocio.getStock(sede));

        // Un invitado no puede usar la entrada de socio
        Compra c7 = app.compra().iniciarCompra(otro, sede).dato();
        app.compra().agregarEntrada(c7, funcion, sala.obtenerMapaButacas().get(3), socio50); // A4
        System.out.println("Invitado - error esperado: " + app.compra().validarBeneficiosSocio(c7).mensaje());
        app.compra().cancelar(c7);

        // Un socio sin puntos suficientes tampoco
        Cliente nuevo = app.login().registrarCliente("11112222", "Luis", "Perez", new DateTime(1, 1, 1995),
                "luis@mail.com", null, "clave789", "clave789").dato();
        Compra c8 = app.compra().iniciarCompra(nuevo, sede).dato();
        app.compra().agregarEntrada(c8, funcion, sala.obtenerMapaButacas().get(3), socio50); // A4
        System.out.println("Socio sin puntos - error esperado: " + app.compra().validarBeneficiosSocio(c8).mensaje());
        app.compra().cancelar(c8);

        // Reembolso automatico: la funcion se cancela y los puntos vuelven a su saldo
        System.out.println(app.admin().cancelarFuncion(admin, funcion).mensaje());
        System.out.println("Estado c6: " + c6.getEstado() + " | puntos tras reembolso: " + cliente.consultarPuntos());

        // ---------- 7. Expiracion de los 4 minutos y subida de nivel ----------
        titulo("7. Expiracion de los 4 minutos y subida de nivel");
        Sala sala2 = new Sala(2, 2, 10, Sala.ACTIVA, sede, tipo);
        sede.agregarSala(sala2);
        for (int i = 1; i <= 10; i++) {
            sala2.agregarButaca(new Butaca(0, "B", i, Butaca.ESTANDAR, sala2));
        }
        Funcion funcion4 = app.admin().programarFuncion(admin, peli, sala2, fechaFuncion, new DateTime(10, 0), "Doblada").dato();

        // Una compra creada hace 10 minutos ya vencio su plazo de 4 (se arma a mano solo para la prueba)
        Compra vencida = new Compra(0, app.reloj().ahora().sumarMinutos(-10), cliente, sede,
                new SeleccionAsientos(), new CalculadoraImporte(), new GestorPuntos(), app.reloj());
        System.out.println("Error esperado: "
                + app.compra().agregarEntrada(vencida, funcion4, sala2.obtenerMapaButacas().get(0), general).mensaje());
        System.out.println("Estado de la compra vencida: " + vencida.getEstado());

        // 10 compras confirmadas en el anio suben al nivel 2
        Cliente frecuente = app.login().registrarCliente("33334444", "Rosa", "Diaz", new DateTime(10, 10, 1990),
                "rosa@mail.com", null, "clave000", "clave000").dato();
        Compra ultimaFrecuente = null;
        for (int i = 0; i < 10; i++) {
            Compra cv = app.compra().iniciarCompra(frecuente, sede).dato();
            ultimaFrecuente = cv;
            app.compra().agregarEntrada(cv, funcion4, sala2.obtenerMapaButacas().get(i), general);
            app.compra().confirmarAsientos(cv);
            app.pago().pagar(cv, cv.getTotalCentavos(), TransaccionPago.PAGO_QR);
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
        Promocion pHoy = app.admin().registrarPromocion(admin, "Promo hoy", "20% solo hoy", CalculoPorcentaje.TIPO, 20,
                new DateTime(1, 1, 2026), new DateTime(31, 12, 2030), 0, soloHoy).dato();
        Promocion pOtro = app.admin().registrarPromocion(admin, "Promo otro dia", "20% otro dia", CalculoPorcentaje.TIPO, 20,
                new DateTime(1, 1, 2026), new DateTime(31, 12, 2030), 0, otroDia).dato();
        System.out.println("Promociones vigentes hoy: " + app.cartelera().consultarPromociones().dato());
        Compra cp = app.compra().iniciarCompra(cliente, sede).dato();
        System.out.println("Hoy es dia " + diaHoy
                + " | promo de hoy aplicable: " + app.compra().aplicarPromocion(cp, pHoy).ok()
                + " | promo de otro dia aplicable: " + app.compra().aplicarPromocion(cp, pOtro).ok());
        app.compra().cancelar(cp);

        System.out.println("Calificacion registrada: " + resultado(app.postventa().calificar(ultimaFrecuente.getCliente(), ultimaFrecuente, 5, "Excelente atencion"))
                + " | repetida: " + resultado(app.postventa().calificar(ultimaFrecuente.getCliente(), ultimaFrecuente, 4, "Otra vez"))
                + " | compra reembolsada: " + resultado(app.postventa().calificar(compra.getCliente(), compra, 5, null)));
        Compra compraSinCalificar = c3;
        System.out.println("Calificacion con puntuacion 9 - error esperado: "
                + app.postventa().calificar(compraSinCalificar.getCliente(), compraSinCalificar, 9, null).mensaje());

        Cliente ajeno = app.login().ingresarComoInvitado("ajeno@cine.com").dato();
        System.out.println("Reclamo sobre compra ajena - error esperado: "
                + app.postventa().presentarReclamo(ajeno, compra, Reclamo.ATENCION, "No es mia").mensaje());
        Reclamo rec = app.postventa().presentarReclamo(compra.getCliente(), compra, Reclamo.ATENCION, "Demora en la dulceria").dato();
        System.out.println("Responder sin revisar: " + resultado(app.postventa().responderReclamo(admin, rec, "Disculpas")));
        System.out.println("Empleado de dulceria revisa el reclamo - error esperado: "
                + app.postventa().iniciarRevision(empleado, rec).mensaje());
        app.postventa().iniciarRevision(admin, rec);
        app.postventa().responderReclamo(admin, rec, "Se ofrecen disculpas");
        System.out.println("Reclamo: " + rec);
        Compra pendiente = app.compra().iniciarCompra(cliente, sede).dato();
        System.out.println("Reclamo sobre compra en proceso: "
                + resultado(app.postventa().presentarReclamo(cliente, pendiente, Reclamo.SALA_PROYECCION, "Prueba")));
        app.compra().cancelar(pendiente);

        System.out.println("Cliente con DNI de 3 digitos - error esperado: " + app.login().registrarCliente(
                "123", "Ana", "Lopez", new DateTime(1, 1, 2000), "ana.lopez@mail.com", null, "clave123", "clave123").mensaje());
        System.out.println("Correo ya registrado - error esperado: " + app.login().registrarCliente(
                "55556666", "Otro", "Ismael", null, "ismael@mail.com", null, "clave123", "clave123").mensaje());
        // Reglas del wireframe 5 (Crear cuenta)
        System.out.println("Contrasena de 5 caracteres - error esperado: " + app.login().registrarCliente(
                "55557777", "Ana", "Ruiz", null, "ana.ruiz@mail.com", null, "clave", "clave").mensaje());
        System.out.println("Contrasenas que no coinciden - error esperado: " + app.login().registrarCliente(
                "55557777", "Ana", "Ruiz", null, "ana.ruiz@mail.com", null, "clave123", "clave124").mensaje());
        System.out.println("Telefono de 7 digitos - error esperado: " + app.login().registrarCliente(
                "55557777", "Ana", "Ruiz", null, "ana.ruiz@mail.com", "9876543", "clave123", "clave123").mensaje());

        // Cupones con nivel de socio minimo (wireframe 19: «Nivel mín.»); el cliente es socio nivel 1
        app.admin().registrarCupon(admin, "SOCIO1", CalculoPorcentaje.TIPO, 10,
                new DateTime(1, 1, 2026), new DateTime(31, 12, 2030), 5, 1);
        app.admin().registrarCupon(admin, "SOCIO3", CalculoPorcentaje.TIPO, 10,
                new DateTime(1, 1, 2026), new DateTime(31, 12, 2030), 5, 3);
        Compra conCupon = app.compra().iniciarCompra(cliente, sede).dato();
        System.out.println("Socio nivel " + cliente.getNivelSocio() + " usa cupon de nivel 1: "
                + resultado(app.compra().aplicarCupon(conCupon, "SOCIO1")));
        System.out.println("Socio nivel " + cliente.getNivelSocio() + " usa cupon de nivel 3 - error esperado: "
                + app.compra().aplicarCupon(conCupon, "SOCIO3").mensaje());
        app.compra().cancelar(conCupon);
        Cliente invitadoCupon = app.login().ingresarComoInvitado("invitado.cupon@mail.com").dato();
        Compra compraInvitado = app.compra().iniciarCompra(invitadoCupon, sede).dato();
        System.out.println("Invitado usa cupon de nivel 1 - error esperado: "
                + app.compra().aplicarCupon(compraInvitado, "SOCIO1").mensaje());
        app.compra().cancelar(compraInvitado);

        // Stock solo en la sede del administrador y aviso de stock bajo (wireframe 20: «Stock bajo» / «Agotado»)
        Producto bebida = app.admin().registrarProducto(admin, "Bebida mediana", "Gaseosa 22 oz", "Bebidas",
                ConversorDinero.aCentavos("9.00"), false, 0, 20).dato();
        System.out.println("Stock inicial de la bebida en " + sede.getNombre() + ": " + bebida.getStock(sede));
        Producto agua = app.admin().registrarProducto(admin, "Agua sin gas", "Botella 625 ml", "Bebidas",
                ConversorDinero.aCentavos("5.00"), false, 0, 0).dato();
        Compra conBebidas = app.compra().iniciarCompra(cliente, sede).dato();
        Funcion enVenta = app.admin().programarFuncion(admin, peli, sala, fechaFuncion, new DateTime(12, 0), "Doblada").dato();
        app.compra().agregarEntrada(conBebidas, enVenta, enVenta.obtenerButacasDisponibles().get(0), general);
        app.compra().agregarProducto(conBebidas, bebida, 6, null);
        app.compra().confirmarAsientos(conBebidas);
        System.out.println("Venta con 6 bebidas: "
                + resultado(app.pago().pagar(conBebidas, conBebidas.getTotalCentavos(), TransaccionPago.YAPE)));
        System.out.println("Tras vender 6 bebidas quedan " + bebida.getStock(sede) + " | stock bajo: "
                + bebida.tieneStockBajo(sede) + " | agua agotada: " + agua.estaAgotado(sede));
        Respuesta<List<Producto>> alerta = app.admin().listarProductosConStockBajo(admin);
        System.out.println("Alerta del administrador: " + alerta.mensaje() + " -> " + alerta.dato());
        System.out.println("Panel de dulceria: " + app.dulceria().consultarStockBajo(empleado).dato());
        System.out.println(app.admin().reponerStock(admin, bebida, 1).mensaje());
        System.out.println(app.admin().reponerStock(admin, bebida, 10).mensaje());
        Sede otraSede = new Sede(2, "Cineplanet Norte", "Av. Mendiola 1400", "Independencia", null, null, Sede.ACTIVA);
        Empleado adminNorte = new Empleado(9, "99887766", "Luis", "Paz", new DateTime(1, 1, 1985),
                "luis.paz@cine.com", null, "admin456", "Tarde", rolAdmin, otraSede, app.reloj());
        System.out.println(app.admin().reponerStock(adminNorte, agua, 30).mensaje() + " (admin de " + otraSede.getNombre() + ")");
        System.out.println("Agua: en " + otraSede.getNombre() + " hay " + agua.getStock(otraSede)
                + " y en " + sede.getNombre() + " sigue en " + agua.getStock(sede));
        System.out.println("Alerta del admin de " + otraSede.getNombre() + " incluye el agua: "
                + app.admin().listarProductosConStockBajo(adminNorte).dato().contains(agua)
                + " | alerta del admin de " + sede.getNombre() + " la incluye: "
                + app.admin().listarProductosConStockBajo(admin).dato().contains(agua));

        // Recuperar contrasena con un codigo enviado al correo (wireframe 4: «¿Olvidaste tu contraseña?»)
        titulo("Recuperar contrasena");
        int correosAntes = ((CorreoSimulado) app.correo()).getEnviados().size();
        System.out.println("Correo que no existe: " + app.login().solicitarRecuperacion("nadie@mail.com").mensaje()
                + " | correos enviados: " + (((CorreoSimulado) app.correo()).getEnviados().size() - correosAntes));
        System.out.println("Correo de un invitado: " + app.login().solicitarRecuperacion("invitado.cupon@mail.com").mensaje()
                + " | correos enviados: " + (((CorreoSimulado) app.correo()).getEnviados().size() - correosAntes));
        System.out.println("Socio: " + app.login().solicitarRecuperacion("ismael@mail.com").mensaje()
                + " -> " + ultimoCorreo(app));
        String codigo = codigoDelUltimoCorreo(app);
        String malo = codigo.equals("000000") ? "111111" : "000000";
        System.out.println("Codigo equivocado - error esperado: "
                + app.login().restablecerContrasena("ismael@mail.com", malo, "nueva1234", "nueva1234").mensaje());
        System.out.println("Contrasenas distintas - error esperado: "
                + app.login().restablecerContrasena("ismael@mail.com", codigo, "nueva1234", "nueva9999").mensaje());
        System.out.println("Contrasena corta - error esperado: "
                + app.login().restablecerContrasena("ismael@mail.com", codigo, "corta", "corta").mensaje());
        System.out.println("Codigo correcto: "
                + app.login().restablecerContrasena("ISMAEL@mail.com", codigo, "nueva1234", "nueva1234").mensaje());
        System.out.println("Reusar el codigo - error esperado: "
                + app.login().restablecerContrasena("ismael@mail.com", codigo, "otra12345", "otra12345").mensaje());
        System.out.println("Login con la contrasena anterior - error esperado: "
                + app.login().iniciarSesion("ismael@mail.com", "clave123").mensaje());
        System.out.println("Login con la nueva: " + resultado(app.login().iniciarSesion("ismael@mail.com", "nueva1234")));
        app.login().solicitarRecuperacion("ismael@mail.com");
        Respuesta<Void> intento = null;
        for (int i = 0; i < AutenticacionService.MAX_INTENTOS_CODIGO; i++) {
            intento = app.login().restablecerContrasena("ismael@mail.com", "abc", "nueva5678", "nueva5678");
        }
        System.out.println("5 codigos equivocados - error esperado: " + intento.mensaje());
        System.out.println("El codigo real ya no sirve - error esperado: " + app.login().restablecerContrasena(
                "ismael@mail.com", codigoDelUltimoCorreo(app), "nueva5678", "nueva5678").mensaje());
        // Vencimiento: un servicio aparte con un reloj que se puede adelantar
        DateTime[] hora = {new DateTime(18, 11, 2026, 10, 0, 0)};
        CorreoSimulado buzon = new CorreoSimulado();
        ClienteMemoriaRepository soloClientes = new ClienteMemoriaRepository();
        AutenticacionService auth = new AutenticacionService(soloClientes, new EmpleadoMemoriaRepository(), buzon,
                () -> hora[0]);
        auth.registrarCliente("44445555", "Eva", "Luna", null, "eva@mail.com", null, "clave123", "clave123");
        auth.solicitarRecuperacion("eva@mail.com");
        String codigoEva = buzon.getEnviados().get(0).cuerpo().replaceAll("(?s).*?(\\d{6}).*", "$1");
        hora[0] = hora[0].sumarMinutos(AutenticacionService.MINUTOS_VIGENCIA_CODIGO + 1);
        try {
            auth.restablecerContrasena("eva@mail.com", codigoEva, "nueva1234", "nueva1234");
            System.out.println("Codigo vencido: FALLA, se acepto");
        } catch (IllegalStateException e) {
            System.out.println("Codigo vencido a los 11 minutos - error esperado: " + e.getMessage());
        }

        System.out.println("Correo enviado al cliente: " + ultimoCorreo(app));

        // ---------- 9. Sala con pasillos, recomendacion y volver a elegir butacas ----------
        titulo("9. Sala con pasillos (4 | 10 | 4), recomendacion y volver a elegir");
        TipoSala tipo3d = new TipoSala(2, "3D", "Sala 3D", ConversorDinero.aCentavos("5.00"));
        Sala grande = app.admin().registrarSala(admin, sede, 3, tipo3d, 9, 4, 10).dato();
        System.out.println(grande + " | capacidad: " + grande.getCapacidad() + " | filas: " + grande.obtenerFilas());
        System.out.println("Fila A: " + filaComoTexto(grande, "A"));
        System.out.println("Fila G: " + filaComoTexto(grande, "G"));
        Funcion funcion5 = app.admin().programarFuncion(admin, peli, grande, fechaFuncion, new DateTime(16, 0), "Subtitulada").dato();

        Compra cg = app.compra().iniciarCompra(cliente, sede).dato();
        System.out.println("Sugerencia para 2 en sala vacia: " + app.compra().consultarMapa(cg, funcion5, 2, false).dato().sugeridas());
        System.out.println("Sugerencia para 4: " + app.compra().consultarMapa(cg, funcion5, 4, false).dato().sugeridas());
        System.out.println("Sugerencia para 12 (no caben en el bloque central): "
                + app.compra().consultarMapa(cg, funcion5, 12, false).dato().sugeridas());
        System.out.println("Sugerencia para 2 con butacas accesibles: "
                + app.compra().consultarMapa(cg, funcion5, 2, true).dato().sugeridas());
        System.out.println("Pedir 500: " + app.compra().consultarMapa(cg, funcion5, 500, false).mensaje());

        for (Butaca b : app.compra().consultarMapa(cg, funcion5, 2, false).dato().sugeridas()) {
            app.compra().agregarEntrada(cg, funcion5, b, general);
        }
        System.out.println("Estado al elegir: " + cg.getEntradas().get(0).getEstado());
        app.compra().confirmarAsientos(cg);
        System.out.println("Estado al confirmar: " + cg.getEntradas().get(0).getEstado()
                + " | libres: " + funcion5.consultarDisponibilidad());
        System.out.println("Quitar sin liberar - error esperado: "
                + app.compra().quitarEntrada(cg, cg.getEntradas().get(0)).mensaje());
        System.out.println(app.compra().liberarAsientos(cg).mensaje() + " | estado: " + cg.getEntradas().get(0).getEstado()
                + " | libres: " + funcion5.consultarDisponibilidad());
        System.out.println("Nueva sugerencia (sin repetir las ya elegidas): "
                + app.compra().consultarMapa(cg, funcion5, 2, false).dato().sugeridas());
        app.compra().cancelar(cg);

        // ---------- 10. Mis compras: Ver, imprimir, enviar al correo y recuperar ----------
        titulo("10. Mis compras: Ver, imprimir, correo y recuperar entradas");
        System.out.println(app.emision().verDetalle(ultimaFrecuente).dato());
        System.out.println("Ver una compra reembolsada (sin QR):");
        System.out.println(app.emision().verDetalle(compra).dato());
        System.out.println(app.emision().enviarPorCorreo(ultimaFrecuente, null).mensaje() + " -> " + ultimoCorreo(app));
        String serie = ultimaFrecuente.getSerieNumero();
        String correoComprador = ultimaFrecuente.getCliente().getCorreo();
        System.out.println("Recuperar con el codigo " + serie + " y su correo: "
                + resultado(app.emision().recuperarPorCodigo(serie, correoComprador)));
        System.out.println("Recuperar con el codigo y un correo ajeno - error esperado: "
                + app.emision().recuperarPorCodigo(serie, "otro@mail.com").mensaje());
        System.out.println("Recuperar por correo: " + app.emision().recuperarPorCorreo("rosa@mail.com").mensaje());
        System.out.println("Recuperar por correo sin compras vigentes: " + app.emision().recuperarPorCorreo("ismael@mail.com").mensaje());
        System.out.println("Imprimir: " + resultado(app.emision().imprimir(ultimaFrecuente)));
        System.out.println("Codigo inexistente - mismo mensaje: " + resultado(app.emision().recuperarPorCodigo("B001-999999", correoComprador)));
        // Ya van 2 fallos; 3 series probadas al azar completan los 5 y el kiosco se bloquea.
        for (int i = 1; i <= 3; i++) {
            app.emision().recuperarPorCodigo(String.format("B001-%06d", 900000 + i), correoComprador);
        }
        System.out.println("Tras 5 fallos, incluso con datos correctos - error esperado: "
                + app.emision().recuperarPorCodigo(serie, correoComprador).mensaje());

        // ---------- 11. Administracion: sedes, salas, productos, cupones y funciones ----------
        titulo("11. Administracion");
        Sede nueva = app.admin().registrarSede(admin, "Cineplanet Norte", "Av. Tupac Amaru 500", "Independencia", null, "10:00 - 23:00").dato();
        app.admin().actualizarSede(admin, nueva, "Cineplanet Norte Plaza", "Av. Tupac Amaru 500", "Independencia", "01-555-0000", "10:00 - 23:00");
        System.out.println("Sedes: " + app.admin().listarSedes(admin).dato());
        System.out.println("Sala con numero repetido: " + resultado(app.admin().registrarSala(admin, sede, 3, tipo, 5, 2, 6)));
        System.out.println("Redistribuir sala con funciones: " + resultado(app.admin().redistribuirSala(admin, grande, 10, 4, 12)));
        System.out.println("Dar de baja sala con funciones pendientes: " + resultado(app.admin().darDeBajaSala(admin, grande)));
        System.out.println("Editar combo: " + resultado(app.admin().actualizarProducto(admin, combo, "Combo Mediano",
                "Canchita salada y gaseosa", "Combos", ConversorDinero.aCentavos("27.50"), true, 0)) + " | " + combo);
        System.out.println("Bajar limite del cupon por debajo de sus usos: " + resultado(app.admin().actualizarCupon(admin, cupon,
                "CINE10", CalculoPorcentaje.TIPO, 10, new DateTime(1, 1, 2026), new DateTime(31, 12, 2030), 0, 0)));
        System.out.println("Editar funcion con entradas vendidas: " + resultado(app.admin().editarFuncion(admin, funcion4,
                fechaFuncion, new DateTime(11, 0), "Doblada")));
        System.out.println("Reactivar funcion 3: " + resultado(app.admin().reactivarFuncion(admin, funcion3))
                + " | estado: " + funcion3.getEstado());
        System.out.println("Retirar pelicula con funciones pendientes: " + resultado(app.admin().retirarPelicula(admin, peli)));

        // ---------- 12. Tolerancia de 20 minutos despues del inicio ----------
        titulo("12. Venta hasta " + Funcion.TOLERANCIA_VENTA_MIN + " minutos despues de iniciada la funcion");
        DateTime haceDiez = app.reloj().ahora().sumarMinutos(-10);
        DateTime haceVeinticinco = app.reloj().ahora().sumarMinutos(-25);
        Sala sala4 = app.admin().registrarSala(admin, sede, 4, tipo, 3, 2, 4).dato();
        Sala sala5 = app.admin().registrarSala(admin, sede, 5, tipo, 3, 2, 4).dato();
        Funcion empezoHace10 = app.admin().programarFuncion(admin, peli, sala4,
                new DateTime(haceDiez.getDia(), haceDiez.getMes(), haceDiez.getAnio()),
                new DateTime(haceDiez.getHora(), haceDiez.getMinuto()), "Doblada").dato();
        Funcion empezoHace25 = app.admin().programarFuncion(admin, peli, sala5,
                new DateTime(haceVeinticinco.getDia(), haceVeinticinco.getMes(), haceVeinticinco.getAnio()),
                new DateTime(haceVeinticinco.getHora(), haceVeinticinco.getMinuto()), "Doblada").dato();
        List<Funcion> enCartelera = app.cartelera().consultarCartelera(sede).dato();
        System.out.println("Empezo hace 10 min: " + empezoHace10.getEstado() + " | cierre de venta: "
                + empezoHace10.obtenerCierreVenta().soloHora() + " | en cartelera: " + enCartelera.contains(empezoHace10));
        System.out.println("Empezo hace 25 min: " + empezoHace25.getEstado() + " | cierre de venta: "
                + empezoHace25.obtenerCierreVenta().soloHora() + " | en cartelera: " + enCartelera.contains(empezoHace25));

        Compra tarde = app.compra().iniciarCompra(cliente, sede).dato();
        app.compra().agregarEntrada(tarde, empezoHace10, sala4.obtenerMapaButacas().get(0), general);
        System.out.println("Comprar la que empezo hace 10 min: " + resultado(app.compra().confirmarAsientos(tarde)));
        System.out.println("Pago: " + resultado(app.pago().pagar(tarde, tarde.getTotalCentavos(), TransaccionPago.YAPE)));

        Compra muyTarde = app.compra().iniciarCompra(cliente, sede).dato();
        app.compra().agregarEntrada(muyTarde, empezoHace25, sala5.obtenerMapaButacas().get(0), general);
        System.out.println("Comprar la que empezo hace 25 min - error esperado: "
                + app.compra().confirmarAsientos(muyTarde).mensaje());
        app.compra().cancelar(muyTarde);

        titulo("Resumen del cliente");
        System.out.println(cliente + " | puntos: " + cliente.consultarPuntos()
                + " | compras en historial: " + app.postventa().consultarHistorial(cliente).dato().size());
    }
}
