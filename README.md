# Sensores Beacon

Sistema de medida de sensores mediante iBeacon BLE: un emisor publica las lecturas
por Bluetooth Low Energy, una app Android las escucha y las envía por HTTP, un
servidor REST las valida y las guarda, y una página web las muestra.

---

## 1. Descripción del proyecto

### 1.1 Qué hace

El proyecto mide **CO₂ (ppm)**, **temperatura (°C)** y **ruido** con una placa
Adafruit Bluefruit LE y difunde cada lectura dentro del propio anuncio BLE, sin
necesidad de conexión ni de emparejamiento. El resto del sistema es puramente
software:

1. La **placa emisora** publica cada medición como un anuncio iBeacon.
2. La **app Android** escanea los iBeacon, filtra por UUID, decodifica la trama
   y deduce el tipo de medición y el valor. Hace un `POST` al servidor REST con
   **solo tres campos**: `tipo`, `valor` y `nombre_emisora`. Los datos del
   beacon (`rssi`, `txPower`, `major`, `minor` y `uuid`) se quedan en la app: la
   UUID solo se usa allí para buscar el iBeacon.
3. El **servidor REST** valida el JSON recibido y se lo pasa a la **lógica de
   negocio**.
4. La **lógica de negocio** normaliza los datos y los persiste en una base de
   datos **SQLite**: `tipo_de_medicion`, `nombre_cliente` (el `nombre_emisora`
   de la app), `valor`, `fecha_y_hora` y un `id` único. Ningún dato del beacon
   llega hasta aquí.
5. La **página web** consulta el servidor por `GET` y representa las últimas diez
   mediciones.

### 1.2 Arquitectura y módulos

```
   src/emisor/      src/android/        src/communication/    src/business_logic/  src/database/
     (placa Bluefruit)      (app Android)          (REST Node.js)        (Node.js)        (SQLite)
           |                     |                      |                      |               |
           |  anuncio iBeacon     |  POST /mediciones   |                      |               |
           |  major = tipo        +--------------------->+                      |               |
           |  minor = valor       |                      |  guardarMedicion()   |               |
           |                      |                      +--------------------->+               |
           |                      |                      |                      |  guardar()    |
           |                      |                      |                      +-------------->+
           |                      |    <-- GET /mediciones (JSON, 10 últimas) -----+               |
           |                      |                      |                      |               |
           |                      |                      |                      |  obtenerUltimas10()|
           |                      |                      |                      |<--------------+
           |                      |                      |                      |               |
           |                      |  src/frontend_business_logic/ y src/gui/ -- GET /mediciones ->+
           |                      |                      |                      |               |
           v                      v                      v                      v               v
     salida por            contador en           201 / 400 / 500        normaliza        tabla
     puerto serie          pantalla + LogCat      200 / 404             y consulta      Mediciones
```

| Módulo | Ruta | Tecnología | Responsabilidad |
|---|---|---|---|
| Emisor | `src/emisor/` | Arduino (C++) | Publicar CO₂ y temperatura como iBeacon |
| App Android | `src/android/` | Java + Gradle | Escanear BLE, decodificar la trama, enviar el `POST` |
| Comunicación (REST) | `src/communication/` | Node.js (`http`) | Enrutado, validación HTTP y códigos de respuesta |
| Lógica de negocio | `src/business_logic/` | Node.js | `guardarMedicion()` / `obtenerMedicion()`, sin HTTP ni SQL |
| Base de datos | `src/database/` | SQLite 3 | Tabla `Mediciones` |
| Frontend (proxy de cliente) | `src/frontend_business_logic/` | PHP + JS | `obtenerMedicion()` hacia el REST; encapsula el transporte |
| GUI web | `src/gui/` | PHP + HTML/CSS/JS | Mostrar las últimas 10 mediciones |

