# Especificación de Requerimientos de Software (SRS)

## Sistema de Autoservicio para Cine — Cineplanet

**Versión 2.0 aprobada**

* **Elaborado por:**
  * Alache Barreto, Sebastián Jared
  * Alcarraz Livia, Fernando
  * Chipana Janampa, Ismael
  * Quiroz Bellido, Naomi Salome
* **Docente:** Sánchez Agreda, Zandor
* **Curso:** Análisis, Diseño y Desarrollo de Sistemas
* **Institución:** Universidad Científica del Sur — Ingeniería Empresarial y de Sistemas
* **Fecha:** 26/11/2026
* **Ubicación:** Lima – Perú

---

## Tabla de Contenido

1. Introducción
   1.1 Propósito del Documento
   1.2 Convenciones del Documento
   1.3 Alcance del Proyecto
   1.4 Referencias
2. Descripción General
   2.1 Perspectiva del Producto
   2.2 Clases de Usuarios y Características
   2.3 Entorno Operativo
   2.4 Restricciones de Diseño e Implementación
   2.5 Supuestos y Dependencias
3. Características del Sistema
   3.1 Autenticación
   3.2 Cartelera
   3.3 Entradas
   3.4 Promociones
   3.5 Dulcería
   3.6 Pagos
   3.7 Emisión
   3.8 Postventa
   3.9 Clientes
   3.10 Sede
   3.11 Administración
   3.12 Casos de Operación
   3.13 Matriz Consolidada de Requerimientos Funcionales (RF-01 a RF-48)
4. Requerimientos de Datos
   4.1 Modelo Lógico de Datos
   4.2 Diccionario de Datos
   4.3 Reportes y Salidas del Sistema
   4.4 Adquisición, Integridad, Retención y Eliminación de Datos
5. Requerimientos de Interfaces Externas
   5.1 Interfaces de Usuario
   5.2 Interfaces de Software
   5.3 Interfaces de Hardware
   5.4 Interfaces de Comunicaciones
6. Atributos de Calidad
   6.1 Usabilidad
   6.2 Rendimiento
   6.3 Seguridad
   6.4 Seguridad Operacional e Integridad
   6.5 Otros Atributos Relevantes
   6.6 Matriz Consolidada de RNF y Matriz Cruzada de Atributos de Calidad
7. Requerimientos de Internacionalización y Localización
8. Otros Requerimientos
9. Glosario
10. Modelos de Análisis

---

## Historial de Revisiones

| Nombre | Fecha | Motivo de los Cambios | Versión |
| :--- | :--- | :--- | :--- |
| Alache, S.; Alcarraz, F.; Chipana, I.; Quiroz, N. | 24/09/2026 | Emisión inicial del SRS (RF-01 a RF-42 y RNF-01 a RNF-10). | 1.0 |
| Alache, S.; Alcarraz, F.; Chipana, I.; Quiroz, N. | 26/11/2026 | Alineación integral con el Informe de Avances final: ampliación a RF-01–RF-48 y RNF-01–RNF-12; ajuste de reglas de negocio (bloqueo de 4 min, reintento ante rechazo de pago, reembolso automático por cancelación de función, código temporal de 6 dígitos para invitados, modo cliente para empleados con PIN); completado de secciones 2.4, 4, 5, 7, 8 y 10. | 2.0 |

---

## 1. Introducción

### 1.1 Propósito del Documento

Este documento especifica los requerimientos funcionales y no funcionales del **Sistema de Autoservicio para Cine** (venta autónoma de entradas y productos de dulcería para Cineplanet), versión 2.0, correspondiente al Anexo B del *Informe de Avances Sistema de Cine*. Está dirigido al docente **Sánchez Agreda, Zandor** y al equipo de desarrollo conformado por **Alache Barreto, Sebastián Jared**, **Alcarraz Livia, Fernando**, **Chipana Janampa, Ismael** y **Quiroz Bellido, Naomi Salome**, sirviendo como base técnica formal para el diseño, construcción, verificación y mantenimiento del sistema.

### 1.2 Convenciones del Documento

* **Requisitos de Negocio:** prefijo `RN-XX` (`RN-01` a `RN-04`).
* **Requisitos de Usuario:** prefijo `RU-XX` (`RU-01` a `RU-11`).
* **Casos de Uso:** prefijo `CU-XX` (`CU-01` a `CU-12`).
* **Requerimientos Funcionales:** prefijo `RF-XX` (`RF-01` a `RF-48`), agrupados en 11 módulos según el orden de la Sección 3: *Autenticación, Cartelera, Entradas, Promociones, Dulcería, Pagos, Emisión, Postventa, Clientes, Sede* y *Administración*.
* **Requerimientos No Funcionales:** prefijo `RNF-XX` (`RNF-01` a `RNF-12`), clasificados según su naturaleza en *Internos* y *Externos* en línea con el estándar ISO/IEC 25010.
* **Tipo de Activación (RF):** distingue entre requerimientos **Interactivos** (accionados directamente por el usuario), **No Interactivos** (ejecutados automáticamente por el sistema) y **No Interactivos (Reloj)** (ejecutados automáticamente por vencimiento de tiempo o programación).
* Los nombres exactos de tablas, columnas, estados e identificadores de código fuente se escriben en formato `monoespaciado`.

### 1.3 Alcance del Proyecto

El software especificado es un **sistema de autoservicio en kiosco táctil** para las sedes de Cineplanet en Lima, Perú, que permite al cliente consultar cartelera, elegir sede, función y butacas en tiempo real, adquirir productos de dulcería, aplicar promociones, cupones o puntos de socio, efectuar pagos electrónicos, obtener comprobantes con código QR y gestionar acciones de postventa sin depender de la atención presencial en taquilla.

El proyecto responde directamente a cuatro objetivos estratégicos de negocio (`RN-01` a `RN-04`): reducir el tiempo presencial de compra a menos de 4 minutos por cliente, incrementar la capacidad de atención simultánea en horas pico sin aumentar el personal de taquilla, absorber al menos el 40 % de las ventas mediante autoservicio y reasignar al personal hacia tareas operativas de mayor valor.

Esta versión 2.0 cubre los **48 requerimientos funcionales (`RF-01` a `RF-48`)** y **12 requerimientos no funcionales (`RNF-01` a `RNF-12`)**, incluyendo:

* Flujo completo de compra del cliente (socio registrado e invitado).
* Programa propio de fidelización interna por niveles de socio (niveles 1 a 4), acumulación y canje de puntos y tarifas diferenciadas (`RF-43`, `RF-44`).
* Panel operativo para el **Personal de Dulcería** (gestión de comandas y entrega validada con QR, `CU-08`).
* Módulo de **Administración** (CRUD de películas, funciones con validación de cruce horario + 15 min de limpieza, salas, sedes, promociones, cupones y dulcería, cancelación con reembolso automático y atención de reclamos de su sede, `CU-11`, `CU-12`).

**Exclusiones y alcance académico:** no incluye integración con programas de fidelización externos de terceros, ni venta anticipada mediante aplicación móvil o portal web independiente. Asimismo, por tratarse de un prototipo académico funcional, las integraciones externas de cobro, envío de correos, impresión física y generación estándar de QR operan tras interfaces desacopladas con implementaciones simuladas (`PasarelaSimulada`, `CorreoSimulado`, `CodigoVisual`), documentando su reemplazo para una eventual puesta en producción (Sección 8).

### 1.4 Referencias

| Documento | Autor | Versión / Año | Fecha | Fuente |
| :--- | :--- | :--- | :--- | :--- |
| Informe de Avances Sistema de Cine | Equipo del proyecto | 1.0 | 26/11/2026 | UCSUR – Curso ADDS |
| ISO/IEC 25010:2011 – Systems and software Quality Requirements and Evaluation (SQuaRE) | ISO/IEC | 2011 | — | Estándar internacional |
| ISO 9241-210:2019 – Ergonomics of human-system interaction (Human-centred design) | ISO | 2019 | — | Estándar internacional |
| WCAG 2.1 – Web Content Accessibility Guidelines (Nivel AAA) | W3C | 2.1 | — | Estándar internacional |
| Ley N.° 29733 – Ley de Protección de Datos Personales | Congreso de la República del Perú | 2011 | — | Diario Oficial El Peruano |
| Software Requirements (3.ª ed.) | Wiegers, K. & Beatty, J. | 3.ª ed. | 2013 | Microsoft Press |

---

## 2. Descripción General

### 2.1 Perspectiva del Producto

El sistema sustituye el proceso actual de venta presencial centralizada en taquilla (modelo **AS-IS**, donde el vendedor interviene en 8 actividades del flujo) por un proceso autónomo de autoservicio (**TO-BE**). No constituye una extensión incremental de un software previo, sino una aplicación de escritorio táctil estructurada bajo el patrón **MVC combinado con arquitectura por capas** en **Java 21**, **JavaFX 21** y **MySQL 8 / MariaDB**.

A nivel de contexto (Nivel 0), el sistema actúa como intermediario central entre el **Cliente**, la **Pasarela de Pagos** externa (envía solicitudes de cobro y recibe aprobación o rechazo) y el **Personal de Dulcería** (recibe la comanda confirmada y actualiza su preparación y entrega). El **Administrador** opera sobre el catálogo maestro y la atención de reclamos desde su propio panel autenticado, reflejando los cambios en tiempo real en los kioscos.

> **[Insertar Figura 5 del Informe de Avances: Diagrama de Contexto (nivel 0) del Sistema de Autoservicio para Cine]**

### 2.2 Clases de Usuarios y Características

