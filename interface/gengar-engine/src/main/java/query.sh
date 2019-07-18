#!/bin/bash

set -e
javac ./*.java

time java UAQueryTest "$@" #expects input raf_directory w1 w2 ... wn
