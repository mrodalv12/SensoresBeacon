# Business Logic Component Design (`business_logic`)

## 1. Component Design

**Component:** `business_logic` — Lógica de negocio en `src/business_logic/`.

Es el núcleo del sistema: normaliza las mediciones, las persiste y las
consulta. Es **100% independiente de la capa de comunicación**: no conoce
HTTP, rutas, cabeceras, códigos de estado ni sockets, y no importa ningún
módulo de `src/communication/`. Solo un repositorio de datos (que cumple el
contrato definido en `database_design.md`).

### 1.1 Abstract Data Types

```text
TipoMedicion = { co2, temperatura, ruido }

MedicionNueva = ( tipo: TipoMedicion, valor: R, nombre_cliente: Text )

Medicion = ( fecha_y_hora: Text, tipo: Text, valor: R )

Repositorio = ( guardar(), obtenerUltimas10() )   // interfaz de acceso a datos
```

### 1.2 Class Diagram

```text
         --------- LogicaNegocio (SQLiteP y *Async) -------------
         |
         | repositorio: Repositorio
         |
         |
repositorio: Repositorio --> LogicaNegocio() -->
         |
         |
medicion: MedicionNueva --> guardarMedicion() --> (ok: B, id: N)
         |
         |
         |        obtenerMedicion() <-- [ Medicion ]
         |
         |
         |        (no depende de communication: sin HTTP ni sockets)
         |
         ----------------------------------------

         --------- Repositorio (interfaz) ----------------------
         |
         |
datos: MedicionNueva --> guardar() --> id: N
         |
         |
         |        obtenerUltimas10() <-- [ Medicion ]
         |
         ----------------------------------------
```

### 1.3 Function/Method Logical Designs

```text
repositorio: Repositorio --> LogicaNegocio( ) --> nueva instancia
```

- Guarda la referencia al repositorio con el que trabajará la lógica.

```text
medicion: MedicionNueva --> guardarMedicion() --> ( ok: B, id: N )
```

- Normaliza la medición: `tipo` como texto (de `TipoMedicion`), `valor` como
  `R`, y `nombre_cliente` como texto (el nombre de emisora = nombre del
  cliente que envía la app).
- Delega el alta en el repositorio (`guardar()`) y devuelve el `id` asignado.

```text
obtenerMedicion() --> [ Medicion ]
```

- Pide al repositorio las últimas 10 mediciones (`obtenerUltimas10()`) y las
  entrega con `fecha_y_hora`, `tipo` y `valor`.

### 1.4 Database Awareness

La lógica se apoya en el repositorio, que escribe y lee la tabla `Mediciones`
definida en `database_design.md`:

| Operación | Tabla de `database_design.md` | Columnas implicadas |
|---|---|---|
| `guardar()` | `Mediciones` | `tipo_de_medicion`, `nombre_cliente`, `valor` (INSERT) |
| `obtenerUltimas10()` | `Mediciones` | `fecha_y_hora`, `tipo_de_medicion`, `valor` (SELECT, orden por `id`) |

## 2. Design Clarifications

- **Telemetría del beacon:** la implementación recibe además
  (`uuid: Text`, `minor: N`, `major: N`, `txPower: Z`) por compatibilidad de
  firma con la capa de comunicación, pero **no los persiste**; no forman parte
  del diseño lógico de `guardarMedicion()` (la firma pura usa solo tipos de
  dominio).
- **Varias implementaciones, un solo contrato:** existen variantes
  (`logicaNegocio.js`, `logicaNegocioConRepositorio.js`,
  `logicaNegocioSqlitePy.js`, `logicaNegocioSqlitePyAsync.js`) con la misma
  firma lógica; las sincrónicas/asíncronas se diferencian solo en cómo
  esperan el resultado del repositorio (para pruebas).
- **Repositorio intercambiable:** `repositorioMemoria.js` se usa en pruebas
  (sin base de datos) y `repositorioSqlitePython*.js` en producción; este
  último invoca `python3` (proceso externo, no un framework de transporte)
  con el módulo `sqlite3` de la biblioteca estándar.
- **Independencia de comunicación:** ningún archivo de `src/business_logic/`
  importa `src/communication/`, ni módulos HTTP/red, ni frameworks web.
  Únicas dependencias de la biblioteca estándar de Node:
  `child_process` y `path`.

## 3. General Rules

- **Programming Language:** Node.js (JavaScript, CommonJS), sin dependencias
  externas de npm.
- **Function/Method Headers:** cada función o método lleva su diseño lógico en
  un bloque de comentario delimitado por una línea de guiones (`-----------`),
  con tipos abstractos (`N`, `Z`, `R`, `B`, `Text`, `[Tipo]`).
- **Code Readability:** código claro y autoexplicativo, mínimo de comentarios
  adicionales.
- **Automated Testing:** casos de prueba unitarias e integración para todas
  las funciones críticas (`guardarMedicion`, `obtenerMedicion`, repositorios);
  se ejecutan con `node --test` en `src/business_logic/tests/`.