#!/usr/bin/env bash

javac Lox.java && java Lox $@
status=$?
rm -f *.class
exit ${status}
