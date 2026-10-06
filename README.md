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
2. La **app Android** escanea los iBeacon, decodifica la trama, deduce el tipo de
   medición y el valor, y hace un `POST` al servidor REST.
3. El **servidor REST** valida el JSON recibido y se lo pasa a la **lógica de
   negocio**.
4. La **lógica de negocio** normaliza los datos y los persiste en una base de
   datos **SQLite**.
5. La **página web** consulta el servidor por `GET` y representa las últimas diez
   mediciones.

### 1.2 Arquitectura y módulos

```
   HolaMundoIBeacon/          proyecto/            servidor-rest/        logica-negocio/      bbdd/
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
           |                      |            pagina-web/ (PHP)  ---- curl GET /mediciones ------>+
           |                      |                      |                      |               |
           v                      v                      v                      v               v
     salida por            contador en           201 / 400 / 500        normaliza        tabla
     puerto serie          pantalla + LogCat      200 / 404             y consulta      Mediciones
```

| Módulo | Ruta | Tecnología | Responsabilidad |
|---|---|---|---|
| Emisor | `HolaMundoIBeacon/` | Arduino (C++) | Publicar CO₂ y temperatura como iBeacon |
| App Android | `proyecto/` | Java + Gradle | Escanear BLE, decodificar la trama, enviar el `POST` |
| Servidor REST | `servidor-rest/` | Node.js (`http`) | Enrutado, validación HTTP y códigos de respuesta |
| Lógica de negocio | `logica-negocio/` | Node.js | `guardarMedicion()` / `obtenerMedicion()`, sin HTTP ni SQL |
| Base de datos | `bbdd/` | SQLite 3 | Tabla `Mediciones` |
| Página web | `pagina-web/` | PHP + HTML/CSS/JS | Mostrar las últimas 10 mediciones |

La separación entre capas es estricta: el servidor REST **no** contiene lógica de
negocio ni accede a la base de datos, y la lógica de negocio **no** conoce HTTP.
El acceso a datos se aísla detrás de un *repositorio* intercambiable
(`repositorioMemoria.js` en pruebas, `repositorioSqlitePythonAsync.js` en
producción), lo que permite probar la lógica sin base de datos y la base de datos
sin HTTP.

### 1.3 Estructura de carpetas

```
.
├── HolaMundoIBeacon/          # Firmware del emisor (Arduino IDE)
│   ├── HolaMundoIBeacon.ino   #   setup() / loop() y orquestación
│   ├── Publicador.h           #   publicaCO2() / publicarTemperatura()
│   ├── EmisoraBLE.h           #   control del hardware BLE (Bluefruit)
│   ├── Medidor.h              #   leer sensores analógicos (CO₂ y temperatura)
│   ├── LED.h                  #   LED de feedback
│   ├── PuertoSerie.h          #   salida por consola
│   └── ServicioEnEmisora.h    #   definición del servicio BLE
│
├── proyecto/                  # App Android (Gradle)
│   ├── app/src/main/java/com/upv/sensoresbeacon/
│   │   ├── MainActivity.java  #   escaneo BLE, 3 botones, contadores, LogCat
│   │   ├── TramaIBeacon.java  #   decodificación de la trama iBeacon
│   │   ├── Medicion.java      #   tipoDeMedicion + valor
│   │   ├── MedicionesID.java  #   enumeración co2=11, temperatura=12, ruido=13
│   │   ├── LogicaFake.java    #   lógica "fake": POST al servidor REST
│   │   ├── PeticionarioREST.java
│   │   ├── TransporteHttp.java    # interfaz (permite dobles de prueba)
│   │   ├── TransporteHttpReal.java# implementación con HttpURLConnection
│   │   ├── RespuestaREST.java
│   │   └── Utilidades.java    #   conversión de bytes, UUID, hex
│   ├── app/src/test/java/...  #   108 pruebas unitarias JUnit
│   └── app/build.gradle
│
├── servidor-rest/             # Servidor REST (Node.js, sin dependencias)
│   ├── src/server.js          #   arranque y puerto
│   ├── src/app.js             #   servidor HTTP + 404
│   ├── src/routes/medicionesRoutes.js   # POST y GET /mediciones
│   ├── src/logica/medicionesLogica.js   # puente con la lógica de negocio
│   └── tests/mediciones.test.js         # 6 pruebas HTTP reales
│
├── logica-negocio/            # Lógica de negocio (Node.js, sin dependencias)
│   ├── src/logicaNegocio.js                    # núcleo, sin BD
│   ├── src/logicaNegocioConRepositorio.js      # con repositorio inyectado
│   ├── src/logicaNegocioSqlitePy.js            # + SQLite vía Python (sync)
│   ├── src/logicaNegocioSqlitePyAsync.js       # + SQLite vía Python (async)
│   ├── src/repositorioMemoria.js               # doble en memoria
│   ├── src/repositorioSqlitePython.js          # SQLite (sync)
│   ├── src/repositorioSqlitePythonAsync.js     # SQLite (async)
│   └── tests/                                  # 10 pruebas
│
├── pagina-web/                # Página web (PHP)
│   ├── index.php              #   renderizado en servidor
│   ├── php/logicaWeb.php      #   obtenerMedicion(): GET al REST
│   ├── includes/mediciones.php
│   ├── api/mediciones.php     #   endpoint JSON para el refresco del navegador
│   ├── js/app.js              #   actualiza tarjetas y tabla cada 5 min
│   ├── css/variables.css      #   paleta y medidas, todo en variables CSS
│   ├── css/style.css          #   usa las variables de variables.css
│   └── tests/                 #   8 pruebas JS + 2 comprobaciones PHP
│
└── bbdd/
    ├── mediciones.sql          # CREATE TABLE + fila de ejemplo
    └── mediciones.db           # base de datos generada (no versionada)
```

