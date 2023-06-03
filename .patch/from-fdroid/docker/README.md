# Docker Build Environment

A Docker build environment intended to make a local build easier.
Dockerfile based loosely on FDroid runner setup.

Caveat: these are rough instructions, not intended to be robust, but to
help guide a build. You may have to inspect and edit some of the
scripts.

You need about 75Gb of free space for a build.

## Setup

Install docker, then build the image from the directory containing the
Dockerfile:

    $ docker build -t local/osmand .

Then run a shell in the container. Create an external volume with `-v`
so that the build results are available outside of the container.
Replace `/local/path/to/volume` with whichever directory you want.

    $ docker run -it --rm \
        --name osmand \
        -v /local/path/to/volume:/mnt/volume local/osmand \
        /bin/bash

Once inside the container, you can use the `setup-volume.sh` script to
initialise the build directory. This will download the basic source
code.

    % cd /mnt/volume
    % setup-volume.sh

Next, in the OsmAnd-submodules directory, run the prebuild script with the
OsmAnd version you're building. E.g.

    % ./prebuild.sh "4.2.7" "4207"

## Build

Then build the external deps.

    % ./build.sh

Finally, to build OsmAnd, cd to the app directory and compile.

    % cd android/Osmand
    % gradle assembleAndroidFullOpenglFatRelease

In total, the process takes about 2.5 hours on my machine.

You can find the APK in `build/ouputs/...../Osmand-...-release.apk`.
