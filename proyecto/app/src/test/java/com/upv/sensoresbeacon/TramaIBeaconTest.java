package com.upv.sensoresbeacon;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.UUID;

//==============================================================================================
//Nombre: TramaIBeaconTest.java
//Descripcion: Pruebas de la descomposicion de la trama iBeacon dentro de un anuncio BLE.
//Copyright:
//Fecha de creacion: 2026-10-01
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Pruebas de la clase TramaIBeacon, incluida la trama tal y como la entrega
 * Android en ScanRecord.getBytes().
 */
public class TramaIBeaconTest {

    static final String UUID_HEX = "F7826DA64FA24E988024BC5B71E0893E";
    static final int MAJOR = 12;
    static final int MINOR = 2450;
    static final int TX_POWER = -59;

    /**
     * bytes: [Z] --> tramaCompleta() --> bytes: [Z]
     *
     * Trama iBeacon desnuda de 27 bytes, como la del enunciado: dos bytes de
     * cabecera (advFlags, advHeader) seguidos de los datos iBeacon.
     */
    static byte[] tramaCompleta(int major, int minor, int txPower) {
        byte[] bytes = new byte[TramaIBeacon.TAM_TRAMA];
        bytes[0] = 0x06;
        bytes[1] = (byte) 0xFF;
        bytes[2] = 0x4C;
        bytes[3] = 0x00;
        return rellenarDatos(bytes, 2, major, minor, txPower);
    }

    /**
     * bytes: [Z] --> anuncioCompleto() --> bytes: [Z]
     *
     * Anuncio real de Android: estructura AD "Flags" + estructura
     * "Manufacturer Specific Data" con la trama iBeacon dentro.
     */
    static byte[] anuncioCompleto(int major, int minor, int txPower) {
        byte[] anuncio = new byte[31];
        anuncio[0] = 0x02;
        anuncio[1] = 0x01;
        anuncio[2] = 0x06;
        anuncio[3] = 0x1A;
        anuncio[4] = (byte) 0xFF;
        return rellenarDatos(anuncio, 5, major, minor, txPower);
    }

    // rellenarDatos(): monta un anuncio con la trama iBeacon en la posicion pedida.
    private static byte[] rellenarDatos(byte[] bytes, int inicio, int major, int minor, int txPower) {
        bytes[inicio] = 0x4C;
        bytes[inicio + 1] = 0x00;
        bytes[inicio + 2] = TramaIBeacon.IBEACON_TYPE;
        bytes[inicio + 3] = TramaIBeacon.IBEACON_LENGTH;
        System.arraycopy(Utilidades.stringToBytes(UUID_HEX), 0, bytes, inicio + 4, 16);
        // major y minor viajan little-endian, como en cualquier iBeacon
        bytes[inicio + 20] = (byte) (major & 0xFF);
        bytes[inicio + 21] = (byte) ((major >> 8) & 0xFF);
        bytes[inicio + 22] = (byte) (minor & 0xFF);
        bytes[inicio + 23] = (byte) ((minor >> 8) & 0xFF);
        bytes[inicio + 24] = (byte) txPower;
        return bytes;
    }

    // esTramaIBeacon(): reconoce el anuncio completo que emite Android.
    @Test
    public void detectaElAnuncioRealDeAndroid() {
        assertTrue(TramaIBeacon.esTramaIBeacon(anuncioCompleto(MAJOR, MINOR, TX_POWER)));
    }

    // del anuncio real de Android extrae bien uuid, major, minor y txPower.
    @Test
    public void extraeLosCamposDelAnuncioRealDeAndroid() {
        TramaIBeacon trama = new TramaIBeacon(anuncioCompleto(MAJOR, MINOR, TX_POWER));
        assertEquals(5, trama.getInicio());
        assertEquals(MAJOR, trama.getMajorComoInt());
        assertEquals(MINOR, trama.getMinorComoInt());
        assertEquals(TX_POWER, trama.getTxPower());
        assertEquals(UUID.fromString("F7826DA6-4FA2-4E98-8024-BC5B71E0893E"),
                trama.getUUIDComoUUID());
        assertEquals(TramaIBeacon.COMPANY_ID_APPLE, trama.getCompanyIDComoInt());
    }

    // esTramaIBeacon(): reconoce la trama iBeacon sin cabecera, la del laboratorio.
    @Test
    public void detectaLaTramaDesnudaDelLaboratorio() {
        assertTrue(TramaIBeacon.esTramaIBeacon(tramaCompleta(MAJOR, MINOR, TX_POWER)));
    }

    // de la trama desnuda extrae bien uuid, major, minor y txPower.
    @Test
    public void extraeLosCamposDeLaTramaDesnudaDelLaboratorio() {
        TramaIBeacon trama = new TramaIBeacon(tramaCompleta(MAJOR, MINOR, TX_POWER));
        assertEquals(2, trama.getInicio());
        assertEquals(MAJOR, trama.getMajorComoInt());
        assertEquals(MINOR, trama.getMinorComoInt());
        assertEquals(TX_POWER, trama.getTxPower());
        assertEquals(UUID.fromString("F7826DA6-4FA2-4E98-8024-BC5B71E0893E"),
                trama.getUUIDComoUUID());
    }

