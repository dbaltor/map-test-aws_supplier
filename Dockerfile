FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG TAR_FILE
ADD ${TAR_FILE} /
COPY *.csv /
COPY start.sh /usr/local/bin/
RUN ln -s usr/local/bin/start.sh / # backwards compat
RUN adduser -D dbaltor  # using Alpine
#RUN useradd -m myuser  # using Ubuntu
USER dbaltor
ENTRYPOINT ["start.sh"]