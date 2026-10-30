# Лабораторная №3 — один WAR для Tomcat 11 и GlassFish 8

## Как устроено

- `Lab3Client` — прежний Java Swing-клиент, **не меняем**.
- `Lab3Server` — исходники сервера (Servlet, Dictionary, Word, Gold, Music).
  В обычном NetBeans Java SE-проекте **Run** запускает `ServerMain`
  (встроенный HTTP-сервер, не Tomcat / GlassFish).
- `Lab3Web` — **отдельный Maven-проект** для сборки настоящего `Lab3Server.war`.
  Он компилирует Java-файлы из `Lab3Server/src` и берёт веб-ресурсы из
  `Lab3Server/web`, поэтому исходники не дублируются.

Требуется **JDK 21**, NetBeans c Maven, Tomcat **11.x** или GlassFish **8.x**.
Servlet API: Jakarta **6.1**, используются импорты `jakarta.servlet.*`.
Maven автоматически скачает Servlet API и PostgreSQL JDBC; последний попадёт
в WAR, Servlet API предоставляет контейнер и в WAR он не упаковывается.

## 1. Собрать WAR в NetBeans

1. Сделайте `git pull` и откройте проект **Lab3Web** через File → Open Project.
   Отдельно можете оставить открытыми `Lab3Client` и `Lab3Server`.
2. На `Lab3Web` → **Clean and Build** (Maven скачает зависимости).
   Или в терминале в папке `Lab3Web`: `mvn clean package`.
3. Результат: `Lab3Web/target/Lab3Server.war`.

**Важно:** в корне репозитория есть старый `Lab3Server.war`: он не
обновляется автоматически и НЕ соответствует новой Jakarta-версии.
Используйте исключительно `Lab3Web/target/Lab3Server.war`.

## 2. Tomcat 11

1. Скачайте Tomcat 11: https://tomcat.apache.org/download-11.cgi
2. Распакуйте ZIP, скопируйте `Lab3Web/target/Lab3Server.war` в
   `<tomcat>/webapps/Lab3Server.war`.
3. Запустите `<tomcat>/bin/startup.bat` (или `catalina.bat run`).
4. Проверьте: http://localhost:8080/Lab3Server/WordServlet

## 3. GlassFish 8

1. Скачайте GlassFish 8: https://glassfish.org/download
2. Распакуйте ZIP, перейдите в `glassfish8/bin` (или папку `glassfish/bin`
   внутри вашего ZIP), выполните `asadmin.bat start-domain`.
3. Разверните **тот же WAR**, например:
   `asadmin.bat deploy --force=true --contextroot Lab3Server "C:\\Projects\\javaServer\\Lab3Web\\target\\Lab3Server.war"`
   (подставьте реальный путь к WAR).
4. Проверьте: http://localhost:8080/Lab3Server/WordServlet

**Не запускайте оба сервера на одном порту 8080 одновременно.**
Для переключения остановите предыдущий сервер.

## 4. Настройка Swing-клиента

В `Lab3Client` → Properties → Run → **VM Options**:
`-Dlab3.serverUrl=http://localhost:8080/Lab3Server`

Это адрес контекста приложения; он одинаков для обоих серверов,
если развернуть WAR под именем `Lab3Server`.

## 5. PostgreSQL

Для переводов создайте таблицу скриптами `Lab3Server/sql/`
и задайте переменные окружения серверного процесса:
- `LAB3_DB_URL=jdbc:postgresql://localhost:5432/lab3_dictionary`
- `LAB3_DB_USER=lab3_user`
- `LAB3_DB_PASSWORD=<ваш_пароль>`

В Windows задайте их в окружении **перед запуском** Tomcat/GlassFish,
либо системно, перезапустив процесс сервера. Не коммитьте пароль.
Подробности: `POSTGRES_SETUP.txt`.

## 6. Проверка на защите

В каждом контейнере откройте:
- `/Lab3Server/WordServlet` — документ Word (без PostgreSQL)
- `/Lab3Server/MusicServlet` — WAV
- `/Lab3Server/MyServlet?txt=кот&dir=ru&format=text` — `cat` при доступной БД
- `/Lab3Server/MyServlet?txt=dog&dir=en&format=text` — `собака` при доступной БД

GoldServlet зависит от доступности биржевых сайтов: HTTP 403 от внешнего
источника не связан с выбором Tomcat или GlassFish.

## Что проверено, а что нет

Код сервлетов компилируется с соответствующими методами Jakarta Servlet
(локальная проверка с компиляционными заглушками API).
Реальная сборка через Maven, установка серверов и развёртывание WAR
не выполнялись в тестовой среде: это нужно проверить в NetBeans на вашем ПК.
