#!/bin/bash

set -e # Fail on error
set -u # Fail on uninitialized

install_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.."; pwd) # Absolute path
cd "${install_root}"

simple_class_name=$1
shift
java \
    -Dorg.slf4j.simpleLogger.showDateTime=true \
    -Dorg.slf4j.simpleLogger.dateTimeFormat="yy-MM-dd'T'HH:mm.z" \
    -cp lib/${project.build.finalName}.jar \
    "net.florianschoppmann.issuetracking.${simple_class_name}" "$@"
