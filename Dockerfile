#FROM eclipse-temurin:11
FROM eclipse-temurin:11.0.19_7-jdk-alpine

RUN mkdir /opt/app
#FROM gradle:7.4.2-jdk8-jammy
#FROM cangol/android-gradle

#RUN apt-get update -y && apt-get upgrade -y \
#    && apt-install git

RUN apk add git

WORKDIR /opt/app

RUN git clone https://github.com/osmandapp/OsmAnd-resources.git    resources
RUN git clone https://github.com/osmandapp/OsmAnd-core.git         core
RUN git clone https://github.com/osmandapp/OsmAnd-core-legacy.git  core-legacy
RUN git clone https://github.com/osmandapp/OsmAnd-build.git        build
RUN git clone https://github.com/osmandapp/OsmAnd-tools.git        tools
RUN git clone https://github.com/osmandapp/OsmAnd-misc.git         misc

RUN mkdir -p android
WORKDIR /opt/app/android

COPY . .

RUN chmod +x ./gradlew

RUN printf "\norg.gradle.jvmargs=-Xmx2048m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8\n" >> gradle.properties

# build
#RUN ./gradlew clean
RUN ./gradlew assembleNightlyFreeLegacyFatDebug

# rename with commit hash
###RUN mv OsmAnd/build/outputs/apk/nightlyFreeLegacyFat/debug/OsmAnd-nightlyFree-legacy-fat-debug.apk OsmAnd/build/outputs/apk/nightlyFreeLegacyFat/debug/OsmAnd-nightlyFree-legacy-fat-debug-$(git log -n 1 --format='%h').apk
