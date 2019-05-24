#!/bin/bash
#
# Deploy a jar, source jar, and javadoc jar to Sonatype's snapshot repo.
#
# Adapted from https://coderwall.com/p/9b_lfq and
# http://benlimmer.com/2013/12/26/automatically-publish-javadoc-to-gh-pages-with-travis-ci/

SLUG="Polidea/RxAndroidBle"
JDK="oraclejdk8"
WHITELIST_BRANCHES=("master" "develop")
# TODO [PU] Eventually branch name should be set to master and the RxJava 1.0 version should be moved to "master-rxjava1.X

function contains {
  local n=$#
  local value=${!n}
  for ((i=1;i < $#;i++)) {
    if [ "${!i}" == "${value}" ]; then
      echo "true"
      return 0
    fi
  }
  echo "false"
}

function joinBy {
  local d=$1
  shift
  echo -n "$1"
  shift
  printf "%s" "${@/#/$d}"
}

function getProperty {
  PROP_KEY=$2
  PROP_VALUE=`cat $1 | grep "$PROP_KEY" | cut -d'=' -f2`
  echo $PROP_VALUE
}

function getVersionName {
  echo $(getProperty "gradle.properties" "VERSION_NAME")
}

function hasSnapshotSuffix { # true if parameter '*-SNAPSHOT'
  local VERSION_NAME_END=$(echo $1 | cut -d'-' -f2)
  if [ ${VERSION_NAME_END} == "SNAPSHOT" ]; then
    echo "true"
  else
    echo "false"
  fi
}

set -e
if [ "$TRAVIS_REPO_SLUG" != "$SLUG" ]; then
  echo "Skipping snapshot deployment: wrong repository. Expected '$SLUG' but was '$TRAVIS_REPO_SLUG'."
elif [ "$TRAVIS_JDK_VERSION" != "$JDK" ]; then
  echo "Skipping snapshot deployment: wrong JDK. Expected '$JDK' but was '$TRAVIS_JDK_VERSION'."
elif [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
  echo "Skipping snapshot deployment: was pull request."
elif [ $(contains "${WHITELIST_BRANCHES[@]}" "$TRAVIS_BRANCH") == "false" ]; then
  PRINT_BRANCHES="['$(joinBy "', '" ${WHITELIST_BRANCHES[@]})']" # i.e. ['master', 'develop']
  echo "Skipping snapshot deployment: wrong branch. Expected one of $PRINT_BRANCHES but was '$TRAVIS_BRANCH'."
elif [ $(hasSnapshotSuffix $(getVersionName)) == "false" ]; then
  echo "Skipping snapshot deployment: wrong version name. Expected name ending with '-SNAPSHOT' but was '$(getVersionName)'"
else
  echo "Deploying snapshot..."
  ./gradlew uploadArchives -PSONATYPE_NEXUS_USERNAME=$SONATYPE_NEXUS_USERNAME -PSONATYPE_NEXUS_PASSWORD=$SONATYPE_NEXUS_PASSWORD
  echo "Snapshot deployed!"
fi
