package com.upv.sensoresbeacon;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//==============================================================================================
//Nombre: MainActivity.java
//Descripcion: Activity principal: pide permisos, escanea BLE, filtra los anuncios iBeacon
//              de la UUID del emisor y muestra y envia las mediciones.
//Copyright:
//Fecha de creacion: 2026-10-01
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Diseno logico (Logical_Design_RE_Rules_v3.pdf, seccion 5 - Clases):
 *
 *        v                       : View
 *        contadorBeacons         : N
 *        ETIQUETA_LOG            : Text          (--x)
 *        CODIGO_PETICION_PERMISOS: Z            (--x)
 *        dispositivoBuscado      : Text
 *        uuidBuscada            : Text
 *        requestCode             : Z
 *        permissions             : [Text]
 *        grantResults            : [Z]
 *        elEscaner              : BluetoothLeScanner
 *        callbackDelEscaneo      : ScanCallback
 *        logicaFake              : LogicaFake
 *  ----------------- MainActivity -------------------
 *      |
 *      |
 *  --> onCreate() -->
 *      |
 *      |
 *  --> onRequestPermissionsResult() -->
 *      |
 *      |
 *  --> inicializarBluetooth() -->
 *      |
 *      |
 *  --> botonBuscarDispositivosBTLEPulsado() -->
 *      |
 *      |
 *  --> botonBuscarNuestroDispositivoBTLEPulsado() -->
 *      |
 *      |
 *  --> botonDetenerBusquedaDeDispositivosBTLEPulsado() -->
 *      |
 *      |
 *  --> buscarTodosLosDispositivosBTLE() -->
 *      |
 *      |
 *  --> buscarEsteDispositivoBTLE() -->
 *      |
 *      |
 *  --> uuidBuscadaEsValida() <--
 *      |
 *      |
 *  --> coincideConLaUuidBuscada() <--
 *      |
 *      |
 *  --> crearScanFilter() <--
 *      |
 *      |
 *      |
 *  --> detenerBusquedaDispositivosBTLE() -->
 *      |
 *      |
 *  --> mostrarInformacionDelDispositivoBTLE() -->
 *      |
 *      |
 *  --> actualizarContador() -->
 *      |
 *      |
 *  --> escribirEnPantalla() -->
 *  -------------------------------------------------
 */
public class MainActivity extends androidx.appcompat.app.AppCompatActivity {

    public static final String DISPOSITIVO_BUSCADO = "GTI-3A-MJ";

    //--------------------
    // Texto que el emisor mete como UUID en sus 16 bytes: Publicador.h declara
    // beaconUUID = {'E','P','S','G','-','G','T','I','-','P','R','O','Y','-','3','A'}.
    // No es una UUID hexadecimal, sino 16 caracteres ASCII. Su equivalente
    // hexadecimal (el valor real que viaja por el aire) es el de abajo.
    //--------------------
    public static final String UUID_EMISORA_TEXTO = "EPSG-GTI-PROY-MJ";
    public static final String UUID_BUSCADA = "45505347-2D47-5449-2D50-524F592D4D4A";

    private static final byte[] MARCA_IBEACON = {0x02, 0x15};

    private static final String ETIQUETA_LOG = "MainActivity";
    private static final int CODIGO_PETICION_PERMISOS = 42;

    /** Tope de claves memorizadas, para que la lista no crezca sin limite. */
    private static final int MAX_CLAVES = 2000;

    /** Intervalo minimo entre repintados del contador, en milisegundos. */
    private static final long REFRESCO_CONTADOR = 500L;

    /** Cuantos avisos de "Apple que no es iBeacon" se escriben en pantalla. */
    private static final int MAX_AVISOS_APPLE = 5;

