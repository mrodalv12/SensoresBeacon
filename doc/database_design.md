# Database Component Design (`database`)

## 1. Component Design

**Component:** `database` — Esquema relacional SQLite en `src/database/`.

Persistencia única del sistema: guarda las mediciones que llegan de la app
móvil a través del servidor REST y de la lógica de negocio. Motor: SQLite 3
(fichero `src/database/mediciones.db`, esquema en `src/database/mediciones.sql`).

Se usan los tipos SQLite acordes a los abstractos del
`Software_Engineering_Spec`: `INTEGER` (**N**), `REAL` (**R**), `TEXT`
(**Text**) y `DATETIME` (texto `'YYYY-MM-DD HH:MM:SS'`).

====================================================================================
TABLE: Mediciones

DESCRIPTION: Almacena las mediciones registradas por la aplicacion movil: tipo
             de medicion, nombre del cliente (emisora), valor numerico y la
             fecha y hora de registro.

COLUMNS:

+ id               | INTEGER PRIMARY KEY AUTOINCREMENT | NOT NULL
+ tipo_de_medicion | TEXT      | NOT NULL
+ nombre_cliente   | TEXT      | NOT NULL
+ valor            | REAL      | NOT NULL
+ fecha_y_hora     | DATETIME  | DEFAULT CURRENT_TIMESTAMP

PRIMARY KEY: id

FOREIGN KEYS:

+ (ninguna)

CONSTRAINTS:

+ NOT NULL en tipo_de_medicion, nombre_cliente y valor
+ CHECK (tipo_de_medicion IN ('co2', 'temperatura', 'ruido')) -- dominio finito
+ CHECK (valor >= 0)
+ id AUTOINCREMENT (generado por SQLite)
+ fecha_y_hora generado por CURRENT_TIMESTAMP si no se indica
====================================================================================

### Notas sobre el diseño

- **`id`** es la clave primaria generada por SQLite; con ella se ordenan las
  lecturas en `obtenerUltimas10()`.
- **`tipo_de_medicion`** responde al enum de la app móvil
  `TipoMedicion = { co2, temperatura, ruido }` (ver `business_logic_design.md`).
- **`nombre_cliente`** guarda el *nombre de emisora* (`nombre_emisora`) que
  envía la app: en la base de datos se llama `nombre_cliente` y es igual al
  nombre del cliente.
- **`fecha_y_hora`** se autogenera: SQLite no tiene tipo temporal nativo y
  `CURRENT_TIMESTAMP` produce texto `'YYYY-MM-DD HH:MM:SS'`.

## 2. Design Clarifications

- La capa de acceso a datos **no** usa driver nativo de Node (`node-sqlite3`):
  el repositorio (en `src/business_logic/`) invoca `python3` con el módulo
  `sqlite3` de la biblioteca estándar (ver `business_logic_design.md`), lo que
  evita dependencias binarias.
- Para reconstruir la base desde cero:
  `sqlite3 src/database/mediciones.db < src/database/mediciones.sql`.
- El fichero `src/database/mediciones.sqbpro` es el proyecto de DB Browser for
  SQLite (edición visual) y no afecta al funcionamiento.

## 3. General Rules

- **Programming Language:** SQL (SQLite 3) para el esquema; acceso desde Node
  vía proceso `python3` con `sqlite3`.
- **Function/Method Headers:** cada acceso a datos en el repositorio lleva su
  diseño lógico en un bloque de comentario con línea de guiones (`-----------`)
  y tipos abstractos, de acuerdo con `business_logic_design.md`.
- **Code Readability:** SQL y código claro y autoexplicativo.
- **Automated Testing:** casos de prueba de integración que insertan y leen
  filas reales en `src/business_logic/tests/` (la lógica con repositorio
  SQLite) y en `src/communication/tests/` (POST/GET de extremo a extremo).