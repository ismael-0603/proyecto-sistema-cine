# Conexión MySQL e interfaz JavaFX (siguiendo MVCone) — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que `cineObjects` guarde y lea de la base `DB_Cine` (MySQL) y se use desde pantallas JavaFX/FXML, con la misma estructura que el ejemplo MVCone del profesor.

**Architecture:** Igual que MVCone: `db/DBConnection` (Singleton que lee `db.properties`) → `db/XxxDao` + `db/XxxMySqlDao` (solo SQL ↔ objetos) → `repository/XxxJdbcRepository` (implementa las interfaces que **ya existen**) → servicios (sin cambios) → controllers (sin cambios) → controllers de vista `@FXML` + FXML + CSS. `App` sigue siendo la raíz de composición; lo único que cambia es qué repositorios crea.

**Tech Stack:** Java 26, Maven, JavaFX (controls + fxml), `mysql-connector-j` 8.4.0, MySQL 8 + Workbench.

**Spec:** Este plan parte de la comparación MVCone vs. Proyecto_Cine (conversación del 2026-11-13). Referencia de código: `C:\Users\user\Downloads\MVConeV1\MVCone` (`DBConnection.java`, `ComprobanteMySqlDao.java`, `ComprobanteJdbcRepository.java`, `App.java`, `ComprobanteController.java`, `comprobantes.fxml`, `estilos.css`).

## Global Constraints

- El modelo usa **solo** `ucsur.pe.model.DateTime`; `java.sql.Timestamp/Date/Time` aparecen únicamente dentro de `ucsur.pe.db` (exigencia del profesor).
- Los servicios (`service/*`) y los controllers lógicos (`controller/*`) **no cambian de firma**; la vista los usa y lee `Respuesta<T>`.
- Los repositorios en memoria se **conservan**: los `demo/Main*` siguen funcionando con `new App()`.
- Nombres como en MVCone: `XxxDao` (interfaz), `XxxMySqlDao` (JDBC), `XxxJdbcRepository` (implementa `XxxRepository`).
- Toda `SQLException` se envuelve en `ucsur.pe.exception.RepositoryException` con un mensaje en español (como MVCone).
- SQL solo con `PreparedStatement` y text blocks `"""`; nunca concatenar datos del usuario.
- `siguienteId()` = `SELECT COALESCE(MAX(id_xxx),0)+1` y el INSERT lleva el id explícito (igual que `siguienteNumero` de MVCone; válido porque la app es de un solo usuario).
- La BD se crea **una vez desde Workbench** ejecutando `BD/BD_Cine.sql` y luego `BD/BD_Cine_datos.sql`. `DBConnection` **no** ejecuta el script: `BD_Cine.sql` usa `DELIMITER $$` en los triggers y el `split(";")` de MVCone lo rompería.
- `db.properties` en `src/main/resources/ucsur/pe/db/` con las claves `db.url`, `db.name`, `db.user`, `db.password` (las de MVCone), y en `.gitignore`. Se sube un `db.properties.example`.
- Paquete de los controllers de vista: `ucsur.pe.vista` (porque `ucsur.pe.controller` ya se usa para los controllers lógicos). FXML en `src/main/resources/ucsur/pe/view/`, CSS en `src/main/resources/ucsur/pe/css/estilos.css`.
- Versión 1: una compra solo se consulta dentro de la sesión en que se creó (`CompraJdbcRepository` la guarda en la BD y la mantiene en caché). Recuperar compras de sesiones anteriores queda fuera de esta versión.

## Review Focus

1. **Butaca ya vendida en la BD**: al cargar una función, las butacas con entrada `BLOQUEADA` (no vencida) o `VENDIDA` deben verse ocupadas y no se pueden elegir → prueba en Task 3.
2. **MySQL apagado o contraseña incorrecta**: la app no se cae con una traza; muestra un `Alert` con "No se pudo conectar a la base de datos. Verifique db.properties." → prueba en Task 2 y Task 7.
3. **Falla a mitad de confirmar una compra** (por ejemplo, choca `uq_entrada_butaca_funcion`): no debe quedar la compra `CONFIRMADA` sin entradas → `rollback`, prueba en Task 5.
4. **Serie del comprobante**: la pone el trigger `trg_compra_serie`; Java la **relee** y no la inventa (no debe aparecer `B001-000001` repetido) → prueba en Task 5.
5. **Se acaban los 4 minutos en la pantalla de pago**: el temporizador llega a 0, la compra pasa a `EXPIRADA` y la vista vuelve a la cartelera → prueba manual en Task 10.

