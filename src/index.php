<?php

//==============================================================================================
//Nombre: index.php
//Descripcion: Redireccion de conveniencia. La raiz del servidor PHP es src/ (se arranca con
//              "-t src"), asi que al abrir http://localhost:8000/ no hay pagina y daba 404.
//              Este fichero reenvia a la GUI real, que vive en src/gui/.
//Copyright:
//Fecha de creacion: 2026-10-11
//Fecha de modificacion: 2026-10-13
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: redireccion a la GUI real
//==============================================================================================

header('Location: /gui/index.php');
exit;
