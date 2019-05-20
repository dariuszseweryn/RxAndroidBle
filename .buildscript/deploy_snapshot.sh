#!/bin/bash
#
# Deploy a jar, source jar, and javadoc jar to Sonatype's snapshot repo.
#
# Adapted from https://coderwall.com/p/9b_lfq and
# http://benlimmer.com/2013/12/26/automatically-publish-javadoc-to-gh-pages-with-travis-ci/

SLUG="Polidea/RxAndroidBle"
JDK="oraclejdk8"
WHITELIST_BRANCHES=("master")
# TODO [PU] Eventually branch name should be set to master and the RxJava 1.0 version should be moved to "master-rxjava1.X

function contains {
  local n=$#
  local value=${!n}
  for ((i=1;i < $#;i++)) {
    if [ "${!i}" == "${value}" ]; then
      echo "y"
      return 0
    fi
  }
  echo "n"
  return 1
}

function join_by {
  local d=$1
  shift
  echo -n "$1"
  shift
  printf "%s" "${@/#/$d}"
}

set -e
if [ "$TRAVIS_REPO_SLUG" != "$SLUG" ]; then
  echo "Skipping snapshot deployment: wrong repository. Expected '$SLUG' but was '$TRAVIS_REPO_SLUG'."
  exit 0
fi

if [ "$TRAVIS_JDK_VERSION" != "$JDK" ]; then
  echo "Skipping snapshot deployment: wrong JDK. Expected '$JDK' but was '$TRAVIS_JDK_VERSION'."
  exit 0
fi

if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
  echo "Skipping snapshot deployment: was pull request."
  exit 0
fi

if [ $(contains "${WHITELIST_BRANCHES[@]}" "$TRAVIS_BRANCH") == "n" ]; then
  PRINT_BRANCHES="['$(join_by "', '" ${WHITELIST_BRANCHES[@]})']" # i.e. ['master', 'develop']
  echo "Skipping snapshot deployment: wrong branch. Expected one of $PRINT_BRANCHES but was '$TRAVIS_BRANCH'."
  exit 0
fi

echo "Deploying snapshot..."
./gradlew uploadArchives -PSONATYPE_NEXUS_USERNAME=$SONATYPE_NEXUS_USERNAME -PSONATYPE_NEXUS_PASSWORD=$SONATYPE_NEXUS_PASSWORD
echo "Snapshot deployed!"
