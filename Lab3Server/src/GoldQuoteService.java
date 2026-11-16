import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** Читаем спотовую цену золота из HTML-тега <p> на Kitco. */
public final class GoldQuoteService {
    private static final String SITE = "https://www.kitco.com/charts/gold";
    private static final Pattern PRICE = Pattern.compile(
            "The current price of gold is \\$([0-9,]+(?:\\.[0-9]{1,2})?) per troy ounce");

    private GoldQuoteService() { }

    public static String getQuote() throws IOException {
        Document html = Jsoup.connect(SITE)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .timeout(12000)
                .get();

        // Цена опубликована в одном из HTML-тегов <p>.
        for (Element p : html.select("p")) {
            Matcher matcher = PRICE.matcher(p.text());
            if (matcher.find()) {
                return "Kitco: золото — " + matcher.group(1)
                        + " USD за тройскую унцию";
            }
        }
        throw new IOException("Тег <p> с ценой золота на Kitco не найден");
    }
}
