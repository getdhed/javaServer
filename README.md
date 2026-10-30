# Лабораторная №3: Java Servlet + Tomcat + PostgreSQL

Два проекта NetBeans:
- **Lab3Client** — Java Swing-клиент. Перевод, DOCX-просмотр, котировка золота, музыка.
- **Lab3Server** — **Maven Web Application (WAR)**. Только сервлеты и Java-бизнес-логика. HTTP-сервер запускает Tomcat.

Самостоятельный `ServerMain.java` и его `HttpHandler` удалены. Другого HTTP-сервера внутри приложения нет.

## Архитектура

```text
Lab3Client (Swing)
       |
       | HTTP: localhost:8080/Lab3Server
       v
Tomcat 11
  +-- /MyServlet    -> Dictionary.java -> JDBC -> PostgreSQL:5432
  +-- /WordServlet  -> WEB-INF/lab3-document.docx
  +-- /GoldServlet  -> GoldQuoteService -> HTML-страницы котировок
  +-- /MusicServlet -> WAV-файл (WEB-INF или LAB3_MUSIC_FILE)
```

Сервлеты зарегистрированы в `Lab3Server/web/WEB-INF/web.xml`.
Для PostgreSQL нужен только сервер, Swing-клиент к базе не подключается.

## Подготовка

Нужны **JDK 21**, NetBeans с Maven, **Tomcat 11.x** (Jakarta Servlet 6.1),
PostgreSQL. Tomcat 9 и 10.1 не подходят к текущему Servlet API 6.1.
Не запускайте старый `ServerMain`: он удалён. Запускать Tomcat и другой
HTTP-сервер на одном порту 8080 одновременно нельзя.

### 1. База данных

- В pgAdmin/psql под администратором PostgreSQL выполните `Lab3Server/sql/create_database.sql`.
  Сначала измените `CHANGE_ME_STRONG_PASSWORD` на свой пароль (в локальном SQL, не коммитьте его).
- Затем подключитесь к созданной БД `lab3_dictionary` пользователем `lab3_user`
  и выполните `Lab3Server/sql/init.sql`.
- В результате будет таблица `translations` с 15 парами слов.
  Файлового словаря больше нет — перевод всегда идёт через SQL.

### 2. Настроить переменные окружения Tomcat

На Windows создайте локальный файл `<TOMCAT_HOME>\bin\setenv.bat`
(в папке Tomcat, **не в Git**) со следующим содержимым:

```bat
set "LAB3_DB_URL=jdbc:postgresql://localhost:5432/lab3_dictionary"
set "LAB3_DB_USER=lab3_user"
set "LAB3_DB_PASSWORD=ВАШ_ЛОКАЛЬНЫЙ_ПАРОЛЬ"
```

Перезапустите Tomcat. Если он работает как служба Windows, учтите: она
может не использовать `setenv.bat`; задайте окружение непосредственно службе.
Не записывайте настоящий пароль в репозиторий.

### Музыка из файла вместо генерации

**Вариант 1 (проще):** скопируйте свой файл WAV в
`Lab3Server/web/WEB-INF/music.wav`. Выполните `Clean and Build`,
затем заново разверните `Lab3Server.war` на Tomcat.
Файл включится в WAR и станет доступен только через `/MusicServlet`.

**Вариант 2 (файл в любой папке компьютера):**
задайте окружение процесса Tomcat, например, в `<TOMCAT_HOME>\\bin\\setenv.bat`:

```bat
set "LAB3_MUSIC_FILE=C:\\Users\\YourUser\\Music\\my-song.wav"
```

Укажите **свой** путь, затем полностью перезапустите Tomcat. Если он
управляется через NetBeans, может потребоваться перезапустить NetBeans,
чтобы сервер получил новые переменные окружения. Этот способ позволяет
менять музыку без пересборки WAR (путь остаётся тем же).

Оба варианта сохраняют учебную архитектуру:
`Swing-клиент → HTTP GET /MusicServlet → Tomcat → WAV → javax.sound.sampled.Clip`.

**Важно:** клиент использует стандартный `javax.sound.sampled` и ожидает
WAV с поддерживаемым аудиоформатом (лучше PCM WAV). MP3 без дополнительных
Java-библиотек может не воспроизводиться. Музыка, доступная только на
компьютере клиента, но не сервера Tomcat, не будет доступна сервлету.
**Личный музыкальный файл не загружайте в публичный GitHub без разрешения
правообладателя.** Файл `Lab3Server/web/WEB-INF/music.wav` игнорируется Git.

### 3. Собрать и развернуть WAR

1. Сделайте `git pull --ff-only origin main`.
2. В NetBeans закройте старый `Lab3Server` как Java SE-проект, если он ещё открыт.
   Через **File → Open Project** откройте `Lab3Server` заново: теперь там `pom.xml`,
   NetBeans должен распознать проект Maven WAR. Также откройте `Lab3Client`.
3. Правый клик на `Lab3Server` → **Clean and Build** (или `mvn clean package` внутри Lab3Server).
   Maven сам загрузит Servlet API для компиляции и JDBC-драйвер PostgreSQL для WAR.
4. Новый файл: `Lab3Server/target/Lab3Server.war`.
5. Скопируйте его в `<TOMCAT_HOME>\webapps\Lab3Server.war`, запустите Tomcat 11
   (`<TOMCAT_HOME>\bin\catalina.bat run`) и проверьте адреса ниже.
   Если Tomcat уже запущен, задеплойте обновлённый WAR с заменой старой версии.
6. Запустите `Lab3Client` (обычный Run). По умолчанию он обращается к
   `http://localhost:8080/Lab3Server`. Для другого контекста:
   **Lab3Client → Properties → Run → VM Options**:
   `-Dlab3.serverUrl=http://localhost:8080/ВАШ_КОНТЕКСТ`.

### Проверка

- `http://localhost:8080/Lab3Server/MyServlet?txt=cat&dir=en&format=text` → `кот`
- `http://localhost:8080/Lab3Server/MyServlet?txt=dog&dir=en&format=text` → `собака`
- `http://localhost:8080/Lab3Server/WordServlet` → DOCX
- `http://localhost:8080/Lab3Server/MusicServlet` → WAV
- `http://localhost:8080/Lab3Server/GoldServlet` → котировка или ошибка источника

Ошибка `HTTP 503` при переводе означает проблему с PostgreSQL, SQL-таблицей
или параметрами подключения. Проверяйте лог Tomcat.
Ошибка `HTTP 403` при получении золота от внешнего сайта **не означает**
ошибку Tomcat или JDBC: источники могут блокировать автоматические запросы.

## Код по классам

- `MyServlet.java`: читает параметры запроса и отправляет текст/HTML.
- `Dictionary.java`: `DriverManager` + `PreparedStatement` + `ResultSet`, только PostgreSQL.
- `WordServlet.java`: отдаёт оригинальный DOCX, клиент отображает содержимое справа.
- `GoldServlet.java`: передаёт результат `GoldQuoteService`, которому может мешать HTTP 403.
- `MusicServlet.java`: читает готовый WAV-файл и передаёт его по HTTP без генерации.

Примечание: здесь выбран простой JDBC по одному соединению на перевод,
без Tomcat JNDI DataSource или connection pool — для учебной работы этого достаточно.

**Статус проверки:** структура Maven и файлы проекта проверены; реальная
сборка WAR, запуск Tomcat и соединение с PostgreSQL должны быть проверены
локально на вашем компьютере.
