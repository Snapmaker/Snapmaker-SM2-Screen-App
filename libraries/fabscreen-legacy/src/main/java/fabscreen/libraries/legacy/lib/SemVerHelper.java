package fabscreen.libraries.legacy.lib;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

/**
 * SerVerHelper: A crude SemVer helper class
 *
 * Semantic Versioning https://semver.org/
 */

// TODO: need to refactor this if having spare time.
public class SemVerHelper {
    final private static int MAX_LENGTH = 256;
    // Compatible with four version numbers like "1.7.0.0", but we only parse three numbers.
    final private static String SNAP_SEM_VER_REGEX = "(\\d)\\.(\\d+)\\.(\\d+)(\\.\\d+)?(-.*)?$";

    private static String clean(String version) {
        return version.trim().replaceFirst("[=v|V]", "");
    }

    private static SemVer parse(String version) {
        return (isValid(version)) ? new SemVer(clean(version)) : null;
    }

    public static boolean isValid(String version) {
        if (version == null || version.isEmpty() || version.length() > MAX_LENGTH) {
            return false;
        }

        final Pattern semVerPattern = Pattern.compile(SNAP_SEM_VER_REGEX);
        return (semVerPattern.matcher(clean(version)).find());
    }

    public static int major(@NonNull String version) {
        final SemVer semVer = parse(version);
        return (semVer != null) ? semVer.major : -1;
    }

    public static int minor(@NonNull String version) {
        final SemVer semVer = parse(version);
        return (semVer != null) ? semVer.minor : -1;
    }

    public static int patch(@NonNull String version) {
        final SemVer semVer = parse(version);
        return (semVer != null) ? semVer.patch : -1;
    }

    public static boolean equal(String versionA, String versionB) throws FabException {
        SemVer verA, verB;
        verA = parse(versionA);
        verB = parse(versionB);

        if (verA == null || verB == null) throw new FabException("Parse version failed!");

        return (verA.major == verB.major)
                && (verA.minor == verB.minor)
                && (verA.patch == verB.patch);
    }

    // TODO
    private static String coerce(String version) {
        return null;
    }

    // greater than
    public static boolean gt(@NonNull String versionA, @NonNull String versionB) throws FabException {
        SemVer verA, verB;
        verA = parse(versionA);
        verB = parse(versionB);

        if (verA == null || verB == null) throw new FabException("Parse version failed!");

        if (verA.major != verB.major) {
            return verA.major > verB.major;
        } else if (verA.minor != verB.minor) {
            return verA.minor > verB.minor;
        } else if (verA.patch != verB.patch) {
            return verA.patch > verB.patch;
        } else {
            // equal
            return false;
        }
    }

    // greater than or equal to
    public static boolean gte(@NonNull String versionA, @NonNull String versionB) throws FabException {
        SemVer verA, verB;
        verA = parse(versionA);
        verB = parse(versionB);

        if (verA == null || verB == null) throw new FabException("Parse version failed!");

        if (verA.major != verB.major) {
            return verA.major > verB.major;
        } else if (verA.minor != verB.minor) {
            return verA.minor > verB.minor;
        } else if (verA.patch != verB.patch) {
            return verA.patch > verB.patch;
        } else {
            // equal
            return false;
        }
    }

    // less than
    public static boolean lt(@NonNull String versionA, @NonNull String versionB) throws FabException {
        SemVer verA, verB;
        verA = parse(versionA);
        verB = parse(versionB);

        if (verA == null || verB == null) throw new FabException("Parse version failed!");

        if (verA.major != verB.major) {
            return verA.major < verB.major;
        } else if (verA.minor != verB.minor) {
            return verA.minor < verB.minor;
        } else if (verA.patch != verB.patch) {
            return verA.patch < verB.patch;
        } else {
            // equal
            return false;
        }
    }

    // less than or equal to
    public static boolean ltb(@NonNull String versionA, @NonNull String versionB) throws FabException {
        SemVer verA, verB;
        verA = parse(versionA);
        verB = parse(versionB);

        if (verA == null || verB == null) throw new FabException("Parse version failed!");

        if (verA.major > verB.major) {
            return false;
        } else if (verA.minor > verB.minor) {
            return false;
        } else {
            return verA.patch <= verB.patch;
        }
    }

    public static String minVersion(@NonNull String versionA, @NonNull String versionB) throws FabException {
        return ltb(versionA, versionB) ? versionA : versionB;
    }

    static class SemVer {
        String raw;
        int major;
        int minor;
        int patch;
        String extensions;

        public SemVer(@NonNull String version) {
            String input = version;
            raw = version;
            if (input.contains("-")) {
                extensions = version.substring(input.indexOf("-"));
                input = input.substring(0, raw.indexOf("-"));
            }

            String[] versions = input.split("\\.");

            int[] m = new int[3];
            try {
                for (int i = 0; i < 3; i++) {
                    m[i] = Integer.parseInt(versions[i]);
                }
            } catch (NumberFormatException e) {
                LogHelper.log(e);
            }

            major = m[0];
            minor = m[1];
            patch = m[2];
        }

        public String getRaw() {
            return raw;
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

        public int getPatch() {
            return patch;
        }

        public String getExtensions() {
            return extensions;
        }

        public String getMainVersion() {
            return major + "." + minor + "." + patch;
        }
    }
}
