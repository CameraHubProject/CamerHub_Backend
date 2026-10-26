package vn.camerahub.moderation.util;

import java.text.Normalizer;
import java.util.*;

public class TextFingerprint {
    public static boolean duplicateText(Map<String, Object> a, Map<String, Object> b) {
        String serial = normalize(a.get("serial_number_private"));
        if (!serial.isEmpty() && serial.equals(normalize(b.get("serial_number_private")))) return true;
        String title = normalize(a.get("title")), description = normalize(a.get("description"));
        if (title.equals(normalize(b.get("title"))) && description.equals(normalize(b.get("description")))) return true;
        return Objects.equals(a.get("seller_id"), b.get("seller_id")) && description.length() >= 40
                && similarity(title, normalize(b.get("title"))) >= 0.9 && similarity(description, normalize(b.get("description"))) >= 0.92;
    }

    public static String normalize(Object text) {
        if (text == null) return "";
        return Normalizer.normalize(text.toString(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replace('đ', 'd').replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
    }

    public static double similarity(String a, String b) {
        var left = new HashSet<>(List.of(a.split(" ")));
        var right = new HashSet<>(List.of(b.split(" ")));
        var union = new HashSet<>(left);
        union.addAll(right);
        left.retainAll(right);
        return union.isEmpty() ? 0 : (double) left.size() / union.size();
    }
}

