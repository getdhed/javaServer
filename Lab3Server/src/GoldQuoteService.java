import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Получение одной цены фьючерса на золото с Google Finance. */
public final class GoldQuoteService {
    private static final String SITE =
            "https://www.google.com/finance/quote/GCW00:COMEX?hl=en";

    private GoldQuoteService() { }

    public static String getQuote() throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(SITE).openConnection();
        con.setConnectTimeout(7000);
        con.setReadTimeout(8500);
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        try {
            if (con.getResponseCode() != 200) {
                throw new IOException("Google Finance вернул HTTP " + con.getResponseCode());
            }

            // Получаем HTML страницы.
            StringBuilder html = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    html.append(line).append(' ');
                    if (html.length() > 2_000_000) {
                        throw new IOException("HTML слишком большой");
                    }
                }
            }

            // Убираем HTML-теги и находим число после Gold $.
            String text = html.toString()
                    .replaceAll("(?is)<script\\b[^>]*>.*?</script>", " ")
                    .replaceAll("(?is)<style\\b[^>]*>.*?</style>", " ")
                    .replaceAll("(?s)<[^>]+>", " ")
                    .replace("&nbsp;", " ")
                    .replace('\u00a0', ' ')
                    .replaceAll("\\s+", " ");
            if (!text.contains("GCW00:COMEX")) {
                throw new IOException("Не удалось распознать котировку GCW00");
            }
            Matcher price = Pattern.compile("Gold\\s*\\$\\s*([0-9][0-9,.]*)").matcher(text);
            if (!price.find()) {
                throw new IOException("Цена не найдена на Google Finance");
            }

            return "Google Finance (фьючерс COMEX): "
                    + price.group(1) + " USD за тройскую унцию";
        } finally {
            con.disconnect();
        }
    }
}
