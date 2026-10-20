import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet-контейнер (Tomcat 9): выдаёт WAV по HTTP. */
public class MusicServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        byte[] wav = MusicGenerator.createWav();
        response.setContentType("audio/wav");
        response.setHeader("Content-Disposition", "inline; filename=lab3-melody.wav");
        response.setContentLength(wav.length);
        response.getOutputStream().write(wav);
    }
}
