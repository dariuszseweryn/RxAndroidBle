#!/bin/bash
# Recommended pre-commit hook for people releasing RxAndroidBle
# This script updates all README.md files with the current value of VERSION_NAME specified in
# $root/gradle.properties when following conditions are met:
# - this script is run on the release branch
# - the VERSION_NAME has changed
# - the VERSION_NAME is not a "SNAPSHOT" release

# Check if on release branch
current_branch="$(git branch --show-current)"
release_branch='master'
echo current_branch: "$current_branch", release_branch: "$release_branch"
if [[ $current_branch != "$release_branch" ]]; then
  exit 0
fi
echo not equal

# Check if VERSION_NAME changed
this_script_path="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root_project_path="${this_script_path%/*}"
git stash -m "PRE-COMMIT-VERSIONS-CHANGE-CHECK-PREPARE" --keep-index --include-untracked
git stash -m "PRE-COMMIT-VERSIONS-CHANGE-CHECK"
version_name_line=$(grep ^VERSION_NAME= "$root_project_path"/gradle.properties)
version_name_old=${version_name_line##*=}
git stash pop
git add .
git stash pop
version_name_line=$(grep ^VERSION_NAME= "$root_project_path"/gradle.properties)
version_name_new=${version_name_line##*=}
if [[ $version_name_old -eq $version_name_new ]]; then
  exit 0
fi

# Check if VERSION_NAME is not SNAPSHOT
if [[ $version_name_new == *"SNAPSHOT"* ]]; then
  exit 0
fi

# Update version for gradle and maven in all README.md files
git stash -m "PRE-COMMIT-HOOK-STASH" --keep-index
find "$root_project_path" -name README.md \
  -exec sed -i '' \
    -e "s~\(.*implementation .*:\)\(.*\)\(\\\"\)~\1$version_name_new\3~g" \
    -e "s~\(.*<version>\)\(.*\)\(<\/version>\)~\1$version_name_new\3~g" {} ";" \
  -exec git add {} ";"
git stash pop
