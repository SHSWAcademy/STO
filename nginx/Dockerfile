FROM node:20 AS builder
WORKDIR /app
COPY ./client/web /app
RUN npm install
RUN npm run build

FROM nginx:latest
COPY ./nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html