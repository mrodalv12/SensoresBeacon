<?php
require_once __DIR__ . '/../php/logicaWeb.php';

function test_obtenerMedicion_devuelve_array() {
    $result = obtenerMedicion();
    if (!is_array($result)) {
        throw new Exception('No devuelve array');
    }
    echo "test_obtenerMedicion_devuelve_array: OK\n";
}

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