La separación entre capas es estricta: la capa de comunicación **no** contiene
lógica de negocio ni accede a la base de datos, y la lógica de negocio **no**
conoce HTTP (cero dependencias de la capa de comunicación ni de frameworks web).
El acceso a datos se aísla detrás de un *repositorio* intercambiable
(`repositorioMemoria.js` en pruebas, `repositorioSqlitePythonAsync.js` en
producción), lo que permite probar la lógica sin base de datos y la base de datos
sin HTTP. La GUI tampoco hace transporte: se comunica solo con
`src/frontend_business_logic/`.

### 1.3 Estructura de carpetas

```
.
├── author.md                  # Nombre del autor (informa el reporte de la revisión)
│
├── doc/                       # Diseños formales por componente
│   ├── communication_design.md
│   ├── business_logic_design.md
│   ├── database_design.md
│   ├── frontend_business_logic_design.md
│   ├── gui_design.md
│   ├── android_design.md
│   └── emisor_design.md
│
└── src/                       # Código, organizado por capas (backend y frontend)
    ├── communication/              # Capa de comunicación: servidor REST (Node.js)
    │   ├── src/server.js           #   arranque y puerto
    │   ├── src/app.js              #   servidor HTTP + 404
    │   ├── src/routes/medicionesRoutes.js   # POST y GET /mediciones
    │   ├── src/logica/medicionesLogica.js   # puente con la lógica de negocio
    │   └── tests/mediciones.test.js         # 6 pruebas HTTP reales
    │
    ├── business_logic/             # Lógica de negocio (Node.js, sin HTTP)
    │   ├── src/logicaNegocio.js                    # núcleo, sin BD
    │   ├── src/logicaNegocioConRepositorio.js      # con repositorio inyectado
    │   ├── src/logicaNegocioSqlitePy.js            # + SQLite vía Python (sync)
    │   ├── src/logicaNegocioSqlitePyAsync.js       # + SQLite vía Python (async)
    │   ├── src/repositorioMemoria.js               # doble en memoria
    │   ├── src/repositorioSqlitePython.js          # SQLite (sync)
    │   ├── src/repositorioSqlitePythonAsync.js     # SQLite (async)
    │   └── tests/                                  # 11 pruebas
    │
    ├── database/                        # Base de datos (SQLite 3)
    │   ├── mediciones.sql               # CREATE TABLE + fila de ejemplo
    │   └── mediciones.db               # base de datos generada (no versionada)
    │
    ├── frontend_business_logic/        # Lógica de negocio de cliente (proxy)
    │   ├── logicaWeb.php               #   obtenerMedicion(): GET al REST
    │   ├── api/mediciones.php          #   endpoint JSON para el refresco
    │   └── js/mediciones.js            #   proxy JS que encapsula fetch/RUTA_API
    │
    ├── gui/                        # GUI web (PHP + HTML/CSS/JS)
    │   ├── index.php              #   renderizado en servidor
    │   ├── includes/mediciones.php
    │   ├── js/app.js              #   actualiza tarjetas y tabla cada 5 min
    │   ├── css/variables.css      #   paleta y medidas, todo en variables CSS
    │   ├── css/style.css          #   usa las variables de variables.css
    │   └── tests/                 #   11 pruebas JS + 2 comprobaciones PHP
    │
    ├── emisor/          # Firmware del emisor (Arduino IDE)
    │   ├── emisor.ino   #   setup() / loop() y orquestación
    │   ├── Publicador.h           #   publicaCO2() / publicarTemperatura()
    │   ├── EmisoraBLE.h           #   control del hardware BLE (Bluefruit)
    │   ├── Medidor.h              #   leer sensores analógicos (CO₂ y temperatura)
    │   ├── LED.h                  #   LED de feedback
    │   ├── PuertoSerie.h          #   salida por consola
    │   └── ServicioEnEmisora.h    #   definición del servicio BLE
    │
    └── android/                   # App Android (Gradle)
        ├── app/src/main/java/com/upv/sensoresbeacon/
        │   ├── MainActivity.java  #   escaneo BLE, 3 botones, contadores, LogCat
        │   ├── TramaIBeacon.java  #   decodificación de la trama iBeacon
        │   ├── Medicion.java      #   tipoDeMedicion + valor
        │   ├── MedicionesID.java  #   enumeración co2=11, temperatura=12, ruido=13
        │   ├── LogicaFake.java    #   lógica "fake": POST al servidor REST
        │   ├── PeticionarioREST.java
        │   ├── TransporteHttp.java    # interfaz (permite dobles de prueba)
        │   ├── TransporteHttpReal.java# implementación con HttpURLConnection
        │   ├── RespuestaREST.java
        │   └── Utilidades.java    #   conversión de bytes, UUID, hex
        ├── app/src/test/java/...  #   108 pruebas unitarias JUnit
        └── app/build.gradle
```

