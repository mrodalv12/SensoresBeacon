<?php

//==============================================================================================
//Nombre: index.php
//Descripcion: Pagina principal: pide las ultimas mediciones al servidor REST y las
//              muestra en las tarjetas y en la tabla.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

require_once 'includes/mediciones.php';

$mediciones = obtenerUltimasMediciones();
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sensores Beacon - Mediciones</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="container">
        <header class="header">
            <h1>Mediciones de Sensores</h1>
        </header>

        <div id="error-container"></div>

        <div class="cards-container">
            <div class="card">
                <div class="card-label">CO₂</div>
                <div class="card-value">
                    <span id="co2-value">--</span>
                    <span class="card-unit">ppm</span>
                </div>
                <div class="card-subtitle">Último valor registrado</div>
            </div>
            <div class="card">
                <div class="card-label">Temperatura</div>
                <div class="card-value">
                    <span id="temp-value">--</span>
                    <span class="card-unit">°C</span>
                </div>
                <div class="card-subtitle">Último valor registrado</div>
            </div>
        </div>

        <div class="table-container">
            <h2 class="table-title">Historial de Mediciones</h2>
            <table>
                <thead>
                    <tr>
                        <th>Fecha y hora</th>
                        <th>Medición</th>
                        <th>Valor</th>
                    </tr>
                </thead>
                <tbody id="tabla-body">
                    <?php if (empty($mediciones)): ?>
                        <tr>
                            <td colspan="3" class="text-center">No hay mediciones disponibles</td>
                        </tr>
                    <?php else: ?>
                        <?php foreach (array_slice(array_reverse($mediciones), 0, 10) as $m): ?>
                            <tr>
                                <td><?php echo htmlspecialchars($m['fecha_y_hora'] ?? '-'); ?></td>
                                <td><?php echo htmlspecialchars($m['tipo'] ?? '-'); ?></td>
                                <td><?php echo htmlspecialchars($m['valor'] ?? '-'); ?></td>
                            </tr>
                        <?php endforeach; ?>
                    <?php endif; ?>
                </tbody>
            </table>
        </div>
    </div>
    <script src="js/app.js"></script>
</body>
</html>
