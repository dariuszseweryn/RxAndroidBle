package com.polidea.rxandroidble2;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

/**
 * The interface used for results of {@link RxBleConnection#readPhy()} and {@link RxBleConnection#setPreferredPhy(Set, Set, RxBlePhyOption)}
 */
public interface PhyPair {

    @NonNull
    RxBlePhy getTxPhy();

    @NonNull
    RxBlePhy getRxPhy();

    int hashCode();

    boolean equals(@Nullable Object obj);
}