    private View v;
    private int contadorBeacons;
    private int contadorOtros;
    private int contadorApple;
    private long ultimaActualizacionContador;
    private String dispositivoBuscado = DISPOSITIVO_BUSCADO;
    private String uuidBuscada = UUID_BUSCADA;
    private int requestCode = CODIGO_PETICION_PERMISOS;
    private String[] permissions;
    private int[] grantResults;
    private BluetoothLeScanner elEscaner;
    private ScanCallback callbackDelEscaneo;
    private LogicaFake logicaFake;
    private TextView tvContador;
    private TextView tvLog;
    private boolean busquedaEnCurso;
    private final List<String> beaconsVistos = new ArrayList<>();
    private final Set<String> avisosAppleVistos = new HashSet<>();

    //--------------------
    // savedInstanceState: Bundle --> onCreate() -->
    //
    // Prepara la interfaz, el escaner BLE y la logica fake.
    //--------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvContador = findViewById(R.id.tvContador);
        tvLog = findViewById(R.id.tvLog);
        logicaFake = new LogicaFake();

        solicitarPermisosSiFaltan();
        inicializarBluetooth();

        v = findViewById(R.id.btnBuscarTodos);
        ((Button) v).setOnClickListener(new View.OnClickListener() {
            // atribuye al boton la accion de buscar todos, buscar el nuestro o detener, segunt sea.
            @Override
            public void onClick(View pulsado) {
                botonBuscarDispositivosBTLEPulsado();
            }
        });

        v = findViewById(R.id.btnBuscarNuestro);
        ((Button) v).setOnClickListener(new View.OnClickListener() {
            // atribuye al boton la accion de buscar todos, buscar el nuestro o detener, segunt sea.
            @Override
            public void onClick(View pulsado) {
                botonBuscarNuestroDispositivoBTLEPulsado();
            }
        });

        v = findViewById(R.id.btnDetener);
        // atribuye al boton la accion de buscar todos, buscar el nuestro o detener, segunt sea.
        ((Button) v).setOnClickListener(new View.OnClickListener() {
            // atribuye al boton la accion de buscar todos, buscar el nuestro o detener, segunt sea.
            @Override
            public void onClick(View pulsado) {
                botonDetenerBusquedaDeDispositivosBTLEPulsado();
            }
        });

