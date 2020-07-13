package com.polidea.rxandroidble2.scan;


import android.bluetooth.BluetoothDevice;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.internal.logger.LoggerUtil;

public class ScanResult {

    /**
     * For chained advertisements, inidcates tha the data contained in this
     * scan result is complete.
     */
    public static final int DATA_COMPLETE = 0x00;

    /**
     * For chained advertisements, indicates that the controller was
     * unable to receive all chained packets and the scan result contains
     * incomplete truncated data.
     */
    public static final int DATA_TRUNCATED = 0x02;

    /**
     * Indicates that the secondary physical layer was not used.
     */
    public static final int PHY_UNUSED = 0x00;

    /**
     * Advertising Set ID is not present in the packet.
     */
    public static final int SID_NOT_PRESENT = 0xFF;

    /**
     * TX power is not present in the packet.
     */
    public static final int TX_POWER_NOT_PRESENT = 0x7F;

    /**
     * Periodic advertising interval is not present in the packet.
     */
    public static final int PERIODIC_INTERVAL_NOT_PRESENT = 0x00;

    /**
     * Mask for checking whether event type represents legacy advertisement.
     */
    private static final int ET_LEGACY_MASK = 0x10;

    /**
     * Mask for checking whether event type represents connectable advertisement.
     */
    private static final int ET_CONNECTABLE_MASK = 0x01;

    private final RxBleDevice bleDevice;
    private final int rssi;
    private final long timestampNanos;
    private final ScanCallbackType callbackType;
    private final ScanRecord scanRecord;

    private int mEventType;
    private int mPrimaryPhy;
    private int mSecondaryPhy;
    private int mAdvertisingSid;
    private int mTxPower;
    private int mPeriodicAdvertisingInterval;

    public ScanResult(RxBleDevice bleDevice, int rssi, long timestampNanos, ScanCallbackType callbackType, ScanRecord scanRecord) {
        this.bleDevice = bleDevice;
        this.rssi = rssi;
        this.timestampNanos = timestampNanos;
        this.callbackType = callbackType;
        this.scanRecord = scanRecord;
        mEventType = (DATA_COMPLETE << 5) | ET_LEGACY_MASK | ET_CONNECTABLE_MASK;
        mPrimaryPhy = RxBleDevice.PHY_LE_1M;
        mSecondaryPhy = PHY_UNUSED;
        mAdvertisingSid = SID_NOT_PRESENT;
        mTxPower = TX_POWER_NOT_PRESENT;
        mPeriodicAdvertisingInterval = PERIODIC_INTERVAL_NOT_PRESENT;
    }

    /**
     * Constructs a new ScanResult.
     *
     * @param bleDevice Remote RxBleDevice found.
     * @param eventType Event type.
     * @param primaryPhy Primary advertising phy.
     * @param secondaryPhy Secondary advertising phy.
     * @param advertisingSid Advertising set ID.
     * @param txPower Transmit power.
     * @param rssi Received signal strength.
     * @param periodicAdvertisingInterval Periodic advertising interval.
     * @param scanRecord Scan record including both advertising data and scan response data.
     * @param timestampNanos Timestamp at which the scan result was observed.
     */
    public ScanResult(RxBleDevice bleDevice, int eventType, int primaryPhy, int secondaryPhy,
                      int advertisingSid, int txPower, int rssi, int periodicAdvertisingInterval,
                      ScanRecord scanRecord, long timestampNanos, ScanCallbackType callbackType) {
        this.bleDevice = bleDevice;
        mEventType = eventType;
        mPrimaryPhy = primaryPhy;
        mSecondaryPhy = secondaryPhy;
        mAdvertisingSid = advertisingSid;
        mTxPower = txPower;
        this.rssi = rssi;
        mPeriodicAdvertisingInterval = periodicAdvertisingInterval;
        this.scanRecord = scanRecord;
        this.timestampNanos = timestampNanos;
        this.callbackType = callbackType;
    }

    public RxBleDevice getBleDevice() {
        return bleDevice;
    }

    /**
     * Returns the received signal strength in dBm. The valid range is [-127, 126].
     */
    public int getRssi() {
        return rssi;
    }

    /**
     * Returns timestamp since boot when the scan record was observed.
     */
    public long getTimestampNanos() {
        return timestampNanos;
    }

    public ScanCallbackType getCallbackType() {
        return callbackType;
    }

    /**
     * Returns the scan record, which is a combination of advertisement and scan response.
     */
    public ScanRecord getScanRecord() {
        return scanRecord;
    }

    /**
     * Returns true if this object represents legacy scan result.
     * Legacy scan results do not contain advanced advertising information
     * as specified in the Bluetooth Core Specification v5.
     */
    public boolean isLegacy() {
        return (mEventType & ET_LEGACY_MASK) != 0;
    }

    /**
     * Returns true if this object represents connectable scan result.
     */
    public boolean isConnectable() {
        return (mEventType & ET_CONNECTABLE_MASK) != 0;
    }

    /**
     * Returns the data status.
     * Can be one of {@link ScanResult#DATA_COMPLETE} or
     * {@link ScanResult#DATA_TRUNCATED}.
     */
    public int getDataStatus() {
        // return bit 5 and 6
        return (mEventType >> 5) & 0x03;
    }

    /**
     * Returns the primary Physical Layer
     * on which this advertisement was received.
     * Can be one of {@link RxBleDevice#PHY_LE_1M} or
     * {@link RxBleDevice#PHY_LE_CODED}.
     */
    public int getPrimaryPhy() {
        return mPrimaryPhy;
    }

    /**
     * Returns the secondary Physical Layer
     * on which this advertisment was received.
     * Can be one of {@link RxBleDevice#PHY_LE_1M},
     * {@link RxBleDevice#PHY_LE_2M}, {@link RxBleDevice#PHY_LE_CODED}
     * or {@link ScanResult#PHY_UNUSED} - if the advertisement
     * was not received on a secondary physical channel.
     */
    public int getSecondaryPhy() {
        return mSecondaryPhy;
    }

    /**
     * Returns the advertising set id.
     * May return {@link ScanResult#SID_NOT_PRESENT} if
     * no set id was is present.
     */
    public int getAdvertisingSid() {
        return mAdvertisingSid;
    }

    /**
     * Returns the transmit power in dBm.
     * Valid range is [-127, 126]. A value of {@link ScanResult#TX_POWER_NOT_PRESENT}
     * indicates that the TX power is not present.
     */
    public int getTxPower() {
        return mTxPower;
    }

    /**
     * Returns the periodic advertising interval in units of 1.25ms.
     * Valid range is 6 (7.5ms) to 65536 (81918.75ms). A value of
     * {@link ScanResult#PERIODIC_INTERVAL_NOT_PRESENT} means periodic
     * advertising interval is not present.
     */
    public int getPeriodicAdvertisingInterval() {
        return mPeriodicAdvertisingInterval;
    }

    @Override
    public String toString() {
        return "ScanResult{"
                + "bleDevice=" + bleDevice
                + ", rssi=" + rssi
                + ", timestampNanos=" + timestampNanos
                + ", callbackType=" + callbackType
                + ", scanRecord=" + LoggerUtil.bytesToHex(scanRecord.getBytes())
                + '}';
    }
}
