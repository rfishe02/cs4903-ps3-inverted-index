#!/bin/bash

set -e
javac ./*.java

time java UAQuery "$@"
