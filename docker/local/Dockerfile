FROM postgres:14

RUN localedef -i nb_NO -c -f UTF-8 -A /usr/share/locale/locale.alias nb_NO.UTF-8
ENV LANG nb_NO.utf8
ENV POSTGRES_HOST_AUTH_METHOD trust

COPY initdb.sql /docker-entrypoint-initdb.d/
RUN chmod 0755 /docker-entrypoint-initdb.d/initdb.sql
EXPOSE 5432
