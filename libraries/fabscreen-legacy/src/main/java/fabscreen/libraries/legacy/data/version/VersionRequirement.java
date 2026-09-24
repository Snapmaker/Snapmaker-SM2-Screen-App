package fabscreen.libraries.legacy.data.version;

public class VersionRequirement {
    int type;
    String name;
    String requiredVersion;

    public VersionRequirement(int type, String name, String requiredVersion) {
        this.type = type;
        this.name = name;
        this.requiredVersion = requiredVersion;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getRequiredVersion() {
        return requiredVersion;
    }
}
