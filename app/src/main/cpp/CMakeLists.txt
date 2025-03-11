cmake_minimum_required(VERSION 3.10...4.0)

# Define the project name
project(TXPacker)

# Set compiler flags for better security
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Werror -fvisibility=hidden -fstack-protector-all -D_FORTIFY_SOURCE=2")

# Add the native library
add_library(
        txpacker
        SHARED
        txpackerlib.cpp
        tokyo.cpp
)

# Link the library with the Android log library
find_library(
        log-lib
        log
)

target_link_libraries(
        txpacker
        ${log-lib}
) 