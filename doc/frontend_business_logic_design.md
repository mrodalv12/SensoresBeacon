# Frontend Business Logic Design (`frontend_business_logic`)

## 1. Component Design

**Component:** `frontend_business_logic` — Lógica de negocio de cliente en
`src/frontend_business_logic/`.

Proxy de la lógica de negocio usado por la GUI: expone a la interfaz de
usuario el **mismo** método que el backend `business_logic`, pero su
implementación encapsula el transporte HTTP hacia el servidor REST. La GUI se
relaciona **solo** con este componente y nunca hace HTTP/fetch directamente.

### 1.1 Types (mismos que el backend, subconjunto)

```text
Medicion = ( fecha_y_hora: Text, tipo: Text, valor: R )
```

### 1.2 Interface Parity with `business_logic`

| Método | Firma lógica en `business_logic` (backend) | Firma lógica aquí (cliente) | Coinciden |
|---|---|---|---|
| `obtenerMedicion()` | `obtenerMedicion() --> [ Medicion ]` | `obtenerMedicion() --> [ Medicion ]` | Sí (idéntica) |

**Subset rule:** la GUI solo consulta mediciones; `guardarMedicion()` (presente
en el backend) no se expone al cliente.

### 1.3 Module Design

```text
         --------- logicaWeb.php (proxy PHP) -----------------
         |
         |
         |        obtenerMedicion() <-- [ Medicion ]
         |        (pide al servidor REST por HTTP, filtra tipos)
         |
         |        --x RUTA_REST = http://localhost:8080/mediciones
         |
         ---------------------------------------

         --------- api/mediciones.php (endpoint JSON) -------
         |
         |
peticion HTTP --> apiMediciones() --> JSON: [ Medicion ] (200/500)
         |
         ---------------------------------------

         --------- js/mediciones.js (proxy JS) --------------
         |
         |
         |        obtenerMedicion() <-- [ Medicion ]
         |        (fetch al API; encapsula el transporte)
         |
         |        errorActual() <-- Text  (mensaje de error, '' si no hay)
         |
         ---------------------------------------
```

### 1.4 Function/Method Logical Designs

```text
obtenerMedicion() --> [ Medicion ]          (PHP, logicaWeb.php)
```

- Hace una petición HTTP GET al servidor REST (`GET /mediciones`).
- Filtra el subconjunto que la GUI muestra (`ozono`, `temperatura`).
- Devuelve las últimas 10 mediciones; si el servidor no responde, devuelve
  `[ ]`.

```text
peticion HTTP --> apiMediciones() --> JSON: [ Medicion ]
```

- Punto de entrada JSON para el navegador: delega en `obtenerMedicion()` de
  `logicaWeb.php` y devuelve el resultado serializado (`200`) o `500`.

```text
obtenerMedicion() --> [ Medicion ]          (JS, mediciones.js)
```

- Encapsula el `fetch` al endpoint `api/mediciones.php` y devuelve la lista;
  si la comunicación falla, anota el error y devuelve `[ ]`.

```text
errorActual() --> Text
```

- Devuelve el último mensaje de error de comunicación (`''` si no hay) para
  que la GUI decida mostrarlo.

## 2. Design Clarifications

- **Encapsulated communication:** la GUI nunca ve `fetch`, `XMLHttpRequest` ni
  URL REST; solo llama a `mediciones.obtenerMedicion()`.
- **Filtro de subconjunto:** la página web muestra únicamente
  `ozono` y `temperatura` (aunque la base de datos admita también `ruido`);
  el filtro vive en esta capa, no en la GUI.
- La URL del REST en el proxy PHP admite configuración por entorno; por defecto
  `localhost:8080`.

## 3. General Rules

- **Programming Language:** PHP 8 (proxy y endpoint) y JavaScript (ES, proxy de
  navegador `mediciones.js`).
- **Function/Method Headers:** cada función lleva su diseño lógico en un
  bloque de comentario con línea de guiones (`-----------`) y tipos abstractos.
- **Code Readability:** código claro y autoexplicativo.
- **Automated Testing:** pruebas para el proxy PHP
  (`src/gui/tests/logicaWeb.test.php`) y pruebas de estructura/desacople en
  `src/gui/tests/logicaWeb.test.js`, que verifican que la GUI no usa
  `fetch` y que el proxy sí lo encapsula.