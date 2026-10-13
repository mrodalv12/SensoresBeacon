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
    <!-- =====================================================================
         CABECERA: metadata del documento, no se ve en pantalla.
         charset UTF-8 es obligatorio para que se vean bien los acentos y
         los simbolos (Ozono, grados C). viewport hace que la pagina se
         adapte al ancho del movil.
         ===================================================================== -->
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sensores Beacon - Mediciones</title>

    <!-- Hoja de variables: colores, formas, espaciados y tipografia del diseno.
         Va antes que style.css porque este fichero solo consume esas variables.
         Para cambiar el aspecto de la pagina, edita css/variables.css. -->
    <link rel="stylesheet" href="css/variables.css">

    <!-- Reglas de las componentes: barras, tarjetas, tabla y mensajes de error. -->
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <!-- =====================================================================
         BARRA SUPERIOR: franja decorativa de color. Es puramente visual y
         no aporta datos.
         ===================================================================== -->
    <div class="top-bar"></div>

    <!-- =====================================================================
         CONTENEDOR PRINCIPAL: todo lo que hay entre las dos barras.
         ===================================================================== -->
    <main class="container">

        <!-- ---------- Titulo ---------- -->
        <header class="header">
            <h1>Mediciones</h1>
        </header>

        <!-- ---------- Aviso de error ----------
             Vacio por defecto. Lo rellena js/app.js si el servidor REST no
             responde, para que la pagina avise en vez de quedarse en blanco. -->
        <div id="error-container"></div>

        <!-- ---------- Tarjetas: las dos cifras principales ----------
             Muestran la ultima lectura de cada magnitud. El "--" es lo que se
             ve hasta que llegan datos: el guion indica "todavia no hay valor".
             Los id ozono-value y temp-value los usa js/app.js para rellenarlas. -->
        <section class="cards-container">
            <article class="card">
                <div class="card-label">Ozono</div>
                <div class="card-value">
                    <span id="ozono-value">--</span>
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

        <!-- ---------- Historial: tabla con las ultimas 10 mediciones ---------- -->
        <section class="table-section">
            <h2 class="table-title">Historial</h2>
            <div class="table-container">
                <table>
                    <!-- Cabecera fija: define las tres columnas. -->
                    <thead>
                        <tr>
                            <th>Fecha y hora</th>
                            <th>Medición</th>
                            <th>Valor</th>
                        </tr>
                    </thead>

                    <!--
                         Cuerpo de la tabla. Hay dos fuentes de filas:

                         1) El bloque PHP de abajo pinta las ultimas 10 al
                            cargar la pagina, para que haya contenido visible
                            desde el primer momento, sin esperar a JavaScript.
                            Los valores se escapan con htmlspecialchars() para
                            que un dato con caracteres especiales no rompa el
                            HTML.

                         2) js/app.js vacia este tbody y lo vuelve a rellenar
                            cada 5 minutos, que es lo que mantiene la tabla
                            al dia sin recargar. Ese intervalo se cambia en
                            la constante INTERVALO_REFRESCO_MS de js/app.js.
                    -->
                    <tbody id="tabla-body">
                        <?php if (empty($mediciones)): ?>
                            <!-- Si el REST no devuelve nada, se avisa en vez
                                 de mostrar una tabla vacia. -->
                            <tr>
                                <td colspan="3" class="text-center">No hay mediciones disponibles</td>
                            </tr>
                        <?php else: ?>
                            <?php
                            // array_reverse() para poner la mas reciente arriba:
                            // el REST devuelve las lecturas en orden ascendente.
                            // array_slice(..., 0, 10) limita el historial a 10 filas.
                            foreach (array_slice(array_reverse($mediciones), 0, 10) as $m): ?>
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

    <!-- =====================================================================
         BARRA INFERIOR: franja decorativa, pareja de la superior.
         ===================================================================== -->
    <div class="bottom-bar"></div>

    <!-- Aviso de la frecuencia de refresco. Lo rellena js/app.js con el valor
         de su constante INTERVALO_REFRESCO_MS, para que el usuario sepa cada
         cuanto se actualizan las cifras sin tener que mirar el codigo. -->
    <p id="proxima-actualizacion" class="refresh-note"></p>

    <!-- =====================================================================
         SCRIPT: va al final del body a proposito. Asi el HTML ya esta
         completo cuando se ejecuta, y la GUI puede buscar los elementos
         del DOM (ozono-value, temp-value, tabla-body) sin esperar a ningun
         evento.

         Primero se carga el proxy de logica de negocio de cliente
         (frontend_business_logic/js/mediciones.js): encapsula el transporte
         HTTP, asi la GUI no hace fetch nunca. Despues app.js, que solo habla
         con ese proxy.
         ===================================================================== -->
    <script src="/frontend_business_logic/js/mediciones.js"></script>
    <script src="js/app.js"></script>
</body>
</html>
