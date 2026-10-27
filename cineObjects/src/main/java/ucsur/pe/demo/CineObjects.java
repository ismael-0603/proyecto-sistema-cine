package ucsur.pe.demo;

import java.util.ArrayList;
import java.util.List;
import ucsur.pe.cineobjects.Butaca;
import ucsur.pe.cineobjects.CalculoPorcentaje;
import ucsur.pe.cineobjects.Calificacion;
import ucsur.pe.cineobjects.CatalogoCupones;
import ucsur.pe.cineobjects.Cliente;
import ucsur.pe.cineobjects.Compra;
import ucsur.pe.cineobjects.Cupon;
import ucsur.pe.cineobjects.DateTime;
import ucsur.pe.cineobjects.DetallePedido;
import ucsur.pe.cineobjects.Empleado;
import ucsur.pe.cineobjects.Entrada;
import ucsur.pe.cineobjects.Funcion;
import ucsur.pe.cineobjects.Genero;
import ucsur.pe.cineobjects.Pedido;
import ucsur.pe.cineobjects.Pelicula;
import ucsur.pe.cineobjects.Producto;
import ucsur.pe.cineobjects.Promocion;
import ucsur.pe.cineobjects.Reclamo;
import ucsur.pe.cineobjects.Rol;
import ucsur.pe.cineobjects.Sala;
import ucsur.pe.cineobjects.Sede;
import ucsur.pe.cineobjects.TipoEntrada;
import ucsur.pe.cineobjects.TipoSala;
import ucsur.pe.cineobjects.TransaccionPago;

/**
 * Por qué existe: programa de prueba que recorre el flujo completo del cine (cartelera, compra,
 * dulcería, pago, puntos y reclamos) e imprime los resultados.
 */
public class CineObjects {

    /** Imprime un encabezado para separar visualmente cada ejemplo. */
    private static void titulo(String texto) {
        System.out.println("\n=== " + texto + " ===");
    }

