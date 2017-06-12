package com.polidea.rxandroidble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.Nullable;
import com.polidea.rxandroidble.helpers.LocationServicesOkObservable;
import com.polidea.rxandroidble.internal.DeviceComponent;
import com.polidea.rxandroidble.internal.scan.InternalToExternalScanResultConverter;
import com.polidea.rxandroidble.internal.scan.RxBleInternalScanResult;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.radio.RxBleRadioImpl;
import com.polidea.rxandroidble.internal.scan.ScanSetupBuilder;
import com.polidea.rxandroidble.internal.scan.ScanSetupBuilderImplApi18;
import com.polidea.rxandroidble.internal.scan.ScanSetupBuilderImplApi21;
import com.polidea.rxandroidble.internal.scan.ScanSetupBuilderImplApi23;
import com.polidea.rxandroidble.scan.ScanResult;
import dagger.Binds;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Named;
import javax.inject.Provider;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

@ClientScope
@Component(modules = {ClientComponent.ClientModule.class, ClientComponent.ClientModuleBinder.class})
public interface ClientComponent {

    class NamedSchedulers {

        public static final String MAIN_THREAD = "main-thread";
        public static final String COMPUTATION = "computation";
        public static final String RADIO_OPERATIONS = "callback-emitter";
        public static final String TIMEOUT = "timeout";
        public static final String GATT_CALLBACK = "callback";
        private NamedSchedulers() {

        }
    }

    class PlatformConstants {

        public static final String INT_TARGET_SDK = "target-sdk";
        public static final String INT_DEVICE_SDK = "device-sdk";
        public static final String BOOL_IS_ANDROID_WEAR = "android-wear";
        private PlatformConstants() {

        }
    }

    class NamedBooleanObservables {

        public static final String LOCATION_SERVICES_OK = "location-ok-boolean-observable";
        private NamedBooleanObservables() {

        }
    }

    class BluetoothConstants {

        public static final String ENABLE_NOTIFICATION_VALUE = "enable-notification-value";
        public static final String ENABLE_INDICATION_VALUE = "enable-indication-value";
        public static final String DISABLE_NOTIFICATION_VALUE = "disable-notification-value";
        private BluetoothConstants() {

        }
    }

    @Module(subcomponents = DeviceComponent.class)
    class ClientModule {

        private final Context context;

        public ClientModule(Context context) {
            this.context = context;
        }

        @Provides
        Context provideApplicationContext() {
            return context;
        }

        @Provides
        BluetoothManager provideBluetoothManager() {
            return (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        }

        @Provides
        @Nullable
        static BluetoothAdapter provideBluetoothAdapter() {
            return BluetoothAdapter.getDefaultAdapter();
        }

        @Provides
        @Named(NamedSchedulers.COMPUTATION)
        static Scheduler provideComputationScheduler() {
            return Schedulers.computation();
        }

        @Provides
        @Named(PlatformConstants.INT_DEVICE_SDK)
        static int provideDeviceSdk() {
            return Build.VERSION.SDK_INT;
        }

        @Provides
        @Named(NamedSchedulers.GATT_CALLBACK)
        @ClientScope
        static ExecutorService provideGattCallbackExecutorService() {
            return Executors.newSingleThreadExecutor();
        }

        @Provides
        @Named(NamedSchedulers.GATT_CALLBACK)
        @ClientScope
        static Scheduler provideGattCallbackScheduler(@Named(NamedSchedulers.GATT_CALLBACK) ExecutorService executorService) {
            return Schedulers.from(executorService);
        }

        @Provides
        LocationManager provideLocationManager() {
            return (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        @Provides
        @Named(NamedSchedulers.MAIN_THREAD)
        static Scheduler provideMainThreadScheduler() {
            return AndroidSchedulers.mainThread();
        }

        @Provides
        @Named(PlatformConstants.INT_TARGET_SDK)
        int provideTargetSdk() {
            try {
                return context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).targetSdkVersion;
            } catch (Throwable catchThemAll) {
                return Integer.MAX_VALUE;
            }
        }

        @Provides
        @Named(PlatformConstants.BOOL_IS_ANDROID_WEAR)
        @SuppressLint("InlinedApi")
        boolean provideIsAndroidWear(@Named(PlatformConstants.INT_DEVICE_SDK) int deviceSdk) {
            return deviceSdk >= Build.VERSION_CODES.KITKAT_WATCH
                    && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
        }

        @Provides
        @ClientScope
        static ScanSetupBuilder provideScanSetupProvider(
                @Named(PlatformConstants.INT_DEVICE_SDK) int deviceSdk,
                Provider<ScanSetupBuilderImplApi18> scanSetupBuilderProviderForApi18,
                Provider<ScanSetupBuilderImplApi21> scanSetupBuilderProviderForApi21,
                Provider<ScanSetupBuilderImplApi23> scanSetupBuilderProviderForApi23
        ) {
            if (deviceSdk < Build.VERSION_CODES.LOLLIPOP) {
                return scanSetupBuilderProviderForApi18.get();
            } else if (deviceSdk < Build.VERSION_CODES.M) {
                return scanSetupBuilderProviderForApi21.get();
            }
            return scanSetupBuilderProviderForApi23.get();
        }

        @Provides
        @Named(BluetoothConstants.ENABLE_NOTIFICATION_VALUE)
        static byte[] provideEnableNotificationValue() {
            return BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
        }

        @Provides
        @Named(BluetoothConstants.ENABLE_INDICATION_VALUE)
        static byte[] provideEnableIndicationValue() {
            return BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
        }

        @Provides
        @Named(BluetoothConstants.DISABLE_NOTIFICATION_VALUE)
        static byte[] provideDisableNotificationValue() {
            return BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
        }
    }

    @Module
    abstract class ClientModuleBinder {

        @Binds
        abstract Observable<RxBleAdapterStateObservable.BleAdapterState> bindStateObs(RxBleAdapterStateObservable stateObservable);

        @Binds
        @Named(NamedBooleanObservables.LOCATION_SERVICES_OK)
        abstract Observable<Boolean> bindLocationServicesOkObs(LocationServicesOkObservable locationServicesOkObservable);

        @Binds
        @ClientScope
        abstract RxBleClient bindRxBleClient(RxBleClientImpl rxBleClient);

        @Binds
        @ClientScope
        abstract RxBleRadio bindRxBleRadio(RxBleRadioImpl rxBleRadio);

        @Binds
        @Named(NamedSchedulers.RADIO_OPERATIONS)
        abstract Scheduler bindCallbackScheduler(@Named(NamedSchedulers.MAIN_THREAD) Scheduler mainThreadScheduler);

        @Binds
        @Named(NamedSchedulers.TIMEOUT)
        abstract Scheduler bindTimeoutScheduler(@Named(NamedSchedulers.COMPUTATION) Scheduler computationScheduler);

        @Binds
        abstract Func1<RxBleInternalScanResult, ScanResult> provideScanResultMapper(InternalToExternalScanResultConverter mapper);
    }

    LocationServicesOkObservable locationServicesOkObservable();

    RxBleClient rxBleClient();
}
