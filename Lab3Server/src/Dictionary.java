import java.io.*;
import java.util.*;

public class Dictionary {

    static HashMap rusToEng = new HashMap();
    static HashMap engToRus = new HashMap();
    static boolean loaded = false;

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        try (InputStream in = new FileInputStream("web/WEB-INF/dictionary.txt")) {
            readWords(in);
        } catch (IOException e) {
            System.err.println("Ошибка чтения словаря: " + e.getMessage());
        }
    }

    public static synchronized void loadFromServlet(InputStream in) {
        if (loaded) {
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            return;
        }
        if (in == null) {
            System.err.println("WEB-INF/dictionary.txt не найден");
            return;
        }
        try (InputStream stream = in) {
            readWords(stream);
        } catch (IOException e) {
            System.err.println("Ошибка чтения словаря: " + e.getMessage());
        }
    }

    private static void readWords(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("-")) {
                    String[] parts = line.split("-", 2);
                    String ru = parts[0].trim().toLowerCase();
                    String en = parts[1].trim().toLowerCase();
                    rusToEng.put(ru, en);
                    engToRus.put(en, ru);
                }
            }
            loaded = true;
        }
    }

    public static String translate(String word, boolean reverse) {
        load();
        if (word == null) {
            return "";
        }
        word = word.trim().toLowerCase();
        if (word.equals("")) {
            return "";
        }
        if (reverse) {
            if (engToRus.containsKey(word)) {
                return (String) engToRus.get(word);
            }
        } else {
            if (rusToEng.containsKey(word)) {
                return (String) rusToEng.get(word);
            }
        }
        return "не найдено";
    }
}
