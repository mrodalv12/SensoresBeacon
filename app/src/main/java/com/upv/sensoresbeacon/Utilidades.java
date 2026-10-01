package com.upv.sensoresbeacon;

import java.util.UUID;

/**
 * Diseno logico (Logical_Design_RE_Rules_v3.pdf, seccion 5 - Clases):
 *
 *        texto : Text
 *        uuid  : UUID
 *  -------------------- Utilidades ------------------
 *      |
 *      |
 *  --> stringToBytes() -->
 *      |
 *      |
 *  --> bytesToInt() <--
 *      |
 *      |
 *  --> stringToUUID() <--
 *      |
 *      |
 *  --> uuidToString() <--
 *      |
 *      |
 *  --> uuidToHexString() <--
 *      |
 *      |
 *  --> bytesToHexString() <--
 *      |
 *      |
 *  --> bytesToString() <--
 *      |
 *      |
 *  --> intToBytes() <--
 *      |
 *      |
 *  --> bytesToLong() <--
 *      |
 *      |
 *  --> bytesToIntOK() <--
 *      |
 *      |
 *  --> dosLongToBytes() <--
 *  ---------------------------------------------------
 *
 * Todos los metodos son --x (static): no dependen del estado de la instancia.
 */
public final class Utilidades {

    private static final String HEX = "0123456789ABCDEF";
    private static final int TAM_UUID = 16;
    private static final int TAM_INT = 4;
    private static final int TAM_LONG = 8;
    private static final int TAM_SHORT = 2;

    private Utilidades() {
    }

    /**
     * texto: Text --> stringToBytes() --> bytes: [Z]
     *
     * Convierte una cadena hexadecimal en su vector de bytes.
     */
    public static byte[] stringToBytes(String texto) {
        if (texto == null) {
            return new byte[0];
        }
        String limpio = texto.replace(" ", "").replace("-", "").replace(":", "");
        if (limpio.length() % 2 != 0) {
            limpio = "0" + limpio;
        }
        byte[] bytes = new byte[limpio.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(limpio.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    /**
     * bytes: [Z] --> bytesToString() --> Text
     *
     * Representacion hexadecimal en mayusculas y sin separadores.
     */
    public static String bytesToString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX.charAt((b >> 4) & 0x0F)).append(HEX.charAt(b & 0x0F));
        }
        return sb.toString();
    }

