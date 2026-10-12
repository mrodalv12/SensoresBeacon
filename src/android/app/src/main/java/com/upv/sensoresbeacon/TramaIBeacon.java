package com.upv.sensoresbeacon;

import java.util.UUID;

//==============================================================================================
//Nombre: TramaIBeacon.java
//Descripcion: Localiza la trama iBeacon de Apple dentro de un anuncio BLE y expone sus
//              campos: uuid, major, minor y txPower.
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
 *        bytes        : [Z]
 *        losBytes     : [Z]
 *        advFlags     : [Z]
 *        advHeader    : [Z]
 *        companyID    : [Z]
 *        prefijo      : [Z]
 *        iBeaconType  : Z
 *        iBeaconLength: Z
 *        uuid         : [Z]
 *        major        : [Z]
 *        minor        : [Z]
 *        txPower      : Z
 *  ------------------ TramaIBeacon ------------------
 *      |
 *      |
 *  --> TramaIBeacon() -->
 *      |
 *      |
 *  --> getLosBytes() <--
 *      |
 *      |
 *  --> getAdvFlags() <--
 *      |
 *      |
 *  --> getAdvHeader() <--
 *      |
 *      |
 *  --> getCompanyID() <--
 *      |
 *      |
 *  --> getPrefijo() <--
 *      |
 *      |
 *  --> getiBeaconType() <--
 *      |
 *      |
 *  --> getiBeaconLength() <--
 *      |
 *      |
 *  --> getUUID() <--
 *      |
 *      |
 *  --> getMajor() <--
 *      |
 *      |
 *  --> getMinor() <--
 *      |
 *      |
 *  --> getTxPower() <--
 * -------------------------------------------------
 *
 * Localizacion de la trama dentro del anuncio:
 *
 * Android devuelve en ScanRecord.getBytes() la estructura AD completa, no un
 * iBeacon desnudo. Un anuncio real de Apple es:
 *
 *   02 01 06                       estructura "Flags"
 *   1A FF 4C 00 02 15 <uuid16> <major2> <minor2> <tx1>   "Manufacturer Specific Data"
 *
 * Por eso la trama no empieza en el byte 0, sino tras la cabecera FF y el
 * companyID. El constructor la localiza en tiempo de construccion.
 *
 * Datos de la trama iBeacon (25 bytes, big-endian, relativos a "inicio"):
 *
 *   0..1   companyID   (0x004C = Apple)
 *   2      iBeaconType (0x02)
 *   3      iBeaconLength (0x15 = 21)
 *   4..19  uuid        (16 bytes)
 *   20..21 major
 *   22..23 minor
 *   24     txPower
 *
 * En el laboratorio la trama llega con dos bytes de cabecera adicionales
 * (advFlags, advHeader), de ahi los 27 bytes del enunciado.
 */
public class TramaIBeacon {

    public static final int TAM_DATOS = 25;
    public static final int TAM_TRAMA = 27;
    public static final int POS_ADV_FLAGS = 0;
    public static final int POS_ADV_HEADER = 1;
    public static final int POS_COMPANY_ID = 0;
    public static final int POS_IBEACON_TYPE = 2;
    public static final int POS_IBEACON_LENGTH = 3;
    public static final int POS_UUID = 4;
    public static final int POS_MAJOR = 20;
    public static final int POS_MINOR = 22;
    public static final int POS_TX_POWER = 24;

    public static final byte IBEACON_TYPE = 0x02;
    public static final byte IBEACON_LENGTH = 0x15;
    public static final int COMPANY_ID_APPLE = 0x004C;

    public static final int TIPO_MANUFACTURER = 0xFF;
    public static final int NO_ENCONTRADA = -1;

    private final byte[] bytes;
    private final int inicio;
    private final byte[] losBytes;
    private final byte[] advFlags;
    private final byte[] advHeader;
    private final byte[] companyID;
    private final byte[] prefijo;
    private final byte[] uuid;
    private final byte[] major;
    private final byte[] minor;
    private final byte iBeaconType;
    private final byte iBeaconLength;
    private final byte txPower;

