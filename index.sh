#!/bin/bash

set -e
javac ./*.java

OUTFILES=(temp tmp output)

for f in "${OUTFILES[@]}"
do
  if [ -d ./$f ]
  then
    rm -rf ./$f/*
  else
    mkdir ./$f
  fi
done

time java UAInvertedIndex $1 $2