---

## Mapa de archivos

```
cineObjects/
├── pom.xml                                   (modificar: dependencias + javafx-maven-plugin)
└── src/main/
    ├── java/
    │   ├── module-info.java                  (nuevo)
    │   └── ucsur/pe/
    │       ├── App.java                      (modificar: constructor App(Connection, Reloj, PasarelaPago))
    │       ├── CineApp.java                  (nuevo: extends Application, como App de MVCone)
    │       ├── db/                           (nuevo, como db/ de MVCone)
    │       │   ├── DBConnection.java
    │       │   ├── Fechas.java               DateTime ↔ java.sql
    │       │   ├── SedeDao.java / SedeMySqlDao.java           sede+sala+butaca+funcion+entradas ocupadas
    │       │   ├── PeliculaDao.java / PeliculaMySqlDao.java   pelicula+pelicula_genero
    │       │   ├── CatalogoDao.java / CatalogoMySqlDao.java   producto, inventario_sede, cupon, promocion(+dia), tipo_entrada
    │       │   ├── PersonaDao.java / PersonaMySqlDao.java     cliente, empleado, rol
    │       │   ├── CompraDao.java / CompraMySqlDao.java       compra, entrada, transaccion_pago, pedido, detalle_pedido
    │       │   └── PostventaDao.java / PostventaMySqlDao.java reclamo, calificacion
    │       ├── repository/                   (agregar un XxxJdbcRepository por cada interfaz existente
    │       │                                  + TipoEntradaRepository y su versión en memoria)
    │       ├── model/Funcion.java            (modificar: marcarButacaOcupada)
    │       ├── vista/                        (nuevo: controllers @FXML)
    │       │   ├── Navegador.java  Sesion.java
    │       │   ├── LoginVista.java  CarteleraVista.java  ButacasVista.java
    │       │   ├── DulceriaVista.java  PagoVista.java  ComprobanteVista.java
    │       │   └── AdminVista.java  PostventaVista.java
    │       └── demo/MainConexion.java        (nuevo: prueba por consola de la BD)
    └── resources/ucsur/pe/
        ├── db/db.properties (+ .example)
        ├── view/login.fxml cartelera.fxml butacas.fxml dulceria.fxml pago.fxml
        │        comprobante.fxml admin.fxml postventa.fxml
        └── css/estilos.css
```

Los DAO se agrupan **por agregado** (no uno por tabla) para no tener 26 DAOs: cada `XxxMySqlDao` es dueño de las tablas que se leen y guardan juntas.

**Cómo se verifica:** el proyecto no usa JUnit (MVCone tampoco); se prueba con clases `demo/Main*` por consola, como hasta ahora. Cada tarea agrega sus comprobaciones a `demo/MainConexion` y estas imprimen `OK`/`FALLA`.

---

### Task 1: Maven, módulos y configuración

**Files:**
- Modify: `cineObjects/pom.xml`
- Create: `cineObjects/src/main/java/module-info.java`
- Create: `cineObjects/src/main/resources/ucsur/pe/db/db.properties`, `db.properties.example`
- Modify: `.gitignore`

**Interfaces:**
- Produces: módulo `ucsur.pe` con `requires javafx.controls; requires javafx.fxml; requires java.sql; requires mysql.connector.j;`, `opens ucsur.pe.vista to javafx.fxml;`, `exports ucsur.pe;`.

