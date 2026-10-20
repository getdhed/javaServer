import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class ServerMain {

    public static void main(String[] args) {
        try {
            Dictionary.load();
            int port = Integer.getInteger("lab3.port", 8080);
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/MyServlet", new ServletHandler());
            server.createContext("/WordServlet", new WordHandler());
            server.createContext("/GoldServlet", new GoldHandler());
            server.createContext("/MusicServlet", new MusicHandler());
            server.createContext("/dictionary.html", new PageHandler());
            server.createContext("/", new PageHandler());
            server.start();
            System.out.println("Server started: http://localhost:" + port + "/dictionary.html");
        } catch (Exception e) {
            System.out.println("Ошибка соединения+" + e);
        }
    }

    static HashMap parseQuery(String query) {
        HashMap map = new HashMap();
        if (query == null || query.equals("")) {
            return map;
        }
        try {
            String[] parts = query.split("&");
            for (int i = 0; i < parts.length; i++) {
                String[] kv = parts[i].split("=", 2);
                String key = URLDecoder.decode(kv[0], "UTF-8");
                String val = "";
                if (kv.length > 1) {
                    val = URLDecoder.decode(kv[1], "UTF-8");
                }
                map.put(key, val);
            }
        } catch (Exception e) {
            System.out.println("Ошибка  " + e);
        }
        return map;
    }

    static class ServletHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            HashMap p = parseQuery(ex.getRequestURI().getRawQuery());
            String word = "";
            if (p.get("txt") != null) {
                word = (String) p.get("txt");
            }
            String dir = "ru";
            if (p.get("dir") != null) {
                dir = (String) p.get("dir");
            }
            String format = "";
            if (p.get("format") != null) {
                format = (String) p.get("format");
            }
            boolean reverse = dir.equals("en");
            String trans = Dictionary.translate(word, reverse);
            byte[] data;
            String type;
            if (format.equals("text")) {
                type = "text/plain; charset=UTF-8";
                data = trans.getBytes("UTF-8");
            } else {
                type = "text/html; charset=UTF-8";
                String html = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">"
                        + "<title>Servlet MyServlet</title></head>"
                        + "<body bgcolor='#aaccff'>"
                        + "<Font color=\"green\" size=\"10\">Форма для работы со словарем</Font><br><br>"
                        + "<form name=\"frm\" method=\"Get\" action=\"MyServlet\">"
                        + "<Font color=\"blue\" size=\"6\"> Введите русское слово:</Font>"
                        + "<Input type=\"Text\" name=\"txt\" value=\"" + word + "\"/>"
                        + "<input type=\"hidden\" name=\"dir\" value=\"ru\"/><br><br>"
                        + "<Font color=\"blue\" size=\"6\">Перевод: </Font>"
                        + "<input type=\"text\" name=\"trans\" value=\"" + trans + "\"/><br>"
                        + "<h4>Кликни здесь для получения перевода :<Input type=\"submit\" value=\"Перевести\"/></h4>"
                        + "</form>"
                        + "<form name=\"frm2\" method=\"Get\" action=\"MyServlet\">"
                        + "<Font color=\"blue\" size=\"6\"> Введите английское слово:</Font>"
                        + "<Input type=\"Text\" name=\"txt\" value=\"\"/>"
                        + "<input type=\"hidden\" name=\"dir\" value=\"en\"/>"
                        + "<h4>Обратный перевод :<Input type=\"submit\" value=\"Обратный перевод\"/></h4>"
                        + "</form>"
                        + "<br><a href=\"WordServlet\">Документ Word (вариант 4)</a>"
                        + "</body></html>";
                data = html.getBytes("UTF-8");
            }
            ex.getResponseHeaders().set("Content-Type", type);
            ex.sendResponseHeaders(200, data.length);
            OutputStream os = ex.getResponseBody();
            os.write(data);
            os.close();
        }
    }

    // Самостоятельный HTTP-сервер: доступ к Word без Tomcat.
    // Настоящий HttpServlet для развёртывания в Tomcat находится в WordServlet.java.
    static class WordHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            File file = new File("web/WEB-INF/lab3-document.docx");
            if (!file.isFile()) {
                byte[] error = "Word document not found".getBytes("UTF-8");
                ex.sendResponseHeaders(404, error.length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(error);
                }
                return;
            }
            byte[] data = Files.readAllBytes(file.toPath());
            ex.getResponseHeaders().set("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"lab3-document.docx\"");
            ex.sendResponseHeaders(200, data.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(data);
            }
        }
    }

    // Ровно те же HTTP-маршруты, что у классов GoldServlet / MusicServlet.
    // Для самостоятельного запуска ServerMain Servlet Container не нужен.
    static class GoldHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                sendText(ex, 405, "Только HTTP GET");
                return;
            }
            try {
                sendText(ex, 200, GoldQuoteService.getQuote());
            } catch (IOException err) {
                sendText(ex, 502, "Не удалось загрузить котировку: " + err.getMessage());
            }
        }
    }

    static class MusicHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                sendText(ex, 405, "Только HTTP GET");
                return;
            }
            byte[] audio = MusicGenerator.createWav();
            ex.getResponseHeaders().set("Content-Type", "audio/wav");
            ex.getResponseHeaders().set("Content-Disposition", "inline; filename=lab3-melody.wav");
            ex.sendResponseHeaders(200, audio.length);
            try (OutputStream out = ex.getResponseBody()) {
                out.write(audio);
            }
        }
    }

    static void sendText(HttpExchange ex, int status, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        ex.sendResponseHeaders(status, data.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(data);
        }
    }

    static class PageHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            File file = new File("web/dictionary.html");
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            ex.sendResponseHeaders(200, data.length);
            OutputStream os = ex.getResponseBody();
            os.write(data);
            os.close();
        }
    }
}
