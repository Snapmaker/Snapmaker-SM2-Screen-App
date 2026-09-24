package fabscreen.libraries.legacy.data.api;

import java.util.ArrayList;

public class VersionResponse {
    public int code;
    public String msg;
    public VersionResponseData data;

    public VersionResponse() {
        code = 0;
        msg = "";
        data = new VersionResponseData();
    }

    public static class VersionResponseData {
        public NewVersionData new_version;

        public VersionResponseData() {
            new_version = new NewVersionData();
        }
    }

    public static class NewVersionData {
        public final String version;
        public final String url;
        public final VersionChangeLog change_log;
        public final int package_size;
        public final ArrayList<String> summary;

        public NewVersionData() {
            version = "";
            url = "";
            change_log = new VersionChangeLog();
            package_size = 0;
            summary = new ArrayList<>();
        }
    }
}
