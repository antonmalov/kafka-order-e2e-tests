# Kafka Order Processing - E2E Tests

[![GitHub](https://img.shields.io/badge/Java-21-blue)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.4-green)](https://spring.io/projects/spring-boot)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-4.1.2-red)](https://kafka.apache.org/)

End-to-end тесты для проверки работы продюсера, консьюмера и механизма ретраев с DLT.

## Сценарии тестирования

| Тест | Описание |
|------|----------|
| **Позитивный** | Успешная обработка заказа (`product=book`) — сообщение в main topic |
| **Негативный** | Ошибка (`product=fail`) → 3 ретрая → сообщение в DLT |
| **Частичный сбой** | 2 ошибки, 3-я успешна (`orderId=partial-*`) — сообщение в main topic |

## Ожидаемый результат

```
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

## Запуск

### Автоматический запуск (рекомендуется)

```bash
.\run-tests.bat
```

Скрипт автоматически:
1. Запускает Kafka через Docker Compose
2. Собирает common-dto
3. Собирает и копирует jar-файлы сервисов
4. Выполняет E2E-тесты
5. Останавливает Kafka

### Ручной запуск

1. Запустить Kafka:
   ```bash
   docker-compose up -d
   ```

2. Собрать и скопировать сервисы:
   ```bash
   cd ../common-dto && mvn install
   cd ../order-service && mvn package
   cd ../notification-service && mvn package
   cp ../order-service/target/*.jar apps/producer-service.jar
   cp ../notification-service/target/*.jar apps/notification-service.jar
   ```

3. Запустить тесты:
   ```bash
   mvn clean test
   ```

4. Остановить Kafka:
   ```bash
   docker-compose down
   ```

## Структура

```
e2e-tests/
├── apps/                  # jar-файлы сервисов (создаётся при сборке)
├── src/
│   └── test/java/tests/
│       └── OrderE2ETest   # E2E тесты
├── run-tests.bat          # Скрипт автоматизации
└── pom.xml
```

## Связанные репозитории

| Репозиторий | Описание | Ссылка |
|-------------|----------|--------|
| **Главный репозиторий** | Документация и инфраструктура | [kafka-order-processing](https://github.com/antonmalov/kafka-order-processing) |
| **common-dto** | Общие DTO | [kafka-order-common-dto](https://github.com/antonmalov/kafka-order-common-dto) |
| **producer** | Продюсер (REST → Kafka) | [kafka-order-producer](https://github.com/antonmalov/kafka-order-producer) |
| **consumer** | Консьюмер с ретраями и DLT | [kafka-order-consumer](https://github.com/antonmalov/kafka-order-consumer) |

## Лицензия

MIT