    /** Ejecuta ejemplos de cartelera, compras, asientos, pedidos, pagos y beneficios. */
    public static void main(String[] args) {

        // ---------- 1. Infraestructura y cartelera ----------
        titulo("1. Infraestructura y cartelera");
        Sede sede = new Sede(1, "Cineplanet Prueba", "Av. Lima 123", "Miraflores", null, null, Sede.ACTIVA);
        TipoSala tipo = new TipoSala(1, "2D", "Sala estandar", 0);
        Sala sala = new Sala(1, 1, 10, Sala.ACTIVA, sede, tipo);
        sede.agregarSala(sala);
        for (int i = 1; i <= 5; i++) {
            sala.agregarButaca(new Butaca(0, "A", i, Butaca.ESTANDAR, sala));
        }

        Genero accion = new Genero(1, "Accion");
        Pelicula peli = new Pelicula(1, "Pelicula de prueba", "Sinopsis", 120, "+14",
                null, null, new DateTime(1, 1, 2026), Pelicula.EN_CARTELERA);
        peli.agregarGenero(accion);

        // Funciones de manana, para que no hayan empezado
        DateTime manana = DateTime.ahora().sumarDias(1);
        DateTime fechaFuncion = new DateTime(manana.getDia(), manana.getMes(), manana.getAnio());

        Funcion funcion = new Funcion(1, fechaFuncion, new DateTime(19, 0), "Doblada", null, peli, sala);
        sede.agregarFuncion(funcion);
        System.out.println("Funcion: " + funcion + " | hora fin: " + funcion.calcularHoraFin().soloHora());

        // Solapamiento: empieza a las 20:00 y la anterior termina a las 21:00 (+15 min de limpieza)
        try {
            sede.agregarFuncion(new Funcion(2, fechaFuncion, new DateTime(20, 0), "Doblada", null, peli, sala));
            System.out.println("ERROR: debio detectar el solapamiento");
        } catch (IllegalStateException ex) {
            System.out.println("Solapamiento detectado: " + ex.getMessage());
        }

        // Esta si cabe: empieza a las 21:30, despues de la limpieza (21:15)
        Funcion funcion3 = new Funcion(3, fechaFuncion, new DateTime(21, 30), "Subtitulada", null, peli, sala);
        sede.agregarFuncion(funcion3);
        System.out.println("Funciones en cartelera: " + sede.consultarCartelera().size());
        System.out.println("Filtro idioma Doblada + genero Accion: "
                + sede.filtrarCartelera("Doblada", null, accion).size());

        // ---------- 2. Compra basica ----------
        titulo("2. Compra basica");
        Cliente cliente = new Cliente(1, "12345678", "Ismael", "Chipana",
                new DateTime(1, 1, 2000), "ismael@mail.com", null, "clave123");
        TipoEntrada general = new TipoEntrada(1, "GENERAL", "Entrada general", 20.0, 2, false, true);

        Compra compra = cliente.realizarCompra(sede);
        System.out.println("Asientos recomendados: " + funcion.recomendarMejoresAsientos(2));
        for (Butaca b : funcion.recomendarMejoresAsientos(2)) {
            compra.agregarEntrada(new Entrada(0, funcion, b, general));
        }
        System.out.println("Asientos bloqueados: " + compra.confirmarSeleccionAsientos());

        TransaccionPago pago = new TransaccionPago(0, compra.getTotal(), TransaccionPago.TARJETA, compra);
        System.out.println("Pago aprobado: " + pago.procesarPago());
        compra.asignarSerie("B001-000001"); // simula la serie que asigna la base al confirmar
        System.out.println(compra.generarComprobante());
        System.out.println("Puntos del cliente: " + cliente.consultarPuntos());
        System.out.println("Butacas libres en funcion 1: " + funcion.consultarDisponibilidad());

        // ---------- 3. Conflicto de asiento ----------
        titulo("3. Dos clientes eligen la misma butaca (A5)");
        Cliente otro = Cliente.ingresarComoInvitado("otro@mail.com");
        Compra c1 = cliente.realizarCompra(sede);
        Compra c2 = otro.realizarCompra(sede);
        Butaca a4 = sala.obtenerMapaButacas().get(3);
        Butaca a5 = sala.obtenerMapaButacas().get(4);

        c1.agregarEntrada(new Entrada(0, funcion, a5, general));
        Entrada entradaC2 = new Entrada(0, funcion, a5, general);
        c2.agregarEntrada(entradaC2);            // todavia esta libre, se acepta
        c1.confirmarSeleccionAsientos();          // c1 bloquea A5

        try {
            c2.confirmarSeleccionAsientos();      // debe fallar
            System.out.println("ERROR: debio fallar");
        } catch (IllegalStateException ex) {
            System.out.println("Error esperado: " + ex.getMessage());
        }
        c2.quitarEntrada(entradaC2);
        c2.agregarEntrada(new Entrada(0, funcion, a4, general));
        System.out.println("Reintento con A4: " + c2.confirmarSeleccionAsientos());

        c1.cancelar();
        c2.cancelar();
        System.out.println("Estado c1: " + c1.getEstado() + " | estado c2: " + c2.getEstado());
        System.out.println("Butacas libres tras cancelar: " + funcion.consultarDisponibilidad());

        // ---------- 4. Dulceria, cupon y pedido ----------
        titulo("4. Entrada + combo + cupon + pedido");
        Producto combo = new Producto(1, "Combo Mediano", "Canchita y gaseosa", "Combos", 25.0, true, true);
        combo.reponerStock(sede, 10);
        Cupon cupon = new Cupon(1, "CINE10", new CalculoPorcentaje(10),
                new DateTime(1, 1, 2026), new DateTime(31, 12, 2030), 5, true);
        CatalogoCupones catalogo = new CatalogoCupones();
        catalogo.registrar(cupon);

        Rol rolDulceria = new Rol(1, "Personal de dulceria", "Prepara y entrega pedidos", Rol.NIVEL_DULCERIA);
        Empleado empleado = new Empleado(1, "87654321", "Ana", "Quispe", new DateTime(5, 5, 1999),
                "ana@mail.com", null, "clave456", "Tarde", rolDulceria, sede);

        Compra c3 = cliente.realizarCompra(sede);
        c3.agregarEntrada(new Entrada(0, funcion3, sala.obtenerMapaButacas().get(0), general)); // A1
        Pedido pedido = new Pedido(0);
        pedido.agregarDetalle(new DetallePedido(0, combo, 2, "Sin sal"));
        c3.asignarPedido(pedido);
        System.out.println("Cupon aplicado: " + c3.aplicarCupon("CINE10", catalogo));
        System.out.println("Asientos bloqueados: " + c3.confirmarSeleccionAsientos());

        TransaccionPago pago3 = new TransaccionPago(0, c3.getTotal(), TransaccionPago.QR, c3);
        System.out.println("Pago aprobado: " + pago3.procesarPago());
        c3.asignarSerie("B001-000002"); // simula la serie que asigna la base
        System.out.println(c3.generarComprobante());
        System.out.println("Stock del combo: " + combo.getStock(sede));
        System.out.println("Puntos del cliente: " + cliente.consultarPuntos());

        empleado.prepararPedido(pedido);
        System.out.println(pedido);
        pedido.marcarListo();
        System.out.println("Codigo QR valido: " + pedido.validarCodigo(pedido.getCodigoQR()));
        empleado.entregarPedido(pedido);
        System.out.println(pedido);

        // ---------- 5. Pago rechazado y reembolso ----------
        titulo("5. Pago rechazado y reembolso");
        Compra c5 = cliente.realizarCompra(sede);
        c5.agregarEntrada(new Entrada(0, funcion3, sala.obtenerMapaButacas().get(2), general)); // A3
        c5.confirmarSeleccionAsientos();
        TransaccionPago pago5 = new TransaccionPago(0, 1.0, TransaccionPago.TARJETA, c5);
        System.out.println("Pago con monto incorrecto: " + pago5.procesarPago()
                + " | motivo: " + pago5.getMotivoRechazo());
        c5.cancelar();
        System.out.println("Estado c5: " + c5.getEstado());

        Compra c4 = cliente.realizarCompra(sede);
        c4.agregarEntrada(new Entrada(0, funcion3, sala.obtenerMapaButacas().get(1), general)); // A2
        c4.confirmarSeleccionAsientos();
        TransaccionPago pago4 = new TransaccionPago(0, c4.getTotal(), TransaccionPago.TARJETA, c4);
        System.out.println("Pago c4 aprobado: " + pago4.procesarPago());
        System.out.println("Puntos antes del reembolso: " + cliente.consultarPuntos());
        System.out.println("Reembolso con funcion activa: " + pago4.reembolsar());
        funcion3.setEstado(Funcion.CANCELADA);
        System.out.println("Reembolso con funcion cancelada: " + pago4.reembolsar());
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
        comboNivel2.reponerStock(sede, 5);
        Producto comboSocio = new Producto(3, "Combo Socio", "Oferta para socios", "Combos", 25.0, true, 1, true);
        comboSocio.reponerStock(sede, 5);
        System.out.println("Tarifa normal: " + normal.getTarifa());
        System.out.println("Tarifa socio (mitad): " + socio50.getTarifa());
        System.out.println("Puntos que cuesta: " + socio50.getPuntosCanje());

        Compra c6 = cliente.realizarCompra(sede);
        c6.agregarEntrada(new Entrada(0, funcion, sala.obtenerMapaButacas().get(0), socio50)); // A1

        // Un combo de nivel 2 no esta disponible para un cliente de nivel 1
        Pedido pedidoFallido = new Pedido(0);
        pedidoFallido.agregarDetalle(new DetallePedido(0, comboNivel2, 1, null));
        try {
            c6.asignarPedido(pedidoFallido);
            System.out.println("ERROR: debio rechazar el combo de nivel 2");
        } catch (IllegalStateException ex) {
            System.out.println("Error esperado: " + ex.getMessage());
        }

        Pedido pedido6 = new Pedido(0);
        pedido6.agregarDetalle(new DetallePedido(0, comboSocio, 1, null));
        c6.asignarPedido(pedido6);
        System.out.println("Asientos bloqueados: " + c6.confirmarSeleccionAsientos());

        TransaccionPago pago6 = new TransaccionPago(0, c6.getTotal(), TransaccionPago.TARJETA, c6);
        System.out.println("Pago aprobado: " + pago6.procesarPago());
        c6.asignarSerie("B001-000003"); // simula la serie que asigna la base
        System.out.println(c6.generarComprobante());
        System.out.println("Puntos del cliente: " + cliente.consultarPuntos());
        System.out.println("Stock del Combo Socio: " + comboSocio.getStock(sede));

        // Un invitado no puede usar la entrada de socio
        Compra c7 = otro.realizarCompra(sede);
        c7.agregarEntrada(new Entrada(0, funcion, sala.obtenerMapaButacas().get(3), socio50)); // A4
        try {
            c7.validarBeneficiosSocio();
            System.out.println("ERROR: debio rechazar al invitado");
        } catch (IllegalStateException ex) {
            System.out.println("Invitado - error esperado: " + ex.getMessage());
        }
        c7.cancelar();

        // Un socio sin puntos suficientes tampoco
        Cliente nuevo = new Cliente(2, "11112222", "Luis", "Perez", new DateTime(1, 1, 1995),
                "luis@mail.com", null, "clave789");
        Compra c8 = nuevo.realizarCompra(sede);
        c8.agregarEntrada(new Entrada(0, funcion, sala.obtenerMapaButacas().get(3), socio50)); // A4
        try {
            c8.validarBeneficiosSocio();
            System.out.println("ERROR: debio rechazar por puntos");
        } catch (IllegalStateException ex) {
            System.out.println("Socio sin puntos - error esperado: " + ex.getMessage());
        }
        c8.cancelar();

        // Reembolso: la funcion se cancela y los puntos vuelven a su saldo
        funcion.setEstado(Funcion.CANCELADA);
        System.out.println("Reembolso c6: " + pago6.reembolsar());
        System.out.println("Estado c6: " + c6.getEstado() + " | puntos tras reembolso: " + cliente.consultarPuntos());

        // ---------- 7. Expiracion de los 4 minutos y subida de nivel ----------
        titulo("7. Expiracion de los 4 minutos y subida de nivel");
        Sala sala2 = new Sala(2, 2, 10, Sala.ACTIVA, sede, tipo);
        sede.agregarSala(sala2);
        for (int i = 1; i <= 10; i++) {
            sala2.agregarButaca(new Butaca(0, "B", i, Butaca.ESTANDAR, sala2));
        }
        Funcion funcion4 = new Funcion(4, fechaFuncion, new DateTime(10, 0), "Doblada", null, peli, sala2);
        sede.agregarFuncion(funcion4);

        // Una compra creada hace 10 minutos ya vencio su plazo de 4
        Compra vencida = new Compra(0, DateTime.ahora().sumarMinutos(-10), cliente, sede);
        try {
            vencida.agregarEntrada(new Entrada(0, funcion4, sala2.obtenerMapaButacas().get(0), general));
            System.out.println("ERROR: la compra vencida debio rechazar la entrada");
        } catch (IllegalStateException ex) {
            System.out.println("Error esperado: " + ex.getMessage());
        }
        System.out.println("Estado de la compra vencida: " + vencida.getEstado());

        // 10 compras confirmadas en el anio suben al nivel 2
        Cliente frecuente = new Cliente(3, "33334444", "Rosa", "Diaz", new DateTime(10, 10, 1990),
                "rosa@mail.com", null, "clave000");
        for (int i = 0; i < 10; i++) {
            Compra cv = frecuente.realizarCompra(sede);
            cv.agregarEntrada(new Entrada(0, funcion4, sala2.obtenerMapaButacas().get(i), general));
            cv.confirmarSeleccionAsientos();
            new TransaccionPago(0, cv.getTotal(), TransaccionPago.QR, cv).procesarPago();
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
        Compra cp = cliente.realizarCompra(sede);
        System.out.println("Hoy es dia " + diaHoy
                + " | promo de hoy aplicable: " + cp.aplicarDescuento(pHoy)
                + " | promo de otro dia aplicable: " + cp.aplicarDescuento(pOtro));
        cp.cancelar();

        Calificacion cal = new Calificacion(0, 5, "Excelente atencion", compra);
        System.out.println("Calificacion registrada: " + cal.registrar() + " | repetida: " + cal.registrar());
        try {
            new Calificacion(0, 9, null, compra);
            System.out.println("ERROR: debio rechazar la puntuacion 9");
        } catch (IllegalArgumentException ex) {
            System.out.println("Calificacion con puntuacion 9 - error esperado: " + ex.getMessage());
        }

        Reclamo rec = new Reclamo(0, Reclamo.QUEJA, "Demora en la dulceria", compra);
        rec.registrar();
        rec.iniciarRevision();
        rec.responderReclamo("Se ofrecen disculpas");
        System.out.println("Reclamo: " + rec);
        Compra pendiente = cliente.realizarCompra(sede);
        Reclamo rec2 = new Reclamo(0, Reclamo.RECLAMO, "Prueba", pendiente);
        System.out.println("Reclamo sobre compra en proceso: " + rec2.registrar());
        pendiente.cancelar();

        try {
            new Cliente(9, "123", "Ana", "Lopez", new DateTime(1, 1, 2000), "ana@mail.com", null, "clave");
            System.out.println("ERROR: debio rechazar el DNI");
        } catch (IllegalArgumentException ex) {
            System.out.println("Cliente con DNI de 3 digitos - error esperado: " + ex.getMessage());
        }

        titulo("Resumen del cliente");
        System.out.println(cliente + " | puntos: " + cliente.consultarPuntos());
    }
}
