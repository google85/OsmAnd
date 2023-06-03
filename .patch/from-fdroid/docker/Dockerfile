FROM registry.gitlab.com/fdroid/fdroidserver:buildserver-bullseye
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=${ANDROID_HOME}
ENV ANDROID_SDK=${ANDROID_HOME}
ENV ANDROID_NDK=/opt/android-ndk-r23c
ENV FDROIDSERVER=/home/vagrant/fdroidserver
ENV PATH="/opt/gradle/bin:${FDROIDSERVER}:${PATH}"
ENV PYTHONPATH="${FDROIDSERVER}:${FDROIDSERVER}/examples"
ENV PYTHONUNBUFFERED=true
RUN apt-get update
RUN apt-get dist-upgrade
RUN mkdir ${FDROIDSERVER}
RUN curl --silent https://gitlab.com/fdroid/fdroidserver/-/archive/master/fdroidserver-master.tar.gz | tar -xz --directory=${FDROIDSERVER} --strip-components=1
RUN rm -rf $ANDROID_HOME/tools
RUN sdkmanager "tools" "platform-tools" "build-tools;31.0.0" "cmake;3.22.1"
RUN apt-get update || apt-get update
RUN apt-get install --yes swig openjdk-11-jdk-headless g++ clang gradle vim build-essential python
RUN update-alternatives --auto java
RUN curl -Lo cmake.tar.gz https://github.com/Kitware/CMake/releases/download/v3.21.2/cmake-3.21.2-linux-x86_64.tar.gz
RUN echo "d5517d949eaa8f10a149ca250e811e1473ee3f6f10935f1f69596a1e184eafc1 cmake.tar.gz" | sha256sum -c -
RUN tar xzf cmake.tar.gz --strip-components=1 -C /usr/local/
RUN curl "https://dl.google.com/android/repository/android-ndk-r23c-linux.zip" --output android-ndk-r23c-linux.zip
RUN unzip android-ndk-r23c-linux -d /opt/
COPY setup-volume.sh /usr/bin/setup-volume.sh
RUN chmod +x /usr/bin/setup-volume.sh
