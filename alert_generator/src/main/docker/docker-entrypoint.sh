#!/usr/bin/env bash
# Copyright 2018 Dematic, Corp.
# Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
set -e
. /opt/dlabs/entrypoint_functions/*

while [ $# -gt 0 ]; do
    case $1 in
      "")
        # maven can set empty parameters...
        ;;
      jpda)
        JAVA_OPTS+=" $jvm_jpda_config"
        ;;
      jacoco)
        JAVA_OPTS+=" $jvm_jacoco_config"
        ;;
      --)
        shift
        break
        ;;
      *)
        break
    esac
    shift
done

JAVA_OPTS+=" -Dlogback.configurationFile=/opt/dlabs/conf/logback.xml"
JAVA_OPTS+=" -Dconfig.file=/opt/dlabs/conf/application.conf"

exec $JAVA_HOME/bin/java $JAVA_OPTS -jar /opt/dlabs/lib/server.jar
