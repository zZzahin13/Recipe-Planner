package com.recipeplanner.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ingredient quantities are stored as free text ("2 cups", "1 1/2 tbsp",
 * "200g", "a pinch") rather than structured amount+unit columns, so
 * scaling them for a different serving count is necessarily best-effort:
 * this parses a LEADING number (including simple fractions like "1/2"
 * and mixed numbers like "1 1/2"), scales it, and reattaches whatever
 * text followed it unchanged. Quantities with no leading number ("a
 * pinch", "to taste") are left as-is -- callers should treat those as
 * "not scaled" rather than silently wrong.
 */
public final class QuantityScaler {

    // Matches an optional whole number, an optional fraction (n/d), then the rest of the string.
    private static final Pattern LEADING_NUMBER =
            Pattern.compile("^\\s*(\\d+\\s+\\d+/\\d+|\\d+/\\d+|\\d+(?:\\.\\d+)?)\\s*(.*)$");

    private QuantityScaler() {
    }

    /** True if this quantity string starts with a number we can actually scale. */
    public static boolean isScalable(String quantity) {
        return quantity != null && LEADING_NUMBER.matcher(quantity.trim()).matches();
    }

    /**
     * Scales the leading numeric portion of {@code quantity} by {@code factor}
     * (e.g. factor = desiredServings / baseServings). Returns the original
     * string unchanged if no leading number could be parsed.
     */
    public static String scale(String quantity, double factor) {
        if (quantity == null || quantity.isBlank()) {
            return quantity;
        }
        Matcher m = LEADING_NUMBER.matcher(quantity.trim());
        if (!m.matches()) {
            return quantity; // e.g. "a pinch", "to taste" -- nothing numeric to scale
        }

        double original = parseNumberOrFraction(m.group(1));
        double scaled = original * factor;
        String rest = m.group(2);

        return formatNumber(scaled) + (rest.isEmpty() ? "" : " " + rest);
    }

    private static double parseNumberOrFraction(String token) {
        if (token.contains(" ")) { // mixed number: "1 1/2"
            String[] parts = token.split("\\s+", 2);
            return Double.parseDouble(parts[0]) + parseFraction(parts[1]);
        }
        if (token.contains("/")) {
            return parseFraction(token);
        }
        return Double.parseDouble(token);
    }

    private static double parseFraction(String fraction) {
        String[] parts = fraction.split("/");
        return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
    }

    /** Whole numbers print without decimals; everything else rounds to 1 decimal place. */
    private static String formatNumber(double value) {
        double rounded = Math.round(value * 10) / 10.0;
        if (rounded == Math.floor(rounded)) {
            return String.valueOf((long) rounded);
        }
        return String.valueOf(rounded);
    }
}
