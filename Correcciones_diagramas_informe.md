# Textos corregidos de los diagramas — Informe de Avances

Cada bloque indica **dónde va**, el **texto para pegar** y, cuando corresponde, **qué cambiar en la imagen**.
Las cifras se verificaron contra `cineObjects` y `BD/BD_Cine.sql` al 25/11/2026.

---

## 1. Figura 4 — TO-BE (añadir al final del párrafo que sigue a la figura)

> El diagrama representa el flujo con cliente registrado. La compra como invitado (RF-01) sigue el mismo recorrido, con una diferencia: en lugar de «Iniciar sesión», el cliente ingresa solo su correo electrónico, al que se envía el comprobante. Además, no acumula puntos ni accede a beneficios de socio.

---

## 2. Figura 6 — Casos de uso (reemplaza el párrafo «Como se observa en el diagrama…»)

> Como se observa en el diagrama, el Cliente concentra las interacciones del flujo de compra. Puede registrarse como usuario, iniciar sesión, actualizar los datos de su perfil, comprar entradas y productos de dulcería, consultar promociones, consultar la cartelera y consultar su historial de compras. Los casos «Comprar entrada» y «Comprar dulcería» incluyen el caso «Realizar pago», que se comunica con la Pasarela de Pago. Además, «Comprar dulcería» desencadena «Preparar pedido». «Consultar cartelera» incluye «Personalizar compra», que a su vez puede extenderse con «Cancelar» y «Modificar». A su vez, «Consultar cartelera» se extiende mediante «Filtrar película».

(El párrafo del Administrador y el del personal de Dulcería quedan igual.)

---

## 3. Figura 7 — Componentes MVC

### 3.1 Cambios en la imagen
| Caja | Dice | Debe decir |
|---|---|---|
| Presentación | 50 clases JavaFX y 40 FXML | **51 clases** JavaFX y 40 FXML |
| Control | 14 clases; ControllerBase y Respuesta | 12 controladores + ControllerBase y Respuesta (14 clases) |
| Aplicación | 16 servicios (…) | 11 servicios + 5 clases de apoyo (Sesion, CuentaPersonal, PermisosAdmin, ResultadoCancelacion) — 16 clases |
| Dominio | 39 clases e interfaces · Entidades, reglas de negocio y Strategy | 39 clases e interfaces · Entidades y reglas de negocio |
| *(nueva, junto a Dominio)* | — | «paquete» **ucsur.pe.tipo** — 4 clases · Strategy de descuentos (CalculoDescuento, CalculoPorcentaje, CalculoMontoFijo, FabricaCalculoDescuento) |
| *(nota al pie, opcional)* | — | Transversales: `util` (ConversorDinero) y `exception` (ReglaNegocioException, RepositoryException) |
| Orden de ensamblaje, paso 3 | 16 servicios recibiendo… | 11 servicios recibiendo interfaces y Reloj |
| Orden de ensamblaje, paso 4 | 14 controladores con sus servicios | 12 controladores con sus servicios |

### 3.2 Nota de la figura (corrige «estrutura»)
> Nota: Elaboración propia (2026) a partir de la estructura real de paquetes.

### 3.3 Párrafo «Qué hace cada capa» (reemplazo)
> **Qué hace cada capa.** La vista (`vista`, con 51 clases, 40 archivos FXML y la hoja de estilos) muestra las pantallas y recoge lo que el cliente toca. El controlador (`controller`, con 12 controladores) traduce cada acción en una llamada a un servicio y devuelve una `Respuesta` con el resultado o con el mensaje de error. No valida reglas ni guarda datos. El servicio (`service`, con 11 servicios) coordina cada caso de uso, por ejemplo `CompraService` o `PagoService`. El modelo (`model`, con 39 clases e interfaces) contiene las reglas del negocio: compra, butacas, descuentos y puntos. Las formas de calcular un descuento están en el paquete `tipo` (patrón Strategy).

