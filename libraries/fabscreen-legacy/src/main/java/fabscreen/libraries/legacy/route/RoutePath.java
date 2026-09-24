package fabscreen.libraries.legacy.route;

/**
 * Route paths use for ARouter.
 * ARouter requires at least two-level path. We use feature name(former "module" name) as the first
 * level, and activity name(with snake-case) as the second level.
 */
public class RoutePath {
    private static final String HOME = "/home";
    public static final String MAIN_ACTIVITY = HOME + "/main-activity";
    public static final String HOME_ACTIVITY = HOME + "/home-activity";
    private static final String REMOTE = "/remote";
    public static final String REMOTE_ACTIVITY = REMOTE + "/remote-activity";
    private static final String PRINT = "/print";
    public static final String PRINT_ACTIVITY = PRINT + "/print-activity";
    private static final String PREVIEW = "/preview";
    public static final String PREVIEW_ACTIVITY = PREVIEW + "/preview-activity";
}
