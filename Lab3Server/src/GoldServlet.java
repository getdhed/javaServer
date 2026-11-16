import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Servlet-контейнер (Tomcat 11 / GlassFish 8): HTTP GET /GoldServlet. */
public class GoldServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/plain; charset=UTF-8");
        try {
            byte[] body = GoldQuoteService.getQuote().getBytes(StandardCharsets.UTF_8);
            response.getOutputStream().write(body);
        } catch (IOException ex) {
            getServletContext().log("[LAB3-GOLD] Ошибка получения цены: " + ex.getMessage(), ex);
            // Текст ошибки вместо HTML-страницы Tomcat.
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.getWriter().print("Kitco недоступен: " + ex.getMessage());
        }
    }
}
