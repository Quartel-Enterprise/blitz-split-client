#!/usr/bin/env bash
#
# Fit Gradle to the runner. Run by .github/actions/gradle/action.yml.
#
set -eo pipefail

mkdir -p ~/.gradle
{
  echo 'org.gradle.jvmargs=-Xmx4608M -Dfile.encoding=UTF-8'
  echo 'kotlin.daemon.jvmargs=-Xmx1024M'
  echo 'org.gradle.workers.max=2'
} >> ~/.gradle/gradle.properties
