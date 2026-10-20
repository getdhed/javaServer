ЛАБОРАТОРНАЯ №3: JAVA HTTP + ЗОЛОТО COMEX + МУЗЫКА + DOCX
======================================================

В архиве ДВА проекта NetBeans Ant:
  Lab3Server — HTTP-сервер, сервлеты для Tomcat 9, парсер HTML котировок,
               генератор музыки WAV, Word-документ, словарь.
  Lab3Client — Swing-клиент: Translate, Reverse, Word, Gold (COMEX), Музыка.

ИСПОЛЬЗУЕМЫЕ ТЕХНОЛОГИИ
----------------------
Только Java (JDK 21+), Java Swing, HttpURLConnection, BufferedReader,
java.util.regex, javax.sound.sampled, стандартные ZIP/XML-парсеры JDK.
Формат обмена для золота: text/plain; для мелодии: audio/wav;
для DOCX: application/vnd.openxmlformats-officedocument.wordprocessingml.document.
В проекте НЕТ JavaScript, Python-скриптов и JSON.

ЗАПУСК БЕЗ TOMCAT (РЕКОМЕНДУЕТСЯ)
--------------------------------
1. Распакуйте архив.
2. В NetBeans: File -> Open Project -> lab3/Lab3Server и lab3/Lab3Client.
3. Остановите старый Lab3Server, если он всё ещё занимает порт 8080.
4. Запустите Lab3Server с главным классом ServerMain.
   Сервер должен вывести: Server started: http://localhost:8080/dictionary.html
5. Запустите Lab3Client с главным классом apacheclient.ApacheClient.
6. В поле введите "кот", нажмите Translate -> "cat".
7. В поле введите "dog", нажмите Reverse -> "собака".
8. Нажмите Word — DOCX загрузится с /WordServlet и отобразится справа
   в JEditorPane (НЕ в Microsoft Word).
9. Нажмите Gold (COMEX) — появится котировка золота в ПРЕЖНЕМ поле
   результата перевода с указанием источника, времени и единицы измерения.
10. Нажмите Музыка ▶ — клиент получит WAV по /MusicServlet и зациклит мелодию.
    Нажмите ту же кнопку (Музыка ■ Стоп) — воспроизведение прекратится.

ПРОВЕРКА HTTP НАПРЯМУЮ В БРАУЗЕРЕ
--------------------------------
http://localhost:8080/MyServlet?txt=cat&dir=en&format=text
http://localhost:8080/WordServlet
http://localhost:8080/GoldServlet
http://localhost:8080/MusicServlet

КОТИРОВКИ ЗОЛОТА: ОБНОВЛЁННЫЙ ИСТОЧНИК (ИСПРАВЛЕНИЕ 403)
--------------------------------------------------------
Причина прошлой ошибки: CME и eSignal вернули HTTP 403 — отказали
автоматическому Java-клиенту. Наш сервер корректно вернул HTTP 502.
Код не подставляет выдуманную цену и не обходит защиту сайтов.

1) Основной источник — открытая HTML-страница Google Finance:
   https://www.google.com/finance/quote/GCW00:COMEX?hl=en
   На ней показаны фьючерсы на золото GCW00 биржи COMEX.
   GoldQuoteService.java читает HTML по GET через HttpURLConnection,
   BufferedReader и извлекает цену регулярным выражением.
   Google Finance — отображающий котировку агрегатор, а не сайт биржи.
2) Резервный источник — Markets Insider:
   https://markets.businessinsider.com/commodities/gold-price
   Здесь опубликована СПОТОВАЯ цена золота, а не фьючерс COMEX.
   Если отработал резерв, клиент явно показывает слово СПОТОВАЯ.
3) Если обе HTML-страницы закрыты или поменяли HTML, возвращается 502.
   При отсутствии доступа в интернет подставной котировки НЕ будет.

Цена — USD за тройскую унцию (31.1034768 г). Временная отметка
«Время получения» относится к моменту HTTP-запроса, а НЕ моменту сделки.
Котировки могут быть задержаны; для гарантированного доступа нужен
разрешённый поставщиком канал рыночных данных.

Тестирование новых источников выполнено на локальных образцах HTML и HTTP.
Реальную доступность внешних сайтов необходимо проверить на ПК пользователя.
Требования методички: только Java, HTTP GET, чтение HTML; нет JS, JSON, Python.

МУЗЫКА — ПОЛНОСТЬЮ НА JAVA
-------------------------
MusicGenerator.java генерирует короткую мелодию как PCM и упаковывает
её в WAV через AudioSystem.write(), без готовых mp3 и внешних библиотек.
/MusicServlet возвращает WAV как audio/wav.
Клиент получает аудиопоток по HTTP, открывает Clip через AudioSystem,
запускает Clip.LOOP_CONTINUOUSLY. Повторное нажатие вызывает stop/close.
Для воспроизведения компьютер должен иметь доступное аудиоустройство.

КАКИЕ КЛАССЫ ЗА ЧТО ОТВЕЧАЮТ
----------------------------
Lab3Server/src/ServerMain.java — запускает Java HttpServer БЕЗ Tomcat;
                            маршруты /MyServlet, /WordServlet,
                            /GoldServlet, /MusicServlet.
Lab3Server/src/GoldQuoteService.java — HTTP GET, разбор HTML, цена GC.
Lab3Server/src/MusicGenerator.java — генерирует WAV мелодию.
Lab3Server/src/GoldServlet.java — настоящий HttpServlet для Tomcat.
Lab3Server/src/MusicServlet.java — настоящий HttpServlet для Tomcat.
Lab3Server/src/WordServlet.java — настоящий HttpServlet для Tomcat.
Lab3Server/src/MyServlet.java — настоящий HttpServlet для Tomcat.
Lab3Server/web/WEB-INF/web.xml — URL-маппинги Servlet API.
Lab3Server/web/WEB-INF/lab3-document.docx — исходный Word-файл.
Lab3Client/src/apacheclient/ApacheClient.java — Swing, HTTP GET,
                   кнопки золота, Word и включения/выключения музыки.
Lab3Client/src/apacheclient/DocxPreview.java — ZIP/XML -> HTML предпросмотр.

ВАЖНО: ServerMain использует java.net.httpserver.HttpHandler, а не Tomcat.
При таком запуске web.xml НЕ используется. Для настоящих Servlet классов
можно развернуть lab3/Lab3Server.war в Tomcat 9 (javax.servlet 4.0).
Tomcat 10 / GlassFish 7 используют jakarta.servlet — это несовместимый
с javax.servlet импорт; для них нужна миграция исходников.
Для Tomcat 9 выставьте клиенту VM option:
  -Dlab3.serverUrl=http://localhost:8080/Lab3Server

ЕСЛИ ПОРТ 8080 ЗАНЯТ
--------------------
В NetBeans -> Lab3Server -> Properties -> Run -> VM Options:
  -Dlab3.port=18080
В клиенте -> Properties -> Run -> VM Options:
  -Dlab3.serverUrl=http://localhost:18080

Учебная тема: HTTP GET, HttpURLConnection, BufferedReader, сервер и клиент
на Java, двоичные HTTP-ответы (DOCX/WAV), обработка HTTP-кодов.