| Clase de Usuario | Naturaleza / Perfil | Características y Responsabilidades |
| :--- | :--- | :--- |
| **Cliente (Socio registrado)** | Usuario final (clase favorecida) | Asistente al cine que inicia sesión con correo y contraseña (`nivel_socio` 1 a 4). Accede a compra rápida táctil, sugerencia automática de butacas, acumulación y canje de puntos según multiplicador de nivel, tarifas y combos exclusivos de socio, historial («Mis compras»), actualización de datos, calificación y registro/consulta de reclamos. |
| **Cliente (Invitado)** | Usuario final (clase favorecida) | Asistente sin cuenta que compra de forma espontánea ingresando únicamente su correo electrónico válido para recibir el comprobante digital (`es_invitado = 1`, `nivel_socio = 0`). No acumula puntos ni accede a tarifas o productos exclusivos de socio; puede recuperar sus entradas mediante código temporal de 6 dígitos enviado al correo. |
| **Personal de Dulcería** | Usuario interno operativo | Empleado (`nivel_acceso = 1`) asignado a una sede. Visualiza en su panel la cola de comandas en cuatro estados (`PENDIENTE`, `EN_PREPARACION`, `LISTO`, `ENTREGADO`), actualiza el avance de preparación, notifica pedidos listos (`RF-34`) y valida la entrega física mediante código QR o serie del comprobante impidiendo dobles entregas. |
| **Administrador** | Usuario interno de gestión | Empleado (`nivel_acceso = 2`) con acceso al panel administrativo. Realiza CRUD sobre películas, funciones (con validación de cruces y cancelación con reembolso automático), salas y butacas, sedes, promociones, cupones y productos/stock de dulcería por sede; además atiende y resuelve reclamos correspondientes únicamente a su sede (`CU-12`). |
| **Modo Cliente para Empleados** | Capacidad operativa transversal | Tanto el Personal de Dulcería como el Administrador pueden vincular una única cuenta personal de cliente con su mismo DNI (`empleado.id_cliente`) para pasar a **modo cliente** (`Modo.CLIENTE`) desde su panel y realizar compras como socio; para regresar al panel operativo (`Modo.EMPLEADO`) deben ingresar su **PIN de 4 dígitos** (tres intentos fallidos cierran la sesión por seguridad). |

### 2.3 Entorno Operativo

El sistema opera en kioscos físicos de autoservicio instalados en cada sede de Cineplanet en Lima, Perú, conectados mediante red local cableada a un servidor de base de datos centralizado por sede o complejo. De acuerdo con la especificación técnica del proyecto, los requisitos mínimos del entorno son:

| Componente | Kiosco de Autoservicio | Servidor de Base de Datos |
| :--- | :--- | :--- |
| **Hardware** | Pantalla táctil de 21,5 pulgadas o más, resolución 1920 × 1080 px; procesador de 4 núcleos; 8 GB de RAM; 128 GB SSD | Procesador de 4 núcleos; 16 GB de RAM; 256 GB de almacenamiento SSD |
| **Periféricos** | Impresora térmica ESC/POS y terminal POS de pago (`RNF-09`) | Unidad o servicio de respaldo diario |
| **Sistema Operativo** | Windows 10/11 de 64 bits o Linux | Linux o Windows Server |
| **Software** | Aplicación empaquetada con entorno Java 21 y JavaFX (`jpackage`) | MySQL 8.4 LTS o MariaDB LTS vigente *(en desarrollo se probó sobre MariaDB 10.4.32 bajo XAMPP, compatible con el protocolo MySQL)* |
| **Red** | Red local cableada de la sede | Accesible únicamente desde la red interna de los kioscos |

### 2.4 Restricciones de Diseño e Implementación

1. **Lenguaje, interfaz y herramientas:** Java 21 LTS (`release 21` en `pom.xml`), JavaFX 21.0.9 (`javafx-controls` y `javafx-fxml`), tipografía corporativa Manrope (`SIL Open Font License 1.1`), controlador JDBC `MySQL Connector/J 8.4.0`, gestión de dependencias con Maven y pruebas automatizadas con JUnit Jupiter 5.11.0 y Maven Surefire 3.2.5.
2. **Arquitectura MVC por capas e inversión de dependencias:** separación estricta en paquetes bajo `ucsur.pe`: `vista` (51 clases JavaFX, 40 FXML y `estilos.css`), `controller` (14 clases incluyendo `ControllerBase` y `Respuesta`), `service` (16 clases de lógica de aplicación incluyendo `Sesion`, `CuentaPersonal`, `PermisosAdmin`, `ResultadoCancelacion`), `model` (39 clases e interfaces de dominio), `tipo` (4 clases del patrón Strategy), `repository` y `db` (contratos de persistencia y DAOs JDBC), `pasarela`, `correo`, `util` y `exception`. La clase principal `App` actúa como raíz de composición e inyecta dependencias por constructor.
3. **Patrones y precisión técnica:**
   * Patrón **Strategy** (`CalculoDescuento`, `CalculoPorcentaje`, `CalculoMontoFijo`, `FabricaCalculoDescuento`) para cálculo extensible de descuentos.
   * Principios **SOLID** (responsabilidad única en `Compra` delegando en `SeleccionAsientos`, `CalculadoraImporte` y `GestorPuntos`; segregación de interfaces `Activable`, `Totalizable`, `Registrable`, `Descuento`, `DescuentoConUsos`).
   * Aritmética monetaria entera en céntimos (`long`) mediante `ConversorDinero` y almacenamiento `DECIMAL` en BD para eliminar errores de punto flotante.
   * Clases propias de dominio `DateTime` y `Reloj` para evitar dependencias externas en el cálculo de vigencias y plazos.
4. **Motor de base de datos:** esquema relacional `DB_Cine` en InnoDB normalizado hasta la **Tercera Forma Normal (3FN)** con 26 tablas, 4 disparadores (`triggers`) y 2 eventos programados (`events`).
5. **Restricciones normativas y de negocio:** precios al público con **IGV del 18 % incluido** y desglosado en el comprobante (`Operación Gravada + IGV = Total`); Libro de Reclamaciones digital con máximo de 500 caracteres; validación de DNI de 8 dígitos, teléfono peruano de 9 dígitos y edad entre 15 y 110 años; cumplimiento de **PCI-DSS / TLS 1.3** y **Ley N.° 29733** en despliegue productivo.
6. **Alcance académico:** las contraseñas en el prototipo se almacenan sin hash y los proveedores externos (`PasarelaPago`, `ServicioCorreo`, `CodigoVisual`) utilizan implementaciones simuladas intercambiables sin modificar la capa de servicio.

### 2.5 Supuestos y Dependencias

* **Disponibilidad de servicios externos:** en producción, la pasarela de pagos (`PasarelaPago`) y el servicio de correo transaccional (`ServicioCorreo`) deben cumplir con un SLA compatible con la disponibilidad del 99,5 % (`RNF-05`).
* **Conectividad de red y sincronización horaria:** se asume red local estable entre los kioscos, el servidor MySQL e impresoras térmicas. El servidor de BD debe mantener activo el programador de eventos (`SET GLOBAL event_scheduler = ON`) y configurado en la zona horaria oficial de Perú (`UTC-5` / `-05:00`) para que la expiración de bloqueos a los 4 minutos y el estreno automático a las 00:05 operen con exactitud.
* **Actualización operativa del catálogo:** Cineplanet es responsable de mantener actualizadas las películas, horarios de funciones, precios, promociones, cupones y stock de dulcería por sede mediante el módulo administrativo.
* **Infraestructura mínima por sede:** cada sede cuenta con al menos un kiosco táctil operativo y un punto de entrega de dulcería con acceso al panel de comandas.

---

## 3. Características del Sistema

Organizadas en los 11 módulos del sistema, detallando prioridad, casos de uso asociados, secuencia estímulo/respuesta y requerimientos funcionales (`RF-01` a `RF-48`).

### 3.1 Autenticación

* **Descripción y Prioridad:** Permite al cliente registrarse como socio, iniciar sesión con correo y contraseña, recuperar su contraseña por código temporal o continuar como invitado ingresando únicamente su correo; también autentica a empleados y administradores según su rol. **Prioridad:** Alta.
* **Casos de Uso:** `CU-01`, `CU-02`.
* **Secuencia de Estímulo/Respuesta:**
  1. El usuario elige *Iniciar sesión*, *Crear cuenta* o *Continuar como invitado* (solo cliente).
  2. Si inicia sesión, ingresa correo y contraseña; el sistema valida que la cuenta exista, esté activa y coincida la clave, redirigiendo a cartelera, panel de dulcería o administración según su rol.
  3. Si solicita *Recuperar contraseña*, el sistema envía un código temporal al correo registrado y permite definir una nueva clave de mínimo 8 caracteres.
  4. Si crea una cuenta, ingresa DNI (8 dígitos), nombres, apellidos, fecha de nacimiento (15–110 años), correo, teléfono (9 dígitos), contraseña (≥ 8 caracteres) y aceptación de términos; el sistema valida formato y unicidad (`RNF-12`) y crea la cuenta con `nivel_socio = 1`, `puntos = 0`.
  5. Si continúa como invitado, ingresa únicamente un correo válido para el envío del comprobante.
* **Requerimientos Funcionales:**
  * **RF-01:** Permitir al cliente iniciar sesión con correo y contraseña, registrarse con su DNI (8 dígitos) o continuar como invitado con su correo. *(Interactivo)*

### 3.2 Cartelera

* **Descripción y Prioridad:** Permite consultar las películas vigentes y en preestreno disponibles en la sede seleccionada, aplicar filtros y visualizar detalles técnicos, sinopsis y tráiler. **Prioridad:** Alta.
* **Casos de Uso:** `CU-03`.
* **Secuencia de Estímulo/Respuesta:**
  1. El cliente accede a la cartelera filtrada por la sede elegida; el sistema despliega las películas, formatos y horarios en tiempo real.
  2. Opcionalmente, el cliente filtra por género, idioma, clasificación o búsqueda por título; si no hay resultados, el sistema informa y ofrece «Limpiar filtros».
  3. Al elegir una película, el sistema muestra sinopsis, duración, clasificación, tráiler y horarios agrupados por tipo de sala.
* **Requerimientos Funcionales:**
  * **RF-02:** Mostrar la lista de películas, formatos (2D, 3D, XD, etc.) y horarios en tiempo real. *(Interactivo)*
  * **RF-03:** Filtrar funciones por idioma, clasificación o género (además de búsqueda por título y limpieza de filtros). *(Interactivo)*
  * **RF-04:** Desplegar detalles, duración, clasificación, sinopsis y tráiler. *(Interactivo)*