### 1.4 Formato de la trama iBeacon

El emisor (`Publicador.h`) y la app (`TramaIBeacon.java`) comparten este
convenio, que es la clave para que los datosensorsviajen dentro del propio
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
misma dirección, así que la clave de deduplicación combina MAC + `major` +
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
  "uuid": "45505347-2D47-5449-2D50-524F592D4D4A",
  "txPower": -59,
  "major": 12,
  "minor": 2450,
  "nombre_emisora": "GTI-3A-MJ"
}
```

| Código | Significado |
|---|---|
| `201 Created` | Medición guardada. Responde `{ "status": "ok", "resultado": { "ok": true, "id": N } }` |
| `400 Bad Request` | Campo obligatorio ausente, tipo incorrecto o JSON malformado |
| `500 Internal Server Error` | Fallo al guardar en la base de datos |

`uuid`, `major`, `minor` y `txPower` son obligatorios en la petición, igual que
`tipo` y `valor`. `nombre_emisora` es opcional.

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

`bbdd/mediciones.sql` crea la tabla `Mediciones` con los tipos abstractos del
documento de reglas de diseño (`N`, `Z`, `R`, `Text`):

| Columna | Tipo SQLite | Nulable | Origen |
|---|---|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | no | generado por SQLite |
| `tipo_de_medicion` | `TEXT NOT NULL` | no | `tipo` del POST |
| `valor` | `REAL NOT NULL` | no | `valor` del POST |
| `nombre_emisora` | `TEXT NOT NULL` | no | `nombre_emisora` del POST |
| `fecha_y_hora` | `DATETIME DEFAULT CURRENT_TIMESTAMP` | sí | generado por SQLite |
| `uuid` | `TEXT` | sí | `uuid` del POST |
| `major` | `INTEGER` | sí | `major` del POST |
| `minor` | `INTEGER` | sí | `minor` del POST |
| `txPower` | `INTEGER` | sí | `txPower` del POST |

Los tipos `Z` (`major`, `minor`, `txPower`) y `uuid` no llevan `NOT NULL` porque son
atributos opcionales del dispositivo emisor. El script incluye además una fila de
ejemplo.

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
| Página web | **PHP ≥ 7.4** con la extensión **curl** | Necesario para `paginaWeb.php` |
| App Android | **JDK 17+** y **Android SDK** (compileSdk 34) | Se usa el *Gradle Wrapper* incluido |
| Emisor | **Arduino IDE 2.x** + soporte de placa **Adafruit Bluefruit LE** | Bibliotecas `BluefruitLE` y `Adafruit_TinyUSB` |

> **Nota:** las pruebas de Node.js y de Android se ejecutan sin instalar nada.
> En la máquina donde se generó este README, **PHP no estaba instalado**, por lo
> que la página web y `pagina-web/tests/logicaWeb.test.php` no se pudieron
> ejecutar; los comandos están documentados en los apartados 3.3 y 4.4.

---

## 3. Despliegue

El orden importa: la base de datos antes que el servidor, el servidor antes que
la página web y el emisor antes que la app.

### 3.1 Crear la base de datos

`bbdd/mediciones.db` **no está versionada** (está en `.gitignore`, porque es un
fichero binario que cambia en cada escritura). Hay que generarla a partir del
script SQL:

```bash
cd bbdd
sqlite3 mediciones.db < mediciones.sql
```

Comprobación:

```bash
sqlite3 mediciones.db ".schema Mediciones"
sqlite3 mediciones.db "SELECT COUNT(*) FROM Mediciones;"
```

Debería aparecer el esquema `CREATE TABLE Mediciones` y un conteo de `1` (la fila
de ejemplo que incluye el script).

### 3.2 Servidor REST

```bash
cd servidor-rest
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
  -d '{"tipo":"co2","valor":812,"uuid":"45505347-2D47-5449-2D50-524F592D4D4A","txPower":-59,"major":11,"minor":812,"nombre_emisora":"GTI-3A-MJ"}'