### 3.4 Tabla 11 — agregar una fila después de «Dominio»
| Capa | Paquete | Rol MVC | Responsabilidad | Ejemplo |
|---|---|---|---|---|
| Dominio (estrategias) | tipo | Modelo | Formas intercambiables de calcular un descuento. | CalculoPorcentaje |

---

## 4. Figura 8 — Modelo conceptual

### 4.1 Cambios en la imagen
1. Agregar la entidad **NIVEL_SOCIO** (idNivel, nombre, visitasMinimas, multiplicadorPuntos), relacionada 1:N con CLIENTE («PERTENECE A») y 1:N con PRODUCTO («RESTRINGE»).
2. Agregar la relación **SEDE – PRODUCTO** M:N «MANTIENE STOCK», con el atributo *stock* en la relación. Quitar «Stock» de PRODUCTO.
3. Agregar la relación **SEDE – COMPRA** 1:N «REGISTRA».
4. Agregar la relación **EMPLEADO – CLIENTE** 1:0..1 «VINCULA CUENTA PERSONAL». Agregar a EMPLEADO el atributo *pin*.
5. Agregar a TIPO_ENTRADA los atributos *puntosPorEntrada*, *puntosCanje* y *soloCumpleanos*.
6. CORRELATIVO **no** se dibuja: es una tabla técnica de numeración, no una entidad del negocio. Por eso sale del texto del conceptual y aparece en el lógico.

### 4.2 Texto (reemplaza los dos párrafos posteriores a la figura)
> El modelo conceptual (Figura 8) representa las entidades del negocio y sus relaciones mediante la notación entidad-relación. El núcleo transaccional es COMPRA: un CLIENTE realiza muchas compras y cada compra se registra en una SEDE. Cada compra incluye una o más ENTRADAS, puede generar un PEDIDO de dulcería y se paga mediante una o más TRANSACCION_PAGO. Cada entrada corresponde a una FUNCION, ocupa una BUTACA y se clasifica según un TIPO_ENTRADA. Una sede cuenta con varias SALAS (clasificadas por TIPO_SALA). Cada sala aloja funciones y contiene butacas, y cada función proyecta una PELICULA asociada a uno o más GENEROS.
>
> NIVEL_SOCIO define las categorías del programa de fidelización (niveles 0 a 4, visitas mínimas anuales y multiplicador de puntos). Se vincula con los clientes y con los productos exclusivos para socios. Cada sede mantiene stock de cada PRODUCTO. El EMPLEADO posee un ROL, labora en una sede y atiende pedidos, y puede vincular una cuenta personal de cliente con su mismo DNI para comprar como socio. Por último, la compra puede canjear un CUPON, aplicar una PROMOCION (vigente en determinados días de la semana), recibir una CALIFICACION y registrar RECLAMOS. La numeración de comprobantes (CORRELATIVO) es un control técnico y se incorpora recién en el diseño lógico.

---

## 5. Figura 9 — Modelo lógico

### 5.1 Cambios en la imagen
1. Agregar la tabla **correlativo**: `serie CHAR(4) PK`, `ultimo_numero INT`. No tiene FK: la usa el trigger `trg_compra_serie`.
2. En **tipo_entrada**, agregar `solo_cumpleanos BOOLEAN` (entre `puntos_canje` y `estado`).
3. En **cliente** y **empleado**, cambiar `nombre` / `apellido(s)` por `nombres` / `apellidos`.
4. En **compra**, corregir `serie__numero` → `serie_numero` y `DATATIME` → `DATETIME`.
5. En **cliente**, opcionalmente, agregar la columna generada `correo_unico` (ver Restricciones de integridad).

