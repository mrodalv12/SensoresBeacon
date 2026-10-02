<?php
/**
 * Lógica de Negocio Web (PHP)
 * 
 * Diseño según reglas del PDF "Logical_Design_RE_Rules_v3.pdf"
 * Tipos de datos abstractos: N, Z, R, Text, [Tipo]
 */

/**
 * obtenerMedicion() --> [Tipo]
 * 
 * Parámetros de entrada (-->):
 *   - (ninguno)
 * 
 * Salidas (<--):
 *   - Array<{ fecha_y_hora: Text, tipo: Text, valor: R }>
 * 
 * Interacción (--x): Realiza petición HTTP GET asíncrona hacia REST /mediciones (cliente HTTP)
 */
function obtenerMedicion() {
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
    
    return array_slice($data, -10);
}