    // con cabecera y sin cabecera se leen los mismos campos.
    @Test
    public void elAnuncioYLaTramaDesnudaProducenLosMismosCampos() {
        TramaIBeacon delAnuncio = new TramaIBeacon(anuncioCompleto(MAJOR, MINOR, TX_POWER));
        TramaIBeacon desnuda = new TramaIBeacon(tramaCompleta(MAJOR, MINOR, TX_POWER));

        assertArrayEquals(desnuda.getUUID(), delAnuncio.getUUID());
        assertArrayEquals(desnuda.getMajor(), delAnuncio.getMajor());
        assertArrayEquals(desnuda.getMinor(), delAnuncio.getMinor());
        assertEquals(desnuda.getTxPower(), delAnuncio.getTxPower());
        assertEquals(desnuda.getCompanyIDComoInt(), delAnuncio.getCompanyIDComoInt());
    }

    // localizarTrama(): encuentra la trama este donde este dentro del anuncio.
    @Test
    public void localizaLaTramaEnLasPosicionesEsperadas() {
        assertEquals(5, TramaIBeacon.localizarTrama(anuncioCompleto(MAJOR, MINOR, TX_POWER)));
        assertEquals(2, TramaIBeacon.localizarTrama(tramaCompleta(MAJOR, MINOR, TX_POWER)));
    }

    // los campos se leen igual aunque el UUID no sea el del emisor del laboratorio.
    @Test
    public void funcionaConTramasDeOtrosEmisoresDistintosDeApple() {
        byte[] anuncio = anuncioCompleto(MAJOR, MINOR, TX_POWER);
        anuncio[5] = (byte) 0x76;
        anuncio[6] = (byte) 0x89;

        TramaIBeacon trama = new TramaIBeacon(anuncio);
        assertNotEquals(TramaIBeacon.COMPANY_ID_APPLE, trama.getCompanyIDComoInt());
        assertEquals(MAJOR, trama.getMajorComoInt());
        assertEquals(MINOR, trama.getMinorComoInt());
    }

    // esTramaIBeacon(): rechaza anuncios sin la marca iBeacon 02 15.
    @Test
    public void esTramaIBeaconRechazaAnunciosSinLaMarca02_15() {
        byte[] anuncio = anuncioCompleto(MAJOR, MINOR, TX_POWER);
        anuncio[5] = 0x4C;
        anuncio[6] = 0x00;
        anuncio[5 + 3] = 0x03;
        assertFalse(TramaIBeacon.esTramaIBeacon(anuncio));
    }

    // esTramaIBeacon(): rechaza anuncios demasiado cortos para la trama.
    @Test
    public void esTramaIBeaconRechazaAnunciosDemasiadoCortos() {
        assertFalse(TramaIBeacon.esTramaIBeacon(new byte[]{0x06, (byte) 0xFF, 0x4C}));
        assertFalse(TramaIBeacon.esTramaIBeacon(null));
    }

    // esTramaIBeacon(): rechaza datos que no son de Bluetooth.
    @Test
    public void esTramaIBeaconRechazaAnunciosQueNoSonDeBluetooth() {
        assertFalse(TramaIBeacon.esTramaIBeacon(
                Utilidades.stringToBytes("02010611FF0706554C0050534B32315A0C")));
    }

    // TramaIBeacon(): separa los bytes de cabecera de los datos de la trama.
    @Test
    public void constructorDescomponeLosCabeceros() {
        TramaIBeacon trama = new TramaIBeacon(tramaCompleta(MAJOR, MINOR, TX_POWER));
        assertEquals(TramaIBeacon.IBEACON_TYPE, trama.getiBeaconType());
        assertEquals(TramaIBeacon.IBEACON_LENGTH, trama.getiBeaconLength());
        assertEquals(2, trama.getLosBytes().length);
        assertEquals(4, trama.getPrefijo().length);
        assertEquals(16, trama.getUUID().length);
    }

    // los setters de bytes copian el array, no guardan la referencia recibida.
    @Test
    public void losSettersDeBytesDevuelvenCopiasDefensivas() {
        TramaIBeacon trama = new TramaIBeacon(tramaCompleta(MAJOR, MINOR, TX_POWER));
        byte[] uuid = trama.getUUID();
        uuid[0] = 0x00;
        assertEquals(0xF7, trama.getUUID()[0] & 0xFF);
    }

    // getMajor(): interpreta el major como entero de 16 bits sin signo.
    @Test
    public void majorSeInterpretaComoEnteroSinSignoDe16Bits() {
        TramaIBeacon trama = new TramaIBeacon(tramaCompleta(0xFFFF, 0xFFFF, 0));
        assertEquals(65535, trama.getMajorComoInt());
        assertEquals(65535, trama.getMinorComoInt());
    }

    // TramaIBeacon(): falla si el anuncio no contiene una trama iBeacon.
    @Test
    public void constructorRechazaAnunciosSinTramaIBeacon() {
        try {
            new TramaIBeacon(Utilidades.stringToBytes("02010611FF0706554C0050534B32315A0C"));
            fail("Se esperaba IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // comportamiento correcto
        }
    }
}
