package com.polidea.rxandroidble2.internal;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.polidea.rxandroidble2.PhyPair;
import com.polidea.rxandroidble2.RxBlePhy;

import java.util.Objects;

public class PhyPairImpl implements PhyPair {
    public final RxBlePhy txPhy;
    public final RxBlePhy rxPhy;

    public PhyPairImpl(@NonNull final RxBlePhy txPhy, @NonNull final RxBlePhy rxPhy) {
        this.txPhy = txPhy;
        this.rxPhy = rxPhy;
    }

    @NonNull
    @Override
    public RxBlePhy getTxPhy() {
        return txPhy;
    }

    @NonNull
    @Override
    public RxBlePhy getRxPhy() {
        return rxPhy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rxPhy, txPhy);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof PhyPair)) return false;
        PhyPair phyPair = (PhyPair) obj;
        return txPhy.equals(phyPair.getTxPhy()) && rxPhy.equals(phyPair.getRxPhy());
    }
}
