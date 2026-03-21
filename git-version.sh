#!/bin/bash

while getopts "v:" opt; do
  case $opt in
    v)
      NEW_VERSION="$OPTARG"
      ;;
    *)
      echo "Usage: $0 -v major.minor.patch (0.0.0)"
      exit 1
      ;;
  esac
done

if [ -z "$NEW_VERSION" ]; then
  echo "Usage: $0 -t -v major.minor.patch (0.0.0)"
  exit 1
fi

git tag "$NEW_VERSION"
git push origin HEAD --follow-tags