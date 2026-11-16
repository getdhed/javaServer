import java.sql.SQLException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * При старте Lab3Server делает реальную попытку подключения к PostgreSQL
 * и проверяет таблицу translations. Результат выводится в журналы Tomcat.
 * Ошибка БД не мешает работе других сервлетов.
 */
public class DbStartupListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        context.log("[LAB3-DB] STARTUP: " + Dictionary.diagnosticConfig());
        context.log("[LAB3-DB] STARTUP: пробуем подключиться к PostgreSQL и прочитать таблицу translations");
        try {
            long count = Dictionary.checkDatabase();
            context.log("[LAB3-DB] SUCCESS: PostgreSQL подключен, translations доступна, записей=" + count);
        } catch (SQLException ex) {
            context.log("[LAB3-DB] FAILED: SQLState=" + ex.getSQLState()
                    + ", errorCode=" + ex.getErrorCode() + ", message=" + ex.getMessage(), ex);
        }
    }
}
