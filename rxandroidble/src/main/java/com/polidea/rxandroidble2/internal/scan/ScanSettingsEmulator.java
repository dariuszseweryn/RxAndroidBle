package com.polidea.rxandroidble2.internal.scan;


import android.support.annotation.IntRange;

import com.polidea.rxandroidble2.ClientComponent;
import com.polidea.rxandroidble2.internal.RxBleLog;
import com.polidea.rxandroidble2.scan.ScanCallbackType;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.concurrent.TimeUnit;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;
import io.reactivex.observables.GroupedObservable;

import static com.polidea.rxandroidble2.internal.util.ObservableUtil.identityTransformer;

public class ScanSettingsEmulator {

    private final Scheduler scheduler;
    private ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult> emulateFirstMatch;

    @Inject
    public ScanSettingsEmulator(@Named(ClientComponent.NamedSchedulers.COMPUTATION) final Scheduler scheduler) {
        this.scheduler = scheduler;

        this.emulateFirstMatch = new ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult>() {

            private Function<RxBleInternalScanResult, RxBleInternalScanResult> toFirstMatchFunc = toFirstMatch();
            private final Observable<Long> timerObservable = Observable.timer(10L, TimeUnit.SECONDS, scheduler);
            private final Function<RxBleInternalScanResult, Observable<?>> emitAfterTimerFunc
                    = new Function<RxBleInternalScanResult, Observable<?>>() {
                @Override
                public Observable<?> apply(RxBleInternalScanResult internalScanResult) {
                    return timerObservable;
                }
            };
            private final Function<Observable<RxBleInternalScanResult>, Observable<RxBleInternalScanResult>> takeFirstFromEachWindowFunc
                    = new Function<Observable<RxBleInternalScanResult>, Observable<RxBleInternalScanResult>>() {
                @Override
                public Observable<RxBleInternalScanResult> apply(
                        Observable<RxBleInternalScanResult> rxBleInternalScanResultObservable) {
                    return rxBleInternalScanResultObservable.take(1);
                }
            };

            @Override
            public Observable<RxBleInternalScanResult> apply(Observable<RxBleInternalScanResult> observable) {
                return observable.publish(new Function<Observable<RxBleInternalScanResult>, ObservableSource<RxBleInternalScanResult>>() {

                    @Override
                    public ObservableSource<RxBleInternalScanResult>
                    apply(final Observable<RxBleInternalScanResult> publishedObservable) throws Exception {
                        final Observable<Object> closeTenSecondsAfterMostRecentEmissionFunc = publishedObservable
                                .switchMap(emitAfterTimerFunc);
                        return publishedObservable
                                .window(closeTenSecondsAfterMostRecentEmissionFunc)
                                .flatMap(takeFirstFromEachWindowFunc)
                                .map(toFirstMatchFunc);
                    }
                });
            }
        };
    }

    ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult> emulateScanMode(@ScanSettings.ScanMode int scanMode) {
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

    private ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult> scanModeBalancedTransformer() {
        return repeatedWindowTransformer(2500);
    }

    private ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult> scanModeLowPowerTransformer() {
        return repeatedWindowTransformer(500);
    }

    /**
     * A convenience method for running a scan for a period of time and repeat in five seconds intervals.
     *
     * @param windowInMillis window for which the observable should be active
     * @return Observable.Transformer that will take the original observable for specific time and repeat subscription after 5 seconds
     */
    private ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult> repeatedWindowTransformer(
            @IntRange(from = 0, to = 4999) final int windowInMillis
    ) {
        final long repeatCycleTimeInMillis = TimeUnit.SECONDS.toMillis(5);
        final long delayToNextWindow = Math.max(repeatCycleTimeInMillis - windowInMillis, 0); // to be sure that it won't be negative
        return new ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult>() {
            @Override
            public Observable<RxBleInternalScanResult> apply(final Observable<RxBleInternalScanResult> rxBleInternalScanResultObservable) {
                return rxBleInternalScanResultObservable.take(windowInMillis, TimeUnit.MILLISECONDS, scheduler)
                        .repeatWhen(new Function<Observable<Object>, ObservableSource<?>>() {
                            @Override
                            public ObservableSource<?> apply(Observable<Object> observable) throws Exception {
                                return observable.delay(delayToNextWindow, TimeUnit.MILLISECONDS, scheduler
                                );
                            }
                        });
            }
        };
    }

    ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult> emulateCallbackType(
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

    private ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult> splitByAddressAndForEach(
            final ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult> compose
    ) {
        return new ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult>() {
            @Override
            public Observable<RxBleInternalScanResult> apply(Observable<RxBleInternalScanResult> observable) {
                return observable
                        .groupBy(new Function<RxBleInternalScanResult, String>() {
                            @Override
                            public String apply(RxBleInternalScanResult rxBleInternalScanResult) {
                                return rxBleInternalScanResult.getBluetoothDevice().getAddress();
                            }
                        })
                        .flatMap(new Function<GroupedObservable<String, RxBleInternalScanResult>, Observable<RxBleInternalScanResult>>() {
                            @Override
                            public Observable<RxBleInternalScanResult> apply(
                                    GroupedObservable<String, RxBleInternalScanResult> groupedObservable) {
                                return groupedObservable.compose(compose);
                            }
                        });
            }
        };
    }

    private Function<RxBleInternalScanResult, RxBleInternalScanResult> toFirstMatch() {
        return new Function<RxBleInternalScanResult, RxBleInternalScanResult>() {
            @Override
            public RxBleInternalScanResult apply(RxBleInternalScanResult rxBleInternalScanResult) {
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

    private ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult> emulateMatchLost
            = new ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult>() {
        @Override
        public Observable<RxBleInternalScanResult> apply(Observable<RxBleInternalScanResult> observable) {
            return observable.debounce(10, TimeUnit.SECONDS, scheduler).map(toMatchLost());
        }
    };

    private Function<RxBleInternalScanResult, RxBleInternalScanResult> toMatchLost() {
        return new Function<RxBleInternalScanResult, RxBleInternalScanResult>() {
            @Override
            public RxBleInternalScanResult apply(RxBleInternalScanResult rxBleInternalScanResult) {
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

    private ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult> emulateFirstMatchAndMatchLost
            = new ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult>() {
        @Override
        public Observable<RxBleInternalScanResult> apply(Observable<RxBleInternalScanResult> observable) {
            return observable.publish(new Function<Observable<RxBleInternalScanResult>, Observable<RxBleInternalScanResult>>() {
                @Override
                public Observable<RxBleInternalScanResult> apply(Observable<RxBleInternalScanResult> observable) {
                    return Observable.merge(
                            observable.compose(emulateFirstMatch),
                            observable.compose(emulateMatchLost)
                    );
                }
            });
        }
    };
}
