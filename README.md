# Sistema de autoservicio de cine

Modelo de dominio desarrollado en Java para representar las operaciones principales de un sistema de autoservicio de cine. Incluye entidades y reglas de negocio para la cartelera, la reserva de butacas, la venta de entradas, las compras y los pedidos de dulcería.

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

- Java 26
- Maven

## Requisitos

- JDK 26 o compatible con la versión configurada en `cineObjects/pom.xml`.
- Apache Maven.

## Compilar y ejecutar

Desde la raíz del repositorio: 

```bash
cd cineObjects
mvn clean package
mvn exec:java
```

La clase de entrada configurada es `ucsur.pe.cineobjects.CineObjects`.

## Estructura del proyecto

```text
.
├── README.md
└── cineObjects/
    ├── pom.xml
    └── src/main/java/ucsur/pe/cineobjects/
        ├── Clases del dominio (películas, funciones, compras, entradas, pedidos, etc.)
        └── cineInterfaz/
            └── Interfaces del modelo
```

## Estado actual

El repositorio implementa principalmente el modelo de dominio y sus reglas de negocio. La clase principal `CineObjects` todavía es un punto de entrada básico que imprime `Hello World!`; aún no incluye una interfaz de usuario ni integración con una base de datos.

## Contexto académico

Proyecto académico de la Universidad Científica del Sur (UCSUR).
