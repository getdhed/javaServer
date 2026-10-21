import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Перевод слов из PostgreSQL. База доступна только серверу, не Swing-клиенту.
 * Данные больше НЕ читаются из dictionary.txt или HashMap.
 */
public final class Dictionary {

    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/lab3_dictionary";

    private Dictionary() { }

    private static String setting(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Connection connect() throws SQLException {
        String url = setting("LAB3_DB_URL", DEFAULT_URL);
        String user = setting("LAB3_DB_USER", "postgres");
        String password = System.getenv("LAB3_DB_PASSWORD");
        // JDBC-драйвер PostgreSQL должен быть в classpath (lib/postgresql-jdbc.jar).
        DriverManager.setLoginTimeout(5);
        return DriverManager.getConnection(url, user, password == null ? "" : password);
    }

    public static void checkConnection() throws SQLException {
        try (Connection ignored = connect()) {
            // Служит для диагностики запуска; таблица проверяется при запросе перевода.
        }
    }

    public static String translate(String word, boolean reverse) throws SQLException {
        if (word == null || word.trim().isEmpty()) {
            return "";
        }
        String normalized = word.trim().toLowerCase(Locale.ROOT);
        String sql = reverse
                ? "SELECT russian_word FROM translations WHERE english_word = ?"
                : "SELECT english_word FROM translations WHERE russian_word = ?";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalized);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? results.getString(1) : "не найдено";
            }
        }
    }
}
