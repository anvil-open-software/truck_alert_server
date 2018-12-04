FROM us.gcr.io/dlabs-dev-primary/jdk_8:${docker_base.version}

# 6300: jacoco
EXPOSE 6300 8080

RUN mkdir /opt/dlabs/store
VOLUME /opt/dlabs/store

COPY --chown=dlabs docker-entrypoint.sh healthcheck.sh /opt/dlabs/bin/
COPY --chown=dlabs maven/*.jar /opt/dlabs/lib/

HEALTHCHECK CMD /opt/dlabs/bin/healthcheck.sh

ENTRYPOINT ["/opt/dlabs/bin/docker-entrypoint.sh"]
