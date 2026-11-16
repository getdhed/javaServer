import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** PostgreSQL-backed dictionary. No in-memory word list or fallback data. */
public final class Dictionary {
    private Dictionary() { }

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static Connection connect() throws SQLException {
        // Tomcat инициализирует DriverManager до загрузки WEB-INF/lib.
        // Поэтому JDBC-драйвер из WAR нужно загрузить явно.
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException(
                    "JDBC-драйвер PostgreSQL не найден: проверьте WEB-INF/lib/postgresql-*.jar в развёрнутом WAR",
                    "08001", ex);
        }
        String url = setting("LAB3_DB_URL", "jdbc:postgresql://localhost:5432/lab3_dictionary");
        String user = setting("LAB3_DB_USER", "lab3_user");
        String password = System.getenv("LAB3_DB_PASSWORD");
        if (password == null || password.isEmpty()) {
            throw new SQLException("Не задан LAB3_DB_PASSWORD (пароль PostgreSQL)");
        }
        DriverManager.setLoginTimeout(5);
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Диагностика конфигурации без вывода пароля.
     * В частности, показывает, получил ли Tomcat LAB3_DB_PASSWORD.
     */
    public static String diagnosticConfig() {
        String configuredUrl = System.getenv("LAB3_DB_URL");
        String configuredUser = System.getenv("LAB3_DB_USER");
        String password = System.getenv("LAB3_DB_PASSWORD");

        String url = setting("LAB3_DB_URL", "jdbc:postgresql://localhost:5432/lab3_dictionary");
        // На случай, если пароль ошибочно передали параметром URL.
        String safeUrl = url.replaceAll("(?i)(password=)[^;&]+", "$1***");

        return "url=" + safeUrl
                + " (" + (configuredUrl == null || configuredUrl.isBlank() ? "DEFAULT" : "ENV") + ")"
                + ", user=" + setting("LAB3_DB_USER", "lab3_user")
                + " (" + (configuredUser == null || configuredUser.isBlank() ? "DEFAULT" : "ENV") + ")"
                + ", LAB3_DB_PASSWORD=" + (password == null || password.isBlank() ? "MISSING" : "SET");
    }

    /** Проверяет не только соединение с PostgreSQL, но и наличие таблицы translations. */
    public static long checkDatabase() throws SQLException {
        try (Connection conn = connect();
             PreparedStatement statement = conn.prepareStatement("SELECT COUNT(*) FROM translations")) {
            statement.setQueryTimeout(8);
            try (ResultSet results = statement.executeQuery()) {
                results.next();
                return results.getLong(1);
            }
        }
    }

    public static String translate(String word, boolean reverse) throws SQLException {
        if (word == null || word.trim().isEmpty()) return "";
        String sql = reverse
                ? "SELECT word_ru FROM translations WHERE word_en = LOWER(?)"
                : "SELECT word_en FROM translations WHERE word_ru = LOWER(?)";
        try (Connection conn = connect();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, word.trim());
            statement.setQueryTimeout(8);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? results.getString(1) : "не найдено";
            }
        }
    }
}
