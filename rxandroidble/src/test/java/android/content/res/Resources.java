package android.content.res;
/**
 * Used for mocks and constants
 */
public class Resources {
    public Configuration getConfiguration() {
        return configuration;
    }
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
    private Configuration configuration;
    public Resources(Configuration configuration) {
        this.configuration = configuration;
    }
    public final class Theme {
    }
    public String getResourceName(int resid) {
        return null;
    }
}
