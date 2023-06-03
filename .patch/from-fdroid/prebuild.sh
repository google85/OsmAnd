#!/bin/bash

# Arguments: the FDroid build script variables:
#  * $$VERSION$$
#  * $$VERCODE$$

version=$1
vercode=$2

# Changes marked
#   - BUILD: required for FDroid build
#   - COSMETIC: tidy up app to hide necessary FDroid changes
#   - CUSTOM: bespoke customisations from FDroid user requests

# Fail on any error

set -e

# Always start from the directory this script is in

script_dir="$(dirname -- "$( readlink -f -- "$0"; )")";
pushd "$script_dir"

android_dir="$script_dir/android"
osmand_dir="$android_dir/OsmAnd"
osmand_java_dir="$android_dir/OsmAnd-java"
core_legacy_dir="$script_dir/core-legacy"
core_dir="$script_dir/core"
stubs_dir="$script_dir/stubs"
mpchartlib_dir="$script_dir/MPAndroidChart"

# BUILD: Add enough memory for the build on FDroid
echo -e "\norg.gradle.jvmargs=-XX:MaxHeapSize=4096m" \
    >> "$android_dir/gradle.properties"

# BUILD: Remove OsmAnd self-hosted ivy binary repository.
sed -i \
    -e "/ivy {/,+6d" \
    "$android_dir/build.gradle"

# BUILD: Remove maven publishing as it trips up the build now.
sed -i \
    -e "/publishing {/,+18d" \
    "$osmand_java_dir/build.gradle"

# BUILD: Sub in the right build information (version/appname)
sed -i \
    -e "s/System.getenv(\"APK_VERSION\")/\"$version\"/g" \
    "$osmand_dir/build.gradle"
sed -i \
    -e "s/System.getenv(\"APK_NUMBER_VERSION\")/\"$vercode\"/g" \
    "$osmand_dir/build.gradle"
sed -i \
    -e "s/System.getenv(\"TARGET_APP_NAME\")/\"OsmAnd~\"/g" \
    "$osmand_dir/build.gradle"

# BUILD: Remove upstream non-free code including self-hosted pre-built
# binaries. In particular, the OsmAnd core renderer and company code for
# e.g. billing.

sed -i \
    -e "/.*mplementation.*OsmAndCore.*/d" \
    -e "/play-services-location/d" \
    -e '/MPAndroidChart/d' \
    "$osmand_dir/build-common.gradle"
sed -i \
    -e "/.*mplementation.*OsmAndCore.*/d" \
    -e "/play-services-location/d" \
    -e '/MPAndroidChart/d' \
    "$osmand_dir/build-library.gradle"

sed -i \
    -e "/.*com.google.android.play.*/d" \
    "$osmand_dir/build-common.gradle"

perl -i -0 -p \
    -e "s|maven \{\n\s*url 'https://developer.huawei.com/repo/'\n\s*}||g" \
    "$android_dir/build.gradle"
sed -i \
    -e "/huaweiImplementation/d" \
    "$osmand_dir/build.gradle"

sed -i \
    -e "s/, ':OsmAnd-telegram'//" \
    "$android_dir/settings.gradle"

sed -i \
    -e "/.*com.amazon.in-app-purchasing.*/d" \
    "$osmand_dir/build.gradle"
sed -i \
    -e "/.*com.android.billingclient.*/d" \
    "$osmand_dir/build-common.gradle"

sed -i \
    -e "/.*antpluginlib.*/d" \
    "$osmand_dir/build-common.gradle"
rm -r "$osmand_dir/src/net/osmand/plus/plugins/antplus"
rm "$osmand_dir/res/xml/antplus_settings.xml"
sed -i \
    -e "/.*com.dsi.ant.plugins.antplus.*/d" \
    "$osmand_dir/AndroidManifest.xml"
sed -i \
    -e "/.*AntPlus.*/d" \
    "$osmand_dir/src/net/osmand/plus/plugins/PluginsHelper.java"
sed -i \
    -e "/.*AntPlus.*/d" \
    "$osmand_dir/src/net/osmand/plus/settings/fragments/SettingsScreenType.java"
sed -i \
    -e "/.*AntPlusPlugin.*/d" \
    "$osmand_dir/src/net/osmand/plus/views/mapwidgets/WidgetGroup.java"
sed -i \
    -e "s/WEATHER || this == ANT_PLUS/WEATHER/" \
    "$osmand_dir/src/net/osmand/plus/views/mapwidgets/WidgetGroup.java"
sed -i \
    -e "/.*ANT_PLUS.*/d" \
    "$osmand_dir/src/net/osmand/plus/views/mapwidgets/WidgetGroup.java"
sed -i \
    -e "/.*AntPlus.*/d" \
    "$osmand_dir/src/net/osmand/plus/views/mapwidgets/WidgetGroup.java"
