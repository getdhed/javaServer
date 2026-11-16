import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Отдаёт готовый MP3 из WEB-INF. Никакой генерации и конвертации. */
public class MusicServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try (InputStream in = getServletContext().getResourceAsStream("/WEB-INF/music.mp3")) {
            if (in == null) {
                response.sendError(404, "Положите music.mp3 в Lab3Server/web/WEB-INF и пересоберите WAR");
                return;
            }
            response.setContentType("audio/mpeg");
            try (OutputStream out = response.getOutputStream()) {
                in.transferTo(out);
            }
        }
    }
}