> La estructura sigue el layout de la revisión de sprint: `author.md` con el
> autor, `doc/` con un diseño por componente y `src/<componente>/` con el código.

### 1.4 Formato de la trama iBeacon

El emisor (`Publicador.h`) y la app (`TramaIBeacon.java`) comparten este
convenio, que es la clave para que los datos del sensor viajen dentro del propio
anuncio:

| Campo | Contenido | Ejemplo |
|---|---|---|
| `companyID` | Fabricante (Apple) | `0x004C` |
| Marca iBeacon | `iBeaconType` + `iBeaconLength` | `02 15` |
| `uuid` (16 bytes) | 16 caracteres **ASCII**, no una UUID hexadecimal | `EPSG-GTI-PROY-MJ` |
| `major` | `(tipoMedición << 8) + contador` | `0x0B07` → CO₂, iteración 7 |
| `minor` | **valor medido** (16 bits) | `2450` → 2450 ppm |
| `txPower` | Potencia de referencia en dBm | `-53` |

- La UUID de 16 caracteres ASCII equivale en hexadecimal a
  `45505347-2D47-5449-2D50-524F592D4D4A`, que es el valor con el que la app
  filtra (`MainActivity.UUID_BUSCADA`).
- El tipo de medición viaja en el **byte alto** del `major`
  (`getTipoDeMedicionComoInt()`) y el número de iteración en el byte bajo
  (`getContadorComoInt()`).
- `major` y `minor` son **little-endian**, como en cualquier iBeacon; la app lo
  tiene en cuenta en `Utilidades.bytesToShortLE()`.
- La tabla de asignación es `MedicionesID`: `co2 = 11`, `temperatura = 12`,
  `ruido = 13`. Cualquier otro valor devuelve `"desconocido"`.

La app **no** deduplica por MAC: la misma placa emite CO₂ y temperatura con la
misma dirección, así que la clave de duplicación combina MAC + `major` +
`minor` (`MainActivity.claveDeMedicion()`). Además muestra tres contadores
diferentes —iBeacons reconocidos, anuncios de Apple que no son iBeacon y otros
anuncios BLE— para poder distinguir "no llega nada" de "llega pero no se
reconoce".

### 1.5 Contrato de la API REST

Servidor en `http://localhost:8080` (configurable con `PORT` y `HOST`).

**`POST /mediciones`** — registra una medición.

```json
{
  "tipo": "temperatura",
  "valor": 23.5,
  "nombre_emisora": "GTI-3A-MJ"
}
```

| Código | Significado |
|---|---|
| `201 Created` | Medición guardada. Responde `{ "status": "ok", "resultado": { "ok": true, "id": N } }` |
| `400 Bad Request` | Campo obligatorio ausente, tipo incorrecto o JSON malformado |
| `500 Internal Server Error` | Fallo al guardar en la base de datos |

`tipo` y `valor` son obligatorios; `nombre_emisora` es opcional y se guarda en
la base de datos como `nombre_cliente`. El `POST` **no** lleva `rssi`,
`txPower`, `major`, `minor` ni `uuid`: esos datos se quedan en la app Android
(la UUID solo se usa allí para buscar el iBeacon). La tabla `Mediciones` guarda
únicamente `id`, `tipo_de_medicion`, `nombre_cliente`, `valor` y `fecha_y_hora`.

