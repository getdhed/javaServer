import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/** Генерирует короткую мелодию WAV целиком средствами Java, без файлов и библиотек. */
public final class MusicGenerator {
    private static final int RATE = 22050;
    private MusicGenerator() { }

    public static byte[] createWav() throws IOException {
        // Ноты в MIDI-номерах; -1 — пауза. Простой учебный мотив.
        int[] notes = {60,64,67,72, 71,67,64,60,
                       62,65,69,74, 72,69,65,62,
                       64,67,72,76, 74,72,67,64,
                       65,69,72,77, 76,72,69,65,
                       60,67,72,76, 72,67,64,60};
        int[] durations = {280,280,280,430, 280,280,280,430,
                           280,280,280,430, 280,280,280,430,
                           280,280,280,430, 280,280,280,430,
                           280,280,280,430, 280,280,280,430,
                           260,260,280,400, 260,260,280,700};
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        for (int i = 0; i < notes.length; i++) {
            int samples = RATE * durations[i] / 1000;
            double freq = 440.0 * Math.pow(2.0, (notes[i] - 69.0) / 12.0);
            for (int s = 0; s < samples; s++) {
                double t = (double) s / RATE;
                double fadeIn = Math.min(1.0, s / (RATE * 0.025));
                double fadeOut = Math.min(1.0, (samples - s) / (RATE * 0.07));
                double envelope = Math.min(fadeIn, fadeOut);
                double x = (Math.sin(2 * Math.PI * freq * t)
                        + 0.22 * Math.sin(4 * Math.PI * freq * t)) * envelope;
                short sample = (short) (x * 11500);
                pcm.write(sample & 0xFF);
                pcm.write((sample >>> 8) & 0xFF);
            }
        }
        byte[] raw = pcm.toByteArray();
        AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
        try (AudioInputStream sound = new AudioInputStream(
                new ByteArrayInputStream(raw), format, raw.length / 2);
             ByteArrayOutputStream wav = new ByteArrayOutputStream()) {
            AudioSystem.write(sound, AudioFileFormat.Type.WAVE, wav);
            return wav.toByteArray();
        }
    }
}
