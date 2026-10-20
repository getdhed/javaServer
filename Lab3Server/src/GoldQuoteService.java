import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Учебная работа: GET сайта, проверка HTTP-кода, чтение HTML через BufferedReader.
 * Без JavaScript, JSON, Python, дополнительных библиотек и обхода защиты сайтов.
 *
 * Google Finance отображает цену фьючерса GCW00 на бирже COMEX.
 * Резервный источник Markets Insider отображает СПОТОВУЮ цену золота.
 * Эти два инструмента не идентичны! Различие отражено в ответе сервера.
 */
public final class GoldQuoteService {
    private static final String GOOGLE_FINANCE_URL =
            "https://www.google.com/finance/quote/GCW00:COMEX?hl=en";
    private static final String MARKETS_INSIDER_URL =
            "https://markets.businessinsider.com/commodities/gold-price";
    private static final int MAX_HTML = 2_000_000;
    private static final Pattern GOOGLE_PRICE = Pattern.compile(
            "(?is)GCW00:COMEX.{0,500}?\\bGold\\s*\\$\\s*([0-9][0-9,.]*)");
    private static final Pattern INSIDER_PRICE = Pattern.compile(
            "(?is)\\bGold\\s+Price\\s+([0-9][0-9,.]*)\\s+[+\\-]?[0-9][0-9,.]*\\s+[+\\-]?[0-9.,]+%");

    private GoldQuoteService() { }

    public static String getQuote() throws IOException {
        StringBuilder errors = new StringBuilder();
        String googleUrl = System.getProperty("lab3.gold.primaryUrl", GOOGLE_FINANCE_URL);
        try {
            String price = parseGoogleFinance(getHtml(googleUrl));
            return renderPrice(price,
                    "Золото — фьючерс GCW00 (COMEX)",
                    "Google Finance (данные COMEX)", googleUrl,
                    "Биржевые данные могут публиковаться с задержкой.");
        } catch (IOException e) {
            errors.append("Google Finance: ").append(e.getMessage()).append("; ");
        }

        String insiderUrl = System.getProperty("lab3.gold.backupUrl", MARKETS_INSIDER_URL);
        try {
            String price = parseMarketsInsider(getHtml(insiderUrl));
            return renderPrice(price,
                    "Золото — СПОТОВАЯ цена (не фьючерс COMEX!)",
                    "Markets Insider (Gold Spot)", insiderUrl,
                    "Резервный источник: это спотовая цена, она может отличаться от GCW00 COMEX. "
                    + "Время публикации смотрите на сайте источника.");
        } catch (IOException e) {
            errors.append("Markets Insider: ").append(e.getMessage());
        }
        throw new IOException("Не удалось прочитать котировку с открытых HTML-страниц. "
                + errors + ". Внешние сайты могут блокировать HTTP-клиентов (403), "
                + "поэтому гарантировать доступ без официального API нельзя.");
    }

    static String parseGoogleFinance(String html) throws IOException {
        String plain = plainText(html);
        if (!plain.contains("GCW00:COMEX") || !plain.contains("COMEX")) {
            throw new IOException("страница фьючерса GCW00:COMEX не распознана");
        }
        Matcher matcher = GOOGLE_PRICE.matcher(plain);
        if (!matcher.find()) {
            throw new IOException("на странице нет цены GCW00 в открытом HTML");
        }
        return validatePrice(matcher.group(1));
    }

    static String parseMarketsInsider(String html) throws IOException {
        String plain = plainText(html);
        if (!plain.contains("Gold Snapshot") || !plain.contains("Gold")) {
            throw new IOException("страница спотовой цены не распознана");
        }
        Matcher matcher = INSIDER_PRICE.matcher(plain);
        if (!matcher.find()) {
            throw new IOException("на странице нет спотовой цены в открытом HTML");
        }
        return validatePrice(matcher.group(1));
    }

    private static String validatePrice(String raw) throws IOException {
        // Проверяем формат: 4181.20 или 4,181.20, затем разумный диапазон.
        if (!raw.matches("[0-9]{1,6}(?:,[0-9]{3})*(?:\\.[0-9]{1,4})?")) {
            throw new IOException("неожиданный формат цены: " + raw);
        }
        try {
            double number = Double.parseDouble(raw.replace(",", ""));
            if (!Double.isFinite(number) || number < 100 || number > 100000) {
                throw new IOException("цена вне допустимого диапазона: " + raw);
            }
        } catch (NumberFormatException e) {
            throw new IOException("некорректная цена: " + raw, e);
        }
        return raw;
    }

    private static String renderPrice(String raw, String instrument, String source,
                                      String url, String note) throws IOException {
        double price = Double.parseDouble(validatePrice(raw).replace(",", ""));
        return instrument + "\n"
                + "Последняя показанная цена: " + raw + " USD / тройская унция\n"
                + String.format(Locale.US, "Пересчёт на 1 грамм: %.2f USD (расчётный)\n",
                        price / 31.1034768)
                + "Источник: " + source + "\n"
                + "Время получения: " + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")) + "\n"
                + note + "\n"
                + "Ссылка: " + url;
    }

    /** Стандартный учебный HTTP GET: проверяем статус, читаем HTML с ограничением. */
    private static String getHtml(String address) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(address).openConnection();
        con.setConnectTimeout(7000);
        con.setReadTimeout(8500);
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
        con.setRequestProperty("Accept", "text/html,application/xhtml+xml");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        con.setInstanceFollowRedirects(true);
        try {
            int status = con.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + status);
            }
            StringBuilder html = new StringBuilder();
            try (InputStream in = con.getInputStream();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(in, StandardCharsets.UTF_8))) {
                char[] buffer = new char[8192];
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    html.append(buffer, 0, n);
                    if (html.length() > MAX_HTML) {
                        throw new IOException("HTML-страница слишком большая");
                    }
                }
            }
            return html.toString();
        } finally {
            con.disconnect();
        }
    }

    static String plainText(String html) {
        return html.replaceAll("(?is)<script\\b[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style\\b[^>]*>.*?</style>", " ")
                .replaceAll("(?s)<[^>]+>", " ")
                .replace("&nbsp;", " ").replace("&#160;", " ")
                .replace("&amp;", "&").replace("&#36;", "$")
                .replace('\u00a0', ' ')
                .replaceAll("\\s+", " ").trim();
    }
}