**`GET /mediciones`** — devuelve las últimas 10 mediciones.

```json
[
  { "fecha_y_hora": "2026-09-15 10:23:41", "tipo": "co2", "valor": 812.0 }
]
```

| Código | Significado |
|---|---|
| `200 OK` | Lista JSON (array) de las últimas 10 mediciones |
| `500 Internal Server Error` | Fallo al leer de la base de datos |

Cualquier otra ruta devuelve `404 Not Found`.

### 1.6 Esquema de la base de datos

`src/database/mediciones.sql` crea la tabla `Mediciones` con los tipos abstractos del
documento de reglas de diseño (`N`, `Z`, `R`, `Text`):

| Columna | Tipo SQLite | Nulable | Origen |
|---|---|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | no | generado por SQLite |
| `tipo_de_medicion` | `TEXT NOT NULL` | no | `tipo` del POST |
| `nombre_cliente` | `TEXT NOT NULL` | no | `nombre_emisora` del POST |
| `valor` | `REAL NOT NULL` | no | `valor` del POST |
| `fecha_y_hora` | `DATETIME DEFAULT CURRENT_TIMESTAMP` | sí | generado por SQLite |

> **Nombre de emisora = nombre del cliente.** La app envía el nombre de la
> emisora en el campo `nombre_emisora` de la petición; en la base de datos ese
> dato es el **nombre del cliente** y se almacena en la columna
> `nombre_cliente`. La tabla solo guarda esos cuatro datos: tipo de medición,
> nombre del cliente, valor y fecha/hora.

El script incluye además una fila de ejemplo.

La capa de acceso a datos **no** usa `node-sqlite3`: el repositorio invoca
`python3` con el módulo `sqlite3` de la biblioteca estándar
(`repositorioSqlitePythonAsync.js`). Así el proyecto no necesita ninguna
dependencia nativa ni `npm install`.

### 1.7 Convención de documentación del código

Todos los módulos siguen las reglas del documento de diseño lógico del enunciado:
sobre cada función o método hay un comentario con el **diseño** en cajitas y
flechas ASCII, con tipos abstractos `N`, `Z`, `R`, `Text` y `[Tipo]`:

```java
/**
 * valor: N --> asignarTipo() --> tipo: Text
 *
 * El valor numérico recibido en el campo "minor" de la trama iBeacon
 * identifica el tipo de medicion segun la enumeracion MedicionesID.
 */
```

Las flechas `-->` marcan entradas, `<--` salidas y `--x` la interacción con el
estado o con otro sistema.

---

## 2. Requisitos previos

| Componente | Requisito | Notas |
|---|---|---|
| Servidor REST y lógica de negocio | **Node.js ≥ 18** | Probado con v22.13.1. Sin dependencias: no hace falta `npm install` |
| Base de datos | **Python 3** con el módulo `sqlite3` | Incluido en Python de serie |
| Crear la base de datos | **Cliente `sqlite3`** (opcional) | Solo para el paso 3.1 |
| Página web | **PHP ≥ 7.4** con la extensión **curl** | Necesario para `logicaWeb.php` |
| App Android | **JDK 17+** y **Android SDK** (compileSdk 34) | Se usa el *Gradle Wrapper* incluido |
| Emisor | **Arduino IDE 2.x** + soporte de placa **Adafruit Bluefruit LE** | Bibliotecas `BluefruitLE` y `Adafruit_TinyUSB` |

> **Nota:** las pruebas de Node.js y de Android se ejecutan sin instalar nada.
> En la máquina donde se generó este README, **PHP no estaba instalado**, por lo
> que la página web y `src/gui/tests/logicaWeb.test.php` no se pudieron
> ejecutar; los comandos están documentados en los apartados 3.3 y 4.4.

---

## 3. Despliegue

El orden importa: la base de datos antes que el servidor, el servidor antes que
la página web y el emisor antes que la app.