    //--------------------
    // bytes: [Z] --> TramaIBeacon() -->
    //
    // Localiza la trama iBeacon dentro del anuncio y la descompone en sus
    // campos.
    //--------------------
    public TramaIBeacon(byte[] bytes) {
        int encontrada = localizarTrama(bytes);
        if (encontrada == NO_ENCONTRADA) {
            throw new IllegalArgumentException("El anuncio recibido no contiene una trama iBeacon: "
                    + (bytes == null ? "null" : Utilidades.bytesToString(bytes)));
        }
        this.bytes = bytes;
        this.inicio = encontrada;
        this.losBytes = new byte[]{bytes[POS_ADV_FLAGS], bytes[POS_ADV_HEADER]};
        this.advFlags = new byte[]{bytes[POS_ADV_FLAGS]};
        this.advHeader = new byte[]{bytes[POS_ADV_HEADER]};
        this.companyID = new byte[]{
                bytes[inicio + POS_COMPANY_ID], bytes[inicio + POS_COMPANY_ID + 1]};
        this.prefijo = new byte[]{
                bytes[inicio + POS_COMPANY_ID], bytes[inicio + POS_COMPANY_ID + 1],
                bytes[inicio + POS_IBEACON_TYPE], bytes[inicio + POS_IBEACON_LENGTH]
        };
        this.iBeaconType = bytes[inicio + POS_IBEACON_TYPE];
        this.iBeaconLength = bytes[inicio + POS_IBEACON_LENGTH];
        this.uuid = new byte[16];
        System.arraycopy(bytes, inicio + POS_UUID, this.uuid, 0, 16);
        this.major = new byte[]{bytes[inicio + POS_MAJOR], bytes[inicio + POS_MAJOR + 1]};
        this.minor = new byte[]{bytes[inicio + POS_MINOR], bytes[inicio + POS_MINOR + 1]};
        this.txPower = bytes[inicio + POS_TX_POWER];
    }

    //--------------------
    // bytes: [Z] --> esTramaIBeacon() --> B --x
    //
    // Indica si el anuncio contiene una trama iBeacon.
    //--------------------
    public static boolean esTramaIBeacon(byte[] bytes) {
        return localizarTrama(bytes) != NO_ENCONTRADA;
    }

    //--------------------
    // bytes: [Z] --> localizarTrama() --> inicio: Z --x
    //
    // Devuelve la posicion del primer byte de la trama (companyID) dentro del
    // anuncio, o NO_ENCONTRADA si no hay ninguna.
    //
    // Primero recorre las estructuras AD buscando los datos de fabricante
    // (tipo 0xFF); si el anuncio no sigue esa estructura, busca la marca
    // 02 15 en cualquier posicion, lo que cubre la trama desnuda del
    // laboratorio (con cabecera advFlags/advHeader delante).
    //--------------------
    public static int localizarTrama(byte[] bytes) {
        if (bytes == null || bytes.length < TAM_DATOS) {
            return NO_ENCONTRADA;
        }
        int posicion = 0;
        while (posicion + 1 < bytes.length) {
            int longitud = bytes[posicion] & 0xFF;
            if (longitud <= 0 || posicion + 1 + longitud > bytes.length) {
                break;
            }
            if ((bytes[posicion + 1] & 0xFF) == TIPO_MANUFACTURER) {
                int inicio = posicion + 2;
                if (inicio + TAM_DATOS <= bytes.length && tieneMarcaIBeacon(bytes, inicio)) {
                    return inicio;
                }
            }
            posicion += 1 + longitud;
        }
        for (int i = 2; i + 3 < bytes.length; i++) {
            if (tieneMarcaIBeacon(bytes, i)) {
                return i - 2;
            }
        }
        return NO_ENCONTRADA;
    }

    //--------------------
    // bytes: [Z], companyId: N --> localizarManufacturerData() --> inicio: Z --x
    //
    // Devuelve la posicion de los datos de un fabricante concreto dentro del
    // anuncio, o NO_ENCONTRADA si el anuncio no los lleva. Sirve para
    // diagnosticar: si el beacon esta cerca pero la app no lo reconoce, hay
    // que distinguir "no hay ningun iBeacon cerca" de "hay uno pero la trama
    // no se parece a la que esperamos".
    //--------------------
    public static int localizarManufacturerData(byte[] bytes, int companyId) {
        if (bytes == null) {
            return NO_ENCONTRADA;
        }
        int posicion = 0;
        while (posicion + 1 < bytes.length) {
            int longitud = bytes[posicion] & 0xFF;
            if (longitud <= 0 || posicion + 1 + longitud > bytes.length) {
                break;
            }
            if ((bytes[posicion + 1] & 0xFF) == TIPO_MANUFACTURER) {
                int inicio = posicion + 2;
                if (inicio + 2 <= bytes.length) {
                    int id = (bytes[inicio] & 0xFF) | ((bytes[inicio + 1] & 0xFF) << 8);
                    if (id == companyId) {
                        return inicio;
                    }
                }
            }
            posicion += 1 + longitud;
        }
        return NO_ENCONTRADA;
    }

    //--------------------
    // bytes: [Z] --> esAnuncioConIBeaconDeApple() --> B
    //
    // Indica si el anuncio lleva datos de fabricante de Apple, paso previo a
    // comprobar si ademas es una trama iBeacon valida.
    //--------------------
    public static boolean esAnuncioConIBeaconDeApple(byte[] bytes) {
        return localizarManufacturerData(bytes, COMPANY_ID_APPLE) != NO_ENCONTRADA;
    }

