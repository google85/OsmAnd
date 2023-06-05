#FROM eclipse-temurin:11
FROM eclipse-temurin:11.0.19_7-jdk-alpine

#FROM gradle:7.4.2-jdk8-jammy
#FROM cangol/android-gradle

ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=${ANDROID_HOME}
ENV ANDROID_SDK=${ANDROID_HOME}
ENV ANDROID_NDK=/opt/android-ndk-r23c

RUN apk -U update && apk -U add \
  git \
  make \
  wget \
  && rm -rf /tmp/* \
	&& rm -rf /var/cache/apk/*

RUN mkdir -p /opt/app
RUN mkdir -p android

WORKDIR /opt/app

RUN git clone https://github.com/osmandapp/OsmAnd-resources.git    resources
RUN git clone https://github.com/osmandapp/OsmAnd-core.git         core
RUN git clone https://github.com/osmandapp/OsmAnd-core-legacy.git  core-legacy
RUN git clone https://github.com/osmandapp/OsmAnd-build.git        build
RUN git clone https://github.com/osmandapp/OsmAnd-tools.git        tools
RUN git clone https://github.com/osmandapp/OsmAnd-misc.git         misc

WORKDIR /opt/app/android

COPY . .

RUN cat <<EOF > /opt/app/android/local.properties  \
sdk.dir=/opt/android-sdk \
sdk-location=/opt/android-sdk \
EOF

RUN chmod +x ./gradlew

RUN printf "\norg.gradle.jvmargs=-Xmx2048m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8\n" >> gradle.properties


# build
#RUN ./gradlew clean
RUN ./gradlew build
#RUN ./gradlew assembleNightlyFreeLegacyFatDebug

# rename with commit hash
###RUN mv OsmAnd/build/outputs/apk/nightlyFreeLegacyFat/debug/OsmAnd-nightlyFree-legacy-fat-debug.apk OsmAnd/build/outputs/apk/nightlyFreeLegacyFat/debug/OsmAnd-nightlyFree-legacy-fat-debug-$(git log -n 1 --format='%h').apk
