# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.4.1)


# Add library
add_library(serial-port
            SHARED
            src/main/cpp/SerialPort.cpp)

# Searches for a specified prebuilt library and stores the path as a
# variable.
find_library(log-lib log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries(serial-port ${log-lib})
