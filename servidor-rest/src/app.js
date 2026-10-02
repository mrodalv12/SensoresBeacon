const http = require('http');
const { routerMediciones } = require('./routes/medicionesRoutes');

/**
 * Aplicación HTTP - Servidor REST
 * Solo se encarga de enrutar peticiones. No contiene lógica de negocio.
 */
const server = http.createServer((req, res) => {
    // Intentar enrutar a mediciones
    if (routerMediciones(req, res)) {
        return;
    }

    // Ruta no encontrada
    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Ruta no encontrada' }));
});

module.exports = { server };
