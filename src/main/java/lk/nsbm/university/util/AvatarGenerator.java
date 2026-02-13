package lk.nsbm.university.util;

import lk.nsbm.university.entity.User;
import org.springframework.util.StringUtils;

import java.util.UUID;

public final class AvatarGenerator {

    private AvatarGenerator() {
    }

    public static String initials(User user) {
        return initials(user != null ? user.getFullName() : null,
                user != null ? user.getStudentId() : null);
    }

    public static String initials(String fullName, String fallback) {
        String source = StringUtils.hasText(fullName) ? fullName : fallback;
        if (!StringUtils.hasText(source)) {
            return "NA";
        }
        String[] tokens = source.trim().split("\\s+");
        char first = Character.toUpperCase(tokens[0].charAt(0));
        char second = 0;
        if (tokens.length > 1) {
            second = Character.toUpperCase(tokens[tokens.length - 1].charAt(0));
        } else if (StringUtils.hasText(fallback) && fallback.trim().length() > 1) {
            second = Character.toUpperCase(fallback.trim().charAt(1));
        }
        return second == 0 ? String.valueOf(first) : ("" + first + second);
    }

    public static String gradient(User user) {
        return gradient(user != null ? user.getStudentId() : null);
    }

    public static String gradient(String key) {
        String source = StringUtils.hasText(key) ? key : UUID.randomUUID().toString();
        int hash = source.hashCode();
        if (hash == Integer.MIN_VALUE) {
            hash = 0;
        }
        int positiveHash = Math.abs(hash);
        int hue = positiveHash % 360;
        int secondaryHue = (hue + 45) % 360;
        return String.format("linear-gradient(135deg, hsl(%d, 70%%, 55%%), hsl(%d, 70%%, 45%%))", hue, secondaryHue);
    }
}
