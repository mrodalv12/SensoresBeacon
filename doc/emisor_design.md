# Emisor Component Design (`emisor`)

## 1. Component Design

**Component:** Emisor Arduino/Bluefruit (`src/emisor/`).

Es la placa que mide y emite. En cada iteración del `loop()` toma la lectura de
Ozono y la de temperatura y publica **cada una como un anuncio iBeacon distinto**,
codificando el tipo de medición en el campo `major` y el valor medido en el
`minor`, con un UUID fijo. El público receptor es la aplicación Android
(`android_design.md`).

### 1.1 Abstract Data Types

```text
TipoMedicion = { ozono(11), temperatura(12), ruido(13) }

Medicion   = ( tipo: Text, valor: R )

TramaIBeacon = ( uuid: [ Byte ]_16, major: Z, minor: Z, txPower: Z )
```

El campo `major` se compone así:

```text
major = ( codigoTipo << 8 ) + contador
minor = redondear( valorMedido * 10 )
```

El `minor` es un entero de 16 bits y no admite decimales: el valor viaja
multiplicado por 10 (`Publicador.FACTOR_ESCALA`) y la app Android lo divide por
el mismo factor (`Medicion.FACTOR_ESCALA`), conservando 1 decimal.

`uuid` es siempre el mismo en todos los anuncios; `txPower` es fijo.

### 1.2 Class Diagram

```text
       ------------ emisor.ino (programa principal) ------------
       |
       | Globales:: elLED: LED(7), elPuerto: PuertoSerie(115200)
       |            elPublicador: Publicador, elMedidor: Medidor
       | Loop:: cont: N
       |
       | --> inicializarPlaquita() -->     (reserva para configurar la placa)
       | --> setup() -->
       | --> lucecitas() -->              (parpadeo de feedback)
       | --> loop() -->
       -------------------------------------------------------------------

  --- LED ---            --- PuertoSerie ---         --- Publicador ---
  | numeroLED |          | baudios       |           | laEmisora      |
  | encendido |          ------------------           | beaconUUID     |
  | encender  |          | escribir      |           | RSSI           |
  | apagar    |          |               |           | MedicionesID   |
  | alternar  |          ------------------           | publicarOzono    |
  | brillar   |                                       | publicarTemp.  |
  ------------                                        -----------------
                                                              |
                                                              | usa
                                                              v
  --- Medidor ---                                    --- EmisoraBLE ---
  | PIN_VGAS    |                                    | nombreEmisora  |
  | PIN_VREF    |                                    | fabricanteID   |
  | PIN_VTEMP   |                                    | txPower        |
  | iniciar     |                                    | encenderEmisora|
  | medirOzono    |                                    | emitirAnuncio  |
  | medirTemp.  |                                    | detenerAnuncio |
  ---------------                                    -----------------
                                                              |
                                                              | usa
                                                              v
                                                --- ServicioEnEmisora ---
                                                | Caracteristica         |
                                                | activarServicio        |
                                                ----------------------------------
```

### 1.3 Class/Method Logical Designs

```text
--> emisor.inicializarPlaquita() -->
```

- Punto reservado para la configuración inicial del hardware de la placa. Hoy
  el cuerpo está vacío. No recibe ni devuelve nada.

```text
--> emisor.setup() -->
```

- Inicializa la placa, enciende la emisora
  (`elPublicador.encenderEmisora()`) e inicia el medidor
  (`elMedidor.iniciarMedidor()`).

```text
--> emisor.loop() -->
```

- Incrementa `Loop::cont`, avisa por consola, mide Ozono y temperatura y publica
  ambas con `publicarOzono()` y `publicarTemperatura()`, cada una durante
  `tiempoEspera` ms; finalmente detiene el anuncio.

```text
tiempo: N --> esperar() -->
```

- Pausa el programa `tiempo` milisegundos usando `delay()`.

