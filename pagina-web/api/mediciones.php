<?php

//==============================================================================================
//Nombre: mediciones.php
//Descripcion: Endpoint JSON: devuelve en JSON las ultimas mediciones obtenidas del
//              servidor REST.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

header('Content-Type: application/json');
require_once __DIR__ . '/../php/logicaWeb.php';

try {
    $mediciones = obtenerMedicion();
    echo json_encode($mediciones);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([]);
}
