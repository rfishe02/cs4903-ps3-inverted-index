#!/bin/bash

set -e
javac ./*.java

time java UAQuery "$@" #expects input raf_directory w1 w2 ... wn