### 3.3 Entradas

* **Descripción y Prioridad:** Gestiona la selección de función, tipos y cantidad de entradas, mapa visual de sala, recomendación de mejores butacas, bloqueo temporal de 4 minutos, cancelación voluntaria y liberación por vencimiento. **Prioridad:** Alta.
* **Casos de Uso:** `CU-05`.
* **Secuencia de Estímulo/Respuesta:**
  1. El cliente elige fecha, hora y sala de una función disponible; el sistema inicia el contador regresivo de **4 minutos** (`expira_compra`).
  2. El cliente selecciona tipo y cantidad de boletos según tarifas habilitadas (incluyendo exclusivas para socios o cumpleaños).
  3. El sistema despliega el plano de sala con butacas libres, ocupadas, preferenciales y discapacidad, y sugiere automáticamente los mejores asientos contiguos.
  4. El cliente selecciona sus butacas (o acepta la sugerencia) y confirma el resumen; el sistema bloquea temporalmente los asientos en BD garantizando unicidad (`RNF-10`).
  5. Si el cliente cancela antes de pagar o vence el plazo de 4 minutos, el sistema marca la compra como `CANCELADA` o `EXPIRADA` y libera las butacas.
* **Requerimientos Funcionales:**
  * **RF-05:** Elegir fecha, hora y sala de la película. *(Interactivo)*
  * **RF-06:** Seleccionar cantidad de boletos por tarifa (General, Niño, Adulto Mayor, Estudiante, Conadis, Socio, Cumpleañero o Canje de puntos). *(Interactivo)*
  * **RF-07:** Presentar plano visual de la sala con estado de asientos. *(No Interactivo)*
  * **RF-08:** Escoger asientos libres en pantalla táctil. *(Interactivo)*
  * **RF-09:** Retener asientos seleccionados durante la transacción. *(No Interactivo)*
  * **RF-10:** Desbloquear las butacas y expirar la compra si se superan **4 min** sin completar el pago. *(No Interactivo – Reloj)*
  * **RF-29:** Sugerir mejores asientos disponibles automáticamente. *(Interactivo)*
  * **RF-30:** Mostrar resumen antes de continuar al pago. *(Interactivo)*
  * **RF-46:** Permitir al cliente cancelar la compra antes de pagar, liberando las butacas bloqueadas. *(Interactivo)*

### 3.4 Promociones

* **Descripción y Prioridad:** Permite consultar las promociones activas por vigencia, día de la semana y nivel de socio, y aplicar cupones o promociones al pedido. **Prioridad:** Media.
* **Casos de Uso:** `CU-04`, `CU-07`.
* **Secuencia de Estímulo/Respuesta:**
  1. El cliente consulta la sección de promociones o las opciones aplicables en la pantalla de resumen y pago.
  2. El sistema filtra promociones vigentes según la fecha actual, día de la semana y nivel de socio del cliente (para invitados solo muestra `nivel_socio_minimo = 0`).
  3. El cliente ingresa un código promocional o selecciona una promoción; el sistema valida vigencia, límite de usos y nivel mínimo, aplicando el descuento correspondiente o notificando el motivo si no es válido.
* **Requerimientos Funcionales:**
  * **RF-11:** Ingresar y aplicar cupones/promociones, validando su vigencia y su límite de usos. *(Interactivo)*
  * **RF-28:** Mostrar promociones y beneficios disponibles. *(Interactivo)*

### 3.5 Dulcería

* **Descripción y Prioridad:** Permite explorar el catálogo de productos y combos de la sede, personalizar opciones, gestionar el carrito validando stock y nivel mínimo de socio, transmitir la comanda al aprobarse el pago y gestionar la cola de preparación hasta la entrega con notificación de pedido listo. **Prioridad:** Alta.
* **Casos de Uso:** `CU-06`, `CU-08`.
* **Secuencia de Estímulo/Respuesta:**
  1. El cliente visualiza el menú de dulcería por categorías (combos, snacks, bebidas y exclusivos de socio) con precios y stock de su sede, o elige *Continuar sin dulcería*.
  2. Personaliza sabores/tamaños y agrega, modifica o elimina ítems del carrito; el sistema verifica stock en `inventario_sede` y nivel de socio requerido.
  3. Al confirmarse el pago, el sistema descuenta el inventario de la sede y envía la comanda en estado `PENDIENTE` al panel de dulcería.
  4. El personal de dulcería cambia el estado a `EN_PREPARACION` y luego a `LISTO` (notificando al cliente); al recoger el pedido, valida el código QR o serie del comprobante y marca `ENTREGADO`, impidiendo entregas duplicadas.
* **Requerimientos Funcionales:**
  * **RF-12:** Mostrar productos, combos y precios. *(Interactivo)*
  * **RF-13:** Agregar productos al carrito y ajustar opciones. *(Interactivo)*
  * **RF-14:** Agregar/reducir/eliminar ítems del carrito. *(Interactivo)*
  * **RF-15:** Enviar pedido confirmado al personal de dulcería. *(No Interactivo)*
  * **RF-31:** Verificar stock antes de agregar al carrito. *(No Interactivo)*
  * **RF-34:** Notificar al cliente que su pedido está listo. *(No Interactivo)*

### 3.6 Pagos

* **Descripción y Prioridad:** Calcula el importe total aplicando la estrategia de descuento y desglosando el IGV incluido, permite seleccionar el medio de pago electrónico y gestiona la comunicación y reintentos con la pasarela externa. **Prioridad:** Alta.
* **Casos de Uso:** `CU-07`.
* **Secuencia de Estímulo/Respuesta:**
  1. El sistema calcula el subtotal (con precios que ya incluyen IGV), aplica descuentos de promoción y cupón mediante el patrón Strategy, y desglosa la operación gravada y el IGV (18 %).
  2. El cliente elige el medio de pago (`TARJETA_CREDITO`, `TARJETA_DEBITO`, `YAPE`, `PLIN` o `PAGO_QR`).
  3. El sistema verifica que la compra siga vigente y envía la solicitud de cobro a la pasarela externa, comprobando su estado (`APROBADO`, `RECHAZADO`, `PENDIENTE`).
  4. Si el pago es rechazado, el sistema registra la transacción como `RECHAZADO` con su motivo y **mantiene la compra `EN_PROCESO` y las butacas retenidas** para permitir reintentar o cambiar de método mientras no venzan los 4 minutos (o el cliente cancele).
* **Requerimientos Funcionales:**
  * **RF-16:** Elegir tarjeta o billetera digital/QR (`TARJETA_CREDITO`, `TARJETA_DEBITO`, `YAPE`, `PLIN`, `PAGO_QR`). *(Interactivo)*
  * **RF-17:** Transmitir la petición de cobro a la pasarela externa. *(No Interactivo)*
  * **RF-18:** Notificar el rechazo con su motivo; la compra sigue en proceso para reintentar el pago y las butacas se liberan al vencer el plazo o si el cliente cancela. *(No Interactivo)*
  * **RF-32:** Comprobar si la transacción fue aprobada/rechazada/pendiente. *(No Interactivo)*
  * **RF-45:** Calcular el subtotal, con los precios al público (IGV incluido), aplicar los descuentos para obtener el total y desglosar el IGV del 18 %. *(No Interactivo)*

### 3.7 Emisión

* **Descripción y Prioridad:** Confirma atómicamente la venta, genera el comprobante con serie correlativa y códigos QR, permite imprimir y enviar el PDF por correo, y habilita la recuperación segura de entradas para socios e invitados. **Prioridad:** Alta.
* **Casos de Uso:** `CU-07`, `CU-09`.
* **Secuencia de Estímulo/Respuesta:**
  1. Aprobado el pago, el sistema ejecuta una transacción atómica (`RNF-10`) que marca la compra como `CONFIRMADA`, asigna serie correlativa (`B001-XXXXXX`), marca las butacas como `VENDIDA`, descuenta stock de dulcería, actualiza usos del cupón y otorga/descuenta puntos de socio.
  2. Genera el comprobante con códigos QR únicos para cada entrada y pedido de dulcería, habilita la vista previa e impresión térmica y envía el comprobante y entradas en PDF al correo del cliente.
  3. Al seleccionar *Volver al inicio* con sesión activa, consulta si desea cerrar sesión o seguir comprando.
  4. Para recuperar entradas posteriormente: el socio accede a *Mis compras*; el invitado ingresa su correo en *Recuperar entradas* y recibe un código temporal de 6 dígitos (vigente 10 min, un solo uso, máx. 5 intentos fallidos, respuesta genérica para no revelar correos registrados).
* **Requerimientos Funcionales:**
  * **RF-19:** Confirmar transacción, descontar stock, marcar asientos ocupados. *(No Interactivo)*
  * **RF-20:** Generar la boleta/factura con serie correlativa; cada entrada y pedido lleva su propio código QR. *(No Interactivo)*
  * **RF-21:** Enviar orden de impresión al dispensador térmico. *(No Interactivo)*
  * **RF-22:** Enviar boleta y entradas en PDF al correo. *(No Interactivo)*
  * **RF-33:** Recuperar entradas con sesión mediante *Mis compras* o validando correo con código temporal de 6 dígitos si es invitado. *(Interactivo)*

### 3.8 Postventa

* **Descripción y Prioridad:** Permite al cliente registrado calificar su experiencia, presentar quejas o reclamos en el Libro de Reclamaciones digital, consultar el estado y respuesta de sus reclamos, y gestiona el reembolso automático cuando una función es cancelada. **Prioridad:** Media.
* **Casos de Uso:** `CU-10`, `CU-11`, `CU-12`.
* **Secuencia de Estímulo/Respuesta:**
  1. Desde *Mis compras*, el socio elige *Calificar* (1 a 5 estrellas y comentario opcional de hasta 300 caracteres; una calificación por compra `CONFIRMADA`) o *Reclamo* (en compras `CONFIRMADA` o `REEMBOLSADA`).
  2. Para registrar un reclamo, selecciona tipo (`ATENCION`, `PRODUCTO_DULCERIA`, `SALA_PROYECCION`) y redacta descripción (no vacía y ≤ 500 caracteres); queda en estado `REGISTRADO`.
  3. El cliente consulta en línea el estado de sus reclamos (`REGISTRADO`, `EN_REVISION`, `RESUELTO`, `RECHAZADO`) y la respuesta enviada también a su correo.
  4. Si el administrador cancela una función (`RF-48`), el sistema ejecuta automáticamente el reembolso por pasarela de las compras pagadas (si la dulcería ya fue `ENTREGADO`, solo reembolsa el monto de las entradas), revierte puntos y usos de cupón, y cambia la compra a `REEMBOLSADA`.
