package fabscreen.libraries.legacy.data.api;

import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class VersionChangeLog {
    private static final String NEW_FEATURE = "new_feature";
    private static final String BUG_FIXED = "bug_fixed";
    private static final String IMPROVEMENT = "improvement";

    @SerializedName(NEW_FEATURE)
    private List<String> features;
    @SerializedName(BUG_FIXED)
    private List<String> bugFixes;
    @SerializedName(IMPROVEMENT)
    private List<String> improvement;

    public VersionChangeLog() {
        features = new ArrayList<>();
        bugFixes = new ArrayList<>();
        improvement = new ArrayList<>();
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public List<String> getBugFixes() {
        return bugFixes;
    }

    public void setBugFixes(List<String> bugFixes) {
        this.bugFixes = bugFixes;
    }

    public List<String> getImprovement() {
        return improvement;
    }

    public void setImprovement(List<String> Improvement) {
        this.improvement = Improvement;
    }

    @NotNull
    @Override
    public String toString() {
        return "VersionChangeLog{" +
                "features=" + features.toString() +
                ", bugFixes=" + bugFixes.toString() +
                ", improvement=" + improvement.toString() +
                '}';
    }
}
