cmake_minimum_required(VERSION 3.10)

project(libfreedcam LANGUAGES CXX C)

# Set the C++ standard
set(CMAKE_CXX_STANDARD 11)

# Align LOAD segments to 16 KB so the .so files produced below work on
# 16 KB-page devices (required by Play for Android 15+ targets since
# Nov 1 2025). ndkVersion 27.3 doesn't default to this yet (that only
# became automatic in NDK r28), so it's set explicitly here rather than
# relying on the toolchain. Applies to every add_library() below since
# it's a linker flag, not a per-target one.
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

# Include directories for native sources
include_directories(
    ${PROJECT_SOURCE_DIR}/LibRaw
    ${PROJECT_SOURCE_DIR}/tiff/libtiff
    ${PROJECT_SOURCE_DIR}/freedcam
)

# The vendored libraries' sources, as tools/vendor_libs.py imported them from their release
# tarballs (versions, hashes and GlassCam's one libtiff patch: VENDORED.json).
include(${PROJECT_SOURCE_DIR}/vendored_sources.cmake)

add_library(tiff SHARED ${LIBTIFF_SOURCES})

# Link external libraries to libtiff if necessary
target_link_libraries(tiff PRIVATE log)

# Create shared library for libraw
add_library(libraw SHARED ${LIBRAW_SOURCES})

# Link external libraries to libraw if necessary
target_link_libraries(libraw PRIVATE log)

# Create shared library for freedcam
add_library(freedcam SHARED
    freedcam/RawToDng.cpp
    freedcam/DngWriter.cpp
    freedcam/DngStacker.cpp
    freedcam/ExifInfo.cpp
    freedcam/GpsInfo.cpp
    freedcam/DngProfile.cpp
    freedcam/CustomMatrix.cpp
    freedcam/OpCode.cpp
    freedcam/LibRawWrapper.cpp
    freedcam/LibRawJniWrapper.cpp
    freedcam/lj92.cpp
)

# Link libraries to freedcam
target_link_libraries(freedcam PRIVATE tiff libraw jnigraphics android log)

# Set output directory (Gradle will handle placement)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/obj/local/${ANDROID_ABI})
