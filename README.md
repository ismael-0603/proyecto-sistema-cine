# proyecto-sistema-cine

Modelo de dominio desarrollado en Java para representar las operaciones principales de un sistema de autoservicio de cine. El proyecto reúne entidades y reglas de negocio relacionadas con la cartelera, la reserva de butacas, la venta de entradas, las compras y los pedidos de dulcería.

## Funcionalidades modeladas

- Películas, géneros, sedes, salas y funciones.
- Butacas y disponibilidad por función.
- Entradas con estados y bloqueo temporal de asientos.
- Compras con cálculo de subtotal, descuentos, IGV y total.
- Cupones y promociones.
- Pedidos de productos de dulcería e inventario.
- Clientes, puntos y niveles de socio.
- Transacciones de pago y reglas de reembolso.
- Reclamos y calificaciones.

## Tecnologías

- Java
- Maven

## Estructura

```text
src/main/java/ucsur/pe/cineobjects/
├── Entidades del dominio
└── cineInterfaz/
    └── Interfaces del modelo
