#### Changelog #
*************************************

- 2023-06-10
    - updated name for custom artifact APK

- 2023-06-09
    - build using Gradle Build Github Action (`gradle/gradle-build-action@v2`)
    - added `gradlew clean` befofe build

- 2023-06-04
    - [IN-PROGRESS] disabling some, activating some patches...
    - created a Docker image for local build purposes
    - added a `prebuild-only-rename.sh` bash script for temporary success building, without purchase version removes
    - also rename `net.osmand.dev` -> `net.osmand.djbpf`

- 2023-06-03
    - added `.patch` folder for building custom `OsmAnd~` app
    - added but not used `prebuild.sh` script for GitHub actions
    - using `prebuild.sh` for 'sed's and app rename before creating APK

- 2023-05-23
    - added custom APK build workflow [for this branch only]
    - added `USEFUL.md` map file
    - [DONE] first apk artifact!
    - Changing night build information (version/appname)
    - setting `APK_VERSION_SUFFIX` env for '#build' number in versionName
    - added `TARGET_APP_NAME` env, updated `APP_EDITION`
    - added automatic APP_EDITION update (via env set)