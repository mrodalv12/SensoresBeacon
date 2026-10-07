<?php

//==============================================================================================
//Nombre: logicaWeb.php
//Descripcion: Logica de negocio web en PHP: pide las mediciones al servidor REST por
//              HTTP y devuelve las ultimas 10.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Lógica de Negocio Web (PHP)
 * 
 * Diseño según reglas del PDF "Logical_Design_RE_Rules_v3.pdf"
 * Tipos de datos abstractos: N, Z, R, Text, [Tipo]
 */

/**
 * obtenerMedicion() --> [fecha_y_hora: Text, tipo: Text, valor: R]>
 * 
 * Interacción (--x): Realiza petición HTTP GET asíncrona hacia REST /mediciones (cliente HTTP)
 *
 * Pide las mediciones al servidor REST por HTTP, se queda solo con los tipos
 * que la pagina debe mostrar (co2 y temperatura) y devuelve las ultimas 10;
 * si el servidor no responde o devuelve un error, devuelve una lista vacia.
 */
function obtenerMedicion() {
    // Direccion del servidor REST. localhost vale en cualquier computadora,
    // porque el servidor corre en la misma maquina que esta pagina. Si el REST
    // escuchara en otro sitio, se cambia el host (y el puerto) aqui.
    $url = 'http://localhost:8080/mediciones';

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPGET, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 5);
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 5);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);
    
    if ($error || $httpCode < 200 || $httpCode >= 400) {
        return [];
    }
    
    $data = json_decode($response, true);
    if (!is_array($data)) {
        return [];
    }

    // La pagina solo muestra mediciones de co2 y temperatura; el servidor REST
    // acepta cualquier tipo (ruido, distancia, etc.), asi que se filtran aqui.
    $tiposPermitidos = ['co2', 'temperatura'];
    $data = array_values(array_filter($data, function ($m) use ($tiposPermitidos) {
        $tipo = isset($m['tipo']) ? strtolower(trim((string)$m['tipo'])) : '';
        return in_array($tipo, $tiposPermitidos, true);
    }));

    return array_slice($data, -10);
}