        escribirEnPantalla("Listo. UUID buscada: " + uuidBuscada
                + " (" + UUID_EMISORA_TEXTO + ")");
        if (!uuidBuscadaEsValida()) {
            escribirEnPantalla("AVISO: UUID_BUSCADA no es una UUID valida; el boton de "
                    + "busqueda por UUID no funcionara hasta corregirla");
        }
    }

    //--------------------
    // --> solicitarPermisosSiFaltan() -->
    //
    // Pide los permisos de Bluetooth/Locacion segun la version de Android.
    //--------------------
    private void solicitarPermisosSiFaltan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        } else {
            permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        }

        List<String> pendientes = new ArrayList<>();
        for (String permiso : permissions) {
            if (checkSelfPermission(permiso) != PackageManager.PERMISSION_GRANTED) {
                pendientes.add(permiso);
            }
        }
        if (!pendientes.isEmpty()) {
            requestPermissions(pendientes.toArray(new String[0]), CODIGO_PETICION_PERMISOS);
        }
    }

    //--------------------
    // --> inicializarBluetooth() -->
    //
    // Obtiene el adaptador y el escaner BLE del sistema.
    //--------------------
    private void inicializarBluetooth() {
        BluetoothManager gestor = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adaptador = gestor == null ? null : gestor.getAdapter();
        if (adaptador == null) {
            Log.e(ETIQUETA_LOG, "El dispositivo no tiene adaptador Bluetooth");
            escribirEnPantalla("ERROR: este dispositivo no tiene Bluetooth");
            return;
        }
        elEscaner = adaptador.getBluetoothLeScanner();
        Log.i(ETIQUETA_LOG, "Bluetooth inicializado, escaner=" + elEscaner);
    }

    //--------------------
    // requestCode: Z, permissions: [Text], grantResults: [Z] --> onRequestPermissionsResult() -->
    //--------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        this.requestCode = requestCode;
        this.permissions = permissions;
        this.grantResults = grantResults;

        if (requestCode != CODIGO_PETICION_PERMISOS) {
            return;
        }
        for (int resultado : grantResults) {
            if (resultado != PackageManager.PERMISSION_GRANTED) {
                Log.e(ETIQUETA_LOG, "Permiso denegado: " + permissions[0]);
                escribirEnPantalla("ERROR: permiso denegado, no se puede escanear");
                return;
            }
        }
        inicializarBluetooth();
        escribirEnPantalla("Permisos concedidos");
    }

    //--------------------
    // --> puedeEscanear() --> permitido: B
    //
    // Comprueba que estan concedidos los permisos de Bluetooth/Locacion.
    //--------------------
    public boolean puedeEscanear() {
        if (permissions == null) {
            return false;
        }
        for (String permiso : permissions) {
            if (checkSelfPermission(permiso) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    //--------------------
    // --> botonBuscarDispositivosBTLEPulsado() -->
    //
    // Empieza la busqueda de todos los iBeacons.
    //--------------------
    public void botonBuscarDispositivosBTLEPulsado() {
        Log.i(ETIQUETA_LOG, "Boton buscar todos los dispositivos pulsado");
        buscarTodosLosDispositivosBTLE();
    }

    //--------------------
    // --> botonBuscarNuestroDispositivoBTLEPulsado() -->
    //
    // Empieza la busqueda filtrando solo por el nombre del dispositivo buscado.
    //--------------------
    public void botonBuscarNuestroDispositivoBTLEPulsado() {
        Log.i(ETIQUETA_LOG, "Boton buscar nuestro beacon por UUID (" + uuidBuscada + ") pulsado");
        buscarEsteDispositivoBTLE();
    }

    //--------------------
    // --> botonDetenerBusquedaDeDispositivosBTLEPulsado() -->
    //
    // Detiene cualquier busqueda en curso.
    //--------------------
    public void botonDetenerBusquedaDeDispositivosBTLEPulsado() {
        Log.i(ETIQUETA_LOG, "Boton detener busqueda pulsado");
        detenerBusquedaDispositivosBTLE();
    }

    //--------------------
    // --> buscarTodosLosDispositivosBTLE() -->
    //
    // Registra el ScanCallback que procesa todas las tramas iBeacon recibidas.
    //--------------------
    public void buscarTodosLosDispositivosBTLE() {
        if (!prepararEscaneo()) {
            return;
        }
        callbackDelEscaneo = crearCallback(false);
        iniciarEscaneo("Buscando todos los iBeacons (escaneo SIN filtro)...", null);
    }

    //--------------------
    // --> buscarEsteDispositivoBTLE() -->
    //
    // Registra el ScanCallback que solo acepta los beacons cuya UUID coincide
    // con "uuidBuscada".
    //--------------------
    public void buscarEsteDispositivoBTLE() {
        if (!uuidBuscadaEsValida()) {
            escribirEnPantalla("ERROR: la UUID buscada no es valida. Pulsa '"
                    + getString(R.string.boton_buscar_todos) + "' para ver la UUID real");
            return;
        }
        if (!prepararEscaneo()) {
            return;
        }
        callbackDelEscaneo = crearCallback(true);
        iniciarEscaneo("Buscando beacons con UUID " + uuidBuscada + "...", crearScanFilter());
    }

    //--------------------
    // --> uuidBuscadaEsValida() --> valida: B
    //
    // Una UUID son 32 digitos hexadecimales en formato 8-4-4-4-12.
    //--------------------
    public boolean uuidBuscadaEsValida() {
        try {
            Utilidades.stringToUUID(uuidBuscada);
            return true;
        } catch (IllegalArgumentException e) {
            Log.e(ETIQUETA_LOG, "UUID_BUSCADA no es una UUID valida: '" + uuidBuscada
                    + "' (" + e.getMessage() + ")");
            return false;
        }
    }

    //--------------------
    // uuid: Text --> coincideConLaUuidBuscada() --> coincide: B
    //
    // Compara ignorando mayusculas y guiones.
    //--------------------
    public boolean coincideConLaUuidBuscada(String uuid) {
        if (uuid == null) {
            return false;
        }
        return normalizar(uuid).equals(normalizar(uuidBuscada));
    }

    //--------------------
    // texto: Text --> normalizar() --> texto: Text --x
    //
    // Quita guiones, dos puntos y espacios, y pasa a mayusculas, para comparar UUID.
    //--------------------
    private static String normalizar(String texto) {
        return texto.replace("-", "").replace(":", "").replace(" ", "").toUpperCase();
    }

    //--------------------
    // --> crearScanFilter() --> filtros: [ScanFilter]
    //
    // iBeacon no anuncia un Service UUID, por lo que un ScanFilter por servicio
    // devolveria cero resultados. Lo que si funciona es filtrar por los datos
    // de fabricante de Apple, que empiezan por la marca iBeacon 02 15.
    //--------------------
    public List<ScanFilter> crearScanFilter() {
        ScanFilter filtro = new ScanFilter.Builder()
                .setManufacturerData(TramaIBeacon.COMPANY_ID_APPLE,
                        MARCA_IBEACON, MARCA_IBEACON)
                .build();
        List<ScanFilter> filtros = new ArrayList<>();
        filtros.add(filtro);
        return filtros;
    }

    //--------------------
    // --> prepararEscaneo() --> preparado: B
    //
    // Comprueba adaptador y permisos, detiene el escaneo anterior y reinicia
    // el contador de iBeacons. Devuelve false si no se puede escanear.
    //--------------------
    private boolean prepararEscaneo() {
        if (elEscaner == null) {
            inicializarBluetooth();
        }
        if (elEscaner == null) {
            return false;
        }
        if (!puedeEscanear()) {
            Log.e(ETIQUETA_LOG, "Faltan permisos para escanear");
            escribirEnPantalla("ERROR: faltan permisos de Bluetooth, no se puede escanear");
            return false;
        }
        detenerEscaneoSilencioso();
        beaconsVistos.clear();
        avisosAppleVistos.clear();
        contadorBeacons = 0;
        contadorOtros = 0;
        contadorApple = 0;
        ultimaActualizacionContador = 0L;
        actualizarContador();
        return true;
    }

    //--------------------
    // soloElBuscado: B --> crearCallback() --> callback: ScanCallback
    //--------------------
    private ScanCallback crearCallback(final boolean soloElBuscado) {
        return new ScanCallback() {
            // procesa el anuncio BLE recibido de uno en uno.
            @Override
            public void onScanResult(int tipoCallback, ScanResult resultado) {
                mostrarInformacionDelDispositivoBTLE(resultado, soloElBuscado);
            }

            // procesa en bloque los anuncios acumulados.
            @Override
            public void onBatchScanResults(List<ScanResult> resultados) {
                for (ScanResult resultado : resultados) {
                    mostrarInformacionDelDispositivoBTLE(resultado, soloElBuscado);
                }
            }

            // avisa por pantalla de que el escaneo no pudo empezar.
            @Override
            public void onScanFailed(int codigoError) {
                Log.e(ETIQUETA_LOG, "onScanFailed codigo=" + codigoError);
                escribirEnPantalla("ERROR: el escaneo fallo (" + codigoError + ")");
            }
        };
    }

    //--------------------
    // mensaje: Text --> iniciarEscaneo() -->
    //--------------------
    private void iniciarEscaneo(String mensaje, List<ScanFilter> filtros) {
        if (serviciosDeUbicacionDesactivados()) {
            escribirEnPantalla("AVISO: activa la ubicacion (GPS); si no, Android filtra los iBeacon");
        }
        if (!ubicacionPrecisaConcedida()) {
            escribirEnPantalla("AVISO: concede ubicacion PRECISA a la app; con la aproximada "
                    + "Android filtra los iBeacon");
        }
        try {
            // filtros == null significa escaneo SIN filtro: llegan todos los
            // anuncios BLE de alrededor. Es lo que usa "buscar todos" para poder
            // ver lo que realmente hay, en vez de quedarse en silencio si el
            // filtro del fabricante no encaja con el anuncio.
            elEscaner.startScan(filtros,
                    new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .build(),
                    callbackDelEscaneo);
            busquedaEnCurso = true;
            escribirEnPantalla(mensaje);
        } catch (SecurityException e) {
            busquedaEnCurso = false;
            Log.e(ETIQUETA_LOG, "Permiso denegado al iniciar el escaneo: " + e.getMessage());
            escribirEnPantalla("ERROR: permiso denegado, no se puede escanear");
        }
    }

    //--------------------
    // --> serviciosDeUbicacionDesactivados() --> desactivados: B
    //
    // Android exige la ubicacion activada para ver iBeacon: hasta Android 11
    // para escanear en general y en Android 12+ (sin neverForLocation) porque,
    // si esta apagada, el sistema filtra los formatos de beacon y solo deja
    // pasar los anuncios que no son iBeacon.
    //--------------------
    private boolean serviciosDeUbicacionDesactivados() {
        LocationManager ubicacion = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return ubicacion == null || !ubicacion.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && !ubicacion.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    //--------------------
    // --> ubicacionPrecisaConcedida() --> concedida: B
    //
    // En Android 12+ el sistema solo entrega los iBeacon si la ubicacion
    // concedida es la precisa; con la aproximada (solo COARSE) los filtra.
    //--------------------
    private boolean ubicacionPrecisaConcedida() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    //--------------------
    // --> detenerBusquedaDispositivosBTLE() -->
    //
    // Detiene el escaneo BLE registrado en "callbackDelEscaneo".
    //--------------------
    public void detenerBusquedaDispositivosBTLE() {
        boolean habiaEscaneo = detenerEscaneoSilencioso();
        busquedaEnCurso = false;
        escribirEnPantalla((habiaEscaneo ? "Busqueda detenida. " : "")
                + "iBeacons encontrados: " + contadorBeacons);
    }

    //--------------------
    // --> detenerEscaneoSilencioso() --> habiaEscaneo: B
    //
    // Detiene el escaneo sin escribir en pantalla, para reutilizarlo al
    // reiniciar la busqueda.
    //--------------------
    private boolean detenerEscaneoSilencioso() {
        if (elEscaner == null || callbackDelEscaneo == null || !puedeEscanear()) {
            return false;
        }
        try {
            elEscaner.stopScan(callbackDelEscaneo);
        } catch (SecurityException e) {
            Log.e(ETIQUETA_LOG, "Permiso denegado al detener el escaneo: " + e.getMessage());
            return false;
        }
        Log.i(ETIQUETA_LOG, "Escaneo detenido");
        return true;
    }

    //--------------------
    // resultado: ScanResult --> bytesDe() --> bytes: [Z]
    //
    // Extrae los bytes de la trama anunciada. ScanResult.getBytes() solo
    // existe a partir de Android 13, por lo que se usa ScanRecord.
    //--------------------
    public byte[] bytesDe(ScanResult resultado) {
        if (resultado == null) {
            return null;
        }
        ScanRecord registro = resultado.getScanRecord();
        return registro == null ? null : registro.getBytes();
    }

    //--------------------
    // resultado: ScanResult --> mostrarInformacionDelDispositivoBTLE() -->
    //
    // Filtra la trama, registra el beacon en el LogCat, incrementa el contador
    // mostrado en pantalla y lanza la logica fake.
    //--------------------
    public void mostrarInformacionDelDispositivoBTLE(ScanResult resultado, boolean soloElBuscado) {
        byte[] bytes = bytesDe(resultado);
        if (!TramaIBeacon.esTramaIBeacon(bytes)) {
            // No se descarta en silencio: si hay dispositivos cerca que no son
            // iBeacon, queda constancia en el LogCat y se puede diagnosticar.
            contadorOtros++;
            if (TramaIBeacon.esAnuncioConIBeaconDeApple(bytes)) {
                contadorApple++;
                avisarDeAppleNoBeacon(direccionMac(resultado,
                        nombreDelDispositivo(resultado)), bytes);
            }
            actualizarContadorSiToca();
            Log.d(ETIQUETA_LOG, "Anuncio que no es iBeacon ("
                    + direccionMac(resultado, nombreDelDispositivo(resultado)) + "): "
                    + Utilidades.bytesToString(bytes));
            return;
        }
        TramaIBeacon trama = new TramaIBeacon(bytes);
        // Se muestra en formato canonico 8-4-4-4-12, igual que UUID_BUSCADA,
        // para poder compararlas a simple vista.
        String uuid = Utilidades.bytesToHexString(trama.getUUID());

        if (soloElBuscado && !coincideConLaUuidBuscada(uuid)) {
            Log.d(ETIQUETA_LOG, "Descartado (UUID distinta): " + uuid);
            return;
        }
        String nombre = nombreDelDispositivo(resultado);
        String clave = claveDeMedicion(resultado, trama);
        if (beaconsVistos.contains(clave)) {
            return;
        }
        if (beaconsVistos.size() > MAX_CLAVES) {
            beaconsVistos.clear();
        }
        beaconsVistos.add(clave);

        Medicion medicion = Medicion.desdeTrama(trama);
        int major = trama.getMajorComoInt();
        int minor = trama.getMinorComoInt();
        int txPower = trama.getTxPower();

        contadorBeacons++;
        actualizarContador();

        String mensaje = "iBeacon nº" + contadorBeacons
                + " | nombre=" + nombre
                + " | direccion=" + clave
                + " | uuid=" + uuid
                + " | major=" + major
                + " (tipo=" + trama.getTipoDeMedicionComoInt()
                + ", contador=" + trama.getContadorComoInt() + ")"
                + " | minor(valor)=" + minor
                + " | txPower=" + txPower
                + " | " + medicion;
        Log.i(ETIQUETA_LOG, mensaje);
        escribirEnPantalla(mensaje);

        enviarMedicion(medicion, nombre);
    }

    //--------------------
    // texto: Text --> uuidComoHexadecimal() --> hexadecimal: Text
    //
    // Traduce el texto ASCII de 16 caracteres que declara el emisor a la UUID
    // hexadecimal (8-4-4-4-12) que realmente viaja en la trama.
    //--------------------
    public static String uuidComoHexadecimal(String texto) {
        return Utilidades.bytesToHexString(Utilidades.bytesDesdeTextoAscii(texto));
    }

    //--------------------
    // medicion: Medicion, nombre: Text --> enviarMedicion() -->
    //
    // Delega en la logica fake la peticion POST, sin bloquear la interfaz. Solo
    // se envian tipo, valor y nombre: uuid, txPower, major y minor se quedan en
    // la app (la uuid ya se ha usado para filtrar el iBeacon).
    //--------------------
    private void enviarMedicion(final Medicion medicion, final String nombre) {
        new Thread(new Runnable() {
            // se ejecuta en un hilo aparte del de la interfaz para no bloquearla.
            @Override
            public void run() {
                logicaFake.guardarMedicion(medicion, nombre);
            }
        }, "guardarMedicion").start();
    }

    //--------------------
    // resultado: ScanResult --> nombreDelDispositivo() --> nombre: Text
    //
    // Un iBeacon no siempre anuncia su nombre: en la mayoria de los casos
    // llega en el propio anuncio (ScanRecord.getDeviceName()) y no en el
    // BluetoothDevice. Se consulta primero el anuncio y despues el dispositivo,
    // porque el segundo requiere permiso BLUETOOTH_CONNECT desde Android 12.
    //--------------------
    public String nombreDelDispositivo(ScanResult resultado) {
        if (resultado == null) {
            return "desconocido";
        }
        String delAnuncio = nombreEnElAnuncio(resultado);
        if (delAnuncio != null) {
            return delAnuncio;
        }
        return nombreDelDispositivo(resultado.getDevice());
    }

    //--------------------
    // dispositivo: BluetoothDevice --> nombreDelDispositivo() --> nombre: Text
    //--------------------
    public String nombreDelDispositivo(android.bluetooth.BluetoothDevice dispositivo) {
        if (dispositivo == null) {
            return "desconocido";
        }
        try {
            String nombre = dispositivo.getName();
            return nombre == null || nombre.trim().isEmpty() ? "desconocido" : nombre;
        } catch (SecurityException e) {
            Log.e(ETIQUETA_LOG, "Sin permiso para leer el nombre: " + e.getMessage());
            return "desconocido";
        }
    }

    //--------------------
    // resultado: ScanResult --> nombreEnElAnuncio() --> nombre: Text
    //
    // Devuelve el nombre de dispositivo que viaja en el propio anuncio BLE, o
    // null si no lo incluye.
    //--------------------
    private String nombreEnElAnuncio(ScanResult resultado) {
        ScanRecord registro = resultado.getScanRecord();
        if (registro == null) {
            return null;
        }
        String nombre = registro.getDeviceName();
        return nombre == null || nombre.trim().isEmpty() ? null : nombre;
    }

    //--------------------
    // resultado: ScanResult, nombre: Text --> claveDelDispositivo() --> clave: Text
    //
    // Identifica el beacon para no contarlo varias veces. Se usa la direccion
    // MAC; si no es accesible, se recurre a uuid+major+minor.
    //--------------------
    public String claveDelDispositivo(ScanResult resultado, String nombre) {
        return direccionMac(resultado, nombre) + "@" + Utilidades.bytesToString(bytesDe(resultado));
    }

    //--------------------
    // resultado: ScanResult, nombre: Text --> direccionMac() --> direccion: Text
    //
    // La direccion MAC identifica fisicamente a la emisora. Se usa sola para
    // nombrar el dispositivo, nunca para descartar anuncios: la misma placa
    // emite CO2 y temperatura con la misma MAC, asi que deduplicar solo por
    // MAC haria desaparecer todas las mediciones menos la primera.
    //--------------------
    public String direccionMac(ScanResult resultado, String nombre) {
        try {
            android.bluetooth.BluetoothDevice dispositivo = resultado.getDevice();
            if (dispositivo != null) {
                String direccion = dispositivo.getAddress();
                if (direccion != null && !direccion.isEmpty()) {
                    return direccion;
                }
            }
        } catch (SecurityException e) {
            Log.d(ETIQUETA_LOG, "Sin permiso para leer la direccion: " + e.getMessage());
        }
        return nombre;
    }

    //--------------------
    // resultado: ScanResult --> claveDeMedicion() --> clave: Text
    //
    // Identifica una medicion concreta para no contarla miles de veces por
    // segundo: el mismo anuncio se repite en cada evento de escaneo, asi que
    // se descartan los repetidos. La clave incluye la MAC, el major (tipo y
    // contador de iteracion) y el minor (valor medido), de modo que cada
    // medicion nueva de la placa se procesa y solo se filtran sus duplicados.
    //--------------------
    public String claveDeMedicion(ScanResult resultado, TramaIBeacon trama) {
        return claveDeMedicion(direccionMac(resultado, nombreDelDispositivo(resultado)),
                trama.getMajorComoInt(), trama.getMinorComoInt());
    }

    //--------------------
    // direccion: Text, major: N, minor: N --> claveDeMedicion() --> clave: Text
    //
    // Version sin dependencias de Android, para poder probarla.
    //--------------------
    public static String claveDeMedicion(String direccion, int major, int minor) {
        return direccion + "|major=" + major + "|minor=" + minor;
    }

    //--------------------
    // --> actualizarContador() -->
    //
    // Muestra los dos contadores que permiten diagnosticar una deteccion
    // fallida: cuantos iBeacons se han reconocido y cuantos anuncios BLE se han
    // visto sin ser iBeacon. Si el primero es 0 pero el segundo no, el
    // escaner funciona y el problema es reconocer la trama; si los dos son 0,
    // el problema es que no llega nada.
    //--------------------
    public void actualizarContador() {
        final int total = contadorBeacons;
        final int otros = contadorOtros;
        final int apple = contadorApple;
        runOnUiThread(new Runnable() {
            // se ejecuta en un hilo aparte del de la interfaz para no bloquearla.
            @Override
            public void run() {
                if (tvContador != null) {
                    tvContador.setText("iBeacons: " + total
                            + "   |   Apple: " + apple
                            + "   |   otros BLE: " + otros);
                }
            }
        });
    }

    //--------------------
    // bytes: [Z] --> avisarDeAppleNoBeacon() -->
    //
    // Un anuncio con datos de Apple que no es un iBeacon es normal (son los
    // mensajes Continuity de Auriculares, relojes y telefonos). Se avisa solo
    // de los anuncios distintos y como mucho de los primeros MAX_AVISOS_APPLE,
    // porque volcar cientos de lineas en pantalla haria la app inutilizable.
    //--------------------
    private void avisarDeAppleNoBeacon(String direccion, byte[] bytes) {
        String firma = direccion + "|"
                + Utilidades.bytesToString(bytes).substring(0, Math.min(20, bytes.length));
        if (avisosAppleVistos.contains(firma)) {
            return;
        }
        avisosAppleVistos.add(firma);
        if (avisosAppleVistos.size() <= MAX_AVISOS_APPLE) {
            escribirEnPantalla("AVISO: datos de Apple que NO son un iBeacon: "
                    + Utilidades.bytesToString(bytes));
        } else if (avisosAppleVistos.size() == MAX_AVISOS_APPLE + 1) {
            escribirEnPantalla("AVISO: hay mas anuncios de Apple que no son iBeacons; "
                    + "ya no se muestran mas. Mira el contador 'Apple'.");
        }
    }

    //--------------------
    // El escaneo puede entregar cientos de anuncios por segundo. Solo se
    // repinta el contador como mucho una vez cada REFRESCO_CONTADOR milisegundos.
    //--------------------
    private void actualizarContadorSiToca() {
        long ahora = System.currentTimeMillis();
        if (ahora - ultimaActualizacionContador < REFRESCO_CONTADOR) {
            return;
        }
        ultimaActualizacionContador = ahora;
        actualizarContador();
    }

    //--------------------
    // texto: Text --> escribirEnPantalla() -->
    //--------------------
    public void escribirEnPantalla(final String texto) {
        Log.i(ETIQUETA_LOG, texto);
        // se ejecuta en un hilo aparte del de la interfaz para no bloquearla.
        runOnUiThread(new Runnable() {
            // se ejecuta en un hilo aparte del de la interfaz para no bloquearla.
            @Override
            public void run() {
                if (tvLog != null) {
                    tvLog.setText(texto + "\n" + tvLog.getText());
                }
            }
        });
    }

    //--------------------
    // --> getContadorBeacons() <--
    //
    // devuelve cuantos iBeacons de nuestro emisor se han detectado.
    //--------------------
    public int getContadorBeacons() {
        return contadorBeacons;
    }

    //--------------------
    // --> getDispositivoBuscado() <--
    //
    // devuelve el nombre del dispositivo que se esta buscando.
    //--------------------
    public String getDispositivoBuscado() {
        return dispositivoBuscado;
    }

    //--------------------
    // --> getUuidBuscada() <--
    //
    // devuelve la UUID por la que se filtra el escaneo.
    //--------------------
    public String getUuidBuscada() {
        return uuidBuscada;
    }

    //--------------------
    // uuidBuscada: Text --> setUuidBuscada() -->
    //--------------------
    public void setUuidBuscada(String uuidBuscada) {
        this.uuidBuscada = uuidBuscada;
    }

    //--------------------
    // --> isBusquedaEnCurso() <--
    //
    // indica si el escaneo de dispositivos esta activo.
    //--------------------
    public boolean isBusquedaEnCurso() {
        return busquedaEnCurso;
    }
}
