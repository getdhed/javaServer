import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Отдаёт настоящий WAV-файл без генерации звука.
 * 1. Путь можно указать в -Dlab3.musicFile=... или LAB3_MUSIC_FILE.
 * 2. Иначе используется ресурс /WEB-INF/music.wav, включённый в WAR.
 * Файл должен находиться на той машине, где запущен Tomcat.
 */
public class MusicServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String musicPath = System.getProperty("lab3.musicFile");
        if (musicPath == null || musicPath.isBlank()) {
            musicPath = System.getenv("LAB3_MUSIC_FILE");
        }

        InputStream source;
        if (musicPath != null && !musicPath.isBlank()) {
            try {
                Path file = Paths.get(musicPath);
                if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".wav")) {
                    response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                            "Нужен WAV-файл: стандартный Java Sound не поддерживает MP3 без дополнительного декодера.");
                    return;
                }
                if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND,
                            "Музыкальный файл не найден или недоступен для чтения на сервере");
                    return;
                }
                source = Files.newInputStream(file);
            } catch (InvalidPathException error) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Некорректный путь к WAV");
                return;
            }
        } else {
            source = getServletContext().getResourceAsStream("/WEB-INF/music.wav");
            if (source == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Положите music.wav в Lab3Server/web/WEB-INF и пересоберите WAR или настройте LAB3_MUSIC_FILE");
                return;
            }
        }

        response.setContentType("audio/wav");
        response.setHeader("Content-Disposition", "inline; filename=\"music.wav\"");
        try (InputStream in = source; OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
        }
    }
}
