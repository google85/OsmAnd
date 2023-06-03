#!/bin/bash

# Exit as soon as something fails

set -e

# Always start from the directory this script is in

script_dir="$(dirname -- "$( readlink -f -- "$0"; )")";
pushd "$script_dir"

osmand_dir="$script_dir/android/OsmAnd"
osmand_java_dir="$script_dir/android/OsmAnd-java"

# Build MP Android Chart

pushd MPAndroidChart
gradle assembleRelease
cp MPChartLib/build/outputs/aar/MPChartLib-release.aar "$osmand_dir/libs/"
popd

# Build OsmAnd core and copy into libs folder

pushd core/wrappers/android/
# build, assemble, assemble so that native libs are included
gradle build
gradle assembleRelease
gradle assembleRelease
cp build/outputs/aar/OsmAndCore_android-release.aar "$osmand_dir/libs/"
cp NativeCoreRelease/build/outputs/aar/OsmAndCore_androidNativeRelease-release.aar "$osmand_dir/libs/"
popd
cp core/externals/qtbase-android/upstream.patched.android.clang-x86.shared/jar/QtAndroid.jar "$osmand_dir/libs/"
cp core/externals/qtbase-android/upstream.patched.android.clang-x86.shared/jar/QtAndroidBearer.jar "$osmand_dir/libs/"

# Build OsmAnd patched version of ICU. Remove a bunch of unused data
# files to keep file size down. Copy into OsmAnd lib dirs.

pushd "icu-release-50-2-1-patched-mirror/icu4j"
ant jar
zip -d icu4j.jar "com/ibm/icu/impl/data/icudt50b/brkitr/*"
zip -d icu4j.jar "com/ibm/icu/impl/data/icudt50b/coll/*"
zip -d icu4j.jar "com/ibm/icu/impl/data/icudt50b/curr/*"
zip -d icu4j.jar "com/ibm/icu/impl/data/icudt50b/lang/*"
zip -d icu4j.jar "com/ibm/icu/impl/data/icudt50b/rbnf/*"
zip -d icu4j.jar "com/ibm/icu/impl/data/icudt50b/region/*"
zip -d icu4j.jar "com/ibm/icu/impl/data/icudt50b/translit/*"
zip -d icu4j.jar "com/ibm/icu/impl/data/icudt50b/zone/*"

cp icu4j.jar "$osmand_dir/libs/"
cp icu4j.jar "$osmand_java_dir/libs/"
popd

# return from whence we came (just in case)
popd
