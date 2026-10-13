# Database Component Design (`database`)

## 1. Component Design

**Component:** `database` — Esquema relacional SQLite en `src/database/`.

Persistencia única del sistema: guarda las mediciones que llegan de la app
móvil a través del servidor REST y de la lógica de negocio. Motor: SQLite 3
(fichero `src/database/mediciones.db`, esquema en `src/database/mediciones.sql`).

Se usan los tipos SQLite acordes a los abstractos del
`Software_Engineering_Spec`: `REAL` (**R**), `TEXT` (**Text**) y `DATETIME`
(texto `'YYYY-MM-DD HH:MM:SS.SSS'`). No hay columnas enteras: la clave primaria
es compuesta y no existe una columna `id` autonumerica.

====================================================================================
TABLE: Mediciones

DESCRIPTION: Almacena las mediciones registradas por la aplicacion movil: tipo
             de medicion, nombre del cliente (emisora), valor numerico y la
             fecha y hora de registro.

COLUMNS:

+ tipo_de_medicion | TEXT     | NOT NULL
+ nombre_cliente   | TEXT     | NOT NULL
+ valor            | REAL     | NOT NULL
+ fecha_y_hora     | DATETIME | NOT NULL | strftime('%Y-%m-%d %H:%M:%f','now','localtime')

PRIMARY KEY: (nombre_cliente, fecha_y_hora)

FOREIGN KEYS:

+ (ninguna)

CONSTRAINTS:

+ NOT NULL en tipo_de_medicion, nombre_cliente, valor y fecha_y_hora
+ Clave primaria compuesta (nombre_cliente, fecha_y_hora): una emisora no puede
  tener dos mediciones con la misma marca de tiempo (con milisegundos)
+ fecha_y_hora generado por strftime('%Y-%m-%d %H:%M:%f','now','localtime') si
  no se indica
====================================================================================

### Notas sobre el diseño

- **Clave primaria compuesta `(nombre_cliente, fecha_y_hora)`:** la identidad
  natural de cada medicion es la emisora mas la marca de tiempo; no hay columna
  `id` autonumerica. Las lecturas se ordenan por `fecha_y_hora` en
  `obtenerUltimas10()`.
- **`fecha_y_hora` con milisegundos:** SQLite no tiene tipo temporal nativo y
  `strftime('%Y-%m-%d %H:%M:%f','now','localtime')` produce texto
  `'YYYY-MM-DD HH:MM:SS.SSS'` en hora **local** del sistema. La precision de
  milisegundos evita que dos mediciones de la misma emisora en el mismo segundo
  colisionen en la clave primaria. No se usa `CURRENT_TIMESTAMP` porque devuelve
  UTC y desfasa la hora mostrada.
- **`tipo_de_medicion`** responde al enum de la app móvil
  `TipoMedicion = { ozono, temperatura, ruido }` (ver `business_logic_design.md`).
- **`nombre_cliente`** guarda el *nombre de emisora* (`nombre_emisora`) que
  envía la app: en la base de datos se llama `nombre_cliente` y es igual al
  nombre del cliente.

## 2. Design Clarifications

- La capa de acceso a datos **no** usa driver nativo de Node (`node-sqlite3`):
  el repositorio (en `src/business_logic/`) invoca un proceso `php` con la
  extensión `PDO` + `sqlite3` (ver `business_logic_design.md`), lo que evita
  dependencias binarias. El binario PHP se toma de la variable de entorno
  `PHP_BIN` o, si no, del XAMPP instalado.
- Para reconstruir la base desde cero:
  `sqlite3 src/database/mediciones.db < src/database/mediciones.sql`.
- El fichero `src/database/mediciones.sqbpro` es el proyecto de DB Browser for
  SQLite (edición visual) y no afecta al funcionamiento.
- **Sin `CHECK` en el esquema real:** el dominio finito de `tipo_de_medicion`
  (`ozono`, `temperatura`, `ruido`) y la no-negatividad de `valor` son convenciones
  que aplica la aplicación/la lógica de negocio, no `CHECK` de la base de datos;
  el diseño refleja el esquema tal cual está implementado en `mediciones.sql`.

## 3. General Rules

- **Programming Language:** SQL (SQLite 3) para el esquema; acceso desde Node
  vía proceso `php` con `PDO` + `sqlite3`.
- **Function/Method Headers:** cada acceso a datos en el repositorio lleva su
  diseño lógico en un bloque de comentario con línea de guiones (`-----------`)
  y tipos abstractos, de acuerdo con `business_logic_design.md`.
- **Code Readability:** SQL y código claro y autoexplicativo.
- **Automated Testing:** casos de prueba de integración que insertan y leen
  filas reales en `src/business_logic/tests/` (la lógica con repositorio
  SQLite) y en `src/communication/tests/` (POST/GET de extremo a extremo).