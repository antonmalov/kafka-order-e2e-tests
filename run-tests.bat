@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo  E2E Tests Automation
echo ========================================

REM
set BASE_DIR=C:\Users\Anton\IdeaProjects
set COMMON_DIR=%BASE_DIR%\common-dto
set PRODUCER_DIR=%BASE_DIR%\order-service
set CONSUMER_DIR=%BASE_DIR%\notification-service
set E2E_DIR=%BASE_DIR%\e2e-tests
set KAFKA_DIR=C:\Users\Anton\kafka-compose
set APPS_DIR=%E2E_DIR%\apps

REM
echo [1] Запуск Kafka через Docker Compose...
cd %KAFKA_DIR%
docker-compose up -d
if %errorlevel% neq 0 (
    echo Ошибка запуска Docker Compose. Убедитесь, что Docker запущен.
    exit /b %errorlevel%
)

echo Ожидание готовности Kafka...
timeout /t 10 /nobreak >nul

REM
echo [2] Сборка common-dto...
cd %COMMON_DIR%
call mvn clean install
if %errorlevel% neq 0 (
    echo Ошибка сборки common-dto.
    exit /b %errorlevel%
)

REM
echo [3] Сборка order-service...
cd %PRODUCER_DIR%
call mvn clean package
if %errorlevel% neq 0 (
    echo Ошибка сборки order-service.
    exit /b %errorlevel%
)

REM
echo [4] Сборка notification-service...
cd %CONSUMER_DIR%
call mvn clean package
if %errorlevel% neq 0 (
    echo Ошибка сборки notification-service.
    exit /b %errorlevel%
)

REM
echo [5] Копирование jar-файлов в папку apps...
if not exist %APPS_DIR% mkdir %APPS_DIR%
copy /Y %PRODUCER_DIR%\target\order-service-0.0.1-SNAPSHOT.jar %APPS_DIR%\producer-service.jar
copy /Y %CONSUMER_DIR%\target\notification-service-0.0.1-SNAPSHOT.jar %APPS_DIR%\notification-service.jar

REM
echo [6] Запуск E2E тестов...
cd %E2E_DIR%
call mvn clean test
set TEST_RESULT=%errorlevel%

REM
echo [6.5] Генерация Allure отчёта...
cd %E2E_DIR%
if exist %E2E_DIR%\target\allure-results (
    echo Найдены результаты тестов. Генерируем Allure отчёт...
    allure generate %E2E_DIR%\target\allure-results -o %E2E_DIR%\target\site\allure-maven --clean
    echo Allure отчёт сгенерирован: %E2E_DIR%\target\site\allure-maven\index.html
    echo Открыть отчёт можно командой: start %E2E_DIR%\target\site\allure-maven\index.html
    explorer "%E2E_DIR%\target\site\allure-maven\index.html"
) else (
    echo Предупреждение: папка с результатами allure-results не найдена.
)

REM
echo [7] Остановка Kafka...
cd %KAFKA_DIR%
docker-compose down

echo ========================================
if %TEST_RESULT% equ 0 (
    echo Тесты успешно завершены.
) else (
    echo Ошибка при выполнении тестов.
)
exit /b %TEST_RESULT%