- [ ] **Step 1:** En `pom.xml` agregar las dependencias `org.openjfx:javafx-controls` y `javafx-fxml` (misma versión; la última estable compatible con JDK 26, buscar en Maven Central, ≥ 25) y `com.mysql:mysql-connector-j:8.4.0`; el plugin `maven-compiler-plugin` 3.13.0 con `<release>26</release>`; y el plugin `org.openjfx:javafx-maven-plugin:0.0.8` con `<mainClass>ucsur.pe/ucsur.pe.CineApp</mainClass>`. Mantener `exec.mainClass` = `ucsur.pe.demo.CineObjects`.
- [ ] **Step 2:** Crear `module-info.java` con lo de *Produces* (agregar `exports ucsur.pe.demo;` para que `exec:java` encuentre los demos).
- [ ] **Step 3:** Crear `db.properties` (`db.url=jdbc:mysql://localhost:3306/`, `db.name=DB_Cine`, `db.user=root`, `db.password=`) y una copia llamada `db.properties.example`. Agregar `cineObjects/src/main/resources/ucsur/pe/db/db.properties` al `.gitignore`.
- [ ] **Step 4: Verificar** — `cd cineObjects && mvn -q clean compile` → sin errores; `mvn -q exec:java` → `CineObjects` imprime el mismo flujo que antes.
- [ ] **Step 5: Commit** — `git add -A && git commit -m "build: agrega JavaFX, MySQL connector y module-info"`

---

### Task 2: DBConnection y conversión de fechas

**Files:**
- Create: `ucsur/pe/db/DBConnection.java`, `ucsur/pe/db/Fechas.java`, `ucsur/pe/demo/MainConexion.java`

**Interfaces:**
- Produces:
  - `DBConnection.getInstance(): DBConnection`, `getConexion(): java.sql.Connection`, `cerrar(): void` (copia de MVCone **sin** `ejecutarSchema`/`leerSchema`; lee `/ucsur/pe/db/db.properties` y se conecta a `url + name + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Lima"`).
  - `Fechas.aTimestamp(DateTime): Timestamp`, `Fechas.aDate(DateTime): java.sql.Date`, `Fechas.aTime(DateTime): java.sql.Time`, `Fechas.desdeTimestamp(Timestamp): DateTime`, `Fechas.desdeDate(java.sql.Date): DateTime` (usa `DateTime(dia,mes,anio)`), `Fechas.desdeTime(Time): DateTime` (usa `DateTime(hora,minuto)`). Todas devuelven `null` si reciben `null`.

- [ ] **Step 1: Escribir la comprobación** en `MainConexion.main`:
  - `probar("conecta", () -> DBConnection.getInstance().getConexion().isValid(2))`
  - `probar("fecha ida y vuelta", () -> Fechas.desdeTimestamp(Fechas.aTimestamp(new DateTime(29,2,2028,19,30,0))).equals(new DateTime(29,2,2028,19,30,0)))`
  - `probar("hora", () -> Fechas.desdeTime(Fechas.aTime(new DateTime(21,30))).equals(new DateTime(21,30)))`
  - `probar("sedes en BD", () -> contar("SELECT COUNT(*) FROM sede") == 15)`
  - `probar(String, BooleanSupplier)` imprime `OK nombre` o `FALLA nombre`, y atrapa excepciones como `FALLA nombre: mensaje`.
- [ ] **Step 2:** `mvn -q compile exec:java -Dexec.mainClass=ucsur.pe.demo.MainConexion` → no compila (faltan las clases).
- [ ] **Step 3:** Implementar `DBConnection` y `Fechas`. Convertir con `Timestamp.valueOf(LocalDateTime.of(...))` **solo dentro de `Fechas`**.
- [ ] **Step 4:** Ejecutar de nuevo → 4 × `OK`. Apagar MySQL (`services.msc` → MySQL80 → Detener) y volver a ejecutar → `FALLA conecta: No se pudo conectar a la base de datos. Verifique db.properties.` (Review Focus 2). Volver a encender MySQL.
- [ ] **Step 5: Commit** — `git commit -m "feat(db): DBConnection singleton y conversión DateTime-SQL"`

---

### Task 3: Catálogo de solo lectura (cartelera)

**Files:**
- Create: `db/SedeDao.java`, `db/SedeMySqlDao.java`, `db/PeliculaDao.java`, `db/PeliculaMySqlDao.java`, `db/CatalogoDao.java`, `db/CatalogoMySqlDao.java`
- Create: `repository/SedeJdbcRepository.java`, `PeliculaJdbcRepository.java`, `ProductoJdbcRepository.java`, `CuponJdbcRepository.java`, `PromocionJdbcRepository.java`
- Create: `repository/TipoEntradaRepository.java`, `TipoEntradaMemoriaRepository.java`, `TipoEntradaJdbcRepository.java`
- Modify: `model/Funcion.java`, `demo/MainConexion.java`

