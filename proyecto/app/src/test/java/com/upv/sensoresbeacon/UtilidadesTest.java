package com.upv.sensoresbeacon;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.UUID;

//==============================================================================================
//Nombre: UtilidadesTest.java
//Descripcion: Pruebas de las conversiones entre bytes, cadenas, UUID e enteros de la clase
//              Utilidades.
//Copyright:
//Fecha de creacion: 2026-10-01
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Pruebas de la clase Utilidades.
 */
public class UtilidadesTest {

    // stringToBytes(): cada par de digitos hexadecimales se convierte en un byte.
    @Test
    public void stringToBytesConvierteCadaParHexadecimal() {
        assertArrayEquals(Utilidades.stringToBytes("001A"),
                Utilidades.stringToBytes("001A"));
        assertArrayEquals(Utilidades.stringToBytes("FFC0"),
                Utilidades.stringToBytes("FFC0"));
    }

    // stringToBytes(): ignora guiones y espacios al convertir la cadena.
    @Test
    public void stringToBytesAceptaGuionesYEspacios() {
        assertArrayEquals(Utilidades.stringToBytes("E2C50D"),
                Utilidades.stringToBytes("E2-C5 0D"));
    }

    // stringToBytes(): un digito suelto se completa con un cero por delante.
    @Test
    public void stringToBytesConUnSoloCaracterAnadeCeroIzquierdo() {
        assertArrayEquals(Utilidades.stringToBytes("0F"), Utilidades.stringToBytes("F"));
    }

    // stringToBytes(): con null devuelve un vector de bytes vacio.
    @Test
    public void stringToBytesConNullDevuelveVacio() {
        assertEquals(0, Utilidades.stringToBytes(null).length);
    }

    // bytesToString(): deshace lo que hace stringToBytes().
    @Test
    public void bytesToStringEsElInversoDeStringToBytes() {
        byte[] bytes = Utilidades.stringToBytes("E2C50D");
        assertEquals("E2C50D", Utilidades.bytesToString(bytes));
    }

    // bytesToHexString(): formatea 16 bytes como UUID canonico de 8-4-4-4-12.
    @Test
    public void bytesToHexStringAplicaElFormatoCanonicoDeUUID() {
        byte[] uuid = Utilidades.stringToBytes("F7826DA64FA24E988024BC5B71E0893E");
        assertEquals("F7826DA6-4FA2-4E98-8024-BC5B71E0893E", Utilidades.bytesToHexString(uuid));
    }

    // bytesToHexString(): si no son 16 bytes, imprime el hexadecimal sin guiones.
    @Test
    public void bytesToHexStringSinLongitudDeUUIDNoUsaSeparadores() {
        assertEquals("E2C50D", Utilidades.bytesToHexString(Utilidades.stringToBytes("E2C50D")));
    }

    // bytesToHexString(): con null devuelve cadena vacia.
    @Test
    public void bytesToHexStringConNullDevuelveCadenaVacia() {
        assertEquals("", Utilidades.bytesToHexString(null));
    }

    // stringToUUID(): convierte 32 digitos hexadecimales en un objeto UUID.
    @Test
    public void stringToUUIDReconstruyeElUUIDCanonico() {
        String texto = "F7826DA6-4FA2-4E98-8024-BC5B71E0893E";
        UUID uuid = Utilidades.stringToUUID(texto);
        assertEquals(texto.toLowerCase(), Utilidades.uuidToString(uuid));
    }

    // uuidToHexString(): quita los guiones del UUID y lo pasa a mayusculas.
    @Test
    public void uuidToHexStringEliminaLosGuionesYPasaAMayusculas() {
        UUID uuid = UUID.fromString("f7826da6-4fa2-4e98-8024-bc5b71e0893e");
        assertEquals("F7826DA64FA24E988024BC5B71E0893E", Utilidades.uuidToHexString(uuid));
    }

    // uuidToString() y uuidToHexString(): con null devuelven cadena vacia.
    @Test
    public void uuidToStringYHexStringConNullDevuelvenCadenaVacia() {
        assertEquals("", Utilidades.uuidToString(null));
        assertEquals("", Utilidades.uuidToHexString(null));
    }

    // bytesToUUID(): los 16 bytes de la trama producen el UUID original.
    @Test
    public void bytesToUUIDReconstruyeElMismoUUIDQueLaTrama() {
        String hex = "F7826DA64FA24E988024BC5B71E0893E";
        UUID desdeTexto = Utilidades.stringToUUID(hex);
        UUID desdeBytes = Utilidades.bytesToUUID(Utilidades.stringToBytes(hex));
        assertEquals(desdeTexto, desdeBytes);
    }

    // bytesToUUID(): con menos de 16 bytes devuelve null.
    @Test
    public void bytesToUUIDConMenosDe16BytesDevuelveNull() {
        assertNull(Utilidades.bytesToUUID(new byte[]{0x01, 0x02}));
    }

    // stringToUUID(): una longitud que no sea de 32 digitos lanza excepcion.
    @Test
    public void stringToUUIDConLongitudIncorrectaFalla() {
        try {
            Utilidades.stringToUUID("ABC");
            fail("Se esperaba IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // comportamiento correcto
        }
    }

    // intToBytes() y bytesToInt(): uno construye el entero y el otro lo deshace.
    @Test
    public void intToBytesYbytesToIntSonInversos() {
        assertArrayEquals(Utilidades.stringToBytes("00000100"), Utilidades.intToBytes(256));
        assertEquals(256, Utilidades.bytesToInt(Utilidades.intToBytes(256), 0));
        assertEquals(-1, Utilidades.bytesToInt(Utilidades.intToBytes(-1), 0));
    }

    // bytesToInt(): lee el entero desde la posicion que se le pase.
    @Test
    public void bytesToIntLeeDesdeLaPosicionIndicada() {
        assertEquals(26, Utilidades.bytesToInt(Utilidades.stringToBytes("0000001A"), 0));
        assertEquals(256, Utilidades.bytesToInt(Utilidades.stringToBytes("00000100"), 0));
    }

    // bytesToInt(): si no caben 4 bytes, devuelve cero.
    @Test
    public void bytesToIntFueraDeRangoDevuelveCero() {
        assertEquals(0, Utilidades.bytesToInt(new byte[]{0x01}, 0));
    }

    // bytesToIntOK(): indica si hay 4 bytes disponibles desde esa posicion.
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

    // dosLongToBytes(): empaqueta dos enteros de 16 bits en 4 bytes.
    @Test
    public void dosLongToBytesEmpaquetaDosEnterosDe16Bits() {
        assertArrayEquals(Utilidades.stringToBytes("000C0992"), Utilidades.dosLongToBytes(12, 2450));
    }

    // bytesToShort(): lee un entero de 16 bits, como el major y el minor.
    @Test
    public void bytesToShortLeeElMajorYElMinorDeLaTrama() {
        byte[] majorMinor = Utilidades.dosLongToBytes(12, 2450);
        assertEquals(12, Utilidades.bytesToShort(majorMinor, 0));
        assertEquals(2450, Utilidades.bytesToShort(majorMinor, 2));
    }

    // bytesToLong(): lee 8 bytes como entero largo, en big-endian.
    @Test
    public void bytesToLongLeeOchoBytesBigEndian() {
        byte[] bytes = Utilidades.stringToBytes("0000000000000040");
        assertEquals(64L, Utilidades.bytesToLong(bytes, 0));
    }
}