### 3.1 Crear la base de datos

`src/database/mediciones.db` **no está versionada** (está en `.gitignore`,
porque es un fichero binario que cambia en cada escritura). Hay que generarla a
partir del script SQL:

```bash
cd src/database
sqlite3 mediciones.db < mediciones.sql
```

Comprobación:

```bash
sqlite3 mediciones.db ".schema Mediciones"
sqlite3 mediciones.db "SELECT COUNT(*) FROM Mediciones;"
```

Debería aparecer el esquema `CREATE TABLE Mediciones` y un conteo de `1` (la fila
de ejemplo que incluye el script).

### 3.2 Capa de comunicación (servidor REST)

```bash
cd src/communication
npm start
```

Salida esperada:

```
Servidor REST escuchando en http://0.0.0.0:8080
```

Configuración por variables de entorno:

| Variable | Por defecto | Descripción |
|---|---|---|
| `PORT` | `8080` | Puerto de escucha |
| `HOST` | `0.0.0.0` | Interfaz de escucha |

Para comprobarlo:

```bash
curl http://localhost:8080/mediciones

curl -X POST http://localhost:8080/mediciones \
  -H 'Content-Type: application/json' \
  -d '{"tipo":"co2","valor":812,"nombre_emisora":"GTI-3A-MJ"}'
```

La segunda orden debe responder `201 Created` con `{"status":"ok",...}`, y la
primera debe devolver un array JSON con la medición recién insertada.

### 3.3 GUI web (frontend)

Requiere PHP con `curl` y el servidor REST ya levantado (el paso 3.2), porque
`src/frontend_business_logic/logicaWeb.php` hace un `GET` a
`http://localhost:8080/mediciones`. La raíz del servidor PHP es la carpeta `src/`
(contiene toda la parte web):

```bash
php -S localhost:8000 -t src
```

Abre <http://localhost:8000/gui/index.php> en el navegador. Verás las dos
tarjetas superiores (último CO₂ en ppm y última temperatura en °C) y la tabla con
las últimas 10 mediciones.

- El renderizado inicial lo hace `src/gui/index.php` en el servidor.
- `src/gui/js/app.js` refresca los datos cada 5 minutos contra
  `src/frontend_business_logic/api/mediciones.php`, que actúa de proxy hacia el
  REST y evita problemas de CORS. `app.js` **no** hace `fetch` directamente: usa
  el proxy `src/frontend_business_logic/js/mediciones.js`, que es el único que
  conoce la ruta del API (`RUTA_API`). La frecuencia se cambia en la constante
  `INTERVALO_REFRESCO_MS` de `js/app.js` (en milisegundos).
- Si el servidor REST no está disponible, la página muestra un aviso de error en
  lugar de romperse.

> Para desplegar en producción, colocar `src/` como document root (o publicar
> `src/gui/` y `src/frontend_business_logic/` bajo un servidor Apache o Nginx
> con PHP) y apuntar el servidor REST a una ruta real: en PHP está fija en
> `src/frontend_business_logic/logicaWeb.php` (`$url`) y en el navegador la
> define `RUTA_API` en `src/frontend_business_logic/js/mediciones.js`.

### 3.4 Emisor (placa Adafruit Bluefruit LE)

1. Abrir `src/emisor/emisor.ino` en el Arduino IDE.
2. Seleccionar la placa **Adafruit Bluefruit LE** y el puerto USB correspondiente.
3. Instalar las bibliotecas `BluefruitLE` y `Adafruit_TinyUSB`.
4. Verificar en `Publicador.h` el nombre de la emisora (`"GTI-3A-MJ"`, el mismo
   que envía la app en `nombre_emisora` y que en la base de datos se guarda como
   `nombre_cliente`, el nombre del cliente) y el `beaconUUID`.
5. Subir el programa con el botón de upload.

Al arrancar, el `loop()` publica CO₂ y temperatura cada 2 segundos con valores
fijos de prueba (`56.78` y `12.34`). Para leer sensores reales, descomentar
`elMedidor.medirCO2()` y `elMedidor.medirTemperatura()` en `emisor.ino`.

