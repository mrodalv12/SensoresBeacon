const { server } = require('./app');

const PORT = process.env.PORT || 8080;
const HOST = process.env.HOST || '0.0.0.0';

server.listen(PORT, HOST, () => {
    console.log(`Servidor REST escuchando en http://${HOST}:${PORT}`);
});

module.exports = { server };