* **Requerimientos Funcionales:**
  * **RF-23:** Formulario digital para reclamos/quejas (Libro de Reclamaciones). *(Interactivo)*
  * **RF-24:** Reembolso automático por cancelación de función; si se entregó dulcería solo se reembolsan entradas. *(No Interactivo)*
  * **RF-25:** Valorar usabilidad y atención en el módulo táctil (1 a 5 estrellas). *(Interactivo)*
  * **RF-35:** Consultar estado y respuesta de reclamos previos. *(Interactivo)*

### 3.9 Clientes

* **Descripción y Prioridad:** Administra el perfil del cliente registrado, su historial de compras, el programa de niveles de socio (1 a 4) y la acumulación y canje de puntos. **Prioridad:** Media.
* **Casos de Uso:** `CU-04`, `CU-09`.
* **Secuencia de Estímulo/Respuesta:**
  1. El socio accede a *Mi cuenta* para revisar su nivel actual (1 a 4), multiplicador de puntos, visitas confirmadas en el año, saldo de puntos disponibles y actualizar teléfono o correo.
  2. Accede a *Mis compras* para visualizar sus transacciones pasadas con detalle de entradas, dulcería y estado.
  3. Al confirmar una compra, el sistema suma puntos según las entradas/combos multiplicados por el factor de su nivel y recalcula automáticamente si asciende de nivel; al canjear puntos por entradas, descuenta el saldo correspondiente.
* **Requerimientos Funcionales:**
  * **RF-26:** Ver entradas y productos adquiridos anteriormente (*Mis compras*). *(Interactivo)*
  * **RF-27:** Registrar/actualizar datos personales y correo. *(Interactivo)*
  * **RF-43:** Otorgar puntos por las entradas y combos comprados y permitir canjearlos por entradas, aplicando el multiplicador del nivel de socio. *(Interactivo)*
  * **RF-44:** Actualizar el nivel de socio (1 a 4) según las visitas del año y restringir las entradas y combos exclusivos al nivel requerido. *(No Interactivo)*

### 3.10 Sede

* **Descripción y Prioridad:** Permite al cliente seleccionar la sede de Cineplanet donde realizará la compra y garantiza la coherencia territorial de cartelera, salas, butacas, inventario, comandas de dulcería y reclamos. **Prioridad:** Alta.
* **Casos de Uso:** `CU-03`, `CU-08`, `CU-12`.
* **Secuencia de Estímulo/Respuesta:**
  1. Al iniciar la compra, el cliente selecciona una sede activa (con búsqueda por nombre o distrito).
  2. El sistema filtra automáticamente cartelera, horarios, salas, stock de dulcería y promociones para esa sede, validando en base de datos la coherencia sala–función–sede.
  3. En los paneles internos, las comandas de dulcería y los reclamos visibles quedan restringidos a la sede a la que pertenece el empleado o administrador.
* **Requerimientos Funcionales:**
  * **RF-36:** Permitir al cliente elegir la sede/local de Cineplanet donde desea comprar; la cartelera, horarios y disponibilidad se filtran según la sede seleccionada. *(Interactivo)*

### 3.11 Administración

* **Descripción y Prioridad:** Provee al administrador un panel de gestión integral del catálogo operativo, programación de funciones con control de cruces horarios y estados, control de stock por sede y atención formal de reclamos de su sede. **Prioridad:** Alta.
* **Casos de Uso:** `CU-11`, `CU-12`.
* **Secuencia de Estímulo/Respuesta:**
  1. El administrador inicia sesión (`nivel_acceso = 2`) y selecciona el módulo correspondiente: Películas, Funciones, Salas, Promociones y Cupones, Productos de Dulcería, Sedes o Reclamos.
  2. Al programar o editar funciones, el sistema impide guardar si existe cruce horario en la misma sala considerando la duración de la película más **15 minutos de limpieza**.
  3. Al cancelar una función, el sistema dispara el reembolso automático (`RF-24`) y permite reintentar reembolsos pendientes en caso de falla de pasarela.
  4. En *Reclamos*, visualiza únicamente los de su sede, pasa un caso `REGISTRADO` a `EN_REVISION`, registra la respuesta obligatoria y lo cierra como `RESUELTO` o `RECHAZADO` enviando correo al cliente.
* **Requerimientos Funcionales:**
  * **RF-37:** Permitir al administrador crear, consultar, actualizar y dar de baja películas del catálogo. *(Interactivo)*
  * **RF-38:** Permitir al administrador programar, consultar, actualizar y eliminar funciones. *(Interactivo)*
  * **RF-39:** Permitir al administrador crear, consultar, actualizar y eliminar salas y su capacidad por sede. *(Interactivo)*
  * **RF-40:** Permitir al administrador crear, consultar, actualizar y eliminar promociones y sus cupones asociados. *(Interactivo)*
  * **RF-41:** Permitir al administrador crear, consultar, actualizar y eliminar productos del catálogo de dulcería, incluyendo precio y stock por sede. *(Interactivo)*
  * **RF-42:** Permitir al administrador crear, consultar, actualizar y eliminar las sedes/locales de Cineplanet. *(Interactivo)*
  * **RF-47:** Impedir que dos funciones se crucen en la misma sala, considerando 15 minutos de limpieza entre funciones. *(No Interactivo)*
  * **RF-48:** Actualizar el estado de la función (`DISPONIBLE`, `AGOTADA`, `EN_CURSO`, `FINALIZADA` o `CANCELADA`); la cancelación habilita el reembolso automático. *(No Interactivo)*

### 3.12 Casos de Operación

Además de los casos de uso interactivos, el sistema contempla seis procesos operativos automáticos o de operación continua:

1. **Operación normal:** disponibilidad continua del kiosco durante el horario de atención de la sede.
2. **Bloqueo y liberación automática de asientos:** retención temporal y liberación automática cada 1 minuto mediante el evento `ev_expirar_compras` al superar los 4 minutos sin pago (`RF-09`, `RF-10`).
3. **Estreno automático de películas:** evento diario `ev_estrenar_peliculas` a las `00:05 (UTC-5)` que actualiza de `PREESTRENO` a `EN_CARTELERA` las películas cuya `fecha_estreno <= CURDATE()` (`RF-02`, `RF-37`).
4. **Cierre de sesión por inactividad:** cancelación automática de sesión, cancelación de compra en curso (liberando butacas) y limpieza de pantalla tras 3 minutos sin interacción (`RNF-04`).
5. **Mantenimiento administrativo en línea:** actualización inmediata de catálogo, funciones y stock desde el módulo administrativo sin detener el servicio de los kioscos.
6. **Registro de fallas y contingencia operativa:** bitácora (`logging`) ante pérdidas de comunicación con impresora térmica o pasarela de pagos (`RNF-06`).

### 3.13 Matriz Consolidada de Requerimientos Funcionales (RF-01 a RF-48)

