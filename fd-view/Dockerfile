FROM nginx
# replace this with your application's default port
EXPOSE 80

MAINTAINER Mike Holdsworth "https://github.com/monowai"

# bower install
# grunt build
# docker build -t flockdata/fd-view .
# docker run -p 80:80 flockdata/fd-view


WORKDIR /usr/share/nginx/html
# Add files.
COPY app /usr/share/nginx/html



