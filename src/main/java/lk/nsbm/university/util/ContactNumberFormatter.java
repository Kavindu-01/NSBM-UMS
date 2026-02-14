package lk.nsbm.university.util;

import org.springframework.util.StringUtils;

/**
 * Utility helpers to normalize stored contact numbers so lookups are consistent.
 */
public final class ContactNumberFormatter {

    private ContactNumberFormatter() {
    }

    public static String normalize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        StringBuilder builder = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (Character.isDigit(ch)) {
                builder.append(ch);
                continue;
            }
            if (ch == '+' && builder.length() == 0) {
                builder.append(ch);
            }
        }
        return builder.length() == 0 ? null : builder.toString();
    }
}
