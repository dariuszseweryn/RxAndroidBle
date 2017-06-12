package com.polidea.rxandroidble.internal.scan;


import static com.polidea.rxandroidble.internal.util.ObservableUtil.identityTransformer;

import android.support.annotation.IntRange;
import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.scan.ScanCallbackType;
import com.polidea.rxandroidble.scan.ScanSettings;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.GroupedObservable;

public class ScanSettingsEmulator {

    private final Scheduler scheduler;
    private Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult> emulateFirstMatch;

    @Inject
    public ScanSettingsEmulator(@Named(ClientComponent.NamedSchedulers.COMPUTATION) final Scheduler scheduler) {
        this.scheduler = scheduler;

        this.emulateFirstMatch = new Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult>() {

            private Func1<RxBleInternalScanResult, RxBleInternalScanResult> toFirstMatchFunc = toFirstMatch();
            private final Observable<Long> timerObservable = Observable.timer(10L, TimeUnit.SECONDS, scheduler);
            private final Func1<RxBleInternalScanResult, Observable<?>> emitAfterTimerFunc
                    = new Func1<RxBleInternalScanResult, Observable<?>>() {
                @Override
                public Observable<?> call(RxBleInternalScanResult internalScanResult) {
                    return timerObservable;
                }
            };
            private final Func1<Observable<RxBleInternalScanResult>, Observable<RxBleInternalScanResult>> takeFirstFromEachWindowFunc
                    = new Func1<Observable<RxBleInternalScanResult>, Observable<RxBleInternalScanResult>>() {
                @Override
                public Observable<RxBleInternalScanResult> call(
                        Observable<RxBleInternalScanResult> rxBleInternalScanResultObservable) {
                    return rxBleInternalScanResultObservable.take(1);
                }
            };

            @Override
            public Observable<RxBleInternalScanResult> call(Observable<RxBleInternalScanResult> observable) {
                return observable.publish(new Func1<Observable<RxBleInternalScanResult>, Observable<RxBleInternalScanResult>>() {
                    @Override
                    public Observable<RxBleInternalScanResult> call(final Observable<RxBleInternalScanResult> publishedObservable) {
                        final Func0<Observable<?>> closeTenSecondsAfterMostRecentEmissionFunc = new Func0<Observable<?>>() {
                            @Override
                            public Observable<?> call() {
                                return publishedObservable.switchMap(emitAfterTimerFunc);
                            }
                        };
                        return publishedObservable
                                .window(closeTenSecondsAfterMostRecentEmissionFunc)
                                .flatMap(takeFirstFromEachWindowFunc)
                                .map(toFirstMatchFunc);
                    }
                });
            }
        };
    }

    Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult> emulateScanMode(@ScanSettings.ScanMode int scanMode) {
        switch (scanMode) {

            case ScanSettings.SCAN_MODE_BALANCED:
                return scanModeBalancedTransformer();
            case ScanSettings.SCAN_MODE_OPPORTUNISTIC:
                RxBleLog.d("Cannot emulate opportunistic scan mode since it is OS dependent - fallthrough to low power");
                // fallthrough
            case ScanSettings.SCAN_MODE_LOW_POWER:
                return scanModeLowPowerTransformer();
            case ScanSettings.SCAN_MODE_LOW_LATENCY:
                // return the original observable - fallthrough
            default: // checkstyle always needs default
                return identityTransformer();
        }
    }

    private Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult> scanModeBalancedTransformer() {
        return repeatedWindowTransformer(2500);
    }

    private Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult> scanModeLowPowerTransformer() {
        return repeatedWindowTransformer(500);
    }