```

La segunda orden debe responder `201 Created` con `{"status":"ok",...}`, y la
primera debe devolver un array JSON con la medición recién insertada.

### 3.3 Página web

Requiere PHP con `curl` y el servidor REST ya levantado (el paso 3.2), porque
`pagina-web/php/logicaWeb.php` hace un `GET` a `http://localhost:8080/mediciones`.

```bash
cd pagina-web
php -S localhost:8000
```

Abre <http://localhost:8000> en el navegador. Verás las dos tarjetas superiores
(último CO₂ en ppm y última temperatura en °C) y la tabla con las últimas 10
mediciones.

- El renderizado inicial lo hace `index.php` en el servidor.
- `js/app.js` refresca los datos cada 5 minutos contra `api/mediciones.php`, que
  actúa de proxy hacia el REST y evita problemas de CORS. La frecuencia se
  cambia en la constante `INTERVALO_REFRESCO_MS` de `js/app.js` (en
  milisegundos).
- Si el servidor REST no está disponible, la página muestra un aviso de error en
  lugar de romperse.

> Para desplegar en producción, colocar el directorio `pagina-web/` bajo un
> servidor Apache o Nginx con PHP y apuntar el servidor REST a una ruta real
> (hoy está fija en `logicaWeb.php`, línea 21).

### 3.4 Emisor (placa Adafruit Bluefruit LE)

1. Abrir `HolaMundoIBeacon/HolaMundoIBeacon.ino` en el Arduino IDE.
2. Seleccionar la placa **Adafruit Bluefruit LE** y el puerto USB correspondiente.
3. Instalar las bibliotecas `BluefruitLE` y `Adafruit_TinyUSB`.
4. Verificar en `Publicador.h` el nombre de la emisora (`"GTI-3A-MJ"`, el mismo
   que envía la app en `nombre_emisora`) y el `beaconUUID`.
5. Subir el programa con el botón de upload.

Al arrancar, el `loop()` publica CO₂ y temperatura cada 2 segundos con valores
fijos de prueba (`56.78` y `12.34`). Para leer sensores reales, descomentar
`elMedidor.medirCO2()` y `elMedidor.medirTemperatura()` en `HolaMundoIBeacon.ino`.

La salida por puerto serie (115200 baudios) informa de cada iteración y de los
valores publicados.

### 3.5 App Android

1. Configurar la ruta del SDK en `proyecto/local.properties` (no se versiona):
   ```properties
   sdk.dir=/Users/<tu-usuario>/Library/Android/sdk
   ```
2. Compilar el APK de depuración:
   ```bash
   cd proyecto
   ./gradlew assembleDebug
   ```
   El APK queda en `proyecto/app/build/outputs/apk/debug/app-debug.apk`.
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
sqlite3 bbdd/mediciones.db "SELECT COUNT(*) FROM Mediciones;"

# 2. servidor REST
curl -s http://localhost:8080/mediciones

# 3. página web
curl -s http://localhost:8000/api/mediciones.php

