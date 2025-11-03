import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class LogRotationChecker {
    public Evaluation evaluate(Map<String, String> props) {
        boolean loggingEnabled = getBool(props, "logging.enabled", true);

        if (!loggingEnabled) {
            return new Evaluation(Result.PASS, "Logging disabled");
        }

        boolean centralized = getBool(props, "logging.centralized", false);
        boolean rotationEnabled = getBool(props, "rotation.enabled", false);

        if (!rotationEnabled) {
            return new Evaluation(Result.WARN, "Rotation disabled while logging is enabled");
        }

        String lastRotatedStr = props.get("rotation.lastRotated");

        if (lastRotatedStr == null || lastRotatedStr.isBlank()) {
            return new Evaluation(Result.FAIL, "Missing rotation.lastRotated");
        }

        int maxDays = getInt(props, "rotation.maxDays", 7);

        LocalDate now = LocalDate.now();
        LocalDate lastRotated;

        try {
            lastRotated = LocalDate.parse(lastRotatedStr);
        } catch (DateTimeParseException e) {
            return new Evaluation(Result.FAIL, "Unparseable rotation.lastRotated: " + lastRotatedStr);
        }

        long days = ChronoUnit.DAYS.between(lastRotated, now);

        if (days > maxDays) {
            return new Evaluation(Result.FAIL, "Rotation overdue by " + (days - maxDays) + " day(s)");
        }

        Integer sizeMb = getIntNullable(props, "log.currentSizeMB");
        Integer softLimitMb = getIntNullable(props, "log.softLimitMB");

        if (sizeMb != null && softLimitMb != null && sizeMb > softLimitMb) {
            return new Evaluation(Result.WARN, "Log size " + sizeMb + "MB exceeds the soft limit " + softLimitMb + "MB");
        }

        // Centralized defaults to WARN unless explicitly opted-out
        boolean centralizedPassOptOut = getBool(props, "rotation.centralizedPassAllowed", false);
        if (centralized && !centralizedPassOptOut) {
            return new Evaluation(Result.WARN, "Rotation healthy (centralized logging enabled)");
        }
        if (centralized) {
            return new Evaluation(Result.PASS, "Rotation healthy (centralized logging enabled)");
        }

        return new Evaluation(Result.PASS, "Rotation healthy");
    }

    private boolean getBool(Map<String, String> props, String key, boolean def) {
        String v = props.get(key);

        if (v == null) {
            return def;
        }

        return v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("1");
    }


    private int getInt(Map<String, String> props, String key, int def) {
        String v = props.get(key);

        if (v == null) {
            return def;
        }

        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }


    private Integer getIntNullable(Map<String, String> props, String key) {
        String v = props.get(key);

        if (v == null || v.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Map<String, String> loadProperties(Path path) throws IOException {
        Map<String, String> map = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;

            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();

                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int eq = trimmed.indexOf('=');

                if (eq <= 0) {
                    continue;
                }

                String key = trimmed.substring(0, eq).trim();
                String val = trimmed.substring(eq + 1).trim();

                map.put(key, val);
            }
        }

        return map;
    }
}
