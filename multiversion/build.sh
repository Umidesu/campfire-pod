#!/usr/bin/env bash
# usage: multiversion/build.sh <target>   (run gen.py first)
# Env: JAVA_HOME (JDK that runs Gradle; must be 25 for Loom 1.19), GRADLE (gradle executable, default: gradle on PATH)
set -u
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GRADLE="${GRADLE:-gradle}"
cd "$HERE/build/$1" || { echo "no generated project for $1; run: python3 $HERE/gen.py $1"; exit 1; }
"$GRADLE" build -q --console=plain > build.log 2>&1; rc=$?
echo "$1 exit $rc"; grep -E 'error:|FAIL|What went wrong' -A3 build.log | head -40
exit $rc