```text
--> LED.encender() / apagar() / alternar() / brillar( tiempo: N ) -->
```

- Controlan el LED físico del pin indicado; `brillar()` enciende, espera y
  apaga (un parpadeo).

```text
baudios: N --> PuertoSerie.PuertoSerie() -->
mensaje: [ Type ] --> PuertoSerie.escribir() -->
```

- Abren el puerto serie a `baudios` y escriben cualquier mensaje genérico por
  consola.

```text
nombreEmisora: Text, fabricanteID: N, txPower: Z --> EmisoraBLE.EmisoraBLE() -->
--> EmisoraBLE.encenderEmisora() -->
--> EmisoraBLE.detenerAnuncio() -->
--> EmisoraBLE.estaAnunciando() --> B
```

- Configuran la identidad, el fabricante y la potencia; encienden el stack BLE;
  detienen el anuncio si lo había; indican si hay emisión activa.

```text
beaconUUID: [ Byte ]_16, major: Z, minor: Z, rssi: Z
     --> EmisoraBLE.emitirAnuncioIBeacon() -->
```

- Detiene el anuncio previo, monta un `BLEBeacon` con los cuatro campos y emite
  de forma indefinida (hasta `detenerAnuncio()`), a intervalo de 100 ms.

```text
carga: Text, tamanyoCarga: N --> EmisoraBLE.emitirAnuncioIBeaconLibre() -->
```

- Variante que inyecta hasta 21 bytes de carga libre en lugar de la estructura
  UUID/major/minor (rellena los 4 bytes fijos de cabecera y copia con
  `memcpy`).

```text
servicio: ServicioEnEmisora --> EmisoraBLE.anyadirServicio() --> B
servicio: ServicioEnEmisora, restoCaracteristicas: [ Caracteristica ]
     --> EmisoraBLE.anyadirServicioConSusCaracteristicas() --> B
     --> EmisoraBLE.anyadirServicioConSusCaracteristicasYActivar() --> B
```

- Registran un servicio BLE (y sus características) en el stack; las variantes
  con características son recursivas hasta el caso base.

```text
cb: Callback --> EmisoraBLE.instalarCallbackConexionEstablecida() -->
cb: Callback --> EmisoraBLE.instalarCallbackConexionTerminada() -->
connHandle: N --> EmisoraBLE.getConexion() --> Conexion
```

- Registran los callbacks de conexión/desconexión y devuelven la conexión
  activa por su identificador.

```text
--> Publicador.Publicador() -->
--> Publicador.encenderEmisora() -->
```

- Crean el publicador (sin encender la emisora) y encienden el hardware
  Bluetooth por separado.

```text
valorOzono: R, contador: N, tiempoEspera: N --> Publicador.publicarOzono() -->
valorTemperatura: R, contador: N, tiempoEspera: N --> Publicador.publicarTemperatura() -->
```

- Construyen `major = (MedicionesID << 8) + contador` con el código de Ozono
  (11) o de temperatura (12), emiten el iBeacon con el UUID fijo y el valor
  medido **escalado ×10** (`codificarValor()`) como `minor`, esperan
  `tiempoEspera` ms y detienen el anuncio.

```text
--> Medidor.Medidor() -->
--> Medidor.iniciarMedidor() -->
--> Medidor.medirOzono() --> ppm: R
--> Medidor.medirTemperatura() --> tempC: R
```

- Configuran el ADC a 10 bits y convierten las lecturas analógicas de los
  sensores en Ozono (ppm) y temperatura (°C).

```text
p: [ Type ]_n, n: N --> alReves() --> [ Type ]_n
pString: Text, pUint: [ Byte ]_tamMax, tamMax: N
     --> stringAUint8AlReves() --> [ Byte ]_tamMax
```

- `alReves()` invierte un array sobre sí mismo; `stringAUint8AlReves()`
  convierte el nombre en un UUID de 16 bytes con el byte menos significativo
  primero (orden que espera Bluetooth).

