package com.polidea.rxandroidble.internal.operations;


import android.support.annotation.RestrictTo;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import java.util.concurrent.Semaphore;
import rx.Observable;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Operation<T> extends Runnable, Comparable<Operation<?>> {

    Observable<T> asObservable();

    RxBleRadioOperation.Priority definedPriority();

    void setRadioBlockingSemaphore(Semaphore semaphore);
}
