package com.polidea.rxandroidble2;

/**
 * Coding to be used when transmitting on the LE Coded PHY.
 */
public enum RxBlePhyOption {
    /**
     * No preferred coding.
     */
    PHY_OPTION_NO_PREFERRED(0),

    /**
     * Prefer the S=2 coding.
     */
    PHY_OPTION_S2(1),

    /**
     * Prefer the S=8 coding.
     */
    PHY_OPTION_S8(2);

    private final int value;

    RxBlePhyOption(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
