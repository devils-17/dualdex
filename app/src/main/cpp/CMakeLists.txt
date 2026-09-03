cmake_minimum_required(VERSION 3.22.1)

project("dualdex_native" C)

set(NATIVE_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../../native")

include_directories(
    "${NATIVE_ROOT}/include"
)

add_library(
    dualdex_native
    SHARED
    dualdex_jni.c
    "${NATIVE_ROOT}/src/pokemon_reader.c"
    "${NATIVE_ROOT}/src/pokemon_text.c"
)

find_library(log-lib log)

target_link_libraries(
    dualdex_native
    ${log-lib}
)