**Interfaces:**
- Consumes: `DBConnection.getConexion()`, `Fechas.*` (Task 2).
- Produces:
  - `SedeMySqlDao(Connection)`: `List<Sede> listarSedesCompletas(PeliculaDao peliculas)`: arma Sede → `agregarSala(Sala)` (con su `TipoSala`) → `agregarButaca(Butaca)` → `agregarFuncion(Funcion)`, y por cada función llama `marcarButacaOcupada` con las butacas de `entrada` que estén en `VENDIDA` o en `BLOQUEADA` con `expira_bloqueo > NOW()`. También `void insertarFuncion(Funcion)` y `void actualizarEstadoFuncion(Funcion)`.
  - `PeliculaMySqlDao(Connection)`: `List<Pelicula> listarTodas()` (con géneros), `Optional<Pelicula> buscarPorId(int)`, `void insertar(Pelicula)`, `void actualizar(Pelicula)`.
  - `CatalogoMySqlDao(Connection)`: listar/buscar/insertar/actualizar de producto (con `stock` de `inventario_sede`), cupón (`buscarPorCodigo`, `incrementarUsos(int idCupon)`), promoción (con `promocion_dia`) y `List<TipoEntrada> listarTiposEntradaActivos()`.
  - `TipoEntradaRepository`: `List<TipoEntrada> listarActivos()`.
  - `SedeJdbcRepository(SedeMySqlDao, PeliculaMySqlDao) implements SedeRepository`: carga **una vez** y guarda en un `Map<Integer,Sede>`; `guardar(Sede)` inserta las funciones nuevas (las que tienen un id que no existe en la BD). Así todas las pantallas comparten los mismos objetos `Funcion`, como en memoria.
  - `Funcion.marcarButacaOcupada(Butaca butaca): void` (pública): la guarda en un `Set<Butaca> ocupadasEnBD`; `esButacaDisponible` devuelve `false` si la butaca está en ese conjunto.

- [ ] **Step 1: Escribir las comprobaciones** en `MainConexion`:
  - `"cartelera sede 1"`: `new CarteleraService(sedesJdbc, promosJdbc, new RelojSistema()).consultarCartelera(sede1)` no está vacía.
  - `"butacas = capacidad"`: en cada sala de la sede 1, `obtenerMapaButacas().size() == getCapacidad()`.
  - `"película con géneros"`: `peliculasJdbc.buscarPorId(1).get().getGeneros()` no está vacío.
  - `"cupón por código"`: un código tomado de `BD_Cine_datos.sql` se encuentra.
  - `"butaca vendida ocupada"` (Review Focus 1): toma una `entrada` `VENDIDA` de los datos de prueba y verifica que `funcion.esButacaDisponible(butaca) == false`.
  - `"tipos de entrada"`: `tiposJdbc.listarActivos()` no está vacía.
- [ ] **Step 2:** Ejecutar → no compila / FALLA.
- [ ] **Step 3:** Implementar `Funcion.marcarButacaOcupada` y verificar que `mvn -q exec:java` (CineObjects) sigue igual.
- [ ] **Step 4:** Implementar los DAO con `JOIN` (como `listarTodos` de MVCone: una consulta por nivel, no una por fila) y los `XxxJdbcRepository` que delegan en ellos.
- [ ] **Step 5:** Ejecutar `MainConexion` → todo `OK`.
- [ ] **Step 6: Commit** — `git commit -m "feat(db): repositorios Jdbc del catálogo y la cartelera"`

---

### Task 4: Clientes y empleados (login)

**Files:**
- Create: `db/PersonaDao.java`, `db/PersonaMySqlDao.java`, `repository/ClienteJdbcRepository.java`, `repository/EmpleadoJdbcRepository.java`
- Modify: `demo/MainConexion.java`

**Interfaces:**
- Consumes: `SedeJdbcRepository` (el empleado necesita su `Sede`) (Task 3).
- Produces: `ClienteJdbcRepository(PersonaMySqlDao) implements ClienteRepository` (`guardar` = INSERT si el id no existe, si no UPDATE de `telefono, correo, puntos, nivel_socio`); `EmpleadoJdbcRepository(PersonaMySqlDao, SedeRepository) implements EmpleadoRepository`.

