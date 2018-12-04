# Copyright 2018 Dematic, Corp.
# Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
FROM us.gcr.io/dlabs-dev-primary/jdk_8:${docker_base.version}

# 6300: jacoco
EXPOSE 6300

RUN mkdir /opt/dlabs/store
VOLUME /opt/dlabs/store

COPY --chown=dlabs docker-entrypoint.sh /opt/dlabs/bin/
COPY --chown=dlabs maven/*.jar /opt/dlabs/lib/

ENTRYPOINT ["/opt/dlabs/bin/docker-entrypoint.sh"]
