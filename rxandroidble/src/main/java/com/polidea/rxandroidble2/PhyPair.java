package com.polidea.rxandroidble2;

import androidx.annotation.NonNull;

public class PhyPair {
    public final RxBlePhy txPhy;
    public final RxBlePhy rxPhy;

    public PhyPair(@NonNull final RxBlePhy txPhy, @NonNull final RxBlePhy rxPhy) {
        this.txPhy = txPhy;
        this.rxPhy = rxPhy;
    }
}
