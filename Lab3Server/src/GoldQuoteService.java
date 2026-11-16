import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Берём последнюю доступную котировку золота XAU/USD из CSV Stooq.
 * В отличие от HTML сайта, у CSV есть отдельный столбец Close.
 */
public final class GoldQuoteService {
    private static final String SITE =
            "https://stooq.com/q/l/?s=xauusd&f=sd2t2ohlcv&h&e=csv";

    private GoldQuoteService() { }

    public static String getQuote() throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(SITE).openConnection();
        con.setConnectTimeout(7000);
        con.setReadTimeout(8500);
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        try {
            if (con.getResponseCode() != 200) {
                throw new IOException("Stooq вернул HTTP " + con.getResponseCode());
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                return parseQuote(reader.readLine(), reader.readLine());
            }
        } finally {
            con.disconnect();
        }
    }

    /** Первая строка — заголовок CSV, вторая — значения котировки. */
    static String parseQuote(String header, String row) throws IOException {
        if (header == null || row == null) {
            throw new IOException("Stooq не вернул таблицу с котировкой");
        }
        String[] names = header.split(",", -1);
        String[] values = row.split(",", -1);
        int closeIndex = -1;
        int dateIndex = -1;
        for (int i = 0; i < names.length; i++) {
            if (names[i].trim().equalsIgnoreCase("Close")) closeIndex = i;
            if (names[i].trim().equalsIgnoreCase("Date")) dateIndex = i;
        }
        if (closeIndex < 0 || closeIndex >= values.length) {
            throw new IOException("Stooq вернул не CSV с колонкой Close");
        }

        String price = values[closeIndex].trim();
        if (!price.matches("[0-9]+(?:\\.[0-9]+)?")) {
            throw new IOException("Нет доступной котировки золота (Close=" + price + ")");
        }
        String date = dateIndex >= 0 && dateIndex < values.length
                ? values[dateIndex].trim() : "";
        String suffix = date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")
                ? " (дата: " + date + ")" : "";
        return "Stooq: золото — " + price + " USD за тройскую унцию" + suffix;
    }
}
