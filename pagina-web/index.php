<?php

//==============================================================================================
//Nombre: index.php
//Descripcion: Pagina principal: pide las ultimas mediciones al servidor REST y las
//              muestra en las tarjetas y en la tabla.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-05
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: estructura adaptada al diseño (barras, titulo, historial)
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
    <div class="top-bar"></div>

    <main class="container">
        <header class="header">
            <h1>Mediciones</h1>
        </header>

        <div id="error-container"></div>

        <section class="cards-container">
            <article class="card">
                <div class="card-label">CO₂</div>
                <div class="card-value">
                    <span id="co2-value">--</span>
                    <span class="card-unit">ppm</span>
                </div>
            </article>
            <article class="card">
                <div class="card-label">Temperatura</div>
                <div class="card-value">
                    <span id="temp-value">--</span>
                    <span class="card-unit">°C</span>
                </div>
            </article>
        </section>

        <section class="table-section">
            <h2 class="table-title">Historial</h2>
            <div class="table-container">
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
        </section>
    </main>

    <div class="bottom-bar"></div>

    <script src="js/app.js"></script>
</body>
</html>
