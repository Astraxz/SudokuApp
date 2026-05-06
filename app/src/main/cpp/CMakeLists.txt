cmake_minimum_required(VERSION 3.22.1)
project("sudoku")

add_library(
        sudoku
        SHARED
        sudoku.c
)

find_library(log-lib log)

target_link_libraries(
        sudoku
        ${log-lib}
)
