package com.upv.sensoresbeacon;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.UUID;

/**
 * Pruebas de la clase Utilidades.
 */
public class UtilidadesTest {

    @Test
    public void stringToBytesConvierteCadaParHexadecimal() {
        assertArrayEquals(Utilidades.stringToBytes("001A"),
                Utilidades.stringToBytes("001A"));
        assertArrayEquals(Utilidades.stringToBytes("FFC0"),
                Utilidades.stringToBytes("FFC0"));
    }

    @Test
    public void stringToBytesAceptaGuionesYEspacios() {
        assertArrayEquals(Utilidades.stringToBytes("E2C50D"),
                Utilidades.stringToBytes("E2-C5 0D"));
    }

    @Test
    public void stringToBytesConUnSoloCaracterAnadeCeroIzquierdo() {
        assertArrayEquals(Utilidades.stringToBytes("0F"), Utilidades.stringToBytes("F"));
    }

    @Test
    public void stringToBytesConNullDevuelveVacio() {
        assertEquals(0, Utilidades.stringToBytes(null).length);
    }

    @Test
    public void bytesToStringEsElInversoDeStringToBytes() {
        byte[] bytes = Utilidades.stringToBytes("E2C50D");
        assertEquals("E2C50D", Utilidades.bytesToString(bytes));
    }

    @Test
    public void bytesToHexStringAplicaElFormatoCanonicoDeUUID() {
        byte[] uuid = Utilidades.stringToBytes("F7826DA64FA24E988024BC5B71E0893E");
        assertEquals("F7826DA6-4FA2-4E98-8024-BC5B71E0893E", Utilidades.bytesToHexString(uuid));
    }

    @Test
    public void bytesToHexStringSinLongitudDeUUIDNoUsaSeparadores() {
        assertEquals("E2C50D", Utilidades.bytesToHexString(Utilidades.stringToBytes("E2C50D")));
    }

    @Test
    public void bytesToHexStringConNullDevuelveCadenaVacia() {
        assertEquals("", Utilidades.bytesToHexString(null));
    }

    @Test
    public void stringToUUIDReconstruyeElUUIDCanonico() {
        String texto = "F7826DA6-4FA2-4E98-8024-BC5B71E0893E";
        UUID uuid = Utilidades.stringToUUID(texto);
        assertEquals(texto.toLowerCase(), Utilidades.uuidToString(uuid));
    }

    @Test
    public void uuidToHexStringEliminaLosGuionesYPasaAMayusculas() {
        UUID uuid = UUID.fromString("f7826da6-4fa2-4e98-8024-bc5b71e0893e");
        assertEquals("F7826DA64FA24E988024BC5B71E0893E", Utilidades.uuidToHexString(uuid));
    }

    @Test
    public void uuidToStringYHexStringConNullDevuelvenCadenaVacia() {
        assertEquals("", Utilidades.uuidToString(null));
        assertEquals("", Utilidades.uuidToHexString(null));
    }

    @Test
    public void bytesToUUIDReconstruyeElMismoUUIDQueLaTrama() {
        String hex = "F7826DA64FA24E988024BC5B71E0893E";
        UUID desdeTexto = Utilidades.stringToUUID(hex);
        UUID desdeBytes = Utilidades.bytesToUUID(Utilidades.stringToBytes(hex));
        assertEquals(desdeTexto, desdeBytes);
    }

    @Test
    public void bytesToUUIDConMenosDe16BytesDevuelveNull() {
        assertNull(Utilidades.bytesToUUID(new byte[]{0x01, 0x02}));
    }

    @Test
    public void stringToUUIDConLongitudIncorrectaFalla() {
        try {
            Utilidades.stringToUUID("ABC");
            fail("Se esperaba IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // comportamiento correcto
        }
    }

    @Test
    public void intToBytesYbytesToIntSonInversos() {
        assertArrayEquals(Utilidades.stringToBytes("00000100"), Utilidades.intToBytes(256));
        assertEquals(256, Utilidades.bytesToInt(Utilidades.intToBytes(256), 0));
        assertEquals(-1, Utilidades.bytesToInt(Utilidades.intToBytes(-1), 0));
    }

    @Test
    public void bytesToIntLeeDesdeLaPosicionIndicada() {
        assertEquals(26, Utilidades.bytesToInt(Utilidades.stringToBytes("0000001A"), 0));
        assertEquals(256, Utilidades.bytesToInt(Utilidades.stringToBytes("00000100"), 0));
    }

    @Test
    public void bytesToIntFueraDeRangoDevuelveCero() {
        assertEquals(0, Utilidades.bytesToInt(new byte[]{0x01}, 0));
    }

    @Test
    public void bytesToIntOKValidaLaLongitudMinima() {
        assertEquals(256, Utilidades.bytesToIntOK(Utilidades.stringToBytes("00000100"), 0));
        try {
            Utilidades.bytesToIntOK(new byte[]{0x01, 0x02}, 0);
            fail("Se esperaba IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // comportamiento correcto
        }
    }

    @Test
    public void dosLongToBytesEmpaquetaDosEnterosDe16Bits() {
        assertArrayEquals(Utilidades.stringToBytes("000C0992"), Utilidades.dosLongToBytes(12, 2450));
    }

    @Test
    public void bytesToShortLeeElMajorYElMinorDeLaTrama() {
        byte[] majorMinor = Utilidades.dosLongToBytes(12, 2450);
        assertEquals(12, Utilidades.bytesToShort(majorMinor, 0));
        assertEquals(2450, Utilidades.bytesToShort(majorMinor, 2));
    }

    @Test
    public void bytesToLongLeeOchoBytesBigEndian() {
        byte[] bytes = Utilidades.stringToBytes("0000000000000040");
        assertEquals(64L, Utilidades.bytesToLong(bytes, 0));
    }
}