- [ ] **Step 1: Escribir las comprobaciones**:
  - `"login empleado"`: `new AutenticacionService(clientesJdbc, empleadosJdbc).iniciarSesion(correoAdmin, contraAdmin)` devuelve un `Empleado` (datos tomados de `BD_Cine_datos.sql`).
  - `"registrar cliente"`: `registrarCliente(...)` con un DNI aleatorio de 8 dígitos y luego `buscarPorCorreo` lo encuentra con el mismo id.
  - `"correo repetido"`: registrar otra vez el mismo correo → `RepositoryException` o `IllegalArgumentException`, nunca una `SQLException` sin envolver.
- [ ] **Step 2:** Ejecutar → FALLA.
- [ ] **Step 3:** Implementar. `siguienteId()` según Global Constraints. Leer `fecha_nac` con `Fechas.desdeDate`.
- [ ] **Step 4:** Ejecutar → `OK`.
- [ ] **Step 5: Commit** — `git commit -m "feat(db): repositorios Jdbc de cliente y empleado"`

---

### Task 5: Compra, entradas, pago y pedido (con transacción)

**Files:**
- Create: `db/CompraDao.java`, `db/CompraMySqlDao.java`, `repository/CompraJdbcRepository.java`, `PagoJdbcRepository.java`, `PedidoJdbcRepository.java`
- Modify: `demo/MainConexion.java`

**Interfaces:**
- Consumes: `Fechas.*`, `CatalogoMySqlDao.incrementarUsos`, `SedeJdbcRepository`.
- Produces:
  - `CompraMySqlDao(Connection)`: `void guardarCompraCompleta(Compra)`: en **una transacción** (`setAutoCommit(false)` → … → `commit`, y `rollback` en el `catch`, `setAutoCommit(true)` en el `finally`) hace UPSERT de `compra` (estado, importes de `getImporte()`, puntos, `id_cupon`/`id_promocion`), luego UPSERT de cada `entrada` (estado, `expira_bloqueo`, precio), luego del `pedido` y su `detalle_pedido`, y si la compra está `CONFIRMADA`, `UPDATE inventario_sede` (stock) y `incrementarUsos` del cupón. Después del commit **relee** `serie_numero` y llama `compra.asignarSerie(...)` si en Java es `null`.
  - `CompraJdbcRepository(CompraMySqlDao) implements CompraRepository`: `guardar` → `guardarCompraCompleta` y la guarda en `Map<Integer,Compra> cache`; `buscarPorId`/`listarTodas` leen la caché (versión 1, ver Global Constraints).
  - `PagoJdbcRepository(CompraMySqlDao)`, `PedidoJdbcRepository(CompraMySqlDao)`: INSERT/UPDATE en `transaccion_pago` y `pedido`.

- [ ] **Step 1: Escribir las comprobaciones** (usan `new App(conexion, new RelojSistema(), new PasarelaSimulada())` de la Task 7; si todavía no existe, armar los servicios a mano como en `App`):
  - `"compra confirmada en BD"`: iniciar → agregar 2 entradas → confirmar asientos → pagar → `SELECT estado FROM compra WHERE id_compra=?` = `CONFIRMADA` y `SELECT COUNT(*) FROM entrada WHERE id_compra=? AND estado='VENDIDA'` = 2.
  - `"serie del trigger"` (Review Focus 4): `compra.getSerieNumero()` empieza con `B001-` y es igual al `serie_numero` de la BD; dos compras seguidas tienen series distintas.
  - `"rollback"` (Review Focus 3): fuerza el choque insertando antes con SQL una `entrada` `VENDIDA` para la misma función y butaca → `pagar` devuelve error y `SELECT estado FROM compra` **no** es `CONFIRMADA`.
  - `"butaca ocupada para otra compra"`: después de confirmar, una compra nueva en la misma función recibe `Respuesta.ok()==false` al agregar esa butaca.
- [ ] **Step 2:** Ejecutar → FALLA.
- [ ] **Step 3:** Implementar `CompraMySqlDao` y los tres repositorios.
- [ ] **Step 4:** Ejecutar → todo `OK`.
- [ ] **Step 5: Commit** — `git commit -m "feat(db): compra, entradas, pago y pedido con transacción"`

---

### Task 6: Postventa (reclamos y calificaciones)

**Files:**
- Create: `db/PostventaDao.java`, `db/PostventaMySqlDao.java`, `repository/ReclamoJdbcRepository.java`, `CalificacionJdbcRepository.java`

