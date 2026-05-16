ARG MYSQL_BASE_IMAGE=mysql:8.0
FROM ${MYSQL_BASE_IMAGE}

COPY init/ /docker-entrypoint-initdb.d/
