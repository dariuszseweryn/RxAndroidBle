package android.content;

public class IntentFilter {

    private final String action;

    public IntentFilter(String action) {
        this.action = action;
    }

    public boolean hasAction(String checkAction) {
        return action.equals(checkAction);
    }
}