### 5.2 Texto (reemplaza el párrafo de «Diseño Lógico BD»)
> El diseño lógico transforma el modelo conceptual en un esquema relacional normalizado de 26 tablas, con sus claves primarias (PK), claves foráneas (FK) y cardinalidades. Las relaciones muchos a muchos se resuelven mediante tablas asociativas (`pelicula_genero`, `promocion_dia`, `inventario_sede` y `detalle_pedido`). A estas se suma la tabla técnica `correlativo`, que controla la numeración consecutiva de los comprobantes.

---

## 6. Figura 10 — Modelo físico

### 6.1 Rehacer la imagen
La imagen actual corresponde a una versión anterior del esquema. Le faltan `nivel_socio` y `correlativo`, y en `empleado` no aparecen `pin` ni `id_cliente`. Además, tiene baja resolución. Para regenerarla:
1. MySQL Workbench → *Database → Reverse Engineer…* → esquema `DB_Cine`.
2. Reordenar las tablas y luego *File → Export → Export as PNG*.
3. Comprobar que aparezcan las 26 tablas.

### 6.2 «Restricciones de integridad» — primer párrafo (reemplazo)
> La integridad de los datos se impone directamente en el motor MySQL mediante 26 claves primarias (PRIMARY KEY), 30 claves foráneas (FOREIGN KEY), 23 restricciones de unicidad (UNIQUE) y 51 restricciones de verificación (CHECK). Entre las decisiones físicas más relevantes destacan:

### 6.3 Viñeta nueva (añadir al final de la lista de decisiones físicas)
> **Cuenta personal del empleado:** la tabla `empleado` incorpora `id_cliente` (FK a `cliente`, con `UNIQUE`) para vincular una sola cuenta de cliente por empleado. También incorpora un `pin` de 4 dígitos (`CHECK pin REGEXP '^[0-9]{4}$'`), que se pide al volver del modo cliente al panel de trabajo.

---

## 7. Figura 11 — Diagrama de clases

### 7.1 Cambios en la imagen
| Clase | Cambio |
|---|---|
| Persona | Cambiar los atributos `#` por `-` (en el código son `private`, como dice la Figura 12). Agregar `- reloj: Reloj` y el método `+ verificarContrasena(contra: String): boolean`. Indicar `«implements» Activable`. |
| Empleado | Agregar `- rol: Rol`, `- sede: Sede`, `- pin: String` y `- cuentaPersonal: Cliente`. Agregar la asociación Empleado → Cliente 0..1 («cuenta personal»). |
| Cliente | Agregar `- historialCompras: List<Compra>`. |
| Compra | Agregar `- montoReembolsadoCentavos: long`, `- cliente: Cliente`, `- sede: Sede`, `- descuentos: List<Descuento>` y `- pedido: Pedido`. Agregar `«implements» Totalizable`. |
| Entrada | `- precio: double` → `- precioCentavos: long`. Agregar `- funcion`, `- butaca` y `- tipoEntrada`. |
| Producto | `- precio: double` → `- precioCentavos: long`. Agregar `- imagen: String`. |
| TipoEntrada | `- tarifa: double` → `- tarifaCentavos: long`. Agregar `- soloCumpleanos: boolean`. |
| DescuentoBase | Agregar `- nivelSocioMinimo: int`. Indicar `«implements» Descuento, Activable`. |
| Promocion | Quitar `- soloSocios: boolean`, que ya no existe: ahora es `nivelSocioMinimo` en DescuentoBase. |
| Cupon | Indicar `«implements» DescuentoConUsos`. |
| Nuevas (recomendado) | `SeleccionAsientos`, `CalculadoraImporte` y `GestorPuntos`, en composición con Compra (1 — 1). Las interfaces `Descuento`, `DescuentoConUsos`, `Activable` y `Totalizable`. `CalculoDescuento` con `CalculoPorcentaje` y `CalculoMontoFijo` (asociación desde DescuentoBase). |

> Si el diagrama queda muy cargado, se puede dividir en **11a (entidades)** y **11b (compra, descuentos y Strategy)**.