La salida por puerto serie (115200 baudios) informa de cada iteración y de los
valores publicados.

### 3.5 App Android

1. Configurar la ruta del SDK en `src/android/local.properties` (no se versiona):
   ```properties
   sdk.dir=/Users/<tu-usuario>/Library/Android/sdk
   ```
2. Compilar el APK de depuración:
   ```bash
   cd src/android
   ./gradlew assembleDebug
   ```
   El APK queda en `src/android/app/build/outputs/apk/debug/app-debug.apk`.
3. Instalar en el emulador o en un dispositivo:
   ```bash
   ./gradlew installDebug
   ```
   o bien `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
4. Conceder los permisos de Bluetooth/Locación cuando la app los solicite, y
   activar la ubicación si se usa Android 11 o anterior (el escaneo BLE no ve
   nada sin ella).

**Dirección del servidor.** `LogicaFake` apunta por defecto a
`http://10.0.2.2:8080/mediciones`. `10.0.2.2` es el alias que el emulador usa
para alcanzar el `localhost` de la máquina anfitriona:

| Ejecución | URL correcta |
|---|---|
| Emulador Android | `http://10.0.2.2:8080/mediciones` (valor por defecto) |
| Dispositivo físico | `http://<IP-del-ordenador>:8080/mediciones` |

Para usar un dispositivo físico hay que cambiar la constante en
`LogicaFake.java` y reconstruir la app; el manifiesto ya permite tráfico en
claro (`android:usesCleartextTraffic="true"`), así que no hace falta HTTPS en el
laboratorio.

Al abrir la app se ven tres botones —**Buscar todos los iBeacons**, **Buscar por
UUID** y **Detener búsqueda**—, un contador (`iBeacons: N | Apple: N | otros
BLE: N`) y el registro de la búsqueda. Todo se vuelca también al LogCat con las
etiquetas `MainActivity`, `TramaIBeacon`, `LogicaFake` y `PeticionarioREST`.

> Nota de diseño: el enunciado pedía filtrar por el *nombre* del dispositivo
> buscado (`GTI-3A-MJ`); la implementación filtra por **UUID**, porque el nombre
> no viaja en el anuncio iBeacon. `DISPOSITIVO_BUSCADO` y `UUID_BUSCADA` siguen
> definidos en `MainActivity.java`.

### 3.6 Verificación de extremo a extremo

```bash
# 1. base de datos
sqlite3 src/database/mediciones.db "SELECT COUNT(*) FROM Mediciones;"

# 2. servidor REST
curl -s http://localhost:8080/mediciones

# 3. página web
curl -s http://localhost:8000/frontend_business_logic/api/mediciones.php

# 4. base de datos de nuevo: debe haber una fila más
sqlite3 src/database/mediciones.db "SELECT id, tipo_de_medicion, valor, fecha_y_hora FROM Mediciones ORDER BY id DESC LIMIT 3;"
```

Con la placa emitiendo y la app en marcha, el contador de filas debe aumentar
cada 2 segundos.

---

## 4. Tests

Cada módulo lleva sus propias pruebas. Ninguna requiere `npm install`: se usa el
runner integrado de Node.js (`node --test`).

| Módulo | Comando | Pruebas | Estado |
|---|---|---|---|
| Lógica de negocio | `cd src/business_logic && npm test` | 11 | 11 ✅ |
| Comunicación (REST) | `cd src/communication && npm test` | 6 | 6 ✅ |
| GUI web (estructura) | `cd src/gui && node --test` | 11 | 11 ✅ |
| GUI web (PHP) | `cd src/gui && php tests/logicaWeb.test.php` | 2 | requiere PHP + REST |
| App Android | `cd src/android && ./gradlew test` | 108 × 2 variantes | 108 ✅ |
| Emisor | — | — | sin pruebas automáticas |