```text
nombreServicio: Text --> ServicioEnEmisora.ServicioEnEmisora() -->
--> ServicioEnEmisora.escribeUUID() -->
car: Caracteristica --> ServicioEnEmisora.anyadirCaracteristica() -->
--> ServicioEnEmisora.activarServicio() -->
--> ServicioEnEmisora.operator BLEService&() --> Servicio
```

- Crean el servicio a partir de un nombre (UUID al revés), añaden
  características, dan de alta el servicio en el stack BLE y permiten usarlo
  donde se espere un `BLEService`.

```text
nombreCaracteristica: Text --> Caracteristica.Caracteristica() -->
props: N, permisoRead: [ Type ], permisoWrite: [ Type ], tam: N
     --> Caracteristica.Caracteristica() -->
str: Text --> Caracteristica.escribirDatos() --> r: N
str: Text --> Caracteristica.notificarDatos() --> r: N
cb: Callback --> Caracteristica.instalarCallbackCaracteristicaEscrita() -->
--> Caracteristica.activar() -->
```

- Configuran y gestionan una característica BLE: propiedades, permisos,
  tamaño, escritura/notificación de datos, callback de escritura y alta en el
  stack.

### 1.4 Beacon Frame Layout

```text
02 01 06                         estructura "Flags"
1A FF 4C 00 02 15                datos de fabricante (Apple + marca iBeacon)
<uuid: 16 bytes>                 "EPSG-GTI-PROY-MJ" (ASCII)
<major: 2 bytes>                 (tipo << 8) + contador
<minor: 2 bytes>                 valor medido * 10
<txPower: 1 byte>                potencia en dBm
```

## 2. Design Clarifications

- **UUID de 16 caracteres ASCII:** `beaconUUID` vale
  `{'E','P','S','G','-','G','T','I','-','P','R','O','Y','-','M','J'}` (16
  entradas, guiones incluidos). No es una UUID hexadecimal; su equivalente
  hexadecimal es `45505347-2D47-5449-2D50-524F592D4D4A`, que es lo que filtra
  la app Android (`android_design.md`).
- **Codificación tipo + valor:** el byte alto del `major` lleva el tipo
  (`ozono = 11`, `temperatura = 12`, `ruido = 13`) y el byte bajo el contador de
  iteración; el `minor` lleva el valor medido multiplicado por 10 (`minor` es
  entero y no admite decimales). Dos anuncios por iteración (Ozono y temperatura),
  no uno con dos valores.
- **Lecturas actuales de prueba:** en `loop()` las llamadas a
  `elMedidor.medirOzono()` y `elMedidor.medirTemperatura()` están comentadas y se
  usan valores fijos (`56.78`, `12.34`) hasta conectar los sensores reales.
- **Servicio BLE opcional:** `ServicioEnEmisora` (con sus características) es
  capacidad adicional de la emisora; el flujo principal de mediciones solo usa
  anuncios iBeacon.
- **Potencia de referencia:** `RSSI = -53` es un valor de referencia que hoy no
  se usa para calcular distancia.

## 3. General Rules

- **Programming Language:** C++ (Arduino sobre Adafruit Bluefruit nRF52;
  `bluefruit.h`, `Adafruit_TinyUSB`).
- **Function/Method Headers:** cada función, método o constructor lleva su
  diseño lógico en un bloque de comentario delimitado por una línea de guiones
  (`-----------`), con tipos abstractos (`N`, `Z`, `R`, `B`, `Text`, `[Tipo]`).
- **Code Readability:** código claro y autoexplicativo, con el mínimo de
  comentarios adicionales.
- **Automated Testing:** casos de prueba para las funciones puras y portables
  sin hardware (`alReves`, `stringAUint8AlReves`); el resto se verifica por
  consola serie y con pruebas manuales de emisión, que es lo que permite el
  hardware.