### 7.2 Texto (añadir después de la Figura 11)
> Los importes se representan en céntimos enteros (`long`), por ejemplo `precioCentavos` o `tarifaCentavos`, para evitar errores de redondeo de punto flotante. Compra no calcula todo por sí misma: delega en tres colaboradores. `SeleccionAsientos` maneja las butacas, `CalculadoraImporte` calcula subtotal, descuentos e IGV, y `GestorPuntos` calcula los puntos. Los descuentos se modelan con la interfaz `Descuento` y la clase abstracta `DescuentoBase`, de la que heredan `Cupon` y `Promocion`. Cada descuento delega su cálculo en una estrategia `CalculoDescuento`.

---

## 8. Figuras 22–28 — Diagramas de secuencia

Las siete figuras están vacías. Abajo va, para cada una, **qué dibujar** (líneas de vida y mensajes con los nombres reales del código) y el **párrafo** que va debajo de la imagen.

### Figura 22 — Iniciar sesión (Login)
**Líneas de vida:** Cliente · IniciarSesionVista · LoginController · AutenticacionService · ClienteRepository · EmpleadoRepository · Persona
**Mensajes:**
1. Cliente → IniciarSesionVista: ingresa correo y contraseña
2. IniciarSesionVista → LoginController: `iniciarSesion(correo, contra)`
3. LoginController → AutenticacionService: `iniciarSesion(correo, contra)`
4. AutenticacionService → ClienteRepository: `buscarPorCorreo(correo)`
5. *[alt: no es cliente]* AutenticacionService → EmpleadoRepository: `buscarPorCorreo(correo)`
6. AutenticacionService → Persona: `verificarContrasena(contra)`
7. AutenticacionService → LoginController: `Sesion` (de cliente o de empleado)
8. LoginController → IniciarSesionVista: `Respuesta<Sesion>`. La vista abre la cartelera, el panel de dulcería o el de administración, según la sesión.

**Alternos:** *[credenciales inválidas o cuenta inactiva]* se lanza `ReglaNegocioException` y la vista muestra «Correo o contraseña incorrectos». *[invitado]* `LoginOInvitadoVista → LoginController.ingresarComoInvitado(correo)`.

> El diagrama de la Figura 22 muestra el inicio de sesión. La vista envía las credenciales al `LoginController`, que delega en `AutenticacionService`. El servicio busca primero un cliente con ese correo y, si no lo encuentra, un empleado. En ambos casos, la contraseña la verifica el propio objeto `Persona`, que además rechaza las cuentas inactivas. Si la verificación es correcta, se crea una `Sesion` del tipo correspondiente y la vista dirige al usuario a su pantalla: cartelera, panel de dulcería o panel de administración. Si falla, el sistema responde siempre con el mismo mensaje, «Correo o contraseña incorrectos», para no revelar qué correos están registrados. Como flujo alterno, el cliente puede continuar como invitado ingresando solo su correo.

### Figura 23 — Consultar cartelera
**Líneas de vida:** Cliente · CarteleraVista · CarteleraController · CarteleraService · Sede · Funcion
**Mensajes:**
1. CarteleraVista → CarteleraController: `consultarCartelera(sede)`
2. CarteleraController → CarteleraService: `consultarCartelera(sede)`
3. CarteleraService → Sede: `consultarCartelera()`
4. Sede → Funcion: *[loop por cada función]* valida que la función, su película y su sala estén operativas
5. Retorno: `Respuesta<List<Funcion>>` → la vista agrupa por película
6. *[opt: filtros]* CarteleraVista → CarteleraController: `filtrarCartelera(sede, dia, genero, idioma, formato, clasificacion)` → Sede: `filtrarCartelera(...)`

**Alternos:** *[sede no operativa]* lista vacía. *[pestaña Próximamente]* `listarProximamente()`.

