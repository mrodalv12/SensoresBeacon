<?php

//==============================================================================================
//Nombre: index.php
//Descripcion: Redireccion de conveniencia. La raiz del servidor PHP es src/ (se arranca con
//              "-t src"), asi que al abrir http://localhost:8000/ no hay pagina y daba 404.
//              Este fichero reenvia a la GUI real, que vive en src/gui/.
//Fecha de creacion: 2026-10-11
//==============================================================================================

header('Location: /gui/index.php');
exit;