**Interfaces:**
- Produces: `ReclamoJdbcRepository(PostventaMySqlDao)` (`listarPorCompra(Compra)` filtra por `id_compra`), `CalificacionJdbcRepository(PostventaMySqlDao)` (`existePorCompra` = `SELECT 1 … WHERE id_compra=?`).

- [ ] **Step 1: Escribir las comprobaciones**: `"reclamo guardado"` (`app.postventa().presentarReclamo(compra, "RECLAMO", "texto")` sobre la compra de la Task 5 → `listarPorCompra` devuelve 1) y `"una calificación por compra"` (`calificar(compra, 5, "")` dos veces → la segunda da `Respuesta.ok()==false`, por `uq_calificacion_compra`).
- [ ] **Step 2:** Ejecutar → FALLA. **Step 3:** Implementar. **Step 4:** Ejecutar → `OK`.
- [ ] **Step 5: Commit** — `git commit -m "feat(db): reclamos y calificaciones en MySQL"`

---

### Task 7: Raíz de composición con BD y arranque JavaFX

**Files:**
- Modify: `ucsur/pe/App.java`
- Create: `ucsur/pe/CineApp.java`, `ucsur/pe/vista/Navegador.java`, `ucsur/pe/vista/Sesion.java`, `resources/ucsur/pe/css/estilos.css`

**Interfaces:**
- Consumes: todos los `XxxJdbcRepository` (Tasks 3–6).
- Produces:
  - `App(Connection conexion, Reloj reloj, PasarelaPago pasarela)`: crea los DAO con `conexion` y los `XxxJdbcRepository`; el resto es igual al constructor actual. Para eso, pasar los repositorios de campos inicializados a parámetros de un constructor privado `App(Repositorios r, Reloj, PasarelaPago)`, con `record Repositorios(ClienteRepository clientes, …, TipoEntradaRepository tiposEntrada)` anidado. `new App()` sigue usando memoria.
  - `App.tiposEntrada(): TipoEntradaRepository`.
  - `CineApp extends Application`: `start(Stage)` → `DBConnection.getInstance()` (si lanza `RepositoryException`: `Alert` de error con el mensaje y `Platform.exit()`) → `new App(conexion, new RelojSistema(), new PasarelaSimulada())` → `new Navegador(stage, app)` → `navegador.ir("login")`; `stage.setOnCloseRequest(e -> dbConnection.cerrar())`. `main` → `launch()`.
  - `Navegador(Stage, App)`: `void ir(String vista)` carga `view/<vista>.fxml` con `setControllerFactory(clase -> crear(clase))`, donde `crear` instancia cada `XxxVista` pasándole `(App app, Navegador nav, Sesion sesion)` (inyección por constructor, como MVCone); reutiliza una sola `Scene` 1180×720 con `estilos.css`.
  - `Sesion`: guarda `Persona usuario`, `Sede sede`, `Funcion funcion`, `Compra compra`, `Pedido pedido`, con getters y setters, y `void limpiarCompra()`.

- [ ] **Step 1:** Implementar `App` y comprobar que `mvn -q exec:java` (CineObjects, en memoria) sigue igual y que `MainConexion` sigue todo `OK`.
- [ ] **Step 2:** Crear `estilos.css` partiendo del de MVCone (`root-pane`, `header`, `card`, `card-title`, `field-label`, `btn-primary`, `btn-secondary`, `btn-danger`, `btn-link`, `mensaje.ok/.error`) y agregar `.butaca-libre`, `.butaca-elegida`, `.butaca-ocupada`.
- [ ] **Step 3:** Implementar `CineApp`, `Navegador` y `Sesion`, con un `login.fxml` mínimo (un solo `Label`) para probar el arranque.
- [ ] **Step 4: Verificar** — `mvn -q javafx:run` abre la ventana. Con MySQL detenido aparece el `Alert` y la app se cierra (Review Focus 2).
- [ ] **Step 5: Commit** — `git commit -m "feat(ui): arranque JavaFX con App conectada a MySQL"`

---

### Task 8: Pantalla de Login

**Files:** Create `view/login.fxml`, `vista/LoginVista.java`