| Código | Módulo | Nombre del Requerimiento | Descripción | Tipo de Activación |
| :--- | :--- | :--- | :--- | :--- |
| **RF-01** | Autenticación | Identificación / Registro de Cliente | Permitir al cliente iniciar sesión con correo y contraseña, registrarse con su DNI (8 dígitos) o continuar como invitado con su correo. | Interactivo |
| **RF-02** | Cartelera | Consulta de Cartelera | Mostrar la lista de películas, formatos (2D, 3D, XD) y horarios en tiempo real. | Interactivo |
| **RF-03** | Cartelera | Filtro y Búsqueda Avanzada | Filtrar funciones por idioma, clasificación o género. | Interactivo |
| **RF-04** | Cartelera | Consulta de Sinopsis y Tráiler | Desplegar detalles, duración, clasificación y tráiler. | Interactivo |
| **RF-05** | Entradas | Selección de Función | Elegir fecha, hora y sala de la película. | Interactivo |
| **RF-06** | Entradas | Selección de Tipo de Entradas | Seleccionar cantidad de boletos por tarifa (General, Niño, Adulto Mayor, Estudiante, Conadis, Socio, Cumpleañero, Canje). | Interactivo |
| **RF-07** | Entradas | Despliegue del Mapa de Sala | Presentar plano visual de la sala con estado de asientos. | No Interactivo |
| **RF-08** | Entradas | Selección Manual de Asientos | Escoger asientos libres en pantalla táctil. | Interactivo |
| **RF-09** | Entradas | Bloqueo Temporal de Asientos | Retener asientos seleccionados durante la transacción. | No Interactivo |
| **RF-10** | Entradas | Liberación por Timeout | Desbloquear las butacas y expirar la compra si se superan 4 min sin completar el pago. | No Interactivo (Reloj) |
| **RF-11** | Promociones | Validación de Código Promocional | Ingresar y aplicar cupones/promociones, validando su vigencia y su límite de usos. | Interactivo |
| **RF-12** | Dulcería | Despliegue de Menú Dulcería | Mostrar productos, combos y precios. | Interactivo |
| **RF-13** | Dulcería | Selección y Personalización | Agregar productos al carrito y ajustar opciones. | Interactivo |
| **RF-14** | Dulcería | Edición de Carrito de Compra | Agregar/reducir/eliminar ítems del carrito. | Interactivo |
| **RF-15** | Dulcería | Transmisión de Comanda a Dulcería | Enviar pedido confirmado al personal de dulcería. | No Interactivo |
| **RF-16** | Pagos | Selección del Medio de Pago | Elegir tarjeta o billetera digital/QR. | Interactivo |
| **RF-17** | Pagos | Solicitud a Pasarela de Pagos | Transmitir la petición de cobro a la pasarela externa. | No Interactivo |
| **RF-18** | Pagos | Gestión de Transacción Fallida | Notificar el rechazo con su motivo; la compra sigue en proceso para reintentar el pago y las butacas se liberan al vencer el plazo o si el cliente cancela. | No Interactivo |
| **RF-19** | Emisión | Registro de Venta en BD | Confirmar transacción, descontar stock, marcar asientos ocupados. | No Interactivo |
| **RF-20** | Emisión | Generación de Comprobante Digital | Generar la boleta/factura con serie correlativa; cada entrada y pedido lleva su propio código QR. | No Interactivo |
| **RF-21** | Emisión | Impresión Física de Tickets | Enviar orden de impresión al dispensador térmico. | No Interactivo |
| **RF-22** | Emisión | Envío de Comprobante por Correo | Enviar boleta y entradas en PDF al correo. | No Interactivo |
| **RF-23** | Postventa | Libro de Reclamaciones | Formulario digital para reclamos/quejas. | Interactivo |
| **RF-24** | Postventa | Reembolso automático por cancelación | Procede automáticamente cuando la función es cancelada; si se entregó dulcería solo se reembolsan entradas. | No Interactivo |
| **RF-25** | Postventa | Calificación de Experiencia | Valorar usabilidad y atención en el módulo táctil. | Interactivo |
| **RF-26** | Clientes | Consulta de Historial de Compras | Ver entradas y productos adquiridos anteriormente. | Interactivo |
| **RF-27** | Clientes | Gestión de Datos del Cliente | Registrar/actualizar datos personales y correo. | Interactivo |
| **RF-28** | Promociones | Consulta de Promociones Vigentes | Mostrar promociones y beneficios disponibles. | Interactivo |
| **RF-29** | Entradas | Recomendación de Asientos | Sugerir mejores asientos disponibles automáticamente. | Interactivo |
| **RF-30** | Entradas | Confirmación de Selección | Mostrar resumen antes de continuar al pago. | Interactivo |
| **RF-31** | Dulcería | Consulta de Disponibilidad de Productos | Verificar stock antes de agregar al carrito. | No Interactivo |
| **RF-32** | Pagos | Verificación del Estado del Pago | Comprobar si la transacción fue aprobada/rechazada/pendiente. | No Interactivo |
| **RF-33** | Emisión | Recuperación de Entrada Digital | Recuperar entradas con sesión mediante *Mis compras* o validando correo con código de 6 dígitos si es invitado. | Interactivo |
| **RF-34** | Dulcería | Notificación de Pedido Listo | Notificar al cliente que su pedido está listo. | No Interactivo |
| **RF-35** | Postventa | Consulta del Estado del Reclamo | Consultar estado de reclamos previos. | Interactivo |
| **RF-36** | Sede | Selección de Sede | Permitir al cliente elegir la sede/local de Cineplanet donde desea comprar; la cartelera, horarios y disponibilidad se filtran según la sede seleccionada. | Interactivo |
| **RF-37** | Administración | Gestión de Películas | Permitir al administrador crear, consultar, actualizar y dar de baja películas del catálogo. | Interactivo |
| **RF-38** | Administración | Gestión de Funciones | Permitir al administrador programar, consultar, actualizar y eliminar funciones. | Interactivo |
| **RF-39** | Administración | Gestión de Salas | Permitir al administrador crear, consultar, actualizar y eliminar salas y su capacidad por sede. | Interactivo |
| **RF-40** | Administración | Gestión de Promociones y Cupones | Permitir al administrador crear, consultar, actualizar y eliminar promociones y sus cupones asociados. | Interactivo |
| **RF-41** | Administración | Gestión de Productos de Dulcería | Permitir al administrador crear, consultar, actualizar y eliminar productos del catálogo de dulcería, incluyendo precio y stock. | Interactivo |
| **RF-42** | Administración | Gestión de Sedes | Permitir al administrador crear, consultar, actualizar y eliminar las sedes/locales de Cineplanet. | Interactivo |
| **RF-43** | Clientes | Acumulación y Canje de Puntos | Otorgar puntos por las entradas y combos comprados y permitir canjearlos por entradas, aplicando el multiplicador del nivel de socio. | Interactivo |
| **RF-44** | Clientes | Niveles de Socio y Beneficios | Actualizar el nivel de socio (1 a 4) según las visitas del año y restringir las entradas y combos exclusivos al nivel requerido. | No Interactivo |
| **RF-45** | Pagos | Cálculo del Importe | Calcular el subtotal, con los precios al público (IGV incluido), aplicar los descuentos para obtener el total y desglosar el IGV del 18 %. | No Interactivo |
| **RF-46** | Entradas | Cancelación de la Compra | Permitir al cliente cancelar la compra antes de pagar, liberando las butacas bloqueadas. | Interactivo |
| **RF-47** | Administración | Validación de Horarios de Funciones | Impedir que dos funciones se crucen en la misma sala, considerando 15 minutos de limpieza entre funciones. | No Interactivo |
| **RF-48** | Administración | Gestión de Estados de Función | Actualizar el estado de la función (`DISPONIBLE`, `AGOTADA`, `EN_CURSO`, `FINALIZADA` o `CANCELADA`); la cancelación habilita el reembolso. | No Interactivo |

---

## 4. Requerimientos de Datos

### 4.1 Modelo Lógico de Datos

La base de datos `DB_Cine` transforma el modelo conceptual entidad–relación en un esquema relacional normalizado hasta la **Tercera Forma Normal (3FN)** compuesto por **26 tablas** implementadas en MySQL 8 / MariaDB (motor InnoDB) mediante el script `bd_cine.sql`.

> **[Insertar Figura 9 del Informe de Avances: Diseño Lógico de la base de datos del Sistema de Autoservicio para Cine]**

| Bloque Funcional | Cantidad | Tablas | Propósito |
| :--- | :---: | :--- | :--- |
| **Catálogos y configuración** | 11 | `rol`, `sede`, `genero`, `tipo_sala`, `tipo_entrada`, `nivel_socio`, `promocion`, `cupon`, `producto`, `pelicula`, `correlativo` | Datos maestros gestionados por el administrador, reglas de niveles de socio y control de numeración de comprobantes. |
| **Usuarios y estructura física** | 4 | `cliente`, `empleado`, `sala`, `butaca` | Personas que interactúan con el sistema e infraestructura física de salas y asientos por sede. |
| **Operaciones principales** | 5 | `funcion`, `compra`, `entrada`, `transaccion_pago`, `pedido` | Núcleo transaccional: funciones, compras, entradas bloqueadas/vendidas, pagos y pedidos de dulcería. |
| **Servicio postventa** | 2 | `reclamo`, `calificacion` | Reclamos de atención/dulcería/sala y calificaciones de la experiencia (`RF-23`, `RF-25`, `RF-35`). |
| **Tablas intermedias** | 4 | `pelicula_genero`, `promocion_dia`, `inventario_sede`, `detalle_pedido` | Resolución de relaciones M:N: géneros por película, días de vigencia de promociones, stock por sede y detalle de pedidos. |
| **Total** | **26** | — | — |

**Evidencia de normalización (1FN a 3FN):**

* **1FN:** separación de listas multivaluadas en tablas propias (`pelicula_genero`, `promocion_dia`, `detalle_pedido`).
* **2FN:** en `inventario_sede (id_sede, id_producto)` se almacena únicamente `stock`, manteniendo nombre y precio en `producto`.
* **3FN:** aislamiento de catálogos (`sede`, `tipo_sala`, `rol`, `nivel_socio`) referenciados por clave foránea para actualizar recargos o multiplicadores en un solo registro.

### 4.2 Diccionario de Datos

A continuación se resumen las 26 tablas del esquema `DB_Cine`, sus claves y restricciones de dominio verificadas en `bd_cine.sql`:

