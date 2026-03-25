@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo  E2E Tests Automation
echo ========================================

REM Пути
set BASE_DIR=C:\Users\Anton\IdeaProjects
set COMMON_DIR=%BASE_DIR%\common-dto
set PRODUCER_DIR=%BASE_DIR%\order-service
set CONSUMER_DIR=%BASE_DIR%\notification-service
set E2E_DIR=%BASE_DIR%\e2e-tests
set KAFKA_DIR=C:\Users\Anton\kafka-compose
set APPS_DIR=%E2E_DIR%\apps

REM 1. Запуск Kafka
echo [1] Запуск Kafka через Docker Compose...
cd %KAFKA_DIR%
docker-compose up -d
if %errorlevel% neq 0 (
    echo Ошибка запуска Docker Compose. Убедитесь, что Docker запущен.
    exit /b %errorlevel%
)

echo Ожидание готовности Kafka...
timeout /t 10 /nobreak >nul

REM 2. Сборка common-dto
echo [2] Сборка common-dto...
cd %COMMON_DIR%
call mvn clean install
if %errorlevel% neq 0 (
    echo Ошибка сборки common-dto.
    exit /b %errorlevel%
)

REM 3. Сборка order-service
echo [3] Сборка order-service...
cd %PRODUCER_DIR%
call mvn clean package
if %errorlevel% neq 0 (
    echo Ошибка сборки order-service.
    exit /b %errorlevel%
)

REM 4. Сборка notification-service
echo [4] Сборка notification-service...
cd %CONSUMER_DIR%
call mvn clean package
if %errorlevel% neq 0 (
    echo Ошибка сборки notification-service.
    exit /b %errorlevel%
)

REM 5. Копирование jar
echo [5] Копирование jar-файлов в папку apps...
if not exist %APPS_DIR% mkdir %APPS_DIR%
copy /Y %PRODUCER_DIR%\target\order-service-0.0.1-SNAPSHOT.jar %APPS_DIR%\producer-service.jar
copy /Y %CONSUMER_DIR%\target\notification-service-0.0.1-SNAPSHOT.jar %APPS_DIR%\notification-service.jar

REM 6. Запуск тестов
echo [6] Запуск E2E тестов...
cd %E2E_DIR%
call mvn clean test
set TEST_RESULT=%errorlevel%

REM 7. Остановка Kafka
echo [7] Остановка Kafka...
cd %KAFKA_DIR%
docker-compose down

echo ========================================
if %TEST_RESULT% equ 0 (
    echo Тесты успешно завершены.
    echo.
    echo Для генерации Allure отчёта выполните:
    echo cd %E2E_DIR%
    echo allure generate target\allure-results -o target\site\allure-maven --clean
    echo allure open target\site\allure-maven
) else (
    echo Ошибка при выполнении тестов.
)
exit /b %TEST_RESULT%