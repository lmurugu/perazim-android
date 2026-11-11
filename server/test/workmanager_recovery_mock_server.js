const http = require('http');

const receivedEntityIds = [];
const server = http.createServer((request, response) => {
  if (request.method === 'GET' && request.url === '/__received') {
    response.writeHead(200, { 'Content-Type': 'application/json' });
    response.end(JSON.stringify({ entityIds: receivedEntityIds }));
    return;
  }
  if (request.method !== 'POST' || request.url !== '/api/sync') {
    response.writeHead(404).end();
    return;
  }

  let body = '';
  request.setEncoding('utf8');
  request.on('data', chunk => { body += chunk; });
  request.on('end', () => {
    try {
      const parsed = JSON.parse(body || '{}');
      const mutations = Array.isArray(parsed.mutations) ? parsed.mutations : [];
      for (const mutation of mutations) receivedEntityIds.push(mutation.entityId);
      response.writeHead(200, { 'Content-Type': 'application/json' });
      response.end(JSON.stringify({
        syncedIds: mutations.map(mutation => mutation.id),
        rejectedMutations: [],
        serverTime: Date.now(),
        hasMore: false,
        nextCursor: null,
        prayers: [],
        updatedPrayers: []
      }));
    } catch (_) {
      response.writeHead(400).end();
    }
  });
});

server.listen(18080, '0.0.0.0');