    /**
     * bytes: [Z] --> bytesToHexString() --> Text
     *
     * Representacion hexadecimal en mayusculas. Para los 16 bytes de un UUID
     * aplica el formato canonico 8-4-4-4-12; en cualquier otro caso devuelve
     * la hexadecimal sin separadores.
     */
    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        if (bytes.length != TAM_UUID) {
            return bytesToString(bytes);
        }
        String hex = bytesToString(bytes);
        return new StringBuilder(hex.length() + 4)
                .append(hex, 0, 8).append('-')
                .append(hex, 8, 12).append('-')
                .append(hex, 12, 16).append('-')
                .append(hex, 16, 20).append('-')
                .append(hex, 20, 32)
                .toString();
    }

    /**
     * texto: Text --> stringToUUID() --> uuid: UUID
     */
    public static UUID stringToUUID(String texto) {
        String limpio = texto.replace("-", "").replace(" ", "").replace(":", "");
        return uuidFromHex(limpio);
    }

    /**
     * bytes: [Z] --> bytesToUUID() --> uuid: UUID
     */
    public static UUID bytesToUUID(byte[] bytes) {
        if (bytes == null || bytes.length < TAM_UUID) {
            return null;
        }
        long msb = 0L;
        long lsb = 0L;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (bytes[i] & 0xFFL);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (bytes[i] & 0xFFL);
        }
        return new UUID(msb, lsb);
    }

    /**
     * uuid: UUID --> uuidToString() --> Text
     *
     * Formato canonico 8-4-4-4-12 en minusculas.
     */
    public static String uuidToString(UUID uuid) {
        return uuid == null ? "" : uuid.toString();
    }

    /**
     * uuid: UUID --> uuidToHexString() --> Text
     *
     * Formato canonico 8-4-4-4-12 en mayusculas, sin guiones bajos.
     */
    public static String uuidToHexString(UUID uuid) {
        return uuid == null ? "" : uuid.toString().replace("-", "").toUpperCase();
    }

    /**
     * n: Z --> intToBytes() --> bytes: [Z]
     *
     * Entero de 32 bits en orden big-endian (4 bytes).
     */
    public static byte[] intToBytes(int n) {
        return new byte[]{
                (byte) ((n >> 24) & 0xFF),
                (byte) ((n >> 16) & 0xFF),
                (byte) ((n >> 8) & 0xFF),
                (byte) (n & 0xFF)
        };
    }

    /**
     * bytes: [Z] --> bytesToInt() --> n: Z
     *
     * Lee un entero de 32 bits big-endian a partir del byte indicado.
     */
    public static int bytesToInt(byte[] bytes, int desde) {
        if (bytes == null || desde < 0 || desde + TAM_INT > bytes.length) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < TAM_INT; i++) {
            n = (n << 8) | (bytes[desde + i] & 0xFF);
        }
        return n;
    }

    /**
     * bytes: [Z] --> bytesToIntOK() --> n: Z
     *
     * Como bytesToInt() pero validating la longitud minima necesaria.
     */
    public static int bytesToIntOK(byte[] bytes, int desde) {
        if (bytes == null || desde < 0 || desde + TAM_INT > bytes.length) {
            throw new IllegalArgumentException("La trama no contiene un entero de 4 bytes en la posicion " + desde);
        }
        return bytesToInt(bytes, desde);
    }

    /**
     * bytes: [Z] --> bytesToLong() --> n: Z
     *
     * Lee 8 bytes big-endian. Los valores con el bit mas significativo a 1
     * (mayor o igual a 2^31) se interpretan como negativos, al ser <i>long</i>
     * con signo en Java.
     */
    public static long bytesToLong(byte[] bytes, int desde) {
        if (bytes == null || desde < 0 || desde + TAM_LONG > bytes.length) {
            return 0L;
        }
        long n = 0L;
        for (int i = 0; i < TAM_LONG; i++) {
            n = (n << 8) | (bytes[desde + i] & 0xFFL);
        }
        return n;
    }

    /**
     * n: Z --> dosLongToBytes() --> bytes: [Z]
     *
     * Empaqueta 2 enteros de 16 bits en 4 bytes big-endian.
     */
    public static byte[] dosLongToBytes(int masSignificativos, int menosSignificativos) {
        return new byte[]{
                (byte) ((masSignificativos >> 8) & 0xFF),
                (byte) (masSignificativos & 0xFF),
                (byte) ((menosSignificativos >> 8) & 0xFF),
                (byte) (menosSignificativos & 0xFF)
        };
    }

    /**
     * bytes: [Z] --> bytesToShort() --> n: Z
     *
     * Lee un entero de 16 bits big-endian.
     */
    public static int bytesToShort(byte[] bytes, int desde) {
        if (bytes == null || desde < 0 || desde + TAM_SHORT > bytes.length) {
            return 0;
        }
        return ((bytes[desde] & 0xFF) << 8) | (bytes[desde + 1] & 0xFF);
    }

    /**
     * bytes: [Z] --> bytesToShortLE() --> n: Z
     *
     * Lee un entero de 16 bits little-endian (byte menos significativo
     * primero), que es como viajan los campos major y minor de un iBeacon.
     */
    public static int bytesToShortLE(byte[] bytes, int desde) {
        if (bytes == null || desde < 0 || desde + TAM_SHORT > bytes.length) {
            return 0;
        }
        return (bytes[desde] & 0xFF) | ((bytes[desde + 1] & 0xFF) << 8);
    }

    /**
     * texto: Text --> bytesDesdeTextoAscii() --> bytes: [Z]
     *
     * Toma los 16 caracteres del texto tal cual, guiones incluidos, y los
     * devuelve como los bytes que el emisor mete en el campo UUID de la
     * trama. No se elimina ningun caracter: el array beaconUUID del emisor
     * tiene exactamente 16 entradas, y los guiones ocupan dos de ellas.
     */
    public static byte[] bytesDesdeTextoAscii(String texto) {
        if (texto == null) {
            return null;
        }
        if (texto.length() != TAM_UUID) {
            throw new IllegalArgumentException(
                    "El texto debe tener " + TAM_UUID + " caracteres, tiene "
                            + texto.length() + ": " + texto);
        }
        byte[] bytes = new byte[TAM_UUID];
        for (int i = 0; i < TAM_UUID; i++) {
            bytes[i] = (byte) texto.charAt(i);
        }
        return bytes;
    }

    /**
     * texto: Text --> uuidDesdeTextoAscii() --> uuid: UUID
     *
     * Muchos emisores de coursework no usan una UUID hexadecimal, sino que
     * meten 16 caracteres ASCII en el campo UUID. Esta funcion traduce el
     * texto a los bytes que realmente viajan por el aire, de modo que
     * "EPSG-GTI-PROY-3A" se convierta en su UUID equivalente.
     */
    public static UUID uuidDesdeTextoAscii(String texto) {
        return bytesToUUID(bytesDesdeTextoAscii(texto));
    }

    private static UUID uuidFromHex(String hex) {
        if (hex.length() != TAM_UUID * 2) {
            throw new IllegalArgumentException("Un UUID requiere 32 caracteres hexadecimales: " + hex);
        }
        return bytesToUUID(stringToBytes(hex));
    }
}