| Tabla | PK | FKs principales | Campos clave y dominios / columnas virtuales |
| :--- | :--- | :--- | :--- |
| `rol` | `id_rol` | — | `nombre` (UQ), `nivel_acceso` (`1` = dulcería, `2` = administrador) |
| `sede` | `id_sede` | — | `nombre` (UQ), `direccion`, `distrito`, `estado` (`ACTIVA`, `INACTIVA`, `EN_MANTENIMIENTO`) |
| `genero` | `id_genero` | — | `nombre` (`VARCHAR(20)`, UQ) |
| `tipo_sala` | `id_tipo_sala` | — | `formato` (`VARCHAR(30)`, UQ), `tarifa_adicional` (`DECIMAL(6,2) >= 0`) |
| `tipo_entrada` | `id_tipo_entrada` | — | `nombre` (UQ), `tarifa` (`DECIMAL(6,2)`), `puntos_por_entrada`, `requiere_socio`, `puntos_canje`, `solo_cumpleanos`, `estado` |
| `promocion` | `id_promocion` | — | `tipo_descuento` (`PORCENTAJE`, `MONTO_FIJO`), `valor`, `fecha_inicio`, `fecha_fin`, `nivel_socio_minimo` (0–4), `estado` |
| `cupon` | `id_cupon` | — | `codigo` (UQ), `tipo_descuento`, `descuento`, `limite_uso`, `usos_realizados <= limite_uso`, `nivel_socio_minimo` (0–4), `estado` |
| `nivel_socio` | `id_nivel` | — | `id_nivel` (0 = invitado, 1–4 = socio), `nombre` (UQ), `visitas_minimas`, `multiplicador_puntos` (`DECIMAL(4,2)`) |
| `producto` | `id_producto` | `nivel_socio_minimo` | `nombre` (UQ), `categoria`, `precio` (`DECIMAL(8,2)`), `es_combo`, `imagen_url`, `estado` |
| `pelicula` | `id_pelicula` | — | `titulo`, `duracion_min > 0`, `clasificacion_edad`, `fecha_estreno`, `estado` (`PROXIMAMENTE`, `PREESTRENO`, `EN_CARTELERA`, `REESTRENO`, `FUERA_DE_CARTELERA`) |
| `correlativo` | `serie` | — | `serie` (`CHAR(4)`, inicial `'B001'`), `ultimo_numero >= 0` |
| `cliente` | `id_cliente` | `nivel_socio` | `dni` (`CHAR(8)`, UQ), `correo`, `contra`, `puntos >= 0`, `es_invitado`, columna virtual `correo_unico` (`CASE WHEN es_invitado = 0 THEN correo ELSE NULL END`, UQ) |
| `empleado` | `id_empleado` | `id_rol`, `id_sede`, `id_cliente` | `dni` (UQ), `correo` (UQ), `pin` (`CHAR(4)`, 4 dígitos), `id_cliente` (UQ opcional para cuenta personal) |
| `sala` | `id_sala` | `id_sede`, `id_tipo_sala` | `numero` (UQ por sede), `capacidad > 0`, `estado` (`ACTIVA`, `INACTIVA`, `EN_MANTENIMIENTO`) |
| `butaca` | `id_butaca` | `id_sala` | `fila`, `numero` (UQ por sala), `tipo` (`ESTANDAR`, `PREFERENCIAL`, `DISCAPACIDAD`), `bloque` (`IZQUIERDA`, `CENTRO`, `DERECHA`) |
| `funcion` | `id_funcion` | `id_pelicula`, `id_sala` | `fecha`, `hora_inicio`, `idioma`, `estado` (`DISPONIBLE`, `AGOTADA`, `EN_CURSO`, `FINALIZADA`, `CANCELADA`), columna virtual `horario_activo` (UQ sala+fecha+hora si no cancelada) |
| `compra` | `id_compra` | `id_cliente`, `id_sede`, `id_cupon`, `id_promocion` | `expira_compra` (`+4 min`), `serie_numero` (`B001-XXXXXX`, UQ), `subtotal`, `descuento`, `igv`, `total`, `puntos_canjeados`, `puntos_otorgados`, `monto_reembolsado`, `estado` (`EN_PROCESO`, `CONFIRMADA`, `CANCELADA`, `EXPIRADA`, `REEMBOLSADA`) |
| `entrada` | `id_entrada` | `id_compra`, `id_funcion`, `id_butaca`, `id_tipo_entrada` | `codigo_qr` (UQ), `precio`, `expira_bloqueo`, `estado` (`PRESELECCIONADA`, `BLOQUEADA`, `VENDIDA`, `EXPIRADA`, `ANULADA`), columna virtual `butaca_ocupada` (UQ por función+butaca) |
| `transaccion_pago` | `id_transaccion` | `id_compra` | `monto`, `medio_pago` (`TARJETA_CREDITO`, `TARJETA_DEBITO`, `YAPE`, `PLIN`, `PAGO_QR`), `estado_pago` (`PENDIENTE`, `APROBADO`, `RECHAZADO`), `referencia_pasarela` (UQ), `motivo_rechazo` |
| `pedido` | `id_pedido` | `id_compra` (UQ), `id_empleado` | `codigo_qr` (UQ), `fecha_hora`, `fecha_entrega`, `estado` (`PENDIENTE`, `EN_PREPARACION`, `LISTO`, `ENTREGADO`, `ANULADO`) |
| `reclamo` | `id_reclamo` | `id_compra` | `tipo` (`ATENCION`, `PRODUCTO_DULCERIA`, `SALA_PROYECCION`), `descripcion`, `estado` (`REGISTRADO`, `EN_REVISION`, `RESUELTO`, `RECHAZADO`), `respuesta` |
| `calificacion` | `id_calificacion` | `id_compra` (UQ) | `puntuacion` (1–5), `comentario`, `fecha` |
| `pelicula_genero` | `(id_pelicula, id_genero)` | `id_pelicula`, `id_genero` | Asociación M:N con `ON DELETE CASCADE` |
| `promocion_dia` | `(id_promocion, dia_semana)` | `id_promocion` | `dia_semana` (`1` = lunes a `7` = domingo) con `ON DELETE CASCADE` |
| `inventario_sede` | `(id_sede, id_producto)` | `id_sede`, `id_producto` | `stock >= 0` |
| `detalle_pedido` | `id_detalle` | `id_pedido`, `id_producto` | `cantidad > 0`, `precio_unitario >= 0`, `personalizacion` |

### 4.3 Reportes y Salidas del Sistema

El sistema genera las siguientes salidas estructuradas en pantalla, correo electrónico o impresión térmica:

* **Comprobante de compra y tickets (PDF / térmico / pantalla):** detalle de película, sede, sala, función, butacas, dulcería, subtotal, descuentos aplicados, operación gravada, IGV (18 %), total pagado, medio de pago, serie correlativa `B001-XXXXXX` y códigos QR individuales por entrada y pedido.
* **Panel de perfil e historial del socio:** nivel de socio (1–4), visitas anuales, multiplicador, saldo de puntos y listado cronológico de compras (`CONFIRMADA`, `REEMBOLSADA`, etc.).
* **Cola operativa de dulcería:** listado en tiempo real filtrado por sede y agrupado en 4 columnas de estado (`PENDIENTE`, `EN_PREPARACION`, `LISTO`, `ENTREGADO`).
* **Vistas de consulta administrativa:** listados filtrables y paginados de películas, funciones (con ocupación y estado), salas, sedes, promociones, cupones, productos con alertas de stock bajo (umbral ≤ 15 unidades) y reclamos de la sede filtrados por estado.

### 4.4 Adquisición, Integridad, Retención y Eliminación de Datos

La integridad se garantiza declarativamente en MySQL mediante **26 claves primarias (`PRIMARY KEY`)**, **30 claves foráneas (`FOREIGN KEY`)**, **23 restricciones de unicidad (`UNIQUE`)** y **51 restricciones de verificación (`CHECK`)**, complementadas por reglas programadas:

* **Prevención de doble reserva (`RNF-10`):** columna virtual `butaca_ocupada` (`1` cuando el estado es `BLOQUEADA` o `VENDIDA`, `NULL` en otro caso) con índice único `uq_entrada_butaca_funcion (id_funcion, id_butaca, butaca_ocupada)`.
* **Unicidad de correo selectiva:** columna virtual `correo_unico` en `cliente` para permitir múltiples compras de invitados con un mismo correo sin bloquear su futuro registro como socio.
* **Disparadores (`triggers`) y eventos programados (`events`):**

| Objeto | Tipo y Momento | Regla de Negocio que Garantiza | Requisito |
| :--- | :--- | :--- | :--- |
| `trg_entrada_coherencia_sala` | Trigger (`BEFORE INSERT ON entrada`) | Verifica que la butaca pertenezca a la sala de la función y la función a la sede de la compra. | `RF-08`, `RF-36` |
| `trg_entrada_coherencia_sala_upd` | Trigger (`BEFORE UPDATE ON entrada`) | Aplica la misma validación de coherencia sala–función–sede al actualizar una entrada. | `RF-08`, `RF-36` |
| `trg_butaca_capacidad` | Trigger (`BEFORE INSERT ON butaca`) | Impide registrar en una sala más butacas que su atributo `capacidad`. | `RF-39` |
| `trg_compra_serie` | Trigger (`BEFORE UPDATE ON compra`) | Incrementa `correlativo` y asigna `B001-XXXXXX` solo al pasar a `CONFIRMADA`. | `RF-20`, `RNF-10` |
| `ev_expirar_compras` | Evento (`EVERY 1 MINUTE`, UTC-5) | Expira compras `EN_PROCESO` vencidas y sus entradas `BLOQUEADA`, y anula pedidos `PENDIENTE`. | `RF-09`, `RF-10` |
| `ev_estrenar_peliculas` | Evento (`EVERY 1 DAY` a las `00:05`, UTC-5) | Pasa de `PREESTRENO` a `EN_CARTELERA` las películas con `fecha_estreno <= CURDATE()`. | `RF-02`, `RF-37` |

* **Retención y eliminación:** no se realizan borrados físicos destructivos sobre compras, entradas, pagos, reclamos o calificaciones; las compras canceladas o vencidas conservan su estado (`CANCELADA`, `EXPIRADA`, `REEMBOLSADA`) sin bloquear futuras ventas, y los catálogos maestros utilizan baja lógica (`estado = FALSE` o `FUERA_DE_CARTELERA` / `INACTIVA`). En entorno productivo se exige respaldo diario de la base de datos.

---

## 5. Requerimientos de Interfaces Externas

### 5.1 Interfaces de Usuario

La interfaz gráfica se diseñó bajo el enfoque de **Diseño Centrado en el Usuario (DCU, ISO 9241-210)** para pantallas táctiles de **1920 × 1080 px**, materializada en **42 pantallas en Figma** y **40 vistas FXML** en JavaFX (`estilos.css`), con barra de progreso en 3 pasos (*1. Asientos, 2. Dulcería, 3. Pago*), contador regresivo visible de 4 minutos y avisos modales para estados alternos (cancelación, tiempo agotado, pago rechazado, inactividad).

* **Paleta cromática oficial y accesibilidad (`RNF-08`, WCAG 2.1 AAA):**

| Color | Código HEX | Función Principal | Relación / Aplicación |
| :--- | :--- | :--- | :--- |
| **Prussian Blue** | `#03132C` | Fondo general y texto primario | Lienzo principal de las vistas, títulos y textos sobre fondos claros. |
| **Oxblood** | `#9B180C` | Navegación activa y alertas | Paso activo en barra superior, filtros activos, butacas ocupadas (`X`) y mensajes de error. |
| **Old Lace** | `#FAF0E0` | Fondos secundarios | Cabeceras de tabla, área QR, recuadros de puntos y columnas del panel de dulcería (contraste **16.9:1** con `#03132C`). |
| **White** | `#FFFFFF` | Tarjetas y paneles principales | Barra superior, tarjetas de películas/combos, modales, mapa de sala y botones secundarios (contraste **18.6:1** con `#03132C`). |
| **Honey Bronze** | `#E5A93C` | Acciones primarias (CTA) y selección | Botones principales de avance, butacas seleccionadas, estrellas de calificación y progreso de socio. |