**Total verificado: 136 pruebas automáticas en verde** (28 de Node.js + 108 de
Android), más 2 comprobaciones en PHP pendientes de instalar PHP.

### 4.1 Lógica de negocio — 11 pruebas

```bash
cd src/business_logic
npm test
```

Tres archivos, todos con el runner de Node:

| Archivo | Pruebas | Qué cubre |
|---|---|---|
| `tests/logicaNegocio.test.js` | 7 | `guardarMedicion()` normaliza tipo, valor y nombre de emisora sin procesar ningún dato del beacon; `obtenerMedicion()` devuelve un array de como mucho 10 elementos con `fecha_y_hora`, `tipo` y `valor` |
| `tests/logicaConRepositorio.test.js` | 3 | La lógica con repositorio inyectado: guarda, devuelve las últimas y procesa la medición sin datos del beacon |
| `tests/logicaSqlitePy.test.js` | 1 | Integración real contra `src/database/mediciones.db`: guarda una fila y la recupera |

Para ver el detalle de cada caso:

```bash
node --test --test-reporter=spec
```

Salida esperada al final:

```
# tests 11
# pass 11
# fail 0
```

### 4.2 Capa de comunicación — 6 pruebas

```bash
cd src/communication
npm test
```

`tests/mediciones.test.js` **levanta el servidor HTTP real** en un puerto
efímero y hace peticiones HTTP de verdad (`http.request`), sin mocks:

| Prueba | Comprueba |
|---|---|
| `POST /mediciones - válida` | Responde `201` solo con `tipo` y `valor` (sin datos del beacon) |
| `POST /mediciones - valida y pasa a lógica de negocio (con todos los campos)` | Responde `201` y `{ "status": "ok", "resultado": { "ok": true, "id": N } }` |
| `POST /mediciones - campos faltantes retorna 400` | Responde `400` si falta `valor` |
| `POST /mediciones - JSON inválido retorna 400` | Responde `400` |
| `GET /mediciones - retorna lista JSON` | Responde `200` con un array |
| `GET /mediciones - campos deben ser fecha_y_hora, tipo, valor` | Exactamente esos tres campos, sin datos del beacon |

Estas pruebas **escriben en la base de datos real** (el `POST` inserta una fila),
así que `src/database/mediciones.db` debe existir (paso 3.1).

### 4.3 GUI web — 11 pruebas de estructura

```bash
cd src/gui
node --test
```

> No usar `node --test tests/`: con Node 22 eso se interpreta como una ruta de
> módulo y falla.

`tests/logicaWeb.test.js` comprueba que existen `index.php`,
`../frontend_business_logic/logicaWeb.php`,
`../frontend_business_logic/api/mediciones.php`, `js/app.js` y
`css/variables.css`; que `index.php` carga `variables.css` antes de `style.css`;
que la paleta del diseño está definida en `variables.css`; que `style.css` no
tiene colores sueltos y que todas las variables que usa están definidas.
Además verifica el desacople de la GUI: `js/app.js` **no** contiene
`fetch`/`XMLHttpRequest`, `../frontend_business_logic/js/mediciones.js` sí
encapsula el `fetch` y la constante `RUTA_API`, e `index.php` carga el proxy
antes que `app.js`.

### 4.4 GUI web — 2 comprobaciones en PHP

```bash
# el servidor REST debe estar levantado (paso 3.2)
cd src/gui
php tests/logicaWeb.test.php
```

`tests/logicaWeb.test.php` llama a `obtenerMedicion()` y verifica que devuelve un
array y que no trae más de 10 elementos. Si el REST no responde, la función
devuelve un array vacío y las comprobaciones siguen pasando; para que la prueba
sea significativa, el REST debe estar en marcha.

Salida esperada:

```
test_obtenerMedicion_devuelve_array: OK
test_obtenerMedicion_maximo_10: OK
Todos los tests pasaron
```

### 4.5 App Android — 108 pruebas

```bash
cd src/android
./gradlew test
```

