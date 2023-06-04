#!/bin/sh

# Fail on any error
set -e

echo "Runing in Docker.."

# if build works ok
docker build -t build-android-img .

docker container create --name temp-android build-android-img  --env-file=.env
# copy from container
docker container cp temp-android:/opt/workspace/android/build/outputs/apk/*/*/*.apk ./
docker container rm temp-android

#docker run --tty --interactive --volume=$(pwd):/opt/workspace --workdir=/opt/workspace --rm cangol/android-gradle  /bin/sh -c "./gradlew clean && ./gradlew build && echo 'Please run ./gradlew build  again if changes are made to the code.' && /bin/bash"

echo "Done."
exit 0