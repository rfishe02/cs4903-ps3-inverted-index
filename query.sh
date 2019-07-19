#!/bin/bash

set -e
javac -cp ./gengar-engine ./gengar-engine/src/main/java/UAQuery*.java

time java -cp ./gengar-engine src/main/java/UAQueryTest "$@" #expects input raf_directory w1 w2 ... wn
