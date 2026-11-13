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

La clase de entrada configurada en `pom.xml` es `ucsur.pe.demo.CineObjects`, que recorre el flujo completo
(cartelera, compra, dulcería, pago, reembolso, socios y postventa). Los demás `Main*` del paquete `demo`
son pruebas de partes concretas del modelo.

## Estructura del proyecto

```text
.
├── README.md
└── cineObjects/
    ├── pom.xml
    └── src/main/java/ucsur/pe/
        ├── App.java       Crea y conecta repositorios, servicios y controllers
        ├── controller/    Recibe las acciones de la vista y llama a los servicios (Respuesta: ok, mensaje, dato)
        ├── service/       Casos de uso: autenticación, cartelera, compra, pago, dulcería, administración, postventa
        ├── repository/    Contratos de persistencia y su versión en memoria (luego, Jdbc contra BD_Cine)
        ├── model/         Entidades y reglas del negocio (Compra, Funcion, Pedido...), DateTime y Reloj
        ├── tipo/          Formas de calcular un descuento (porcentaje, monto fijo) y su fábrica
        ├── pasarela/      Pasarela de pagos (interfaz y versión simulada)
        ├── exception/     ReglaNegocioException y RepositoryException
        └── demo/          Pruebas por consola (CineObjects y los Main*), que usan los controllers
```

## Estado actual

El proyecto está organizado por capas (controller, service, repository, model). Los repositorios guardan en
memoria y se prueba por consola con las clases del paquete `demo`. Aún no incluye las pantallas (FXML) ni los
repositorios Jdbc contra la base de datos (el script SQL se mantiene aparte).

## Contexto académico

Proyecto académico de la Universidad Científica del Sur (UCSUR).
