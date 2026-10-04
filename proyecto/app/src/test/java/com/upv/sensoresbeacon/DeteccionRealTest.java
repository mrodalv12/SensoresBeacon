package com.upv.sensoresbeacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

//==============================================================================================
//Nombre: DeteccionRealTest.java
//Descripcion: Pruebas de deteccion sobre anuncios BLE reales: UUID esperada, major/minor y
//              descarte de anuncios repetidos.
//Copyright:
//Fecha de creacion: 2026-10-01
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Comprueba el comportamiento con el anuncio que produce realmente el emisor.
 *
 * El paquete bluefruit construye el beacon asi (EmisoraBLE.h usa BLEBeacon):
 *
 *   02 01 06                    estructura "Flags"
 *   1A FF 4C 00 02 15           "Manufacturer Specific Data" de Apple
 *   <16 bytes de UUID>          big-endian, en el orden del array
 *   <2 bytes de major>          little-endian
 *   <2 bytes de minor>          little-endian
 *   <1 byte de txPower>
 */
public class DeteccionRealTest {

    /** UUID del emisor tal y como la declara Publicador.h. */
    private static final String UUID_EMISORA = "45505347-2D47-5449-2D50-524F592D4D4A";

    private static final String MAC = "C3:00:00:00:00:00";

    /**
     * Reconstruye byte a byte el anuncio que emite el hardware.
     */
    private static byte[] anuncioDelEmisor(int major, int minor, int txPower) {
        byte[] uuid = Utilidades.stringToBytes(UUID_EMISORA);
        byte[] bytes = new byte[31];
        int i = 0;
        bytes[i++] = 0x02;  // longitud de la estructura Flags
        bytes[i++] = 0x01;  // tipo Flags
        bytes[i++] = 0x06;  // LE General Discoverable + BR/EDR incompatible
        bytes[i++] = (byte) 0x1A;  // longitud de los datos de fabricante (26)
        bytes[i++] = (byte) 0xFF;  // tipo Manufacturer Specific Data
        bytes[i++] = (byte) 0x4C;  // companyID de Apple, little-endian
        bytes[i++] = 0x00;
        bytes[i++] = 0x02;  // iBeaconType
        bytes[i++] = 0x15;  // iBeaconLength
        System.arraycopy(uuid, 0, bytes, i, 16);
        i += 16;
        bytes[i++] = (byte) (major & 0xFF);        // major little-endian
        bytes[i++] = (byte) ((major >> 8) & 0xFF);
        bytes[i++] = (byte) (minor & 0xFF);        // minor little-endian
        bytes[i++] = (byte) ((minor >> 8) & 0xFF);
        bytes[i] = (byte) txPower;
        return bytes;
    }

    // el anuncio del emisor se detecta y su trama iBeacon se localiza.
    @Test
    public void detectaElAnuncioDelEmisorYLocalizaLaTrama() {
        byte[] bytes = anuncioDelEmisor((11 << 8) + 1, 612, 4);
        assertTrue(TramaIBeacon.esTramaIBeacon(bytes));
        assertEquals(5, TramaIBeacon.localizarTrama(bytes));
    }

    // la trama usa la UUID que espera la app.
    @Test
    public void laTramaEmiteLaUuidEsperada() {
        TramaIBeacon trama = new TramaIBeacon(anuncioDelEmisor((11 << 8) + 1, 612, 4));
        // La app muestra la UUID en formato canonico, el mismo que UUID_BUSCADA
        assertEquals(UUID_EMISORA, Utilidades.bytesToHexString(trama.getUUID()));
        assertEquals(MainActivity.UUID_BUSCADA, Utilidades.bytesToHexString(trama.getUUID()));
    }

    // el companyId de la trama se lee como Apple (0x004C).
    @Test
    public void elCompanyIdSeLeeComoApple() {
        TramaIBeacon trama = new TramaIBeacon(anuncioDelEmisor((11 << 8) + 1, 612, 4));
        assertEquals(0x004C, trama.getCompanyIDComoInt());
    }

    // major y minor se interpretan en little-endian.
    @Test
    public void elMajorYElMinorSeLeenLittleEndian() {
        TramaIBeacon trama = new TramaIBeacon(anuncioDelEmisor((11 << 8) + 7, 2450, 4));
        assertEquals(2823, trama.getMajorComoInt());
        assertEquals(2450, trama.getMinorComoInt());
    }

    // el major lleva el tipo en el byte alto y el contador en el bajo.
    @Test
    public void elTipoVaEnElByteAltoYElContadorEnElBajo() {
        TramaIBeacon trama = new TramaIBeacon(anuncioDelEmisor((11 << 8) + 7, 2450, 4));
        assertEquals(11, trama.getTipoDeMedicionComoInt());
        assertEquals(7, trama.getContadorComoInt());
        assertEquals("co2", Medicion.desdeTrama(trama).getTipoDeMedicion());
    }

