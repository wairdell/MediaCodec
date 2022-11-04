# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
add_library(bridge-lib
        SHARED
        com_sharp_ambition_mediacodecdemo_NativeBridge.cpp
        com_android_rtmpvideo_YuvOperateJni.cpp
        com_android_rtmpvideo_RtmpJni.cpp
        rtmpjni/CRtmpWrap.cpp
        yuvengine/yuvengine.cpp)

include_directories(${PROJECT_SOURCE_DIR}/librtmp)

add_subdirectory(librtmp)

#add_definitions(-DNO_CRYPTO)

find_library(
        log-lib
        log)

#link rtmp library
#[[set(RTMP_LIB_DIR ${CMAKE_CURRENT_BINARY_DIR}/rtmp)
add_library(rtpm STATIC IMPORTED)
set_target_properties(rtpm PROPERTIES IMPORTED_LOCATION ${RTMP_LIB_DIR}/librtmp.a)]]

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        bridge-lib
        rtmp
        ${log-lib}
        )