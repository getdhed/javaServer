# Лабораторная №3 — Java, Tomcat, PostgreSQL

Два Maven-проекта для NetBeans:
- **Lab3Server** — WAR с сервлетами, запускается в Tomcat 11.
- **Lab3Client** — обычное Swing-приложение, запускается отдельно через Run.

## Что делает

- **Translate / Reverse** → `MyServlet` → `Dictionary` → PostgreSQL.
- **Word** → `WordServlet` отдаёт DOCX, Swing показывает его справа.
- **Gold** → `GoldServlet` получает страницу Google Finance и возвращает **одну строку**:
  `Google Finance (фьючерс COMEX): <цена> USD за тройскую унцию`.
  Это котировка фьючерса GCW00, **не спотовая цена**. Если сайт недоступен или
  блокирует автоматический доступ, отобразится ошибка, выдуманной цены нет.
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
5. **Lab3Server → Clean and Build**. Скопируй свежий
   `Lab3Server/target/Lab3Server.war` в
   `C:\\Tomcat11\\apache-tomcat-11.0.27\\webapps\\Lab3Server.war`
   и перезапусти Tomcat. Если развёртываешь из NetBeans — используй только
   этот способ, без параллельного ручного копирования.
6. **Lab3Client → Run** (или из папки клиента `mvn exec:java`).

Проверка: `http://localhost:8080/Lab3Server/MusicServlet` должен вернуть MP3,
`http://localhost:8080/Lab3Server/WordServlet` — DOCX.

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
- `GoldQuoteService.java`: HTTP GET Google Finance, чтение HTML, поиск цены.
- `ApacheClient.java`: Swing-кнопки, HTTP-клиент, MP3-воспроизведение.
- `MyServlet.java` / `Dictionary.java`: параметры запроса и JDBC.

**Проверка:** изменения опубликованы в GitHub; локальную сборку и реальное
воспроизведение нужно проверить в NetBeans на твоём компьютере.
