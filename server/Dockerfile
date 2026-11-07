FROM node:24-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev
COPY schema.sql ./
COPY migrations ./migrations
COPY scripts/db-migrate.js ./scripts/db-migrate.js
COPY src ./src
RUN mkdir -p /app/uploads && chown node:node /app/uploads
USER node
EXPOSE 3000
CMD ["npm", "start"]