* **Tipografía y ergonomía táctil (`RNF-07`, `RNF-12`):** fuente **Manrope** con números tabulares (`tabular numerals`) para alineación monetaria y horaria (Títulos 28–32 px Bold, Subtítulos 20–22 px SemiBold, Tarjetas 18 px SemiBold, Cuerpo/Botones 16 px Regular/Bold, Etiquetas 13–14 px Medium). Todos los controles interactivos mantienen un área mínima de **48 × 48 px** (~9 mm) y muestran errores de validación directamente bajo cada campo sin borrar los datos válidos ingresados.

### 5.2 Interfaces de Software

* **Persistencia (JDBC):** conexión mediante `MySQL Connector/J 8.4.0` leyendo credenciales desde `db.properties` (`DBConnection`), invocada exclusivamente a través de interfaces de repositorio (`CompraRepository`, `PagoRepository`, `PeliculaRepository`, `ClienteRepository`, `PedidoRepository`, `SedeRepository`, etc.).
* **Pasarela de Pagos (`ucsur.pe.pasarela.PasarelaPago`):** contrato desacoplado para autorizar cobros y procesar devoluciones; implementada en el prototipo mediante `PasarelaSimulada` y sustituible en producción por una pasarela certificada PCI-DSS.
* **Servicio de Correo (`ucsur.pe.correo.ServicioCorreo`):** contrato para envío de comprobantes PDF, códigos de recuperación de contraseña, códigos de 6 dígitos de invitados y respuestas a reclamos; implementada mediante `CorreoSimulado`.
* **Generación de Códigos QR (`CodigoVisual`):** componente de representación visual en el prototipo, reemplazable por un generador QR estándar escaneable en producción.

### 5.3 Interfaces de Hardware

* **Pantalla táctil capacitiva:** mínimo 21,5 pulgadas y resolución Full HD `1920 × 1080` px para operación directa sin teclado físico externo.
* **Impresora térmica ESC/POS (`RNF-09`):** compatibilidad nativa para impresión física de tickets con código QR desde la vista previa del comprobante (`RF-21`).
* **Terminal POS de pago (`RNF-09`):** compatibilidad nativa con terminales físicos para lectura de tarjetas y cobros QR sin contacto.

### 5.4 Interfaces de Comunicaciones

* **Red local cableada (LAN):** comunicación entre kioscos de la sede y el servidor `DB_Cine` sobre protocolo MySQL/TCP en red interna aislada; en producción debe operar sobre **TLS** con usuario de privilegios mínimos.
* **Comunicaciones externas (producción):** protocolo **HTTPS con TLS 1.3** bajo cumplimiento **PCI-DSS** hacia la pasarela de pagos (`RNF-03`), y **SMTP sobre TLS** o API HTTPS para el servidor de correo transaccional.

---

## 6. Atributos de Calidad

### 6.1 Usabilidad

* **RNF-07 (Diseño de Interfaz Táctil):** diseñar botones y elementos táctiles con un tamaño mínimo de 48 × 48 píxeles independientes de la densidad (aprox. 9 mm) para facilitar la pulsación. *(Externo – Usabilidad)*
* **RNF-08 (Accesibilidad Visual):** ofrecer opción de interfaz en alto contraste y tamaño de fuente ajustable para personas con visión reducida *(en el prototipo actual se implementa la paleta de alto contraste WCAG AAA; el ajuste dinámico de tamaño de fuente queda contemplado para despliegue productivo)*. *(Externo – Usabilidad)*
* **RNF-11 (Facilidad de aprendizaje):** un cliente sin experiencia previa debe completar la compra de una entrada en menos de 4 minutos sin ayuda del personal. *(Externo – Usabilidad)*

### 6.2 Rendimiento

* **RNF-01 (Tiempo de Respuesta):** las consultas a la base de datos y el despliegue del mapa de sala deben responder en menos de 2 segundos. *(Externo – Rendimiento)*
* **RNF-02 (Capacidad de Concurrencia):** el sistema debe soportar un mínimo de 100 transacciones simultáneas de kioscos sin degradar la latencia. *(Interno – Eficiencia)*

### 6.3 Seguridad

* **RNF-03 (Encriptación de Transacciones):** cifrar las transacciones financieras y los datos del usuario bajo el estándar PCI-DSS y el protocolo TLS 1.3, y almacenar las contraseñas con hash (nunca en texto plano). *(Interno – Integridad)* *(Nota de prototipo: el hash con sal BCrypt/Argon2 y TLS 1.3 integran el plan obligatorio de transición a producción de la Sección 8).*
* **RNF-04 (Cierre por Inactividad):** cancelar automáticamente la sesión y limpiar los datos en pantalla tras 3 minutos sin interacción; si había una compra en proceso, se cancela y se liberan las butacas. *(Interno – Integridad)*

### 6.4 Seguridad Operacional e Integridad

* **RNF-10 (Transacciones ACID):** garantizar la atomicidad y consistencia en la BD para evitar la doble reserva del mismo asiento; en el modelo y la base de datos, se rechaza registrar una butaca ya vendida o con bloqueo vigente. *(Interno – Integridad)*
* **RNF-12 (Validación de Datos de Entrada):** validar los datos antes de guardarlos: DNI de 8 dígitos, formato y longitud del correo, longitudes máximas de los textos y fecha de nacimiento no futura correspondiente a una edad entre 15 y 110 años. *(Interno – Integridad)*

### 6.5 Otros Atributos Relevantes

* **RNF-05 (Disponibilidad / Uptime):** garantizar una disponibilidad del sistema del 99,5 % durante el horario de atención del cine. *(Externo – Disponibilidad)*
* **RNF-06 (Detección de Fallas y Log):** generar registros en bitácoras (`logs`) ante pérdidas de comunicación con la impresora o pasarela. *(Interno – Mantenibilidad)*
* **RNF-09 (Integración Hardware):** asegurar compatibilidad nativa con impresoras térmicas ESC/POS y terminales POS de pago. *(Interno – Portabilidad)*

### 6.6 Matriz Consolidada de RNF y Matriz Cruzada de Atributos de Calidad

| Código | Categoría | Requerimiento | Clasificación (ISO/IEC 25010) |
| :--- | :--- | :--- | :--- |
| **RNF-01** | Rendimiento | Tiempo de Respuesta (< 2 s) | Externo (Rendimiento) |
| **RNF-02** | Rendimiento | Capacidad de Concurrencia (≥ 100 transacciones simultáneas) | Interno (Eficiencia) |
| **RNF-03** | Seguridad | Encriptación PCI-DSS / TLS 1.3 y hash de contraseñas | Interno (Integridad) |
| **RNF-04** | Seguridad | Cierre por Inactividad (3 min) y liberación de butacas | Interno (Integridad) |
| **RNF-05** | Disponibilidad | Disponibilidad / Uptime (99,5 %) | Externo (Disponibilidad) |
| **RNF-06** | Confiabilidad | Detección de Fallas y Log ante caída de impresora/pasarela | Interno (Mantenibilidad) |
| **RNF-07** | Usabilidad | Diseño de Interfaz Táctil (≥ 48 × 48 dp) | Externo (Usabilidad) |
| **RNF-08** | Usabilidad | Accesibilidad Visual (alto contraste y fuente ajustable) | Externo (Usabilidad) |
| **RNF-09** | Portabilidad | Integración Hardware (ESC/POS y POS) | Interno (Portabilidad) |
| **RNF-10** | Integridad | Transacciones ACID contra doble reserva | Interno (Integridad) |
| **RNF-11** | Usabilidad | Facilidad de aprendizaje (compra < 4 min sin ayuda) | Externo (Usabilidad) |
| **RNF-12** | Integridad | Validación de Datos de Entrada (DNI, correo, textos, edad 15–110) | Interno (Integridad) |

**Matriz Cruzada de Atributos de Calidad (Internos vs. Externos):**

| Atributo Interno \ Externo | Usabilidad (externo) | Fiabilidad / Disponibilidad (externo) | Rendimiento (externo) |
| :--- | :--- | :--- | :--- |
| **Mantenibilidad** | — | `RNF-06` (Detección de fallas y logs) | — |
| **Eficiencia** | — | — | `RNF-02` (Capacidad de concurrencia) |
| **Portabilidad** | `RNF-09` (Integración con impresora y POS) | `RNF-09` (Integración con impresora y POS) | — |
| **Integridad** | `RNF-04` (Cierre por inactividad) | `RNF-03` (Encriptación PCI-DSS), `RNF-10` (Transacciones ACID), `RNF-12` (Validación de datos) | — |

---

## 7. Requerimientos de Internacionalización y Localización

El sistema está diseñado específicamente para la operación en **Lima, Perú**, sujeto a las siguientes convenciones locales:

* **Idioma:** español (Perú) en toda la interfaz gráfica, mensajes de validación, comprobantes y notificaciones.
* **Moneda e impuestos:** Soles peruanos (`S/`), operados internamente en céntimos enteros (`long`) y presentados con dos decimales (`S/ 0.00`). Todos los precios al público incluyen **IGV (18 %)**, el cual se desglosa en el resumen y comprobante (`Operación Gravada = Total / 1.18`, `IGV = Total - Operación Gravada`).
* **Zona horaria y formatos:** hora oficial de Perú (`UTC-5` / `-05:00`) sincronizada entre Java y MySQL; fechas en formato `dd/MM/yyyy` (o `dd/MM/yyyy HH:mm`).
* **Identificadores y medios de pago locales:** Documento Nacional de Identidad peruano (**DNI** de 8 dígitos numéricos), teléfonos móviles peruanos de 9 dígitos, y soporte para billeteras digitales locales (**Yape** y **Plin**), además de tarjetas de crédito/débito y pago QR.

---

## 8. Otros Requerimientos

### 8.1 Requerimientos Legales y Regulatorios

* **Protección de Datos Personales (Ley N.° 29733):** consentimiento explícito en el registro de cuenta y política de tratamiento de datos personales para operación en producción.
* **Libro de Reclamaciones (Indecopi):** registro formal de reclamos y quejas vinculados a compras con trazabilidad de estado y respuesta escrita (`RF-23`, `RF-35`, `CU-12`).
* **Facturación Electrónica (SUNAT):** la serie interna `B001-XXXXXX` del prototipo debe integrarse con un Proveedor de Servicios Electrónicos (PSE) autorizado para emisión de boletas y facturas electrónicas válidas ante la SUNAT.