    //--------------------
    // bytes: [Z], inicio: Z --> tieneMarcaIBeacon() --> B --x
    //
    // Comprueba que en la posicion indicada estan los dos bytes de cabecera
    // iBeacon (0x02 y 0x15) y que caben los 25 bytes.
    //--------------------
    private static boolean tieneMarcaIBeacon(byte[] bytes, int inicio) {
        return inicio >= 0
                && inicio + TAM_DATOS <= bytes.length
                && (bytes[inicio + POS_IBEACON_TYPE] & 0xFF) == IBEACON_TYPE
                && (bytes[inicio + POS_IBEACON_LENGTH] & 0xFF) == IBEACON_LENGTH;
    }

    //--------------------
    // --> getInicio() <--
    //
    // devuelve la posicion del anuncio donde empieza la trama iBeacon.
    //--------------------
    public int getInicio() {
        return inicio;
    }

    //--------------------
    // --> getLosBytes() <--
    //
    // devuelve una copia de los bytes completos del anuncio.
    //--------------------
    public byte[] getLosBytes() {
        return losBytes.clone();
    }

    //--------------------
    // --> getAdvFlags() <--
    //
    // devuelve una copia del byte de banderas del anuncio.
    //--------------------
    public byte[] getAdvFlags() {
        return advFlags.clone();
    }

    //--------------------
    // --> getAdvHeader() <--
    //
    // devuelve una copia del byte de cabecera del anuncio.
    //--------------------
    public byte[] getAdvHeader() {
        return advHeader.clone();
    }

    //--------------------
    // --> getCompanyID() <--
    //
    // devuelve una copia de los dos bytes del identificador de fabricante.
    //--------------------
    public byte[] getCompanyID() {
        return companyID.clone();
    }

    //--------------------
    // --> getCompanyIDComoInt() <--
    //
    // El company identifier viaja en orden little-endian (4C 00 = 0x004C).
    //--------------------
    public int getCompanyIDComoInt() {
        return (companyID[0] & 0xFF) | ((companyID[1] & 0xFF) << 8);
    }

    //--------------------
    // --> getPrefijo() <--
    //
    // devuelve una copia del prefijo que debe tener la UUID buscada.
    //--------------------
    public byte[] getPrefijo() {
        return prefijo.clone();
    }

    //--------------------
    // --> getiBeaconType() <--
    //
    // devuelve el byte de tipo iBeacon, que debe valer 0x02.
    //--------------------
    public byte getiBeaconType() {
        return iBeaconType;
    }

    //--------------------
    // --> getiBeaconLength() <--
    //
    // devuelve el byte de longitud iBeacon, que debe valer 0x15.
    //--------------------
    public byte getiBeaconLength() {
        return iBeaconLength;
    }

    //--------------------
    // --> getUUID() <--
    //
    // devuelve una copia de los 16 bytes de la UUID.
    //--------------------
    public byte[] getUUID() {
        return uuid.clone();
    }

    //--------------------
    // --> getUUIDComoUUID() <--
    //
    // devuelve la UUID de la trama ya como objeto UUID.
    //--------------------
    public UUID getUUIDComoUUID() {
        return Utilidades.bytesToUUID(uuid);
    }

    //--------------------
    // --> getMajor() <--
    //
    // devuelve una copia de los dos bytes del campo major.
    //--------------------
    public byte[] getMajor() {
        return major.clone();
    }

    //--------------------
    // --> getMajorComoInt() <--
    //
    // El campo major viaja big-endian (byte mas significativo primero), como
    // marca el estandar iBeacon y como lo emite BLEBeacon (Bluefruit).
    //--------------------
    public int getMajorComoInt() {
        return Utilidades.bytesToShort(major, 0);
    }

    //--------------------
    // --> getTipoDeMedicionComoInt() <--
    //
    // El emisor codifica el tipo en el byte alto del major y el numero de
    // iteracion en el byte bajo: major = (tipo << 8) + contador.
    //--------------------
    public int getTipoDeMedicionComoInt() {
        return getMajorComoInt() >> 8;
    }

    //--------------------
    // --> getContadorComoInt() <--
    //
    // Byte bajo del major: numero de iteracion de la medicion.
    //--------------------
    public int getContadorComoInt() {
        return getMajorComoInt() & 0xFF;
    }

    //--------------------
    // --> getMinor() <--
    //
    // devuelve una copia de los dos bytes del campo minor.
    //--------------------
    public byte[] getMinor() {
        return minor.clone();
    }

    //--------------------
    // --> getMinorComoInt() <--
    //
    // El campo minor tambien viaja big-endian. Contiene el valor medido.
    //--------------------
    public int getMinorComoInt() {
        return Utilidades.bytesToShort(minor, 0);
    }

    //--------------------
    // --> getTxPower() <--
    //
    // devuelve el byte de potencia de transmision, en dBm con signo.
    //--------------------
    public byte getTxPower() {
        return txPower;
    }

    //--------------------
    // --> getBytes() <--
    //
    // devuelve una copia de los bytes de la trama.
    //--------------------
    public byte[] getBytes() {
        return bytes.clone();
    }
}
