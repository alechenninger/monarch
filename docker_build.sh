#!/usr/bin/env bash
if [[ -z $1 ]]; then
  echo "Please provide a version."
  exit 2
fi

tags="--tag monarch:$1"

if [[ $2 = "--latest" ]]; then
  tags="$tags --tag monarch:latest"
fi

docker build --build-arg version=$1 $tags .
