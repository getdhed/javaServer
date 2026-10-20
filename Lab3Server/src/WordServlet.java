import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Вариант 4: сервлет отдаёт файл Word клиенту по HTTP. */
public class WordServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // WEB-INF не доступен браузеру напрямую, но доступен сервлету.
        try (InputStream in = getServletContext()
                .getResourceAsStream("/WEB-INF/lab3-document.docx")) {
            if (in == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Word document not found");
                return;
            }
            response.setContentType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setHeader("Content-Disposition", "attachment; filename=\"lab3-document.docx\"");
            try (OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
            }
        }
    }
}
