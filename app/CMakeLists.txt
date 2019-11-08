# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

add_library( # Specifies the name of the library.
        AudioEngine

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/PlaybackEngine.cpp
        src/main/cpp/StreamingEngine.cpp
        src/main/cpp/jni_bridge.cpp)

# Specify the libraries which our native library is dependent on, including Oboe
target_link_libraries (AudioEngine log oboe)

# Build the Oboe library
set (OBOE_DIR ../oboe)
add_subdirectory (${OBOE_DIR} oboe)

# Make the Oboe public headers available to our app
include_directories (${OBOE_DIR}/include)

set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -Ofast")