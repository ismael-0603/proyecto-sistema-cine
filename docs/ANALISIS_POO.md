# Análisis de POO – Sistema de autoservicio de cine

Resumen de dónde se aplica cada pilar de la POO en `cineObjects` y cómo se relaciona con la base de datos `DB_Cine`.

## 1. Pilares aplicados

| Pilar | Dónde se aplica | Evidencia |
|---|---|---|
| **Encapsulamiento** | Todas las clases del dominio | Atributos privados (o `protected` en `Persona` para sus hijas), validación en constructores y setters, copias defensivas (`Sala.obtenerMapaButacas()`, `Cliente.consultarHistorial()`, `Empleado.getPedidosAsignados()`), `Compra.setEstado()` solo visible dentro del paquete. |
| **Abstracción** | Interfaces en `cineInterfaz/` y clases abstractas | `Autenticable` (`Persona`), `Descuento` (`DescuentoBase`), `Inventariable` (`Producto`), `Registrable` (`Pedido`, `Reclamo`, `Calificacion`), `Totalizable` (`Pedido`, `Compra`). Clases abstractas: `Persona`, `DescuentoBase`, `TipoEntrada`. |
| **Herencia** | Tres jerarquías | `Persona` → `Cliente`, `Empleado`; `DescuentoBase` → `Cupon`, `Promocion`; `TipoEntrada` → `TipoEntradaGeneral`, `TipoEntradaSocio`, `TipoEntradaReducida`. |
| **Polimorfismo** | Mains de demostración en `cineInterfaz/` | `MainRegistrable`, `MainTotalizable`, `MainAutenticable`, `MainDescuento`, `MainInventariable`, `MainTipoEntrada`: una lista del tipo de la interfaz o clase base recorre objetos distintos y cada uno responde a su manera. En `Compra.calcularTotal()` se llama `obtenerPuntos()` sin saber qué categoría de entrada es. |

### Nota sobre `protected`
`Persona` mantiene sus atributos como `protected` para que `Cliente` y `Empleado` accedan a ellos. En Java, `protected` también da acceso a las clases del mismo paquete; como todo el dominio vive en `ucsur.pe.cineobjects`, otras clases del paquete podrían leerlos. Es válido para este proyecto, pero si se quisiera máximo encapsulamiento se podrían volver `private` y usar getters.

## 2. Mapeo con la base de datos

| Clase (modelo) | Tabla(s) | Estrategia |
|---|---|---|
| `Persona` (abstracta) → `Cliente`, `Empleado` | `cliente`, `empleado` | Una tabla por clase concreta; `Persona` no tiene tabla. |
| `DescuentoBase` (abstracta) → `Cupon`, `Promocion` | `cupon`, `promocion` | Una tabla por clase concreta; ambas comparten `tipo_descuento`, fechas y `estado`. |
| `TipoEntrada` (abstracta) → 3 subclases | `tipo_entrada` | Una sola tabla. `TipoEntrada.desdeNombre(...)` elige la subclase según `nombre` (SOCIO, GENERAL/ADULTO, resto). Mejora futura opcional: agregar una columna `categoria` y no depender del nombre. |

## 3. Observaciones de la BD (fuera de este refactor)
- `Persona` limita la contraseña a 40 caracteres, pero `cliente.contra` es `VARCHAR(255)`. Además se guarda en texto plano; conviene almacenar un hash.
- `Reclamo.BORRADOR` no está en el `CHECK` de `reclamo.estado`. No genera error porque un reclamo solo se persiste después de `registrar()` (estado `REGISTRADO`).
- `Producto.stock` es un valor simple en el modelo, pero en la BD el stock es por sede (`inventario_sede`).

## 4. Pendiente (no incluido por alcance moderado)
Subclases de `Producto` (combo), `Butaca` (por tipo), `TransaccionPago` (por medio de pago) y `Reclamo` (por tipo), hoy modeladas con un texto o un booleano.