**Interfaces:**
- Consumes: `app.login()` → `LoginController.iniciarSesion(correo, contra)`, `registrarCliente(...)`, `ingresarComoInvitado(correo)` (devuelven `Respuesta<…>`).
- Produces: deja `sesion.setUsuario(persona)`. Si es un `Empleado` con rol administrador va a `ir("admin")`; si no, a `ir("cartelera")`.

Patrón para **todas** las vistas (copiado de `ComprobanteController` de MVCone): campos `@FXML` con el mismo `fx:id` que el FXML, `@FXML initialize()`, un `Label lblMensaje` y un método privado `mostrar(Respuesta<?> r)` que pone `r.mensaje()` con la clase de estilo `ok`/`error`. **La vista no valida reglas**: solo convierte texto a número/`DateTime` (como `leerSubtotal` de MVCone) y llama al controller.

- [ ] **Step 1:** FXML: `BorderPane` con header y una `card` con `TabPane` (Ingresar / Registrarse / Invitado).
- [ ] **Step 2:** `LoginVista` con `onIngresar`, `onRegistrar`, `onInvitado`.
- [ ] **Step 3: Verificar** con `mvn -q javafx:run`: contraseña incorrecta → mensaje rojo; empleado admin de los datos de prueba → va a admin; cliente nuevo → queda en la tabla `cliente` (revisar en Workbench).
- [ ] **Step 4: Commit** — `git commit -m "feat(ui): pantalla de login"`

---

### Task 9: Cartelera y selección de butacas

**Files:** Create `view/cartelera.fxml`, `view/butacas.fxml`, `vista/CarteleraVista.java`, `vista/ButacasVista.java`

**Interfaces:**
- Consumes: `app.cartelera()` (`listarSedes`, `consultarCartelera(sede)`, `filtrarCartelera(...)`), `app.compra()` (`iniciarCompra`, `recomendarAsientos`, `agregarEntrada`, `quitarEntrada`, `confirmarAsientos`), `app.tiposEntrada().listarActivos()`.
- Produces: `sesion.funcion`, `sesion.compra` con entradas bloqueadas → `ir("dulceria")`.

- [ ] **Step 1:** `cartelera.fxml`: `ComboBox<Sede>`, filtros (idioma, clasificación, género) y un `TableView<Funcion>` (película, fecha `soloFecha()`, hora `soloHora()`, sala, formato, disponibles). Con doble clic o con "Elegir butacas": `iniciarCompra` y `ir("butacas")`.
- [ ] **Step 2:** `butacas.fxml`: `GridPane` armado en código desde `funcion.getSala().obtenerMapaButacas()` (una fila por letra), un `ToggleButton` por butaca; las que no están en `obtenerButacasDisponibles()` van deshabilitadas con `.butaca-ocupada`. A la derecha, un `ComboBox<TipoEntrada>` y la lista de entradas elegidas, y los botones "Recomendar", "Quitar", "Continuar" (`confirmarAsientos`) y "Cancelar compra".
- [ ] **Step 3: Verificar**: una butaca `VENDIDA` en la BD aparece deshabilitada (Review Focus 1); al hacer clic en una libre se ve `.butaca-elegida` y la entrada en la lista; con "Continuar" las filas `entrada` quedan `BLOQUEADA` en Workbench.
- [ ] **Step 4: Commit** — `git commit -m "feat(ui): cartelera y mapa de butacas"`

---

### Task 10: Dulcería, pago y comprobante

**Files:** Create `view/dulceria.fxml`, `view/pago.fxml`, `view/comprobante.fxml`, `vista/DulceriaVista.java`, `vista/PagoVista.java`, `vista/ComprobanteVista.java`

**Interfaces:**
- Consumes: `app.dulceria()`, `app.compra()` (`asignarPedido`, `aplicarCupon`, `aplicarPromocion`, `cancelar`, `generarComprobante`), `app.pago()` (`mediosDisponibles`, `pagar`).
- Produces: compra `CONFIRMADA` → `ir("comprobante")`; al terminar, `sesion.limpiarCompra()` → `ir("cartelera")`.

