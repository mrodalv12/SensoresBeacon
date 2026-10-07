# GUI Component Design (`gui`)

## 1. Component Design

**Component:** `gui` — Interfaz de usuario web en `src/gui/`.

Muestra las mediciones de CO₂ y temperatura como una página web servida por
PHP (HTML + CSS + JavaScript de cliente). La verificación de diseño de la GUI
queda fuera del alcance de la revisión; aquí solo se documenta textualmente
cada pantalla. La GUI consume exclusivamente la lógica de negocio de cliente
(`frontend_business_logic`) y **nunca** hace transporte HTTP directamente.

### 1.1 Pantalla principal: `index.php`

**Estructura visual (de arriba a abajo):**

1. **Barra superior** — franja decorativa de color (solo visual).
2. **Contenedor principal:**
   - **Título** "Mediciones".
   - **Aviso de error** (`#error-container`, oculto): se rellena vía el proxy
     `mediciones.errorActual()` cuando el servidor REST no responde.
   - **Dos tarjetas** con la última lectura de cada magnitud:
     - Tarjeta **CO₂** (ppm): valor redondeado.
     - Tarjeta **Temperatura** (°C): valor con 1 decimal.
     - Valor por defecto `--` hasta que llegan datos.
   - **Historial**: tabla con tres columnas — *Fecha y hora*, *Medición*,
     *Valor* — con las últimas 10 mediciones (`co2`/`temperatura`).
3. **Barra inferior** — franja decorativa, pareja de la superior.
4. **Nota de refresco** (`#proxima-actualizacion`): informa cada cuánto se
   actualizan los datos.

**Comportamiento:**

- El renderizado inicial lo hace `index.php` (PHP) leyendo
  `frontend_business_logic` (primeras 10 filas como HTML, escapadas con
  `htmlspecialchars`).
- `js/app.js` refresca tarjetas y tabla cada 5 minutos llamando al proxy
  `mediciones.obtenerMedicion()` (sin `fetch` en la GUI). El intervalo se
  configura en `INTERVALO_REFRESCO_MS` (milisegundos).

### 1.2 Estilos

- `css/variables.css` — paleta y medidas (colores, espaciado, tipografía) en
  variables CSS.
- `css/style.css` — reglas de componentes (barras, tarjetas, tabla, mensajes
  de error); consume las variables, sin colores propios.

## 2. Design Clarifications

- **Desacople de transporte:** la GUI interactúa solo con
  `frontend_business_logic/js/mediciones.js`. `index.php` carga primero el
  proxy y después `app.js`; `app.js` no contiene `fetch` ni `XMLHttpRequest`.
- `htmlspecialchars()` evita que un valor con caracteres especiales rompa el
  HTML de la tabla.

## 3. General Rules

- **Programming Language:** HTML5 + CSS3 + JavaScript (cliente) y PHP 8
  para el renderizado inicial.
- **Function/Method Headers:** cada función de `js/app.js` lleva su diseño
  lógico en un bloque de comentario con línea de guiones (`-----------`).
- **Code Readability:** código claro y autoexplicativo.
- **Automated Testing:** pruebas de estructura y desacople en
  `src/gui/tests/` (`logicaWeb.test.js` y comprobaciones PHP
  `logicaWeb.test.php`).