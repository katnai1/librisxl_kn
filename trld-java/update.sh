#!/bin/bash
set -ex
pushd /tmp
[[ -d trld ]] || git clone https://github.com/niklasl/trld.git
cd trld && git pull && make clean java TRFLAGS=-L
popd
rsync -a --del /tmp/trld/build/java/src/ ./src
