import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet-контейнер (Tomcat 9): HTTP GET /GoldServlet. */
public class GoldServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/plain; charset=UTF-8");
        try {
            byte[] body = GoldQuoteService.getQuote().getBytes(StandardCharsets.UTF_8);
            response.getOutputStream().write(body);
        } catch (IOException ex) {
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY, ex.getMessage());
        }
    }
}
