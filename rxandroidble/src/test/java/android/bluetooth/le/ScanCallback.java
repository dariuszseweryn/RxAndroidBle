package android.bluetooth.le;
import java.util.List;

/**
 * Used for mocks and constants
 */
public abstract class ScanCallback {
    public static final int SCAN_FAILED_ALREADY_STARTED = 1;
    public static final int SCAN_FAILED_APPLICATION_REGISTRATION_FAILED = 2;
    public void onScanResult(int callbackType, ScanResult result) {
    }
    public void onBatchScanResults(List<ScanResult> results) {
    }
    public void onScanFailed(int errorCode) {
    }
}
