<?php

//==============================================================================================
//Nombre: mediciones.php
//Descripcion: Include de compatibilidad: expone obtenerUltimasMediciones() a las
//              vistas de la pagina.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-04
//Autores: Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

require_once __DIR__ . '/../../frontend_business_logic/logicaWeb.php';

/**
 * obtenerUltimasMediciones(): devuelve las ultimas mediciones, llamar a
 * obtenerMedicion() y asi las vistas no dependen de donde vienen.
 */
function obtenerUltimasMediciones() {
    return obtenerMedicion();
}
