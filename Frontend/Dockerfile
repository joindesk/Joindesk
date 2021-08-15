FROM nginx
EXPOSE 443
COPY /dist /usr/share/nginx/jdapp/html
COPY nginx.conf /etc/nginx/nginx.conf
COPY jd.cert /etc/nginx/ssl.cert
COPY jd.key /etc/nginx/ssl.key