> La Figura 23 describe la consulta de la cartelera de la sede elegida. `CarteleraService` delega en `Sede`, que devuelve solo las funciones vendibles: aquellas cuya película, sala y sede están operativas. Cuando el cliente aplica filtros (día, género, idioma, formato de sala o clasificación, RF-03), la vista invoca `filtrarCartelera`. Los filtros no elegidos llegan como nulos y no restringen el resultado. La pestaña «Próximamente» usa `listarProximamente`, que muestra las películas anunciadas ordenadas por fecha de estreno.

### Figura 24 — Comprar entrada
**Líneas de vida:** Cliente · HorarioVista · Navegador · CompraController · CompraService · Compra · CompraRepository
**Mensajes:**
1. Cliente → HorarioVista: elige función
2. HorarioVista → Navegador: `iniciarCompra()`
3. Navegador → CompraController → CompraService: `iniciarCompra(cliente, sede)`
4. CompraService → Compra: `new Compra(...)` (estado EN_PROCESO; empieza el plazo de 4 minutos)
5. CompraService → CompraRepository: `guardar(compra)` (la BD asigna el id)
6. CompraService → CompraController: `listarTiposEntrada(cliente)`. Solo devuelve las tarifas activas para las que el cliente es elegible.
7. *[ref: Selección y venta de butacas, Figura 25]*
8. *[opt]* `aplicarCupon(compra, codigo)` / `aplicarPromocion(compra, promocion)` → `Compra.aplicarDescuento(...)`
9. *[ref: Realizar pago, Figura 27]*

**Alternos:** *[cliente cancela]* `cancelar(compra)` → `Compra.cancelar()` + `guardar`. *[vence el plazo]* `Compra.verificarExpiracion()` → `cancelarPorTimeout()`. En la BD, `ev_expirar_compras` marca la compra como EXPIRADA.

> La Figura 24 muestra el flujo completo de compra de entradas. Al elegir una función, el sistema crea una `Compra` en estado EN_PROCESO y la guarda para obtener su identificador. Desde ese momento corre un plazo máximo de 4 minutos. Luego el cliente elige las butacas (Figura 25) y el tipo de cada entrada, entre las tarifas para las que es elegible. Si lo desea, puede aplicar un cupón o una promoción; en ese caso, la compra recalcula el importe. Por último, se pasa al pago (Figura 27). Si el cliente cancela o vence el plazo, la compra pasa a CANCELADA o EXPIRADA y las butacas se liberan.

### Figura 25 — Selección y venta de butacas
**Líneas de vida:** Cliente · AsientosVista · CompraController · CompraService · Funcion · Compra · SeleccionAsientos · CompraRepository · MySQL
**Mensajes:**
1. AsientosVista → CompraController → CompraService: `consultarMapa(compra, funcion, cantidad, incluirAccesibles)`
2. CompraService → Funcion: `recomendarMejoresAsientos(cantidad, incluirAccesibles, elegidas)` (RF-29)
3. Retorno: `MapaButacas` (mapa, disponibles y sugeridas)
4. *[loop por cada butaca]* AsientosVista → CompraController: `agregarEntrada(compra, funcion, butaca, tipo)` → Compra: `agregarEntrada(entrada)`
5. AsientosVista → CompraController: `confirmarAsientos(compra)`
6. CompraService → Compra: `confirmarSeleccionAsientos()` → SeleccionAsientos: `bloquear(...)`
7. CompraService → CompraRepository: `guardar(compra)` → MySQL: `INSERT entrada (estado BLOQUEADA)`

**Alternos:** *[no hay suficientes butacas libres]* el mapa llega sin sugerencia y con un aviso. *[otra terminal ya bloqueó la butaca]* MySQL rechaza el INSERT por `uq_entrada_butaca_funcion`, se lanza `RepositoryException` y la vista pide elegir otra butaca. *[«Volver» desde dulcería]* `liberarAsientos(compra)`.

