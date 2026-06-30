FROM nginx:alpine
COPY build/ /usr/share/nginx/html/
COPY monitor/ /usr/share/nginx/html/monitor/
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