- [ ] **Step 1:** `dulceria.fxml`: `TableView<Producto>` + `Spinner` de cantidad + observación + carrito; "Omitir" o "Continuar".
- [ ] **Step 2:** `pago.fxml`: resumen (subtotal, descuento, IGV, total desde `compra.getImporte()`), campo de cupón + "Aplicar", `ComboBox` de promociones, `ComboBox` de medios de pago, "Pagar". Un `Label` con el **tiempo restante** que se actualiza con un `Timeline` de 1 s usando `compra.getExpiraCompra()` y `RelojSistema().ahora()`; cuando llega a 0 llama `compra.verificarExpiracion()`, avisa con un `Alert` y hace `ir("cartelera")` (Review Focus 5). Detener el `Timeline` al salir de la pantalla.
- [ ] **Step 3:** `comprobante.fxml`: `TextArea` no editable con `generarComprobante(compra)` (igual que "Vista de impresión" de MVCone) y el botón "Nueva compra".
- [ ] **Step 4: Verificar**: compra completa con combo y cupón → en Workbench `compra.estado='CONFIRMADA'`, la serie `B001-…` se ve en pantalla, `cupon.usos_realizados` sube en 1 y el stock baja. Para probar la expiración, dejar correr los 4 minutos en la pantalla de pago → vuelve a la cartelera.
- [ ] **Step 5: Commit** — `git commit -m "feat(ui): dulcería, pago con temporizador y comprobante"`

---

### Task 11: Panel de administración (CRUD estilo MVCone)

**Files:** Create `view/admin.fxml`, `vista/AdminVista.java`

**Interfaces:**
- Consumes: `app.admin()` (`registrarPelicula`, `actualizarPelicula`, `programarFuncion`, `cancelarFuncion`, `registrarProducto`, `reponerStock`, `registrarCupon`, `registrarPromocion`, `cambiarEstado`), con `(Empleado) sesion.getUsuario()`.

- [ ] **Step 1:** `TabPane` con Películas / Funciones / Productos / Cupones y promociones. Cada pestaña es **la misma distribución que `comprobantes.fxml`**: una `card` con el formulario a la izquierda y un `TableView` a la derecha. Igual que en MVCone: con una fila seleccionada se habilita "Actualizar" (con `disableProperty().bind(seleccion.isNull())`) y sin selección, "Registrar".
- [ ] **Step 2:** Implementar las acciones, cada una con `mostrar(respuesta)` y el refresco de la tabla.
- [ ] **Step 3: Verificar**: programar una función que choca de horario → mensaje rojo del servicio; si no choca, aparece en la cartelera del cliente y en la tabla `funcion`.
- [ ] **Step 4: Commit** — `git commit -m "feat(ui): panel de administración"`

---

### Task 12: Postventa y documentación

**Files:** Create `view/postventa.fxml`, `vista/PostventaVista.java`. Modify `README.md`.

- [ ] **Step 1:** `postventa.fxml` (se abre desde el comprobante): formulario de reclamo (tipo, descripción) y calificación (1–5 con `Slider` o `ToggleGroup`) sobre `sesion.compra`, usando `app.postventa()`.
- [ ] **Step 2:** Actualizar `README.md`, tomando como modelo las Partes 1, 5, 6, 7 y 9 del README de MVCone: instalar MySQL y Workbench, ejecutar `BD/BD_Cine.sql` y luego `BD/BD_Cine_datos.sql`, copiar `db.properties.example` a `db.properties`, `mvn javafx:run`, y la tabla de errores comunes.
- [ ] **Step 3: Verificar**: después de una compra, registrar un reclamo y una calificación → ambos aparecen en Workbench; calificar dos veces → mensaje de error.
- [ ] **Step 4: Commit** — `git commit -m "feat(ui): postventa y guía de instalación"`

---

## Qué se copia de MVCone y qué cambia

| MVCone | Proyecto_Cine | Por qué cambia |
|---|---|---|
| `DBConnection` ejecuta `schema.sql` | Solo se conecta | Los triggers con `DELIMITER` no pasan por JDBC |
| 1 DAO (`ComprobanteDao`) | 6 DAOs por agregado | 26 tablas; un DAO por tabla serían demasiados archivos |
| Controller = `@FXML` + lógica | `controller/` lógico + `vista/` `@FXML` | Ya tienes controllers con `Respuesta<T>`; se reutilizan |
| 1 pantalla | 8 pantallas + `Navegador` | Flujo de varios pasos |
| `siguienteNumero` = MAX+1 | `siguienteId` = MAX+1 | Igual; así los servicios no cambian |