sed -i \
    -e "/.*ANT_PLUS.*/d" \
    "$osmand_dir/src/net/osmand/plus/views/mapwidgets/WidgetType.java"

# BUILD: Switch OsmAndCore_android to the OpenGL core built in build.sh

sed -i \
    -e "/opengldebugImplementation.*OsmAndCore.*/d" \
    -e "s!openglImplementation.*OsmAndCore_androidNativeRelease.*!openglImplementation files('libs/OsmAndCore_androidNativeRelease-release.aar')!" \
    -e "s!openglImplementation.*OsmAndCore_android:.*!openglImplementation files('libs/OsmAndCore_android-release.aar')!" \
    "$osmand_dir/build.gradle"

# BUILD: MPChartLib needs a bit a hack because of a gradle version issue
sed -i \
    -e "s/android {/android { lintOptions { checkReleaseBuilds false }/" \
    "$mpchartlib_dir/MPChartLib/build.gradle"
rm -r "$mpchartlib_dir/MPChartExample"

# BUILD: Use legacy packaging else installation will fail with native
# libs error (-2)

sed -i \
    -e "s/sourceSets {/packagingOptions { jniLibs.useLegacyPackaging = true }\n\tsourceSets {/" \
    "$osmand_dir/build.gradle"

# BUILD: Remove some prebuilt jar libraries and replace with similar
# maven deps
#
# Replacements where versions did not match:
#   gnu-trove-osmand.jar replaced with net.sf.trove4j:trove4j:3.0.3
#
# icu4j-49_1_patched.jar was replaced with the icu50-2-1 srclib, a
# mirror of the nearest icu version available, plus the patch applied
# (http://bugs.icu-project.org/trac/ticket/12021). The build process
# compiles this and removes a bunch of unwanted data files.
#
# classes.jar is the FDroid-built version of the legacy core copied in
# by the build.sh script. Similarly for MPChartLib.
#
# $osmand_dir/build.gradle includes deps on QtAndroid.jar and
# QtAndroidBearer.jar from $osmand_dir/libs. These are deleted by FDroid
# and replaced by the ones built with the core in build.sh.

sed -i \
    -e "s/implementation fileTree.*/\
    implementation fileTree(include: ['icu4j.jar'], dir: 'libs')\\n \
    implementation group: 'net.sf.trove4j', name: 'trove4j', version: '3.0.3'\\n/" \
    "$osmand_java_dir/build.gradle"

sed -i \
    -e "s/implementation fileTree.*/\
    implementation fileTree(\
        include: ['icu4j.jar','MPChartLib-release.aar'], \
        dir: 'libs')\\n \
    implementation group: 'net.sf.trove4j', name: 'trove4j', version: '3.0.3'\\n/" \
    "$osmand_dir/build-common.gradle"

# BUILD (perhaps not essential): For code externally downloaded by the
# OsmAnd build, run a checksum test in cases it's not what we expect.
# First core-legacy, then core.

# checksum protobuf.

sed -i \
    "s/# Extract/\
    sha256sum \$SRCLOC\/upstream.tar.bz2\\\
    | grep 13bfc5ae543cf3aa180ac2485c0bc89495e3ae711fc6fab4f8ffe90dfb4bb677\\\
    || { echo 'Failed checksum' 1>\&2; exit; }/"\
    "$core_legacy_dir/externals/protobuf/configure.sh"

# BUILD (perhaps not essential): core:
#   - checksum of tar/zip files for
#       - boost
#       - expat
#       - geographiclib
#       - giflib
#       - glew
#       - icu4c
#       - libpng
#       - protobuf
#       - sqlite
#       - zlib
#   - gits of
#       - freetype
#       - gdal
#       - glm
#       - jpeg
#       - libarchive
#       - proj
#       - qtbase-android
#       - skia (not using srclib because of build error)
#   - remove
#       - qtbase-desktop
#       - qtbase-ios

function addCheckSum() {
    local checksum="$1"
    local file="$2"
    sed -i \
        "s:patchUpstream.*:\
        echo $checksum \"\$SRCLOC/upstream.pack\" | sha256sum --check -\\\
        || { echo 'Failed checksum' 1>\&2; exit; }\n\0:"\
        "$file"
}

addCheckSum \
    8f32d4617390d1c2d16f26a27ab60d97807b35440d45891fa340fc2648b04406 \
    "$core_dir/externals/boost/configure.sh"

addCheckSum \
    6b902ab103843592be5e99504f846ec109c1abb692e85347587f237a4ffa1033 \
    "$core_dir/externals/expat/configure.sh"

addCheckSum \
    3a0606fd99fb099572ba1923f556b05b545965359edb92930a658fc99172d962 \
    "$core_dir/externals/geographiclib/configure.sh"

addCheckSum \
    0ac8d56726f77c8bc9648c93bbb4d6185d32b15ba7bdb702415990f96f3cb766 \
    "$core_dir/externals/giflib/configure.sh"

