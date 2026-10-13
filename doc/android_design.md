# Android Application Component Design (`android`)

## 1. Component Design

**Component:** Aplicación Android del receptor (`src/android/`, paquete
`com.upv.sensoresbeacon`).

Escanea anuncios BLE, reconoce la trama iBeacon de la emisora, decodifica la
medición y la envía por `POST` al servidor REST. **No accede a la base de
datos**: su única salida es el servidor de comunicación, igual que la GUI se
apoya en el proxy de `frontend_business_logic`. La "lógica fake"
(`LogicaFake`) encapsula el transporte HTTP para que el resto de la app no
conozca `HttpURLConnection`.

### 1.1 Abstract Data Types

```text
TipoMedicion = { ozono(11), temperatura(12), ruido(13), desconocido }

Medicion      = ( tipoDeMedicion: Text, valor: R )

MedicionNueva = ( tipo: Text, valor: R, nombre_emisora: Text )

TramaIBeacon  = ( uuid: [ Byte ]_16, major: Z, minor: Z, txPower: Z )

RespuestaREST = ( codigoRespuesta: Z, cuerpoRespuesta: Text )
```

El emisor codifica `major = (tipo << 8) + contador` y el valor medido
multiplicado por 10 en `minor` (el `minor` es entero y no admite decimales; ver
`emisor_design.md`). La app lo deshace con `getTipoDeMedicionComoInt()` y
`getMinorComoInt()`, y `Medicion.desdeTrama()` divide el `minor` entre
`FACTOR_ESCALA` (10) para recuperar el valor real con 1 decimal.

### 1.2 Class Diagram

```text
                 ----------- MainActivity (Activity) -----------
                 |
                 | logicaFake: LogicaFake
                 | elEscaner: BluetoothLeScanner
                 | callbackDelEscaneo: ScanCallback
                 |
savedInstanceState: Bundle        --> onCreate() -->
                 |
                 |                    solicitarPermisosSiFaltan() -->
                 |                    inicializarBluetooth() -->
                 |                    buscarTodosLosDispositivosBTLE() -->
                 |                    buscarEsteDispositivoBTLE() -->
resultado: ScanResult             --> mostrarInformacionDelDispositivoBTLE() -->
medicion: Medicion, nombre: Text  --> enviarMedicion() -->
                 |
                 -------------------------------------------------

        --------------- LogicaFake (proxy de comunicación) ---------------
        |
        | medicion: Medicion
        | urlServidor: Text
        | transporte: TransporteHttp
        |
medicion: Medicion, nombre_emisora: Text --> guardarMedicion() -->
        |
nombre_emisora: Text                     --> construirCuerpo() --> cuerpo: Text
        |
usuario: lo intercambiable arriba
        ------------------------------------------------------------------

     --- TramaIBeacon ---        --- Medicion ---        --- MedicionesID (enum) ---
     | bytes: [Z]       |        | tipoDeMedicion |      | ozono(11)               |
     | uuid/major/minor |        | valor: R       |      | temperatura(12)       |
     | txPower          |        ------------------      | ruido(13)             |
     ---------------------------------------             | desconocido           |
                                                         -------------------------

   --- TransporteHttp (interfaz) ---   --- RespuestaREST ---   --- Utilidades (--x) ---
   | enviar()                      |   | codigoRespuesta  |   | bytes <-> Text/UUID  |
   ---------------------------------   | cuerpoRespuesta  |   ------------------------
                                       ---------------------
```

### 1.3 Class/Method Logical Designs

```text
savedInstanceState: Bundle --> MainActivity.onCreate() -->
```

- Prepara la interfaz, pide los permisos BLE/Locación, inicializa el escáner y
  la lógica fake, y engancha los tres botones.

```text
--> MainActivity.solicitarPermisosSiFaltan() -->
```

- Pide `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` y `ACCESS_FINE_LOCATION` en
  Android 12+, o solo `ACCESS_FINE_LOCATION` antes; solo solicita los no
  concedidos. No se declara `neverForLocation` en `BLUETOOTH_SCAN` porque el
  sistema filtraría los resultados de los iBeacon.

```text
--> MainActivity.buscarEsteDispositivoBTLE() -->
```

- Valida la UUID buscada, prepara el escaneo y registra el callback filtrando
  por los datos de fabricante de Apple (marca iBeacon `02 15`).

```text
resultado: ScanResult --> MainActivity.mostrarInformacionDelDispositivoBTLE() -->
```

- Comprueba que el anuncio lleva trama iBeacon; si no, lo contabiliza (y avisa
  de los anuncios Apple que no son iBeacon) y sale.
- Descarta duplicados por `claveDeMedicion()` (MAC + major + minor), construye
  la `Medicion` con `Medicion.desdeTrama()` y la envía en un hilo aparte.

```text
medicion: Medicion, nombre: Text --> MainActivity.enviarMedicion() -->
```

- Lanza en un hilo de trabajo `LogicaFake.guardarMedicion(medicion, nombre)`,
  sin bloquear la interfaz. Solo viajan `tipo`, `valor` y `nombre`.

```text
texto: Text --> MainActivity.uuidComoHexadecimal() --> hexadecimal: Text   (--x)
```

- Traduce el texto ASCII de 16 caracteres que declara el emisor a la UUID
  hexadecimal 8-4-4-4-12 que viaja en la trama.

```text
--> LogicaFake.LogicaFake( urlServidor: Text, transporte: TransporteHttp ) -->
```

- Guarda la URL del REST (por defecto `http://10.0.2.2:8080/mediciones` en el
  emulador) y el transporte a usar (real o doble de prueba).

