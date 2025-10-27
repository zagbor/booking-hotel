🏨 Booking Hotel — Демонстрационная система бронирований отелей
Общая концепция

Booking Hotel — учебный пример распределённого приложения, реализующего микросервисную архитектуру для управления бронированиями отелей.
Проект разделён на четыре модуля, взаимодействующих через Eureka Service Registry и API Gateway.
Основная цель — показать интеграцию сервисов бронирования, управления отелями и шлюза с использованием JWT-аутентификации и централизованной маршрутизации запросов.

🧩 Модули проекта
1. Eureka Server (eureka-server)

Центральный реестр сервисов.

Автоматическое обнаружение и регистрация booking-service, hotels и gateway.

Запускается на порту 8761.

Конфигурация в application.yml.

2. API Gateway (gateway)

Реализован на Spring Cloud Gateway.

Отвечает за маршрутизацию запросов к booking-service и hotels.

Поддерживает:

JWT-валидацию на уровне шлюза.

Проксирование заголовков Authorization и X-Correlation-Id.

Использует SecurityConfig, JwtConfig, JwtSecretKeyProvider.

Запускается на порту 8080.

3. Booking Service (booking-service)

Основной сервис для работы с бронированиями и пользователями.

Функции:

Регистрация и аутентификация пользователей.

Создание, просмотр и отмена бронирований.

Администрирование пользователей (AdminController).

Управление JWT-токенами и безопасностью.

Реализует шаблон двухфазного бронирования (PENDING → CONFIRMED / CANCELLED) с возможностью компенсации.

Конфигурация безопасности (SecurityConfig, JwtConfig, JwtService).

Использует встроенную H2-базу.

Порт: динамический (регистрируется в Eureka под booking-service).

4. Hotel Service (hotels)

Отвечает за CRUD-операции по отелям и номерам:

Создание, изменение и удаление отелей (HotelController).

Управление комнатами и их доступностью (RoomController).

Поддерживает подсчёт загруженности номеров и статистику по бронированиям (HotelService).

Использует сущности:

Hotel

Room

ReservationLock — удержание слота перед подтверждением бронирования.

Порт: динамический, сервис регистрируется как hotels.

🔐 Аутентификация и безопасность

Все сервисы используют JWT-аутентификацию.

Ключ задаётся в application.yml через security.jwt.secret.

На этапе демонстрации используется симметричный ключ (HMAC).

Предусмотрены классы:

JwtSecretKeyProvider

JwtConfig

SecurityConfig

Рекомендуется заменить секрет в продакшене или интегрировать с Keycloak / OAuth2.

⚙️ Архитектура и взаимодействие

Взаимодействие между booking-service и hotels выполняется через WebClient.

Конфигурация клиента: WebClientConfig.

Реализована корреляция запросов через X-Correlation-Id.

Все сервисы логируют корреляционный идентификатор.

Транзакции локальные (@Transactional).

🧰 Используемые технологии

Spring Boot 3+

Spring Cloud Eureka / Gateway

Spring Security (JWT)

Spring Data JPA / H2

OpenAPI (Swagger)

Lombok

Maven

🚀 Запуск проекта

Запусти Eureka Server:

mvn -pl eureka-server spring-boot:run


Запусти Gateway:

mvn -pl gateway spring-boot:run


Запусти Booking Service и Hotel Service (в отдельных окнах):

mvn -pl booking-service spring-boot:run
mvn -pl hotels spring-boot:run


После запуска сервисы зарегистрируются в Eureka:
👉 http://localhost:8761

🔎 Примеры API-вызовов (через Gateway на 8080)

Регистрация пользователя

curl -X POST http://localhost:8080/auth/register \
-H 'Content-Type: application/json' \
-d '{"username":"user1","password":"pass"}'


Вход и получение JWT

TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
-H 'Content-Type: application/json' \
-d '{"username":"user1","password":"pass"}' | jq -r .access_token)


Создание отеля (admin)

curl -X POST http://localhost:8080/hotels \
-H "Authorization: Bearer $TOKEN" \
-H 'Content-Type: application/json' \
-d '{"name":"Hotel A","city":"Tel Aviv","address":"Allenby 10"}'


Создание бронирования

curl -X POST http://localhost:8080/bookings \
-H "Authorization: Bearer $TOKEN" \
-H 'Content-Type: application/json' \
-d '{"roomId":1,"startDate":"2025-11-01","endDate":"2025-11-05"}'

💡 Особенности

Идемпотентность операций через requestId.

Повторы с backoff при недоступности удалённого сервиса.

Поддержка админских операций над пользователями и отелями.

Встроенный Swagger:

Booking Service → /swagger-ui.html

Hotel Service → /swagger-ui.html

Gateway (агрегатор) → http://localhost:8080/swagger-ui.html

🧩 Возможные улучшения

Добавить Circuit Breaker (Resilience4j).

Вынести секреты в Config Server.

Добавить централизованный мониторинг и трассировку (Zipkin / Sleuth).

Расширить Hotel Service для бронирования по датам.

Интеграция с внешним Identity Provider.