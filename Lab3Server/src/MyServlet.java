import java.io.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MyServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String rus_word = "" + request.getParameter("txt");
        String dir = "" + request.getParameter("dir");
        String format = "" + request.getParameter("format");
        boolean reverse = dir.equals("en");
        String trans;
        try {
            trans = Dictionary.translate(rus_word, reverse);
        } catch (java.sql.SQLException ex) {
            getServletContext().log("Ошибка словаря PostgreSQL", ex);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "PostgreSQL недоступен: проверьте настройки подключения и JDBC-драйвер");
            return;
        }

        if (format.equals("text")) {
            out.print(trans);
            return;
        }

        out.println("<html>");
        out.println("<head>");
        out.println("<title>Servlet MyServlet</title>");
        out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
        out.println("</head>");
        out.println("<body bgcolor='#aaccff'>");
        out.println("<Font color=\"green\" size=\"10\">");
        out.println("Форма для работы со словарем");
        out.println("</Font><br><br>");
        out.println("<form name=\"frm\" method=\"Get\" action=\"MyServlet\">");
        out.println("<Font color=\"blue\" size=\"6\"> Введите русское слово:</Font>");
        out.println("<Input type=\"Text\" name=\"txt\" value=\"" + rus_word + "\"/>");
        out.println("<input type=\"hidden\" name=\"dir\" value=\"ru\"/>");
        out.println("<br><br>");
        out.println("<Font color=\"blue\" size=\"6\">Перевод: </Font>");
        out.println("<input type=\"text\" name=\"trans\" value=\"" + trans + "\"/><br>");
        out.println("<h4>Кликни здесь для получения перевода :<Input type=\"submit\" value=\"Перевести\"/></h4>");
        out.println("</form>");
        out.println("<form name=\"frm2\" method=\"Get\" action=\"MyServlet\">");
        out.println("<Font color=\"blue\" size=\"6\"> Введите английское слово:</Font>");
        out.println("<Input type=\"Text\" name=\"txt\" value=\"\"/>");
        out.println("<input type=\"hidden\" name=\"dir\" value=\"en\"/>");
        out.println("<h4>Обратный перевод :<Input type=\"submit\" value=\"Обратный перевод\"/></h4>");
        out.println("</form>");
        out.println("<br><a href=\"WordServlet\">Скачать документ Word (вариант 4)</a>");
        out.println("</body>");
        out.println("</html>");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
