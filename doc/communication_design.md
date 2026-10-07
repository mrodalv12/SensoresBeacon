# Communication Component Design (`communication`)

## 1. Component Design

**Component:** `communication` — Servidor REST en `src/communication/`.

Transporta las peticiones entrantes (REST/HTTP) y delega en la lógica de
negocio (`business_logic`). No contiene lógica de negocio ni acceso directo a
la base de datos.

### 1.1 External Transport Contract

```
POST /mediciones  ->  cuerpo: ( tipo: Text, valor: R, nombre_emisora: Text? )
                  ->  201 (ok) / 400 (entrada invalida) / 500 (error interno)

GET  /mediciones  ->  [] u objeto vacio si no hay, lista JSON de [ Medicion ]
                  ->  200 (ok) / 500 (error interno)
```

Los datos del beacon (`rssi`, `txPower`, `major`, `minor`, `uuid`) **no viajan
en el POST**: se quedan en la aplicación Android (la `uuid` se usa solo ahí
para localizar el iBeacon).

Tipos abstractos de dominio utilizados (ver `business_logic_design.md`):

```text
Medicion  = ( fecha_y_hora: Text, tipo: Text, valor: R )
MedicionNueva = ( tipo: Text, valor: R, nombre_cliente: Text )
```

### 1.2 Class/Module Diagram

```text
--------- communication (servidor REST) ----------
|
| (http.createServer)
|
peticion: PeticionHTTP --> enrutar() -->
|
|
ruta, metodo: Text   --> routerMediciones() --> enrutado: B
|
|
peticion HTTP        --> handlePostMediciones() --> respuesta: 201/400/500
|
|
peticion HTTP        --> handleGetMediciones() --> respuesta: 200/500
|
|  (invoca a src/business_logic)
|
-----------------------------------------------
```

### 1.3 Function/Method Logical Designs

```text
peticion: PeticionHTTP --> enrutar() --> enrutado: B
```

- Lee la petición, delega en `routerMediciones()` y, si no se enruta,
  responde `404`.

```text
metodo: Text, ruta: Text --> routerMediciones() --> enrutado: B
```

- Empareja `POST /mediciones` y `GET /mediciones` con sus manejadores.
- Devuelve `B` indicando si la ruta se ha atendido.

```text
cuerpo: Text --> handlePostMediciones() --> codigo: Z
```

- Valida los campos obligatorios y sus tipos (Text para `tipo` y
  `nombre_emisora` opcional, número real finito para `valor`).
- Construye `medicion = (tipo, valor, nombre_emisora?)` y delega en
  `business_logic.guardarMedicion()`.
- Ignora cualquier campo desconocido del cuerpo (los datos del beacon no
  llegan, pero la capa no falla si alguien los envía).
- Traduce el resultado a `201`, `400` o `500`.

```text
handleGetMediciones() --> [ Medicion ]
```

- Pide a `business_logic.obtenerMedicion()` las últimas 10 mediciones y las
  devuelve como JSON (`200`) o devuelve `500` si la lógica falla.

## 2. Design Clarifications

- La validación de tipos de esta capa es sintáctica (presencia y tipo de
  campo); el dominio del tipo de medición responde al enum
  `TipoMedicion = { co2, temperatura, ruido }` documentado en
  `business_logic_design.md` y lo aporta la aplicación móvil.
- El campo `nombre_emisora` del POST es el **nombre del cliente**: la capa de
  comunicación lo reenvía dentro del `medicion` que entrega a la lógica, y la
  lógica lo persiste en la columna `nombre_cliente` de la base de datos (ver
  `database_design.md`).
- **Los datos del beacon no llegan hasta aquí:** `rssi`, `txPower`, `major`,
  `minor` y `uuid` se quedan en la aplicación Android; la `uuid` solo se usa en
  la app para filtrar el iBeacon. El contrato del POST se limita a `tipo`,
  `valor` y `nombre_emisora`, y ninguno de ellos se persiste como beacon.

## 3. General Rules

- **Programming Language:** Node.js (JavaScript, CommonJS). Única dependencia:
  el módulo `http` de la biblioteca estándar. No se usa ningún framework web.
- **Function/Method Headers:** cada función lleva su diseño lógico en un
  bloque de comentario delimitado por una línea de guiones (`-----------`),
  incluyendo tipos abstractos (`N`, `Z`, `R`, `B`, `Text`, `[Tipo]`).
- **Code Readability:** el código debe ser claro y autoexplicativo, con el
  mínimo de comentarios adicionales.
- **Automated Testing:** se generan casos de prueba (unitarias/integración)
  para las funciones críticas; se ejecutan con `node --test` en
  `src/communication/tests/`.