package com.upv.sensoresbeacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

//==============================================================================================
//Nombre: FiltroUuidTest.java
//Descripcion: Pruebas del filtro por UUID: valida el formato y compara ignorando guiones
//              y mayusculas.
//Copyright:
//Fecha de creacion: 2026-10-01
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Pruebas de la validacion y comparacion de la UUID buscada.
 *
 * MainActivity es una Activity, asi que aqui se comprueba la logica de la
 * comprobacion contra los metodos de Utilidades, que es la que decide si el
 * valor configurado es utilizable.
 */
public class FiltroUuidTest {

    static final String UUID_VALIDA = "F7826DA6-4FA2-4E98-8024-BC5B71E0893E";

    // esValida(): indica si la cadena es un UUID con formato correcto.
    private static boolean esValida(String uuid) {
        try {
            Utilidades.stringToUUID(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // normalizar(): quita guiones y espacios y pasa a mayusculas.
    private static String normalizar(String texto) {
        return texto.replace("-", "").replace(":", "").replace(" ", "").toUpperCase();
    }

    // un UUID canonico de 8-4-4-4-12 es valido.
    @Test
    public void unaUuidEnFormatoCanonicoEsValida() {
        assertTrue(esValida(UUID_VALIDA));
    }

    // el mismo UUID escrito sin guiones tambien es valido.
    @Test
    public void unaUuidSinGuionesTambienEsValida() {
        assertTrue(esValida("F7826DA64FA24E988024BC5B71E0893E"));
    }

    // la cadena vacia, el null y los signos de pendiente no son un UUID.
    @Test
    public void elValorPendienteNoEsUnaUuid() {
        assertFalse(esValida("EPSG-GTI-PROY-MJ"));
    }

    // se rechazan las letras que no son hexadecimales.
    @Test
    public void rechazaCadenasConLetrasNoHexadecimales() {
        assertFalse(esValida("G7826DA6-4FA2-4E98-8024-BC5B71E0893E"));
        assertFalse(esValida("EPSG-GTI-PROY-MJ"));
    }

    // se rechaza un UUID con un numero de digitos incorrecto.
    @Test
    public void rechazaUuidConNumeroIncorrectoDeDigitos() {
        assertFalse(esValida("ABC"));
        assertFalse(esValida("F7826DA6-4FA2-4E98-8024-BC5B71E0893"));
    }

    // comparar(): da igual si cambia el formato o las mayusculas.
    @Test
    public void compararIgnoraGuionesYMayusculas() {
        assertEquals(normalizar(UUID_VALIDA), normalizar("f7826da64fa24e988024bc5b71e0893e"));
        assertEquals(normalizar(UUID_VALIDA), normalizar("f782:6da6:4fa2:4e98:8024:bc5b:71e0:893e"));
    }

    // comparar(): normaliza las dos cadenas antes de comparar.
    @Test
    public void laCoincidenciaSeCalculaSobreLaFormaNormalizada() {
        String buscada = UUID_VALIDA;
        String recibida = Utilidades.uuidToHexString(
                Utilidades.stringToUUID(UUID_VALIDA));
        assertEquals(normalizar(buscada), normalizar(recibida));
    }

    // el UUID leido de una trama real coincide con el configurado.
    @Test
    public void laUuidRecibidaDeUnaTramaCoincideConLaBuscada() {
        byte[] anuncio = TramaIBeaconTest.anuncioCompleto(12, 2450, -59);
        TramaIBeacon trama = new TramaIBeacon(anuncio);
        String uuid = Utilidades.uuidToHexString(trama.getUUIDComoUUID());

        assertTrue(normalizar(UUID_VALIDA).equals(normalizar(uuid)));
    }

    // un UUID distinto no coincide con el buscado.
    @Test
    public void unaUuidDistintaNoCoincide() {
        String otra = Utilidades.uuidToHexString(
                Utilidades.stringToUUID("00000000-0000-0000-0000-000000000000"));
        assertFalse(normalizar(UUID_VALIDA).equals(normalizar(otra)));
    }

    // el filtro de fabricante se apoya en la marca iBeacon de Apple.
    @Test
    public void elFiltroDeFabricanteUsaLaMarcaIBeaconDeApple() {
        assertEquals(TramaIBeacon.COMPANY_ID_APPLE, 0x004C);
        assertEquals(0x02, TramaIBeacon.IBEACON_TYPE);
        assertEquals(0x15, TramaIBeacon.IBEACON_LENGTH);
    }

    // el filtro descarta los anuncios que no son trama iBeacon.
    @Test
    public void elFiltroSoloAceptaAnunciosQueSeanTramaIBeacon() {
        assertFalse(TramaIBeacon.esTramaIBeacon(
                Utilidades.stringToBytes("02010611FF0706554C0050534B32315A0C")));
    }

    // un anuncio iBeacon valido si pasa el filtro.
    @Test
    public void unAnuncioIBeaconSiPasaElFiltro() {
        assertTrue(TramaIBeacon.esTramaIBeacon(
                TramaIBeaconTest.anuncioCompleto(12, 2450, -59)));
    }

    // la UUID configurada en la app es un UUID valido.
    @Test
    public void laUuidDelEmisorEstaConfiguradaYEsValida() {
        assertTrue(esValida(MainActivity.UUID_BUSCADA));
    }

    // el texto ASCII del emisor se traduce a su UUID hexadecimal.
    @Test
    public void elTextoAsciiDelEmisorSeTraduceASuUuidHexadecimal() {
        assertEquals(MainActivity.UUID_BUSCADA,
                MainActivity.uuidComoHexadecimal(MainActivity.UUID_EMISORA_TEXTO));
    }

    // cada byte del texto del emisor es su valor hexadecimal.
    @Test
    public void cadaByteDelTextoAsciiCoincideConSuValorHexadecimal() {
        // el array beaconUUID del emisor es {'E','P','S','G','-','G','T','I',
        // '-','P','R','O','Y','-','3','A'}: los guiones son bytes de verdad
        StringBuilder hex = new StringBuilder();
        for (char c : "EPSG-GTI-PROY-MJ".toCharArray()) {
            hex.append(String.format("%02X", (int) c));
        }
        String esperado = hex.toString().replaceAll(
                "(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");
        assertEquals(esperado, MainActivity.uuidComoHexadecimal("EPSG-GTI-PROY-MJ"));
        assertEquals("45505347-2D47-5449-2D50-524F592D4D4A", esperado);
    }

    // un texto ASCII que no mida 16 bytes se rechaza.
    @Test
    public void unTextoAsciiDeLongitudIncorrectaSeRechaza() {
        try {
            Utilidades.uuidDesdeTextoAscii("EPSG-GTI");
            fail("Se esperaba IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // comportamiento correcto
        }
    }
}
