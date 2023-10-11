FROM hapiproject/hapi:v6.8.0 AS org

FROM alpine:3 AS builder
COPY --from=org /app/main.war /tmp/main.war
RUN apk add --no-cache zip unzip
RUN unzip /tmp/main.war -d /tmp/main
COPY ./sample-logo.jpg /tmp/main/img/sample-logo.jpg
WORKDIR /tmp/main
RUN zip -0 -r /tmp/main_mod.war ./*

FROM hapiproject/hapi:v6.8.0
COPY --from=builder /tmp/main_mod.war /app/main.war