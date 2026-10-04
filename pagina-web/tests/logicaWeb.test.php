<?php

//==============================================================================================
//Nombre: logicaWeb.test.php
//Descripcion: Pruebas de la logica web: comprueba que obtenerMedicion() devuelve un array
//              de 10 elementos como maximo.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

require_once __DIR__ . '/../php/logicaWeb.php';

// test_..._devuelve_array(): comprueba que obtenerMedicion() devuelve un array.
function test_obtenerMedicion_devuelve_array() {
    $result = obtenerMedicion();
    if (!is_array($result)) {
        throw new Exception('No devuelve array');
    }
    echo "test_obtenerMedicion_devuelve_array: OK\n";
}

// test_..._maximo_10(): comprueba que no se devuelven mas de 10 mediciones.
function test_obtenerMedicion_maximo_10() {
    $result = obtenerMedicion();
    if (count($result) > 10) {
        throw new Exception('Devuelve más de 10');
    }
    echo "test_obtenerMedicion_maximo_10: OK\n";
}

test_obtenerMedicion_devuelve_array();
test_obtenerMedicion_maximo_10();
echo "Todos los tests pasaron\n";
