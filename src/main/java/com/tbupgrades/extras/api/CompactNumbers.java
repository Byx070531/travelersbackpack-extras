package com.tbupgrades.extras.api;

/**
 * Formats large item amounts the way the backpack displays them: 365 -&gt; {@code 0.3k},
 * 46587 -&gt; {@code 46k}. Values are truncated, never rounded up, and anything below one thousand
 * is shown as a plain number.
 */
public final class CompactNumbers {
    private static final String THOUSAND = "k";
    private static final String MILLION = "m";
    private static final String BILLION = "b";

    private CompactNumbers() {
    }

    public static String format(long value) {
        if (value < 0) {
            return "-" + format(-value);
        }
        if (value < 1000L) {
            return Long.toString(value);
        }
        if (value < 1_000_000L) {
            return scaled(value, 1_000L, THOUSAND);
        }
        if (value < 1_000_000_000L) {
            return scaled(value, 1_000_000L, MILLION);
        }
        return scaled(value, 1_000_000_000L, BILLION);
    }

    /**
     * Truncates to one decimal below ten units of the suffix and to whole units above it, so that
     * 365 becomes "0.3k" (not "0.4k") and 46587 becomes "46k" (not "47k").
     */
    private static String scaled(long value, long divisor, String suffix) {
        long tenths = value * 10L / divisor;
        if (tenths < 100L) {
            return (tenths / 10L) + "." + (tenths % 10L) + suffix;
        }
        return (value / divisor) + suffix;
    }
}
