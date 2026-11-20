# Sistema de autoservicio de cine

Modelo de dominio desarrollado en Java para representar las operaciones principales de un sistema de autoservicio de cine. Incluye entidades y reglas de negocio para la cartelera, la reserva de butacas, la venta de entradas, las compras y los pedidos de dulcería. 

## Funcionalidades modeladas

- Películas, géneros, sedes, salas y funciones.
- Butacas y disponibilidad por función.
- Entradas con estados y bloqueo temporal de asientos.
- Compras con cálculo de subtotal, descuentos, IGV y total.
- Cupones, promociones y socios.
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
mvn clean javafx:run   # interfaz gráfica del kiosco (necesita XAMPP/MySQL con DB_Cine)
```

En NetBeans, el botón «Run» hace lo mismo (`nbactions.xml`).

La entrada es `ucsur.pe.App`, como en el ejemplo MVCone: abre la conexión, conecta repositorios, servicios y
controllers, y muestra la primera pantalla. Cada pantalla es un FXML en `src/main/resources/ucsur/pe/view/` con
su controller de pantalla en `ucsur.pe.vista`, que solo llama a los controllers de `App`. Los estilos están en
`css/estilos.css` (colores y fuente Manrope del Figma). Los pósters se buscan en
`src/main/resources/ucsur/pe/posters/` con el nombre que guarda la BD; si la imagen no está, se dibuja un póster
con el título.

## Estructura del proyecto

```text
.
├── README.md
└── cineObjects/
    ├── pom.xml
    └── src/main/java/ucsur/pe/
        ├── App.java       Entrada JavaFX: abre la conexión y conecta repositorios, servicios y controllers
        ├── vista/         Controllers de pantalla (los @FXML de cada archivo de view/) y el Navegador
        ├── controller/    Recibe las acciones de la vista y llama a los servicios (Respuesta: ok, mensaje, dato)
        ├── service/       Casos de uso: autenticación, cartelera, compra, pago, dulcería, administración, postventa
        ├── repository/    Contratos de persistencia y su versión JDBC contra DB_Cine
        ├── db/            Conexión (DBConnection) y DAO con las consultas SQL
        ├── model/         Entidades y reglas del negocio (Compra, Funcion, Pedido...), DateTime y Reloj
        ├── tipo/          Formas de calcular un descuento (porcentaje, monto fijo) y su fábrica
        ├── pasarela/      Pasarela de pagos (interfaz y versión simulada)
        ├── correo/        Proveedor de correo (interfaz y versión simulada)
        └── exception/     ReglaNegocioException y RepositoryException
```

## Estado actual

El proyecto está organizado por capas (vista, controller, service, repository, model) con repositorios en
MySQL (el script SQL se mantiene aparte). La interfaz gráfica tiene las cuatro fases de los wireframes: el flujo de
compra del cliente (sede, cartelera, horario, identificación, asientos, dulcería, pago y comprobante) y la cuenta
y postventa (promociones, recuperar entradas, mi cuenta, mis compras, calificar y reclamos), el panel de
dulcería para el personal (cola de pedidos y entrega con código) y la administración (películas, funciones,
salas, promociones y cupones, productos, sedes y reclamos).

## Contexto académico

Proyecto académico de la Universidad Científica del Sur (UCSUR).