> La Figura 25 detalla la selección de butacas. El sistema muestra el mapa de la sala con las butacas disponibles y una sugerencia de las mejores ubicaciones para el grupo (RF-29). El cliente agrega cada butaca con su tipo de entrada y, al confirmar, `Compra` delega en `SeleccionAsientos` el bloqueo temporal. Las entradas se insertan en la base de datos con estado BLOQUEADA. Si dos terminales intentan reservar la misma butaca al mismo tiempo, la restricción `uq_entrada_butaca_funcion` rechaza el segundo registro y el segundo cliente debe elegir otra (RNF-10). Las entradas pasan a VENDIDA recién cuando se confirma el pago.

### Figura 26 — Comprar dulcería
**Líneas de vida:** Cliente · DulceriaVista · DulceriaController · PedidoService · CompraController · CompraService · Compra · Pedido
**Mensajes:**
1. DulceriaVista → DulceriaController → PedidoService: `consultarMenu(sede, cliente)`. Devuelve los productos permitidos para el nivel de socio del cliente; los agotados en la sede van al final.
2. *[loop por cada producto]* DulceriaVista → CompraController → CompraService: `agregarProducto(compra, producto, cantidad, personalizacion)`
3. CompraService → Compra: `agregarProducto(detalle)`. Si la compra aún no tiene `Pedido`, lo crea; luego valida las ofertas de socio y recalcula el total.
4. *[opt]* `cambiarCantidad(compra, detalle, cantidad)` / `quitarProducto(compra, detalle)`
5. *[ref: Realizar pago, Figura 27]*. Al confirmarse el pago, el pedido queda PENDIENTE en la cola del personal de dulcería.

**Alternos:** *[producto exclusivo de socio y cliente sin nivel]* se lanza `ReglaNegocioException`. *[stock insuficiente]* `Compra.motivoNoConfirmable` lo detecta antes del cobro y el pago se rechaza sin cobrar.

> La Figura 26 presenta la compra de productos de dulcería. El menú muestra los productos permitidos para el nivel de socio del cliente; los agotados en la sede aparecen al final. Cada producto que el cliente agrega se registra en un `Pedido` asociado a la compra; el pedido se crea con el primer producto. La compra recalcula su total con cada cambio de cantidad o eliminación. El pedido no descuenta stock todavía: lo hace al confirmarse el pago, y en ese momento entra a la cola del personal de dulcería para su preparación.

### Figura 27 — Realizar pago
**Líneas de vida:** Cliente · PantallaPago (PagoTarjetaVista / PagoBilleteraVista) · PagoController · PagoService · Compra · PasarelaPago · PagoRepository · MySQL
**Mensajes:**
1. PantallaPago → PagoController → PagoService: `pagar(compra, totalCentavos, medioPago)`
2. PagoService → Compra: `verificarExpiracion()`, `calcularTotalCentavos()`, `motivoNoConfirmable(ahora)` (validaciones previas, sin cobrar)
3. PagoService → PasarelaPago: `cobrar(medio, monto, "COMPRA-id")` → `ResultadoPago`
4. PagoService → Compra: `confirmar(pago, ahora)`. Canjea puntos, descuenta stock, vende las butacas, registra el uso del cupón, otorga puntos y pasa la compra a CONFIRMADA.
5. PagoService → PagoRepository: `guardarConCompra(pago)` → MySQL (transacción; `trg_compra_serie` asigna B001-xxxxxx)
6. Retorno `Respuesta<TransaccionPago>` → Navegador: `compraPagada()` → ComprobanteVista

**Alternos:** *[compra expirada, monto distinto o compra no confirmable]* el pago se rechaza **sin cobrar**. *[pasarela rechaza]* se guarda el pago RECHAZADO con su motivo y el cliente puede reintentar, cambiar de medio o cancelar; las butacas siguen reservadas. *[falla el guardado tras cobrar]* `deshacerCobro`: `PasarelaPago.reembolsar(...)`, `Compra.anularConfirmacion()` y el pago queda RECHAZADO.

