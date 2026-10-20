package apacheclient;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/**
 * Клиент: словарь слева, встроенный просмотр DOCX справа.
 * Word НЕ запускается: документ скачивается по HTTP и отображается в JEditorPane.
 */
public class ApacheClient extends JFrame {

    // Для Tomcat 9: -Dlab3.serverUrl=http://localhost:8080/Lab3Server
    private static final String BASE_URL = System.getProperty(
            "lab3.serverUrl", "http://localhost:8080");

    private final JTextField wordField = new JTextField();
    private final JTextArea resultArea = new JTextArea(5, 25);
    private final JButton translateButton = new JButton("Translate");
    private final JButton reverseButton = new JButton("Reverse");
    private final JButton wordButton = new JButton("Word");
    private final JButton goldButton = new JButton("Gold");
    private final JButton musicButton = new JButton("Музыка ▶");
    private Clip musicClip;
    private final JLabel status = new JLabel("Введите слово или откройте Word-документ с сервера.");
    private final JEditorPane documentView = new JEditorPane();

    public ApacheClient() {
        super("Лабораторная №3 — HTTP, DOCX, котировки и музыка");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(930, 590));
        setSize(1170, 710);
        setLocationRelativeTo(null);

        Color green = new Color(150, 200, 100);
        JPanel controls = new JPanel(new GridBagLayout());
        controls.setBackground(green);
        controls.setBorder(new EmptyBorder(22, 20, 20, 20));
        controls.setPreferredSize(new Dimension(390, 590));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 10, 0);

        JLabel title = new JLabel("Словарь");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        controls.add(title, c);

        c.gridy++;
        JLabel hint = new JLabel("Введите слово для перевода:");
        controls.add(hint, c);

        c.gridy++;
        wordField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        controls.add(wordField, c);

        c.gridy++;
        JLabel resultLabel = new JLabel("Результат:");
        c.insets = new Insets(12, 0, 5, 0);
        controls.add(resultLabel, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 10, 0);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setFont(new Font("SansSerif", Font.PLAIN, 15));
        JScrollPane resultScroll = new JScrollPane(resultArea);
        resultScroll.setPreferredSize(new Dimension(340, 180));
        resultScroll.setMinimumSize(new Dimension(300, 155));
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0.3;
        controls.add(resultScroll, c);

        c.gridy++;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        c.weightx = 0.33;
        c.insets = new Insets(4, 0, 8, 4);
        controls.add(translateButton, c);
        c.gridx = 1;
        controls.add(reverseButton, c);
        c.gridx = 2;
        c.insets = new Insets(4, 0, 8, 0);
        controls.add(wordButton, c);

        // Второй ряд кнопок: биржевая цена золота и музыка (старт/стоп).
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weightx = 0.67;
        c.insets = new Insets(4, 0, 8, 4);
        controls.add(goldButton, c);
        c.gridx = 2;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        c.weightx = 0.33;
        c.insets = new Insets(4, 0, 8, 0);
        controls.add(musicButton, c);

        c.gridx = 0;
        c.gridwidth = 3;
        c.gridy++;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(16, 0, 6, 0);
        JLabel details = new JLabel("<html><b>Word</b> загружает настоящий DOCX<br>" +
                "и показывает его справа, без Word.<br>"
                + "<b>Gold</b> читает HTML Google Finance / Markets Insider.<br>"
                + "<b>Музыка</b> скачивает WAV и запускает/останавливает его.</html>");
        controls.add(details, c);

        c.gridy++;
        c.weighty = 0;
        c.insets = new Insets(4, 0, 0, 0);
        JButton exit = new JButton("Exit");
        controls.add(exit, c);

        JPanel preview = new JPanel(new BorderLayout(0, 10));
        preview.setBorder(new EmptyBorder(20, 20, 15, 20));
        preview.setBackground(new Color(242, 246, 242));

        JLabel documentTitle = new JLabel("Предпросмотр DOCX");
        documentTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        preview.add(documentTitle, BorderLayout.NORTH);

        documentView.setEditable(false);
        documentView.setContentType("text/html");
        documentView.setText("<html><body style='font-family:SansSerif; padding:16px;'>" +
                "<h2>Документ ещё не загружен</h2><p>Нажмите кнопку <b>Word</b> слева, " +
                "чтобы получить DOCX с сервера и увидеть его содержимое здесь.</p></body></html>");
        documentView.setCaretPosition(0);
        JScrollPane documentScroll = new JScrollPane(documentView);
        documentScroll.setBorder(BorderFactory.createLineBorder(new Color(201, 213, 201)));
        documentScroll.getVerticalScrollBar().setUnitIncrement(18);
        preview.add(documentScroll, BorderLayout.CENTER);

        JPanel lower = new JPanel(new BorderLayout());
        lower.setBorder(new EmptyBorder(8, 12, 8, 12));
        lower.add(status);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controls, preview);
        split.setResizeWeight(0.33);
        split.setDividerLocation(405);
        split.setContinuousLayout(true);
        getContentPane().setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);
        add(lower, BorderLayout.SOUTH);

        translateButton.addActionListener(e -> translate("ru"));
        reverseButton.addActionListener(e -> translate("en"));
        wordButton.addActionListener(e -> showWord());
        goldButton.addActionListener(e -> showGold());
        musicButton.addActionListener(e -> toggleMusic());
        exit.addActionListener(e -> dispose());
        wordField.addActionListener(e -> translate("ru"));
    }

    private void translate(String direction) {
        String word = wordField.getText().trim();
        if (word.isEmpty()) {
            resultArea.setText("Введите слово в поле выше.");
            return;
        }
        translateButton.setEnabled(false);
        reverseButton.setEnabled(false);
        status.setText("Получаем перевод с сервера…");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                String q = URLEncoder.encode(word, "UTF-8");
                URL url = new URL(BASE_URL + "/MyServlet?txt=" + q
                        + "&dir=" + direction + "&format=text");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(10000);
                try {
                    if (connection.getResponseCode() != 200) {
                        throw new IOException("HTTP " + connection.getResponseCode());
                    }
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                            connection.getInputStream(), "UTF-8"))) {
                        String line = reader.readLine();
                        return line == null ? "" : line;
                    }
                } finally {
                    connection.disconnect();
                }
            }

            @Override
            protected void done() {
                translateButton.setEnabled(true);
                reverseButton.setEnabled(true);
                try {
                    resultArea.setText(("ru".equals(direction) ? "Перевод: " : "Обратный перевод: ")
                            + get());
                    status.setText("Перевод получен.");
                } catch (Exception ex) {
                    resultArea.setText("Ошибка перевода: " + errorMessage(ex));
                    status.setText("Не удалось получить перевод.");
                }
            }
        }.execute();
    }

    private void showWord() {
        wordButton.setEnabled(false);
        status.setText("Загружаем DOCX с сервера…");
        new SwingWorker<PreviewResult, Void>() {
            @Override
            protected PreviewResult doInBackground() throws Exception {
                URL url = new URL(BASE_URL + "/WordServlet");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(10000);
                connection.setRequestMethod("GET");
                try {
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        throw new IOException("Сервер вернул HTTP " + connection.getResponseCode());
                    }
                    // Сохраняем именно оригинальный DOCX, а не текстовый суррогат.
                    File wordFile = File.createTempFile("Lab3_Word_", ".docx",
                            new File(System.getProperty("user.home")));
                    try (InputStream in = connection.getInputStream()) {
                        Files.copy(in, wordFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    String html = DocxPreview.fromFile(wordFile);
                    return new PreviewResult(wordFile, html);
                } finally {
                    connection.disconnect();
                }
            }

            @Override
            protected void done() {
                wordButton.setEnabled(true);
                try {
                    PreviewResult result = get();
                    documentView.setText(result.html);
                    documentView.setCaretPosition(0);
                    resultArea.setText("DOCX получен и показан справа.\nФайл: "
                            + result.file.getAbsolutePath());
                    status.setText("DOCX загружен с /WordServlet. Просмотр внутри приложения.");
                } catch (Exception ex) {
                    status.setText("Ошибка загрузки DOCX.");
                    JOptionPane.showMessageDialog(ApacheClient.this,
                            "Не удалось показать документ:\n" + errorMessage(ex),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /** Загрузка котировки без блокировки EDT. Результат идёт в прежнее поле перевода. */
    private void showGold() {
        goldButton.setEnabled(false);
        resultArea.setText("Загружаем цену фьючерса на золото с сайта биржи…");
        status.setText("HTTP GET /GoldServlet → HTML источника → цена золота");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                URL url = new URL(BASE_URL + "/GoldServlet");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                // Сервер может сходить на два внешних сайта, каждый с таймаутом.
                connection.setReadTimeout(30000);
                connection.setRequestMethod("GET");
                try {
                    int code = connection.getResponseCode();
                    InputStream stream = code == 200
                            ? connection.getInputStream() : connection.getErrorStream();
                    if (stream == null) {
                        throw new IOException("HTTP " + code);
                    }
                    StringBuilder result = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(stream, "UTF-8"))) {
                        String line;
                        while ((line = reader.readLine()) != null && result.length() < 8192) {
                            result.append(line).append('\n');
                        }
                    }
                    if (code != 200) {
                        throw new IOException("HTTP " + code + ": " + result);
                    }
                    return result.toString().trim();
                } finally {
                    connection.disconnect();
                }
            }

            @Override
            protected void done() {
                goldButton.setEnabled(true);
                try {
                    resultArea.setText(get());
                    resultArea.setCaretPosition(0);
                    status.setText("Котировка запрошена: COMEX, USD за тройскую унцию.");
                } catch (Exception ex) {
                    resultArea.setText("Ошибка получения золота: " + errorMessage(ex));
                    status.setText("Не удалось прочитать HTML биржевого сайта.");
                }
            }
        }.execute();
    }

    /** Один переключатель: скачать WAV по HTTP и играть по кругу / остановить. */
    private void toggleMusic() {
        if (musicClip != null && musicClip.isOpen()) {
            stopMusic();
            status.setText("Воспроизведение остановлено.");
            return;
        }
        musicButton.setEnabled(false);
        status.setText("Загружаем WAV с сервера…");
        new SwingWorker<Clip, Void>() {
            @Override
            protected Clip doInBackground() throws Exception {
                URL url = new URL(BASE_URL + "/MusicServlet");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(12000);
                connection.setRequestMethod("GET");
                try {
                    if (connection.getResponseCode() != 200) {
                        throw new IOException("MusicServlet вернул HTTP "
                                + connection.getResponseCode());
                    }
                    try (InputStream in = new BufferedInputStream(connection.getInputStream());
                         AudioInputStream sound = AudioSystem.getAudioInputStream(in)) {
                        Clip clip = AudioSystem.getClip();
                        try {
                            clip.open(sound);
                            return clip;
                        } catch (Exception ex) {
                            clip.close();
                            throw ex;
                        }
                    }
                } finally {
                    connection.disconnect();
                }
            }

            @Override
            protected void done() {
                musicButton.setEnabled(true);
                try {
                    musicClip = get();
                    musicClip.setFramePosition(0);
                    musicClip.loop(Clip.LOOP_CONTINUOUSLY);
                    musicButton.setText("Музыка ■ Стоп");
                    status.setText("Играет WAV, полученный по HTTP. Повторное нажатие — стоп.");
                } catch (Exception ex) {
                    status.setText("Не удалось воспроизвести WAV.");
                    resultArea.setText("Ошибка музыки: " + errorMessage(ex));
                }
            }
        }.execute();
    }

    private void stopMusic() {
        if (musicClip != null) {
            musicClip.stop();
            musicClip.close();
            musicClip = null;
        }
        musicButton.setText("Музыка ▶");
    }

    @Override
    public void dispose() {
        stopMusic();
        super.dispose();
    }

    private static String errorMessage(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }

    private static final class PreviewResult {
        final File file;
        final String html;
        PreviewResult(File file, String html) {
            this.file = file;
            this.html = html;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ApacheClient().setVisible(true));
    }
}
