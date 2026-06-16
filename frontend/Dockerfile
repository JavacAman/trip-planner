# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci --silent

COPY . .
RUN npm run build:prod

# ── Stage 2: Serve via Nginx ─────────────────────────────────────────────────
FROM nginx:alpine

COPY --from=builder /app/dist/trip-planner/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=10s CMD wget -qO- http://localhost/index.html || exit 1
