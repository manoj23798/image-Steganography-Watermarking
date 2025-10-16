import javax.imageio.ImageIO;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public class StegoApp {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) { printHelp(); return; }

        if (hasFlag(args, "hide")) {
            String inImg = getArg(args, "-in");
            String dataFile = getArg(args, "-data");
            String outImg = getArg(args, "-out");
            String key = getArg(args, "-key");
            require(inImg, "-in"); require(dataFile, "-data"); require(outImg, "-out"); require(key, "-key");

            byte[] data = readAllBytes(new File(dataFile));
            byte[] encrypted = encryptAES(data, key);
            BufferedImage img = loadImage(inImg);
            byte[] header = ByteBuffer.allocate(4).putInt(encrypted.length).array();
            byte[] all = concat(header, encrypted);
            BufferedImage stego = embedLSB(img, all, key);
            saveImage(stego, outImg);
            System.out.println("✅ Data encrypted & hidden in: " + outImg);

        } else if (hasFlag(args, "extract")) {
            String inImg = getArg(args, "-in");
            String outData = getArg(args, "-out");
            String key = getArg(args, "-key");
            require(inImg, "-in"); require(outData, "-out"); require(key, "-key");

            BufferedImage img = loadImage(inImg);
            byte[] extracted = extractLSB(img, key);
            byte[] decrypted = decryptAES(extracted, key);
            writeAllBytes(new File(outData), decrypted);
            System.out.println("✅ Data extracted & decrypted to: " + outData);

        } else if (hasFlag(args, "watermark-embed")) {
            String inImg = getArg(args, "-in");
            String outImg = getArg(args, "-out");
            String key = getArg(args, "-key");
            String text = getArg(args, "-text");
            String redundancyStr = getArg(args, "-redundancy");
            require(inImg, "-in"); require(outImg, "-out"); require(key, "-key"); require(text, "-text");
            int redundancy = redundancyStr == null ? 10 : Math.max(1, Integer.parseInt(redundancyStr));

            BufferedImage img = loadImage(inImg);
            BufferedImage wm = embedWatermark(img, key, text, redundancy);
            saveImage(wm, outImg);
            System.out.println("✅ Watermark embedded in: " + outImg);

        } else if (hasFlag(args, "watermark-detect")) {
            String inImg = getArg(args, "-in");
            String key = getArg(args, "-key");
            String text = getArg(args, "-text");
            String thStr = getArg(args, "-th");
            require(inImg, "-in"); require(key, "-key"); require(text, "-text");
            double th = thStr == null ? 0.6 : Double.parseDouble(thStr);

            BufferedImage img = loadImage(inImg);
            double score = detectWatermark(img, key, text);
            System.out.printf(Locale.US, "Watermark score: %.3f (threshold=%.2f) => %s%n",
                    score, th, score >= th ? "✅ PRESENT" : "❌ NOT PRESENT");

        } else {
            printHelp();
        }
    }

    // ===== AES Encryption / Decryption =====
    private static byte[] encryptAES(byte[] data, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKey secretKey = new SecretKeySpec(Arrays.copyOf(sha256(key.getBytes()), 16), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    private static byte[] decryptAES(byte[] data, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKey secretKey = new SecretKeySpec(Arrays.copyOf(sha256(key.getBytes()), 16), "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    // ===== Data Hiding =====
    private static BufferedImage embedLSB(BufferedImage src, byte[] data, String key) throws Exception {
        BufferedImage img = toARGB(src);
        int w = img.getWidth(), h = img.getHeight();
        int total = w * h * 3; // RGB channels
        int need = data.length * 8;
        if (need > total) throw new IllegalArgumentException("Not enough capacity for data!");

        int[] order = shuffledIndices(total, key, "DATA");
        BitReader br = new BitReader(data);

        for (int i = 0; i < need; i++) {
            int idx = order[i];
            int pixelIndex = idx / 3;
            int channel = idx % 3;
            int x = pixelIndex % w, y = pixelIndex / w;
            int rgb = img.getRGB(x, y);
            int bit = br.readBit();

            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;

            if (channel == 0) r = (r & 0xFE) | bit;
            else if (channel == 1) g = (g & 0xFE) | bit;
            else b = (b & 0xFE) | bit;

            int newRgb = (0xFF << 24) | (r << 16) | (g << 8) | b;
            img.setRGB(x, y, newRgb);
        }
        return img;
    }

    private static byte[] extractLSB(BufferedImage src, String key) throws Exception {
        BufferedImage img = toARGB(src);
        int w = img.getWidth(), h = img.getHeight();
        int total = w * h * 3;

        int[] order = shuffledIndices(total, key, "DATA");
        BitWriter bw = new BitWriter();

        for (int i = 0; i < total; i++) {
            int idx = order[i];
            int pixelIndex = idx / 3;
            int channel = idx % 3;
            int x = pixelIndex % w, y = pixelIndex / w;
            int rgb = img.getRGB(x, y);
            int bit = 0;

            if (channel == 0) bit = ((rgb >> 16) & 1);
            else if (channel == 1) bit = ((rgb >> 8) & 1);
            else bit = (rgb & 1);

            bw.writeBit(bit);
        }

        byte[] all = bw.toByteArray();
        int len = ByteBuffer.wrap(Arrays.copyOf(all, 4)).getInt();
        return Arrays.copyOfRange(all, 4, 4 + len);
    }

    // ===== Watermarking =====
    private static BufferedImage embedWatermark(BufferedImage src, String key, String text, int redundancy) throws Exception {
        BufferedImage img = toARGB(src);
        int w = img.getWidth(), h = img.getHeight();
        int total = w * h * 3;
        byte[] sig = Arrays.copyOf(sha256((key + "|" + text).getBytes()), 16);
        BitReader bits = new BitReader(sig);
        int sigBits = sig.length * 8;
        int[] order = shuffledIndices(total, key, "WM");

        for (int r = 0; r < redundancy; r++) {
            bits.reset();
            for (int i = 0; i < sigBits; i++) {
                int idx = order[r * sigBits + i];
                int pixelIndex = idx / 3;
                int channel = idx % 3;
                int x = pixelIndex % w, y = pixelIndex / w;
                int rgb = img.getRGB(x, y);
                int bit = bits.readBit();

                int R = (rgb >> 16) & 0xFF;
                int G = (rgb >> 8) & 0xFF;
                int B = rgb & 0xFF;

                if (channel == 0) R = (R & 0xFE) | bit;
                else if (channel == 1) G = (G & 0xFE) | bit;
                else B = (B & 0xFE) | bit;

                int newRgb = (0xFF << 24) | (R << 16) | (G << 8) | B;
                img.setRGB(x, y, newRgb);
            }
        }
        return img;
    }

    private static double detectWatermark(BufferedImage src, String key, String text) throws Exception {
        BufferedImage img = toARGB(src);
        int w = img.getWidth(), h = img.getHeight();
        int total = w * h * 3;

        byte[] sig = Arrays.copyOf(sha256((key + "|" + text).getBytes()), 16);
        int sigBits = sig.length * 8;
        int[] order = shuffledIndices(total, key, "WM");

        int repeats = total / sigBits;
        int correct = 0, totalBits = 0;
        for (int r = 0; r < repeats; r++) {
            BitReader bits = new BitReader(sig);
            for (int i = 0; i < sigBits; i++) {
                int idx = order[r * sigBits + i];
                int pixelIndex = idx / 3;
                int channel = idx % 3;
                int x = pixelIndex % w, y = pixelIndex / w;
                int rgb = img.getRGB(x, y);
                int bit = 0;
                if (channel == 0) bit = ((rgb >> 16) & 1);
                else if (channel == 1) bit = ((rgb >> 8) & 1);
                else bit = (rgb & 1);
                if (bit == bits.readBit()) correct++;
                totalBits++;
            }
        }
        return (double) correct / totalBits;
    }

    // ===== Helper Methods =====
    private static BufferedImage loadImage(String path) throws IOException {
        return ImageIO.read(new File(path));
    }

    private static void saveImage(BufferedImage img, String path) throws IOException {
        ImageIO.write(img, "png", new File(path));
    }

    private static byte[] readAllBytes(File f) throws IOException {
        return java.nio.file.Files.readAllBytes(f.toPath());
    }

    private static void writeAllBytes(File f, byte[] data) throws IOException {
        java.nio.file.Files.write(f.toPath(), data);
    }

    private static byte[] sha256(byte[] in) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(in);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    private static BufferedImage toARGB(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        out.getGraphics().drawImage(src, 0, 0, null);
        return out;
    }

    private static int[] shuffledIndices(int n, String key, String salt) throws Exception {
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = i;
        byte[] seed = sha256((key + "|" + salt).getBytes());
        long s = ByteBuffer.wrap(Arrays.copyOf(seed, 8)).getLong();
        Random rnd = new Random(s);
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
        return arr;
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String s : args) if (s.equalsIgnoreCase(flag)) return true;
        return false;
    }

    private static String getArg(String[] args, String flag) {
        for (int i = 0; i < args.length - 1; i++)
            if (args[i].equalsIgnoreCase(flag)) return args[i + 1];
        return null;
    }

    private static void require(String v, String flag) {
        if (v == null) throw new IllegalArgumentException("Missing " + flag);
    }

    private static class BitReader {
        private final byte[] data;
        private int pos = 0;
        BitReader(byte[] d) { this.data = d; }
        int readBit() {
            int bytePos = pos >> 3;
            int bitPos = 7 - (pos & 7);
            pos++;
            return (data[bytePos] >> bitPos) & 1;
        }
        void reset() { pos = 0; }
    }

    private static class BitWriter {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int cur = 0, count = 0;
        void writeBit(int bit) {
            cur = (cur << 1) | (bit & 1);
            count++;
            if (count == 8) { bos.write(cur); count = 0; cur = 0; }
        }
        byte[] toByteArray() {
            if (count > 0) bos.write(cur << (8 - count));
            return bos.toByteArray();
        }
    }

    private static void printHelp() {
        System.out.println("\nStegoApp - AES Steganography + Watermark Tool");
        System.out.println("Usage:");
        System.out.println("  Hide data:     java StegoApp hide -in input.png -data secret.txt -out stego.png -key myKey");
        System.out.println("  Extract data:  java StegoApp extract -in stego.png -out recovered.txt -key myKey");
        System.out.println("  Watermark add: java StegoApp watermark-embed -in input.png -out wm.png -text \"Owner: Manoj V\" -key myKey -redundancy 10");
        System.out.println("  Watermark chk: java StegoApp watermark-detect -in wm.png -text \"Owner: Manoj V\" -key myKey -th 0.6");
        System.out.println("\nNotes: Use PNG images only. AES ensures encrypted data. Watermarking helps prove ownership.\n");
    }
}
