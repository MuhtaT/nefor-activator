package dev.nefor.activator.plugin.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private TimeUtil() {
    }

    public static String formatInstant(Instant instant) {
        return instant == null ? "n/a" : ISO_FORMATTER.format(instant.atZone(ZoneOffset.UTC));
    }

    public static String humanize(Duration duration) {
        if (duration == null) {
            return "n/a";
        }
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        long days = absSeconds / 86400;
        long hours = (absSeconds % 86400) / 3600;
        long minutes = (absSeconds % 3600) / 60;
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        }
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        }
        if (minutes > 0) {
            return String.format("%dm", minutes);
        }
        return seconds + "s";
    }
}
