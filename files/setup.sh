#!/bin/sh
set -e
exec echo $1; echo $2; tar xf ./build/distributions/$1-$2.tar