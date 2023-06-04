#!/bin/bash

# Run with: ./prebuild.sh "4.2.7" "4207"

# Arguments: the FDroid build script variables:
#  * $$VERSION$$
#  * $$VERCODE$$

version=$1
vercode=$2

appName="OsmAndDjBpF"

# Changes marked
#   - BUILD: required for FDroid build
#   - COSMETIC: tidy up app to hide necessary FDroid changes
#   - CUSTOM: bespoke customisations from FDroid user requests

# Fail on any error

set -e

# Always start from the directory this script is in

script_dir="$(dirname -- "$( readlink -f -- "$0"; )")";
script_dir="$(dirname $PWD)";

echo "SCRIPT DIR: $script_dir"

pushd "$script_dir"

android_dir="$script_dir/android"
osmand_dir="$android_dir/OsmAnd"
osmand_java_dir="$android_dir/OsmAnd-java"
stubs_dir="$android_dir/.patch/stubs"

# BUILD: Sub in the right build information (version/appname)
sed -i \
    -e "s/System.getenv(\"APK_VERSION\")/\"$version\"/g" \
    "$osmand_dir/build.gradle"
sed -i \
    -e "s/System.getenv(\"APK_NUMBER_VERSION\")/\"$vercode\"/g" \
    "$osmand_dir/build.gradle"
sed -i \
    -e "s/System.getenv(\"TARGET_APP_NAME\")/\"$appName\"/g" \
    "$osmand_dir/build.gradle"
sed -i \
    -e "s/\"OsmAnd Nightly\"/\"$appName\"/g" \
    "$osmand_dir/build.gradle"

# return from whence we came (just in case)
popd