> La Figura 27 describe el pago, que se ejecuta en tres pasos. Primero, `PagoService` valida sin cobrar: que la compra no haya expirado, que el monto coincida exactamente con el total en céntimos y que las butacas, la función y los descuentos sigan siendo válidos. Segundo, solicita el cobro a la `PasarelaPago`. Tercero, si el cobro se aprueba, confirma la compra: canjea y otorga puntos, descuenta el stock de dulcería, vende las butacas y registra el uso del cupón. Luego guarda el pago y la compra en una sola transacción; en esa transacción, el trigger `trg_compra_serie` asigna el número de comprobante. Si la pasarela rechaza el pago, el cliente puede reintentarlo o cambiar el medio de pago mientras dure el plazo. Si la base de datos falla después de cobrar, el sistema devuelve el dinero a través de la pasarela y anula la confirmación, de modo que nunca se cobra una compra que no quedó registrada.

### Figura 28 — Crear función
**Líneas de vida:** Administrador · AdminFuncionFormVista · AdminFuncionController · FuncionAdminService · Pelicula · Sede · Sala · SedeRepository
**Mensajes:**
1. Administrador → AdminFuncionFormVista: elige película, sala, fecha, hora e idioma
2. AdminFuncionFormVista → AdminFuncionController → FuncionAdminService: `programarFuncion(admin, pelicula, sala, fecha, hora, idioma)`
3. FuncionAdminService: `exigirAdmin(admin)`
4. FuncionAdminService → Pelicula: `estaEnCartelera()`
5. FuncionAdminService → Sede: `agregarFuncion(funcion)` → Sala: `agregarFuncion(funcion)` → `validarHorarioLibre(...)` (RF-47)
6. FuncionAdminService → SedeRepository: `guardar(sede)` (la BD asigna el id)
7. Retorno `Respuesta<Funcion>` → la vista actualiza la lista

**Alternos:** *[no es administrador]* se rechaza. *[película no está en cartelera, preestreno ni reestreno]* `ReglaNegocioException`. *[cruce de horario o con el tiempo de limpieza]* la sala rechaza la función. *[sala de otra sede]* `IllegalArgumentException`.

> La Figura 28 muestra cómo el administrador programa una nueva función. `FuncionAdminService` verifica primero que el usuario tenga permisos de administración y que la película esté en cartelera, en preestreno o reestrenada. Luego agrega la función a la sede del administrador. La sede comprueba que la sala le pertenezca, y la sala, que el horario no se cruce con otra función ni con el tiempo de limpieza entre funciones (RF-47). Si todo es válido, se guarda la sede con la nueva función y la base de datos le asigna su identificador.

---

## 9. Referencias cruzadas a tablas

| Ubicación | Dice | Debe decir |
|---|---|---|
| «Organización de las tablas» | «tal como se muestra en la Tabla 8» | «…en la **Tabla 13**» |
| «Triggers y eventos programados» | «La Tabla 9 resume los disparadores (triggers) y el evento programado» | «La **Tabla 14** resume los disparadores (triggers) y **los eventos programados**» |
| «Programación orientada a objetos aplicada» | «La Tabla 13 resume…» | «La **Tabla 15** resume…» |
| «Principios SOLID aplicados» | «La Tabla 14 explica…» | «La **Tabla 16** explica…» |
| «Patrón Strategy» | «se resumen en la Tabla 15» | «…en la **Tabla 17**» |
| «Ejemplo comprobado con el código» | «(Tabla 16)» | «(**Tabla 18**)» |

> Si usas *Insertar → Referencia cruzada* de Word en lugar de escribir los números, no volverán a desfasarse.

---

## 10. Erratas menores
- Introducción de la arquitectura: «se adoptó **por** el patrón MVC» → «se adoptó el patrón MVC».
- Figura 21: el título «Strategy: DescuentoBase delega el cálculo en su estrategia» está pegado a la nota y debajo de la imagen. Debe ir antes de la imagen, como en las demás figuras.
