# Лабораторная №3 — Java, Tomcat, PostgreSQL

Два Maven-проекта для NetBeans:
- **Lab3Server** — WAR с сервлетами, запускается в Tomcat 11.
- **Lab3Client** — обычное Swing-приложение, запускается отдельно через Run.

## Что делает

- **Translate / Reverse** → `MyServlet` → `Dictionary` → PostgreSQL.
- **Word** → `WordServlet` отдаёт DOCX, Swing показывает его справа.
- **Gold** → `GoldServlet` вызывает `GoldQuoteService`: Java скачивает
  **HTML-страницу Kitco** (`https://www.kitco.com/charts/gold`).
  Библиотека jsoup перебирает **HTML-теги `<p>`** и извлекает
  число из предложения `The current price of gold is $... per troy ounce`.
  Никаких JSON, CSV или API. Возвращается одна строка:
  `Kitco: золото — <цена> USD за тройскую унцию`.
  Это спотовая цена, а не фьючерс GCW00. Если сайт блокирует запрос
  или изменяет разметку, сервер выдаёт короткую текстовую ошибку HTTP 502.
- **Музыка** → `MusicServlet` передаёт готовый `music.mp3` → клиент
  воспроизводит MP3 через BasicPlayer: запуск / пауза / продолжение.

## Первый запуск после обновления (важно!)

Раньше **Lab3Client** был Ant-проектом. Теперь он **Maven-проект**, чтобы
зависимость MP3-плеера скачивалась автоматически без ручных JAR.

1. Закрой **Lab3Client** в NetBeans (**Close Project**).
2. В корне `lab3` сделай `git pull --ff-only origin main`.
3. В NetBeans **File → Open Project → Lab3Client**: теперь NetBeans узнает его
   по `pom.xml`. При первом открытии Maven скачает BasicPlayer и зависимости.
4. В `Lab3Server/web/WEB-INF/` положи свой файл **music.mp3**
   (не переименованный WAV, а настоящий MP3). Трек **не коммить в Git**.
5. Tomcat должен быть запущен. **Lab3Server → Run (F6)**.
   Мы настроили `Lab3Server/nbactions.xml`: при Run Maven собирает WAR и
   автоматически копирует его в
   `C:/Tomcat11/apache-tomcat-11.0.27/webapps/`.
   Если в `server.xml` у Host стоит `autoDeploy="true"` (значение по умолчанию),
   Tomcat автоматически разворачивает изменённый WAR. **Вручную копировать,
   удалять папку Lab3Server и перезапускать Tomcat больше не требуется.**
   В Output Maven ищи строку `LAB3 DEPLOY: copying Lab3Server.war...`.
   Это подтверждает копирование; успешное развёртывание проверяй в логах Tomcat.
   `Clean and Build` без Run просто создаёт WAR — не копирует его.
6. **Lab3Client → Run** (или из папки клиента `mvn exec:java`).

Проверка: `http://localhost:8080/Lab3Server/MusicServlet` должен вернуть MP3,
`http://localhost:8080/Lab3Server/WordServlet` — DOCX.

## Автообновление WAR из NetBeans

Для этого компьютера настроено действие `Lab3Server → Run (F6)`.
В `Lab3Server/nbactions.xml` указан путь
`C:/Tomcat11/apache-tomcat-11.0.27/webapps`; если Tomcat установлен в
другом месте, отредактируй **только этот путь**.
`pom.xml` содержит Maven-профиль, активирующийся при наличии свойства
`lab3.tomcat.webapps`: во время `package` он копирует готовый WAR в
Tomcat. Обычное `mvn package` без свойства не затрагивает Tomcat.
При стандартной конфигурации `autoDeploy=true` Tomcat сам замечает изменение
WAR и разворачивает новую версию. Для полной проверки:
`http://localhost:8080/Lab3Server/WordServlet` и
`http://localhost:8080/Lab3Server/GoldServlet`.
Если во время Run появился только `BUILD SUCCESS`, но нет строки
`LAB3 DEPLOY`, значит NetBeans запустил другое действие либо не прочитал
`nbactions.xml` — закрой и повторно открой `Lab3Server`.

## Проверка Gold

Открой `http://localhost:8080/Lab3Server/GoldServlet`. Должна появиться одна
строка со спотовой ценой золота, взятой из HTML-тега `<p>` сайта Kitco,
либо короткая ошибка HTTP 502. Доступность и структура сайта не гарантированы.
Сайт: https://www.kitco.com/charts/gold.
При ошибке смотри строки `[LAB3-GOLD]` в логах Tomcat.

## PostgreSQL

Рабочие настройки PostgreSQL **не изменены**. Java читает их из окружения Tomcat:
`LAB3_DB_URL`, `LAB3_DB_USER`, `LAB3_DB_PASSWORD`.
Пароль не добавляй в Git.

При старте WAR в логах Tomcat появятся `[LAB3-DB] STARTUP` и
`[LAB3-DB] SUCCESS` или `FAILED`. В случае 503 смотри эти строки.
Понадобится таблица `translations` из `Lab3Server/sql/init.sql`.

Для Windows Tomcat, запущенного через `catalina.bat`, переменные можно
задавать в `<TOMCAT_HOME>\\bin\\setenv.bat`. Если NetBeans запускает
Tomcat иначе, окружение может не наследоваться.

## Код для защиты

- `MusicServlet.java`: читает MP3 из `WEB-INF` и отправляет байты по HTTP.
- `GoldQuoteService.java`: загрузка HTML Kitco через jsoup и чтение цены из тега `<p>`.
- `ApacheClient.java`: Swing-кнопки, HTTP-клиент, MP3-воспроизведение.
- `MyServlet.java` / `Dictionary.java`: параметры запроса и JDBC.

**Проверка:** изменения опубликованы в GitHub; локальную сборку и реальное
воспроизведение нужно проверить в NetBeans на твоём компьютере.