```text
medicion: Medicion, nombre_emisora: Text --> LogicaFake.guardarMedicion() -->
```

- Serializa el cuerpo y realiza `POST /mediciones` con
  `PeticionarioREST`, delegando el transporte real en `TransporteHttp`.

```text
nombre_emisora: Text --> LogicaFake.construirCuerpo() --> cuerpo: Text
```

- Construye el JSON `{ tipo, valor, nombre_emisora }`. Ningún dato del beacon
  (`uuid`, `txPower`, `major`, `minor`) entra en el cuerpo.

```text
trama: TramaIBeacon --> Medicion.desdeTrama() --> medicion: Medicion   (--x)
```

- El `valor` es `trama.getMinorComoInt()` y el tipo es
  `trama.getTipoDeMedicionComoInt()` (byte alto del major).

```text
bytes: [Z] --> TramaIBeacon.esTramaIBeacon() --> B   (--x)
bytes: [Z] --> TramaIBeacon.localizarTrama() --> inicio: Z   (--x)
```

- Recorre las estructuras AD buscando datos de fabricante (tipo `0xFF`); si el
  anuncio no sigue esa estructura, busca la marca `02 15` en cualquier
  posición (trama del laboratorio). Devuelve `NO_ENCONTRADA` si no hay iBeacon.

```text
--> TramaIBeacon.getUUID() / getMajorComoInt() / getMinorComoInt() / getTxPower() <--
```

- Accesores de los campos de la trama (los dos primeros big/little-endian
  según el estándar iBeacon).

```text
codigo: Z --> MedicionesID.getTipo() --> Text   (--x)
codigo: Z --> MedicionesID.existe() --> B   (--x)
```

- Enumeración de los tipos (`ozono = 11`, `temperatura = 12`, `ruido = 13`) y su
  traducción a/desde el código numérico del `major`.

```text
metodo: Text, urlDestino: Text, cuerpo: Text --> TransporteHttp.enviar() --> RespuestaREST
```

- Interfaz de transporte: permite sustituir `TransporteHttpReal`
  (`HttpURLConnection`) por un doble de prueba.

```text
metodo: Text, urlDestino: Text, elCuerpo: Text --> PeticionarioREST.hacerPeticionarioREST() --> RespuestaREST
```

- Envoltorio asíncrono (`AsyncTask`) que delega en `transporte.enviar()`.

```text
texto: Text --> Utilidades.stringToBytes() --> bytes: [Z]   (--x)
bytes: [Z] --> Utilidades.bytesToHexString() --> Text   (--x)
texto: Text --> Utilidades.bytesDesdeTextoAscii() --> bytes: [Z]   (--x)
```

- Conversiones entre bytes, texto hexadecimal, texto ASCII y `UUID`; incluidas
  las lecturas big/little-endian (`bytesToInt`, `bytesToLong`,
  `bytesToShortLE`).

### 1.4 External Transport Contract

```text
POST http://10.0.2.2:8080/mediciones
     cuerpo: ( tipo: Text, valor: R, nombre_emisora: Text )
  -> 201 (ok) / 400 (entrada invalida) / 500 (error interno)
```

Los datos del beacon no viajan en el `POST`.

## 2. Design Clarifications

- **UUID de 16 caracteres ASCII, no hexadecimal:** el emisor escribe en los 16
  bytes de UUID el texto `"EPSG-GTI-PROY-MJ"` (guiones incluidos como byte
  `0x2D`). Su equivalente hexadecimal, que es lo que viaja por el aire y lo que
  filtra la app, es `45505347-2D47-5449-2D50-524F592D4D4A`
  (`UUID_BUSCADA`).
- **Deduplicación por medición, no por dispositivo:** la misma placa emite Ozono
  y temperatura con la misma MAC; si se dedujera solo por MAC, solo se
  registraría la primera medición. Por eso la clave es
  `direccion | major | minor`.
- **Filtro iBeacon por datos de fabricante:** un iBeacon no anuncia Service
  UUID, así que un `ScanFilter` por servicio devolvería cero resultados; se
  filtra por los datos de fabricante de Apple (`companyID = 0x004C` + marca
  `02 15`). "Buscar todos" escanea sin filtro para diagnóstico.
- **Transporte intercambiable:** `TransporteHttp` desacopla la lógica del
  cliente HTTP real (`TransporteHttpReal`), igual que el proxy web encapsula
  su `fetch`; así la app se puede probar sin servidor.
- **Los datos del beacon se quedan en la app:** `rssi`, `txPower`, `major`,
  `minor` y `uuid` solo se muestran en pantalla; la `uuid` sirve para localizar
  el iBeacon y ninguno de ellos entra en el `POST`.

## 3. General Rules

- **Programming Language:** Java (Android SDK; `minSdk` con BLE y `AsyncTask`,
  `appcompat`).
- **Function/Method Headers:** cada función o método lleva su diseño lógico en
  un bloque de comentario delimitado por una línea de guiones (`-----------`),
  con tipos abstractos (`N`, `Z`, `R`, `B`, `Text`, `[Tipo]`); los métodos
  estáticos se marcan con `--x`.
- **Code Readability:** código claro y autoexplicativo, con el mínimo de
  comentarios adicionales.
- **Automated Testing:** casos de prueba (unitarias/integración) para las
  clases con lógica pura y testeable sin hardware (`Utilidades`,
  `TramaIBeacon`, `Medicion`, `MedicionesID`, `LogicaFake` con `TransporteHttp`
  de doble); se ejecutan con el marco de pruebas de Android/JVM.