    // la misma emisora publica tanto CO2 como temperatura.
    @Test
    public void laMismaEmisoraEmiteCO2YTemperatura() {
        TramaIBeacon co2 = new TramaIBeacon(anuncioDelEmisor((11 << 8) + 3, 812, 4));
        TramaIBeacon temperatura = new TramaIBeacon(anuncioDelEmisor((12 << 8) + 3, 23, 4));
        assertEquals("co2", Medicion.desdeTrama(co2).getTipoDeMedicion());
        assertEquals("temperatura", Medicion.desdeTrama(temperatura).getTipoDeMedicion());
    }

    // dos mediciones distintas de la misma placa no se descartan.
    @Test
    public void dosMedicionesDistintasDeLaMismaPlacaNoSeDescartan() {
        // La placa tiene MAC fija: si la deduplicacion usara solo la MAC, la
        // temperatura se perderia siempre y la app pareceria apagada.
        String co2 = MainActivity.claveDeMedicion(MAC, (11 << 8) + 3, 812);
        String temperatura = MainActivity.claveDeMedicion(MAC, (12 << 8) + 3, 23);
        assertNotEquals(co2, temperatura);
    }

    // un anuncio repetido se descarta, para no enviarlo dos veces.
    @Test
    public void elAnuncioRepetidoSiSeDescarta() {
        String primera = MainActivity.claveDeMedicion(MAC, (11 << 8) + 3, 812);
        String repetido = MainActivity.claveDeMedicion(MAC, (11 << 8) + 3, 812);
        assertEquals(primera, repetido);
    }

    // cada iteracion del contador produce una medicion nueva.
    @Test
    public void cadaIteracionDelContadorDaUnaMedicionNueva() {
        int[][] medidas = {{11, 1, 700}, {11, 2, 701}, {12, 2, 22}, {13, 2, 55}};
        java.util.Set<String> claves = new java.util.LinkedHashSet<>();
        for (int[] m : medidas) {
            claves.add(MainActivity.claveDeMedicion(MAC, (m[0] << 8) + m[1], m[2]));
        }
        assertEquals(medidas.length, claves.size());
    }

    // un anuncio ajeno no se confunde con la trama iBeacon.
    @Test
    public void unAnuncioNoRelacionadoNoSeConfundeConUnIBeacon() {
        // anuncio BLE corriente: solo flags y el nombre "GTI-3A-MJ"
        byte[] anuncio = {
                0x02, 0x01, 0x06,
                0x0A, 0x09, (byte) 'G', (byte) 'T', (byte) 'I', (byte) '-',
                (byte) '3', (byte) 'A', (byte) '-', (byte) 'M', (byte) 'J'
        };
        assertFalse(TramaIBeacon.esTramaIBeacon(anuncio));
    }

    // el anuncio del emisor se reconoce como datos de fabricante de Apple.
    @Test
    public void elAnuncioDelEmisorSeReconoceComoApple() {
        assertTrue(TramaIBeacon.esAnuncioConIBeaconDeApple(anuncioDelEmisor((11 << 8) + 1, 612, 4)));
    }

    // localizarManufacturerData(): encuentra donde estan los datos de Apple.
    @Test
    public void seEncuentraLaPosicionDeLosDatosDeApple() {
        byte[] bytes = anuncioDelEmisor((11 << 8) + 1, 612, 4);
        assertEquals(5, TramaIBeacon.localizarManufacturerData(bytes, 0x004C));
    }

    // los manufacturer data se cuentan en el advertisement correcto.
    @Test
    public void losDatosDeManufacturerCuentanEnElAdvertisementCorrecto() {
        // Un anuncio con la marca iBeacon va detras de la estructura Flags,
        // no al principio: si se buscara en el byte 0 nunca se encontraria.
        byte[] bytes = anuncioDelEmisor((11 << 8) + 1, 612, 4);
        assertEquals(5, TramaIBeacon.localizarManufacturerData(bytes, 0x004C));
        assertTrue(TramaIBeacon.localizarManufacturerData(bytes, 0x0000) == TramaIBeacon.NO_ENCONTRADA);
    }

    // un anuncio sin datos del fabricante no se cuenta.
    @Test
    public void unAnuncioQueNoEsDelFabricanteNoSeCuenta() {
        byte[] anuncio = {
                0x02, 0x01, 0x06,
                0x0A, 0x09, (byte) 'G', (byte) 'T', (byte) 'I', (byte) '-',
                (byte) '3', (byte) 'A', (byte) '-', (byte) 'M', (byte) 'J'
        };
        assertFalse(TramaIBeacon.esAnuncioConIBeaconDeApple(anuncio));
    }

    // un anuncio de Apple que no es beacon no se toma por iBeacon.
    @Test
    public void unAnuncioDeAppleQueNoEsBeaconNoSeConfunde() {
        // datos de fabricante de Apple, pero sin la marca 02 15
        byte[] anuncio = {
                0x02, 0x01, 0x06,
                0x06, (byte) 0xFF, (byte) 0x4C, 0x00, (byte) 0x99, (byte) 0x88
        };
        assertFalse(TramaIBeacon.esTramaIBeacon(anuncio));
    }
}