addCheckSum \
    af58103f4824b443e7fa4ed3af593b8edac6f3a7be3b30911edbc7344f48e4bf \
    "$core_dir/externals/glew/configure.sh"

addCheckSum \
    6d6fb937e671dd80490e19b8b70ea7c3c2a22de0e24793fc563bc87fe87a8eb1 \
    "$core_dir/externals/icu4c/configure.sh"

addCheckSum \
    42f754df633e4e700544e5913cbe2fd4928bbfccdc07708a5cf84e59827fbe60 \
    "$core_dir/externals/libpng/configure.sh"

addCheckSum \
    13bfc5ae543cf3aa180ac2485c0bc89495e3ae711fc6fab4f8ffe90dfb4bb677 \
    "$core_dir/externals/protobuf/configure.sh"

addCheckSum \
    a443aaf5cf345613492efa679ef1c9cc31ba109dcdf37ee377f61ab500d042fe \
    "$core_dir/externals/sqlite/configure.sh"

addCheckSum \
    c3e5e9fdd5004dcb542feda5ee4f0ff0744628baf8ed2dd5d66f8ca1197cb1a1 \
    "$core_dir/externals/zlib/configure.sh"

rm -rf "$core_dir/externals/qtbase-desktop"
rm -rf "$core_dir/externals/qtbase-ios"

# BUILD: Remove billing code and options from menus, using stubs where
# needed.

cp "$stubs_dir/RateUsHelper.java" \
    "$osmand_dir/src/net/osmand/plus/helpers/RateUsHelper.java"
cp "$stubs_dir/InAppPurchaseHelperImpl.java" \
    "$osmand_dir/src-google/net/osmand/plus/inapp/InAppPurchaseHelperImpl.java"

rm "$osmand_dir/src-google/net/osmand/plus/inapp/util/BillingManager.java"
rm "$osmand_dir/src-google/net/osmand/plus/inapp/InAppPurchasesImpl.java"

sed -i -e "/.*Preference purchasesSettings.*/,+1d" \
    "$osmand_dir/src/net/osmand/plus/settings/fragments/MainSettingsFragment.java"
sed -i -e "/addRestorePurchasesRow();/d" \
    "$osmand_dir/src/net/osmand/plus/download/ui/DownloadResourceGroupFragment.java"
sed -i -e "s/return purchases.getSubscriptions();/\
    return new InAppSubscriptionList(new InAppSubscription[] { }) { };/" \
    "$osmand_dir/src/net/osmand/plus/inapp/InAppPurchaseHelper.java"
sed -i -e "s/return purchases\..*;/return null;/" \
    "$osmand_dir/src/net/osmand/plus/inapp/InAppPurchaseHelper.java"

# COSMETIC: hide purchase settings
perl -i -0 -p \
    -e 's|<Preference\n.*android:key="purchases_settings"(.*\n){9}||g' \
    "$osmand_dir/res/xml/settings_main_screen.xml"

# BUILD: Remove location services that needs Google stuff

rm "$osmand_dir/src/net/osmand/plus/helpers/GmsLocationServiceHelper.java"
sed -i \
    -e "s/GmsLocationServiceHelper/AndroidApiLocationServiceHelper/g" \
    "$osmand_dir/src/net/osmand/plus/OsmandApplication.java"

# COSMETIC: hide location services option (no longer works)

sed -i \
    -e 's/android:key="location_source"/\
    android:key="location_source" app:isPreferenceVisible="false"/' \
    "$osmand_dir/res/xml/global_settings.xml"

# CUSTOM: Enable file manager permission and remove warning about not
# being able to access files. See #2691.

sed -i \
    -e '/addItem(sharedStorageItem)/d' \
    "$osmand_dir/src/net/osmand/plus/settings/datastorage/DataStorageHelper.java"
sed -i \
    -e 's!<uses-permission android:name="android.permission.INTERNET" />!\
    <uses-permission android:name="android.permission.INTERNET" />\
    <uses-permission \
        android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />!' \
    "$osmand_dir/AndroidManifest.xml"

# CUSTOM: Remove Mapilliary promotion. See !11525, !11480, and #2701.

sed -i \
    '/MapillaryPlugin/d' \
    "$osmand_dir/src/net/osmand/plus/mapcontextmenu/builders/cards/NoImagesCard.java"

# BUILD (non-essential): remove signing configs (done by FDroid anyway, but
# needed for standalone build to succeed).

# first remove signing config opt lines in buildTypes, then delete block of
# signingConfigs.
sed -i \
    -e "/signingConfig signingConfigs\./d" \
    "$osmand_dir/build.gradle"
sed -i \
    -e "/signingConfigs/,+15d" \
    "$osmand_dir/build.gradle"


# return from whence we came (just in case)
popd

