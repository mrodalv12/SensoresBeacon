<?php
header('Content-Type: application/json');
require_once __DIR__ . '/../php/logicaWeb.php';

try {
    $mediciones = obtenerMedicion();
    echo json_encode($mediciones);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([]);
}
