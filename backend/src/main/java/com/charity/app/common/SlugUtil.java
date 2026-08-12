package com.charity.app.common;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Builds URL slugs from Persian text.
 *
 * <p>Slugs stay in Persian rather than being transliterated. Search engines handle non-ASCII URLs,
 * percent-encode them transparently, and highlight matching Persian terms in the result snippet --
 * a transliterated {@code komak-hazine-darman} matches nothing a Persian speaker would ever type.
 *
 * <p>Normalisation matters more than it looks. The same word can be typed with an Arabic yeh or a
 * Persian yeh, with or without a zero-width non-joiner, and with any of three digit sets; without
 * folding those, «یاری‌جو» and «یاریجو» become different URLs for the same thing.
 */
public final class SlugUtil {

    private static final Pattern NON_SLUG = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern REPEATED_DASH = Pattern.compile("-{2,}");

    private static final char ZWNJ = '‌';
    private static final char ARABIC_INDIC_ZERO = '٠';
    private static final char EXTENDED_ARABIC_INDIC_ZERO = '۰';

    private SlugUtil() {
    }

    /**
     * Folds Persian/Arabic character variants and digit sets to a single canonical form.
     * Used both for slugs and for normalising search terms, so a visitor typing on an Arabic
     * keyboard layout still matches content stored with Persian characters.
     */
    public static String normalizePersian(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            out.append(switch (c) {
                case 'ي' -> 'ی';                 // Arabic yeh  -> Persian yeh
                case 'ك' -> 'ک';                 // Arabic kaf  -> Persian kaf
                case 'ة' -> 'ه';                 // teh marbuta -> heh
                case 'أ', 'إ', 'ٱ' -> 'ا';  // alef variants -> alef
                default -> {
                    if (c >= ARABIC_INDIC_ZERO && c <= ARABIC_INDIC_ZERO + 9) {
                        yield (char) ('0' + (c - ARABIC_INDIC_ZERO));
                    }
                    if (c >= EXTENDED_ARABIC_INDIC_ZERO && c <= EXTENDED_ARABIC_INDIC_ZERO + 9) {
                        yield (char) ('0' + (c - EXTENDED_ARABIC_INDIC_ZERO));
                    }
                    yield c;
                }
            });
        }
        return out.toString();
    }

    /** Slugifies arbitrary text, truncating on a separator so a word is never cut in half. */
    public static String slugify(String input, int maxLength) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String s = normalizePersian(input.toLowerCase(Locale.ROOT)).replace(ZWNJ, '-');
        s = NON_SLUG.matcher(s).replaceAll("-");
        s = REPEATED_DASH.matcher(s).replaceAll("-");
        s = trimDashes(s);
        if (s.length() > maxLength) {
            s = s.substring(0, maxLength);
            int lastDash = s.lastIndexOf('-');
            if (lastDash > maxLength / 2) {
                s = s.substring(0, lastDash);
            }
            s = trimDashes(s);
        }
        return s;
    }

    /**
     * A request slug always ends in its public code. That makes collisions impossible by
     * construction -- no retry loop, no {@code -2} counter -- and lets an incoming slug be resolved
     * by code alone, so a request whose title was edited can still be matched and 301'd to its
     * current canonical URL.
     */
    public static String requestSlug(String title, String code) {
        String base = slugify(title, 180);
        return (base.isEmpty() ? "request" : base) + "-" + code.toLowerCase(Locale.ROOT);
    }

    /** Extracts the trailing {@code rq-123} code from a slug, or null when there isn't one. */
    public static String extractCode(String slug) {
        if (slug == null) {
            return null;
        }
        int dash = slug.lastIndexOf("rq-");
        return dash < 0 ? null : slug.substring(dash).toUpperCase(Locale.ROOT);
    }

    private static String trimDashes(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && s.charAt(start) == '-') {
            start++;
        }
        while (end > start && s.charAt(end - 1) == '-') {
            end--;
        }
        return s.substring(start, end);
    }
}
