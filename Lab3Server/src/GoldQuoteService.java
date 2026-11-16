import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** Читаем цену фьючерса на золото из HTML-тега Google Finance. */
public final class GoldQuoteService {
    private static final String SITE =
            "https://www.google.com/finance/quote/GCW00:COMEX?hl=en";

    private GoldQuoteService() { }

    public static String getQuote() throws IOException {
        // Загружаем HTML-страницу, не JSON и не API.
        Document html = Jsoup.connect(SITE)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .timeout(12000)
                .get();

        // Цена находится в HTML-теге: <div class="YMlKec fxKbKc">...</div>
        Element priceTag = html.selectFirst("div.YMlKec.fxKbKc");
        if (priceTag == null) {
            throw new IOException("HTML-тег с ценой не найден");
        }

        String price = priceTag.text().trim();
        if (!price.matches("\\$[0-9][0-9,]*(?:\\.[0-9]+)?")) {
            throw new IOException("В HTML-теге нет корректной цены");
        }
        return "Google Finance (COMEX): " + price + " за тройскую унцию";
    }
}
