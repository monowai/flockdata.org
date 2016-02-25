package org.flockdata.batch;

import org.flockdata.profile.model.ContentProfile;

/**
 * Created by mike on 25/02/16.
 */
public class StepConfig {
    String query;
    String step;
    String profile;
    private ContentProfile contentProfile;

    StepConfig (){}

    StepConfig (String step, String profile, String query){
        this();
        this.query = query;
        this.step = step;
        this.profile = profile;
    }

    public String getQuery() {
        return query;
    }

    public String getStep() {
        return step;
    }

    public String getProfile() {
        return profile;
    }

    public StepConfig setContentProfile(ContentProfile contentProfile){
        this.contentProfile = contentProfile;
        return this;
    }

    public ContentProfile getContentProfile() {
        return contentProfile;
    }

    @Override
    public String toString() {
        return "StepConfig{" +
                "step='" + step + '\'' +
                ", profile='" + profile + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StepConfig)) return false;

        StepConfig that = (StepConfig) o;

        if (step != null ? !step.equals(that.step) : that.step != null) return false;
        return profile != null ? profile.equals(that.profile) : that.profile == null;

    }

    @Override
    public int hashCode() {
        int result = step != null ? step.hashCode() : 0;
        result = 31 * result + (profile != null ? profile.hashCode() : 0);
        return result;
    }
}
