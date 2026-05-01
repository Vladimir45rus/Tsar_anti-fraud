cmake_minimum_required(VERSION 3.18.1)
project("tsarshield-stt")

# Настройка стандарта C++
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Добавление библиотек
find_library(log-lib log)
find_library(android-lib android)

# STT движок (Vosk/Silero совместимый интерфейс)
add_library(
    tsarshield-stt
    SHARED
    stt_engine.cpp
    audio_routing.cpp
    jni_bridge.cpp
)

# Включение заголовков
target_include_directories(
    tsarshield-stt
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Связывание библиотек
target_link_libraries(
    tsarshield-stt
    ${log-lib}
    ${android-lib}
    # В будущем добавить vosk или silero
)