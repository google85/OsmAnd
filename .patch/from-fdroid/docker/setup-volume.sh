#!/bin/bash

git clone https://gitlab.com/f-droid-mirrors/OsmAnd-submodules.git
pushd OsmAnd-submodules
git submodule init
git submodule update
popd

echo 'Now run OsmAnd-submodules/prebuild.sh <version name> <version code> "/path/to/skia"'
echo 'And then OsmAnd-submodules/build.sh'
echo 'And then in OsmAnd-submodules/android/OsmAnd run ../gradlew assembleRelease'
