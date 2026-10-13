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
TipoMedicion = { ozono, temperatura, ruido }

MedicionNueva = ( tipo: TipoMedicion, valor: R, identificador_placa: Text )

Medicion = ( fecha_y_hora: Text, tipo: Text, valor: R )

Repositorio = ( guardar(), obtenerUltimas10() )   // interfaz de acceso a datos
```

### 1.2 Class Diagram

```text
         --------- LogicaNegocio (SQLitePhp y *Async) ---------
         |
         | repositorio: Repositorio
         |
         |
repositorio: Repositorio --> LogicaNegocio() -->
         |
         |
medicion: MedicionNueva --> guardarMedicion() --> (ok: B, identificador_placa: Text, fecha_y_hora: Text)
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
datos: MedicionNueva --> guardar() --> (identificador_placa: Text, fecha_y_hora: Text)
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
medicion: MedicionNueva --> guardarMedicion() --> ( ok: B, identificador_placa: Text, fecha_y_hora: Text )
```

- Normaliza la medición: `tipo` como texto (de `TipoMedicion`), `valor` como
  `R`, y `identificador_placa` como texto (el identificador de la placa emisora
  que envía la app).
- Delega el alta en el repositorio (`guardar()`) y devuelve la clave compuesta
  (`identificador_placa`, `fecha_y_hora`).

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
| `guardar()` | `Mediciones` | `tipo_de_medicion`, `identificador_placa`, `valor` (INSERT) |
| `obtenerUltimas10()` | `Mediciones` | `fecha_y_hora`, `tipo_de_medicion`, `valor` (SELECT, orden por `fecha_y_hora`) |

## 2. Design Clarifications

- **Los datos del beacon no llegan a esta capa:** `rssi`, `txPower`, `major`,
  `minor` y `uuid` se quedan en la aplicación Android (la `uuid` solo se usa
  ahí para buscar el iBeacon) y no se envían en el `POST`. `guardarMedicion()`
  recibe únicamente `MedicionNueva = (tipo, valor, identificador_placa)`, es decir,
  solo tipos de dominio; ningún transporte ni cabecera.
- **Varias implementaciones, un solo contrato:** existen variantes
  (`logicaNegocio.js`, `logicaNegocioConRepositorio.js`,
  `logicaNegocioSqlitePhp.js`, `logicaNegocioSqlitePhpAsync.js`) con la misma
  firma lógica; las sincrónicas/asíncronas se diferencian solo en cómo
  esperan el resultado del repositorio (para pruebas).
- **Repositorio intercambiable:** `repositorioMemoria.js` se usa en pruebas
  (sin base de datos) y `repositorioSqlitePhp*.js` en producción; este
  último invoca `php` (proceso externo, no un framework de transporte)
  con la extensión `PDO` + `sqlite3`. El binario se toma de la variable de
  entorno `PHP_BIN` o, si no, del XAMPP instalado.
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