//==============================================================================================
//Nombre: server.js
//Descripcion: Arranque del servidor REST: escucha en el puerto y el host fijados por entorno.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

const { server } = require('./app');

const PORT = process.env.PORT || 8080;
const HOST = process.env.HOST || '0.0.0.0';

server.listen(PORT, HOST, () => {
    console.log(`Servidor REST escuchando en http://${HOST}:${PORT}`);
});

module.exports = { server };