# 4. base de datos de nuevo: debe haber una fila más
sqlite3 bbdd/mediciones.db "SELECT id, tipo_de_medicion, valor, fecha_y_hora FROM Mediciones ORDER BY id DESC LIMIT 3;"
```

Con la placa emitiendo y la app en marcha, el contador de filas debe aumentar
cada 2 segundos.

---

## 4. Tests

Cada módulo lleva sus propias pruebas. Ninguna requiere `npm install`: se usa el
runner integrado de Node.js (`node --test`).

| Módulo | Comando | Pruebas | Estado |
|---|---|---|---|
| Lógica de negocio | `cd logica-negocio && npm test` | 10 | 10 ✅ |
| Servidor REST | `cd servidor-rest && npm test` | 6 | 6 ✅ |
| Página web (estructura) | `cd pagina-web && node --test` | 5 | 5 ✅ |
| Página web (PHP) | `cd pagina-web && php tests/logicaWeb.test.php` | 2 | requiere PHP + REST |
| App Android | `cd proyecto && ./gradlew test` | 108 × 2 variantes | 108 ✅ |
| Emisor | — | — | sin pruebas automáticas |

**Total verificado: 129 pruebas automáticas en verde** (21 de Node.js + 108 de
Android), más 2 comprobaciones en PHP pendientes de instalar PHP.

### 4.1 Lógica de negocio — 10 pruebas

```bash
cd logica-negocio
npm test
```

Tres archivos, todos con el runner de Node:

| Archivo | Pruebas | Qué cubre |
|---|---|---|
| `tests/logicaNegocio.test.js` | 6 | `guardarMedicion()` normaliza tipo, valor y beacon; `obtenerMedicion()` devuelve un array de como mucho 10 elementos con `fecha_y_hora`, `tipo` y `valor` |
| `tests/logicaConRepositorio.test.js` | 3 | La lógica con repositorio inyectado: guarda, devuelve las últimas y procesa beacon + medición |
| `tests/logicaSqlitePy.test.js` | 1 | Integración real contra `bbdd/mediciones.db`: guarda una fila y la recupera |

Para ver el detalle de cada caso:

```bash
node --test --test-reporter=spec
```

Salida esperada al final:

```
# tests 10
# pass 10
# fail 0
```

### 4.2 Servidor REST — 6 pruebas

```bash
cd servidor-rest
npm test
```

`tests/mediciones.test.js` **levanta el servidor HTTP real** en un puerto
efímero y hace peticiones HTTP de verdad (`http.request`), sin mocks:

| Prueba | Comprueba |
|---|---|
| `POST /mediciones - válida` | Responde `201` |
| `POST /mediciones - valida y pasa a lógica de negocio` | Responde `201` y `status: "ok"` con todos los campos, incluido `nombre_emisora` |
| `POST /mediciones - campos faltantes retorna 400` | Responde `400` |
| `POST /mediciones - JSON inválido retorna 400` | Responde `400` |
| `GET /mediciones - retorna lista JSON` | Responde `200` con un array |
| `GET /mediciones - campos fecha_y_hora, tipo, valor` | Los tres campos existen |

Estas pruebas **escriben en la base de datos real** (el `POST` inserta una fila),
así que `bbdd/mediciones.db` debe existir (paso 3.1).

### 4.3 Página web — 8 pruebas de estructura

```bash
cd pagina-web
node --test
```

> No usar `node --test tests/`: con Node 22 eso se interpreta como una ruta de
> módulo y falla.

`tests/logicaWeb.test.js` comprueba que existen `index.php`,
`php/logicaWeb.php`, `api/mediciones.php`, `js/app.js` y `css/variables.css`; que
`index.php` carga `variables.css` antes de `style.css`; que la paleta del diseño
está definida en `variables.css`; que `style.css` no tiene colores sueltos y que
todas las variables que usa están definidas.

### 4.4 Página web — 2 comprobaciones en PHP

```bash
# el servidor REST debe estar levantado (paso 3.2)
cd pagina-web
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
cd proyecto
./gradlew test
```

Ejecuta las pruebas unitarias de las dos variantes (`testDebugUnitTest` y
`testReleaseUnitTest`), es decir **108 pruebas × 2 = 216**. Para lanzar solo una
variante:

```bash
./gradlew testDebugUnitTest
```

Informe HTML: `proyecto/app/build/reports/tests/testDebugUnitTest/index.html`.

| Clase de prueba | Pruebas | Qué cubre |
|---|---|---|
| `UtilidadesTest` | 21 | Conversión hex ↔ bytes, `UUID`, little/big-endian, texto ASCII → UUID |
| `FiltroUuidTest` | 16 | Validez y normalización de la UUID, filtro del fabricante `0x004C`, marca `02 15` |
| `DeteccionRealTest` | 15 | Tramas reales: localización, `major`/`minor`/`txPower`, decodificación del tipo, deduplicación, anuncios de Apple que no son iBeacon |
| `TramaIBeaconTest` | 14 | Localización de la trama dentro del anuncio AD completo y de la trama desnuda, campos, casos límite |
| `LogicaFakeTest` | 14 | Cuerpo JSON del `POST`: tipos, valores extremos (`65535`, `-128`), `null` |
| `MedicionTest` | 14 | `asignarTipo()` con los códigos 11/12/13 y desconocidos, `Medicion.desdeTrama()` |
| `PeticionarioRESTTest` | 11 | Método, URL y cuerpo, códigos 2xx/3xx frente a 4xx/5xx, `TransporteHttp` inyectado |
| `LogicaFakeIntegracionTest` | 3 | **Integración real**: levanta un `ServerSocket` en un puerto efímero y comprueba que el `POST` llega con método, ruta, `Content-Type` y JSON correctos; y que un servidor ilegible devuelve código `-1` sin excepción |

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

Resultado: **21 pruebas** (10 de lógica de negocio + 6 del servidor REST + 5 de
la página web).

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
| `SDK location not found` | Falta `local.properties` | Crear `proyecto/local.properties` con `sdk.dir=...` |