Ejecuta las pruebas unitarias de las dos variantes (`testDebugUnitTest` y
`testReleaseUnitTest`), es decir **108 pruebas × 2 = 216**. Para lanzar solo una
variante:

```bash
./gradlew testDebugUnitTest
```

Informe HTML: `src/android/app/build/reports/tests/testDebugUnitTest/index.html`.

| Clase de prueba | Pruebas | Qué cubre |
|---|---|---|
| `UtilidadesTest` | 21 | Conversión hex ↔ bytes, `UUID`, little/big-endian, texto ASCII → UUID |
| `FiltroUuidTest` | 16 | Validez y normalización de la UUID, filtro del fabricante `0x004C`, marca `02 15` |
| `DeteccionRealTest` | 15 | Tramas reales: localización, `major`/`minor`/`txPower`, decodificación del tipo, deduplicación, anuncios de Apple que no son iBeacon |
| `TramaIBeaconTest` | 14 | Localización de la trama dentro del anuncio AD completo y de la trama desnuda, campos, casos límite |
| `LogicaFakeTest` | 14 | Cuerpo JSON del `POST`: los tres campos del contrato, ausencia de datos del beacon (`uuid`, `txPower`, `major`, `minor`, `rssi`), tipos, valores extremos (`65535`, `-72.5`), `null` |
| `MedicionTest` | 14 | `asignarTipo()` con los códigos 11/12/13 y desconocidos, `Medicion.desdeTrama()` |
| `PeticionarioRESTTest` | 11 | Método, URL y cuerpo, códigos 2xx/3xx frente a 4xx/5xx, `TransporteHttp` inyectado |
| `LogicaFakeIntegracionTest` | 3 | **Integración real**: levanta un `ServerSocket` en un puerto efímero y comprueba que el `POST` llega con método, ruta, `Content-Type` y un JSON de solo tres campos (`tipo`, `valor`, `nombre_emisora`); y que un servidor ilegible devuelve código `-1` sin excepción |

Detalle de una sola clase:

```bash
./gradlew testDebugUnitTest --tests 'com.upv.sensoresbeacon.MedicionTest'
```

Estas pruebas no necesitan dispositivo ni emulador: son unitarias (JVM), y
`testOptions.unitTests.returnDefaultValues` resuelve las clases de `android.jar`
que no existen en la JVM. La dependencia `testImplementation 'org.json:json'`
sustituye el stub de `org.json` para que el parsing JSON sea real.

### 4.6 Ejecutar todos los tests de Node.js de una vez

Desde la raíz del repositorio, el runner descubre los tres proyectos:

```bash
node --test
```

Resultado: **28 pruebas** (11 de lógica de negocio + 6 de la capa de
comunicación + 11 de la GUI web).

---

## 5. Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `OperationalError: no such table: Mediciones` | La base de datos existe pero está vacía, o no se creó | Ejecutar el paso 3.1 |
| `python3: command not found` al hacer `POST` | Falta el intérprete que usa el repositorio | Instalar Python 3 y reiniciar el servidor REST |
| La página web muestra el aviso de error | El servidor REST no está levantado, o PHP no tiene `curl` | Levantar el REST (3.2) y verificar `php -m \| grep curl` |
| La app no detecta ningún iBeacon | Permisos sin conceder o ubicación desactivada | Conceder permisos; activar la ubicación en Android ≤ 11 |
| `iBeacons: 0` pero `otros BLE: N` | Llegan anuncios pero no se reconocen como iBeacon | Revisar el LogCat: el contador `Apple` distingue datos de Apple que no son iBeacon |
| La app no recibe nada del REST | `10.0.2.2` solo funciona en el emulador | Cambiar la URL en `LogicaFake.java` por la IP del ordenador |
| `adb: no devices/emulators found` | No hay dispositivo conectado | Arrancar un emulador o conectar el dispositivo por USB con depuración activada |
| `SDK location not found` | Falta `local.properties` | Crear `src/android/local.properties` con `sdk.dir=...` |