ARG NODE_IMAGE=node:20-alpine
ARG NGINX_IMAGE=nginx:alpine

FROM ${NODE_IMAGE} AS build
WORKDIR /app
COPY bids/frontend/package.json bids/frontend/package-lock.json ./
RUN npm ci
COPY bids/frontend .
ARG VITE_API_BASE=
ENV VITE_API_BASE=$VITE_API_BASE
RUN npm run build

FROM ${NGINX_IMAGE}
COPY bids/docker/bids-web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