    /**
     * A convenience method for running a scan for a period of time and repeat in five seconds intervals.
     * @param windowInMillis window for which the observable should be active
     * @return Observable.Transformer that will take the original observable for specific time and repeat subscription after 5 seconds
     */
    private Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult> repeatedWindowTransformer(
            @IntRange(from = 0, to = 4999) final int windowInMillis
    ) {
        final long repeatCycleTimeInMillis = TimeUnit.SECONDS.toMillis(5);
        final long delayToNextWindow = Math.max(repeatCycleTimeInMillis - windowInMillis, 0); // to be sure that it won't be negative
        return new Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult>() {
            @Override
            public Observable<RxBleInternalScanResult> call(final Observable<RxBleInternalScanResult> rxBleInternalScanResultObservable) {
                return rxBleInternalScanResultObservable.take(windowInMillis, TimeUnit.MILLISECONDS, scheduler)
                        .repeatWhen(new Func1<Observable<? extends Void>, Observable<?>>() {
                            @Override
                            public Observable<?> call(Observable<? extends Void> observable) {
                                return observable.delay(
                                        delayToNextWindow,
                                        TimeUnit.MILLISECONDS,
                                        scheduler
                                );
                            }
                        });
            }
        };
    }

    Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult> emulateCallbackType(
            @ScanSettings.CallbackType final int callbackType) {
        switch (callbackType) {
            case ScanSettings.CALLBACK_TYPE_FIRST_MATCH:
                return splitByAddressAndForEach(emulateFirstMatch);
            case ScanSettings.CALLBACK_TYPE_MATCH_LOST:
                return splitByAddressAndForEach(emulateMatchLost);
            case ScanSettings.CALLBACK_TYPE_FIRST_MATCH | ScanSettings.CALLBACK_TYPE_MATCH_LOST:
                return splitByAddressAndForEach(emulateFirstMatchAndMatchLost);
            case ScanSettings.CALLBACK_TYPE_ALL_MATCHES:
                // return the original observable - fallthrough
            default: // checkstyle always needs default
                return identityTransformer();
        }
    }

    private Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult> splitByAddressAndForEach(
            final Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult> compose
    ) {
        return new Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult>() {
            @Override
            public Observable<RxBleInternalScanResult> call(Observable<RxBleInternalScanResult> observable) {
                return observable
                        .groupBy(new Func1<RxBleInternalScanResult, String>() {
                            @Override
                            public String call(RxBleInternalScanResult rxBleInternalScanResult) {
                                return rxBleInternalScanResult.getBluetoothDevice().getAddress();
                            }
                        })
                        .flatMap(new Func1<GroupedObservable<String, RxBleInternalScanResult>, Observable<RxBleInternalScanResult>>() {
                            @Override
                            public Observable<RxBleInternalScanResult> call(
                                    GroupedObservable<String, RxBleInternalScanResult> groupedObservable) {
                                return groupedObservable.compose(compose);
                            }
                        });
            }
        };
    }

    private Func1<RxBleInternalScanResult, RxBleInternalScanResult> toFirstMatch() {
        return new Func1<RxBleInternalScanResult, RxBleInternalScanResult>() {
            @Override
            public RxBleInternalScanResult call(RxBleInternalScanResult rxBleInternalScanResult) {
                return new RxBleInternalScanResult(
                        rxBleInternalScanResult.getBluetoothDevice(),
                        rxBleInternalScanResult.getRssi(),
                        rxBleInternalScanResult.getTimestampNanos(),
                        rxBleInternalScanResult.getScanRecord(),
                        ScanCallbackType.CALLBACK_TYPE_FIRST_MATCH
                );
            }
        };
    }

    private Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult> emulateMatchLost
            = new Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult>() {
        @Override
        public Observable<RxBleInternalScanResult> call(Observable<RxBleInternalScanResult> observable) {
            return observable.debounce(10, TimeUnit.SECONDS, scheduler).map(toMatchLost());
        }
    };

    private Func1<RxBleInternalScanResult, RxBleInternalScanResult> toMatchLost() {
        return new Func1<RxBleInternalScanResult, RxBleInternalScanResult>() {
            @Override
            public RxBleInternalScanResult call(RxBleInternalScanResult rxBleInternalScanResult) {
                return new RxBleInternalScanResult(
                        rxBleInternalScanResult.getBluetoothDevice(),
                        rxBleInternalScanResult.getRssi(),
                        rxBleInternalScanResult.getTimestampNanos(),
                        rxBleInternalScanResult.getScanRecord(),
                        ScanCallbackType.CALLBACK_TYPE_MATCH_LOST
                );
            }
        };
    }

    private Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult> emulateFirstMatchAndMatchLost
            = new Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult>() {
        @Override
        public Observable<RxBleInternalScanResult> call(Observable<RxBleInternalScanResult> observable) {
            return observable.publish(new Func1<Observable<RxBleInternalScanResult>, Observable<RxBleInternalScanResult>>() {
                @Override
                public Observable<RxBleInternalScanResult> call(Observable<RxBleInternalScanResult> observable) {
                    return Observable.merge(
                            observable.compose(emulateFirstMatch),
                            observable.compose(emulateMatchLost)
                    );
                }
            });
        }
    };
}
