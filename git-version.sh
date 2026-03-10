#!/bin/bash

while getopts "t:" opt; do
  case $opt in
    t)
      TYPE="$OPTARG"
      ;;
    *)
      echo "Usage: $0 -f <file> -t <major|minor|patch>"
      exit 1
      ;;
  esac
done

if [ -z "$TYPE" ]; then
  echo "Usage: $0 -f <file> -t <major|minor|patch>"
  exit 1
fi

VERSION=$(cat "version.txt")

IFS='.' read -r MAJOR MINOR PATCH <<< "$VERSION"

case $TYPE in
  major)
    ((MAJOR++))
    MINOR=0
    PATCH=0
    ;;
  minor)
    ((MINOR++))
    PATCH=0
    ;;
  patch)
    ((PATCH++))
    ;;
  *)
    echo "Invalid type: $TYPE (use major, minor, patch)"
    exit 1
    ;;
esac

NEW_VERSION="$MAJOR.$MINOR.$PATCH"
echo "$NEW_VERSION" >| "version.txt"

git add "version.txt"
git diff --cached --quiet && echo "No changes to commit" && exit 1
git commit -m "Update version in version.txt to $NEW_VERSION"
git tag "v$NEW_VERSION"
git push origin HEAD --follow-tags