### 8.2 Empaquetado, Mantenimiento y Transición a Producción

* **Distribución:** empaquetado mediante `jpackage` incluyendo el entorno Java 21 y JavaFX, con inicio automático en pantalla completa en cada kiosco.
* **Mantenimiento previsto:** correctivo (registro de incidencias similar a `DEF-01`–`DEF-06`), adaptativo (actualización de dependencias Java/JavaFX/JDBC/BD LTS y nuevos medios de pago), perfectivo (iteraciones de usabilidad basadas en pruebas UAT/SUS) y preventivo (respaldos diarios, revisión de logs `RNF-06` y ejecución de pruebas automatizadas).
* **Componentes a reemplazar del prototipo académico hacia producción:**

| Componente | Situación en el Prototipo | Requerimiento para Producción | Requisito |
| :--- | :--- | :--- | :--- |
| Pasarela de pagos (`PasarelaPago`) | `PasarelaSimulada` | Implementación certificada en PCI-DSS sin almacenar tarjeta/CVV | `RF-16`, `RNF-03` |
| Envío de correos (`ServicioCorreo`) | `CorreoSimulado` | Servidor SMTP o servicio transaccional que envíe PDF y códigos | `RF-22`, `RF-33` |
| Contraseñas | Texto plano en alcance académico | Hash con sal (`BCrypt` o `Argon2`) y bloqueo tras intentos fallidos | `RNF-03` |
| Conexión a BD | Usuario local sin cifrado | Conexión TLS y usuario con privilegios mínimos sobre `DB_Cine` | `RNF-03`, `RNF-05` |
| Códigos QR y tickets | `CodigoVisual` en pantalla | QR aleatorio/firmado de un solo uso e impresión térmica ESC/POS | `RF-20`, `RF-21`, `RNF-09` |

---

## 9. Glosario

| Término | Definición |
| :--- | :--- |
| **SRS** | *Software Requirements Specification* (Especificación de Requerimientos de Software). Documento formal que describe capacidades y restricciones del sistema. |
| **RF / RNF / RN / RU** | Códigos de trazabilidad para Requerimiento Funcional, Requerimiento No Funcional, Requisito de Negocio y Requisito de Usuario. |
| **Kiosco de autoservicio** | Terminal físico con pantalla táctil (≥ 21,5") instalado en la sede de Cineplanet para realizar compras autónomas. |
| **Bloqueo temporal de asientos** | Mecanismo transaccional que retiene las butacas elegidas durante **4 minutos** (`expira_compra`) para evitar doble reserva mientras se concluye la compra y el pago. |
| **Comanda** | Orden de dulcería generada tras confirmar el pago y enviada al panel del personal de dulcería para su preparación y entrega. |
| **Pasarela de pagos** | Servicio externo encargado de procesar transacciones (tarjeta, Yape, Plin, QR) y devolver aprobación o rechazo. |
| **PCI-DSS / TLS 1.3** | Estándar de seguridad para datos de tarjetas de pago y protocolo criptográfico de transporte seguro versión 1.3. |
| **ACID** | Atomicidad, Consistencia, Aislamiento y Durabilidad; propiedades de transacción garantizadas en MySQL InnoDB. |
| **ESC/POS / POS** | Estándar de comandos para impresoras térmicas de tickets y terminal hardware de cobro con tarjeta. |
| **Uptime** | Porcentaje de tiempo en que el sistema se mantiene operativo y disponible para los usuarios (meta: 99,5 %). |
| **Comprobante digital** | Boleta/factura con serie correlativa (`B001-XXXXXX`), operación gravada, IGV (18 %) y códigos QR por entrada y pedido. |
| **IGV (18 %) / Operación gravada** | Impuesto General a las Ventas en Perú incluido en los precios al público y desglosado en el comprobante. |
| **Nivel de socio** | Categoría del programa interno de fidelización (0 = invitado; 1 a 4 = socio) según visitas anuales, con multiplicador de puntos y beneficios exclusivos. |
| **Libro de Reclamaciones** | Módulo digital regulado para registrar quejas o reclamos formales asociados a una compra y consultar su estado y respuesta. |
| **Código temporal de verificación** | Código numérico de 6 dígitos (vigencia 10 min, un solo uso, máx. 5 intentos) enviado al correo del invitado para recuperar entradas. |
| **PIN de empleado** | Clave numérica de 4 dígitos solicitada al empleado para retornar desde el modo cliente a su panel operativo. |
| **CRUD** | Operaciones básicas de gestión de datos: Crear, Leer (consultar), Actualizar y Eliminar. |
| **Sede** | Local físico de Cineplanet que determina cartelera, salas, stock de dulcería, comandas y ámbito de atención de reclamos. |
| **AS-IS / TO-BE** | Representación del proceso de negocio actual (taquilla presencial) y del proceso mejorado propuesto (autoservicio). |
| **Trigger / Evento programado** | Objetos almacenados en MySQL que ejecutan validaciones automáticas ante eventos DML o tareas periódicas por reloj (`UTC-5`). |
| **MVC / SOLID / Strategy** | Patrón Modelo–Vista–Controlador por capas, cinco principios de diseño orientado a objetos y patrón de diseño para algoritmos intercambiables de descuento. |
| **DCU / UAT / SUS** | Diseño Centrado en el Usuario (ISO 9241-210), Pruebas de Aceptación de Usuario y *System Usability Scale* (escala 0–100 de usabilidad). |
| **FXML** | Lenguaje declarativo basado en XML utilizado por JavaFX para definir las 40 vistas de la interfaz gráfica. |

---

## 10. Modelos de Análisis

El análisis completo y la arquitectura del sistema se documentan en el *Informe de Avances Sistema de Cine* (26/11/2026) mediante los modelos resumidos a continuación:

### 10.1 Resumen de Casos de Uso (`CU-01` a `CU-12`)

| Código | Caso de Uso | Actor Principal | Actores Secundarios |
| :--- | :--- | :--- | :--- |
| **CU-01** | Registrar usuario | Cliente | — |
| **CU-02** | Iniciar sesión (y acceso como invitado / recuperación de contraseña) | Cliente, Administrador, Personal de dulcería | — |
| **CU-03** | Consultar cartelera | Cliente | — |
| **CU-04** | Consultar promociones y puntos de socio | Cliente | — |
| **CU-05** | Comprar entrada | Cliente | Pasarela de Pagos |
| **CU-06** | Comprar dulcería | Cliente | Pasarela de Pagos, Personal de dulcería |
| **CU-07** | Realizar pago | Cliente | Pasarela de Pagos |
| **CU-08** | Gestionar comandas de dulcería (Preparar, Notificar estado, Entregar pedido) | Personal de dulcería | Cliente |
| **CU-09** | Consultar historial y recuperar entrada digital | Cliente | — |
| **CU-10** | Calificar experiencia y Registrar Reclamo | Cliente | — |
| **CU-11** | Gestionar catálogo operativo (Películas, Funciones, Salas, Sedes, Descuentos, Dulcería) | Administrador | — |
| **CU-12** | Atender reclamos | Administrador | — |

### 10.2 Matriz de Trazabilidad (`RU ↔ RF ↔ CU`)

| Requisito de Usuario | Requerimientos Funcionales | Casos de Uso |
| :--- | :--- | :--- |
| **RU-01** (Consulta de disponibilidad) | `RF-02`, `RF-03`, `RF-04`, `RF-36` | `CU-03` |
| **RU-02** (Selección de entradas y asientos) | `RF-05` a `RF-10`, `RF-29`, `RF-30`, `RF-46` | `CU-05` |
| **RU-03** (Compra autónoma) | `RF-01`, `RF-27` | `CU-01`, `CU-02` |
| **RU-04** (Adquisición de dulcería) | `RF-12`, `RF-13`, `RF-14`, `RF-31` | `CU-06` |
| **RU-05** (Aplicación de beneficios) | `RF-11`, `RF-28`, `RF-43`, `RF-44` | `CU-04`, `CU-07` |
| **RU-06** (Pago electrónico) | `RF-16`, `RF-17`, `RF-18`, `RF-32`, `RF-45` | `CU-07` |
| **RU-07** (Recepción de confirmación) | `RF-19`, `RF-20`, `RF-21`, `RF-22` | `CU-05`, `CU-07` |
| **RU-08** (Postventa y recuperación) | `RF-23`, `RF-25`, `RF-26`, `RF-33`, `RF-35` | `CU-09`, `CU-10`, `CU-12` |
| **RU-09** (Recepción y entrega de pedidos) | `RF-15`, `RF-34` | `CU-08` |
| **RU-10** (Gestión del catálogo y reclamos) | `RF-24`, `RF-37` a `RF-42`, `RF-47`, `RF-48` | `CU-11`, `CU-12` |
| **RU-11** (Actualización de información) | `RF-37` a `RF-42` | `CU-11` |

### 10.3 Índice de Modelos Gráficos en el Informe de Avances

* **Diagrama de Contexto (Nivel 0):** Figura 5 (Sección 3.1 del Informe).
* **Diagrama UML de Casos de Uso:** Figura 6 (Sección 3.2 del Informe).
* **Diagrama de Componentes MVC por capas:** Figura 7 (Sección 4.1 del Informe).
* **Modelo Conceptual Entidad–Relación:** Figura 8 (Sección 4.2 del Informe).
* **Modelo Lógico y Físico de Base de Datos (`DB_Cine`):** Figuras 9 y 10 (Secciones 4.3 y 4.4 del Informe).
* **Diagrama de Clases de Dominio:** Figura 11 (Sección 4.5.1 del Informe).
* **Diagramas de Secuencia (`Login`, `Cartelera`, `Comprar entrada`, `Atender pedido`, `Comprar dulcería`, `Realizar pago`, `Crear función`):** Figuras 22 a 28 (Sección 4.6 del Informe).
