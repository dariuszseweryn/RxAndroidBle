package com.polidea.rxandroidble2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.polidea.rxandroidble2.helpers.LocationServicesOkObservable;
import com.polidea.rxandroidble2.internal.DeviceComponent;
import com.polidea.rxandroidble2.internal.scan.BackgroundScannerImpl;
import com.polidea.rxandroidble2.internal.scan.InternalToExternalScanResultConverter;
import com.polidea.rxandroidble2.internal.scan.RxBleInternalScanResult;
import com.polidea.rxandroidble2.internal.scan.ScanPreconditionsVerifier;
import com.polidea.rxandroidble2.internal.scan.ScanPreconditionsVerifierApi18;
import com.polidea.rxandroidble2.internal.scan.ScanPreconditionsVerifierApi24;
import com.polidea.rxandroidble2.internal.scan.ScanSetupBuilder;
import com.polidea.rxandroidble2.internal.scan.ScanSetupBuilderImplApi18;
import com.polidea.rxandroidble2.internal.scan.ScanSetupBuilderImplApi21;
import com.polidea.rxandroidble2.internal.scan.ScanSetupBuilderImplApi23;
import com.polidea.rxandroidble2.internal.serialization.ClientOperationQueue;
import com.polidea.rxandroidble2.internal.serialization.ClientOperationQueueImpl;
import com.polidea.rxandroidble2.internal.serialization.RxBleThreadFactory;
import com.polidea.rxandroidble2.internal.util.LocationServicesOkObservableApi23Factory;
import com.polidea.rxandroidble2.internal.util.LocationServicesStatus;
import com.polidea.rxandroidble2.internal.util.LocationServicesStatusApi18;
import com.polidea.rxandroidble2.internal.util.LocationServicesStatusApi23;
import com.polidea.rxandroidble2.internal.util.ObservableUtil;
import com.polidea.rxandroidble2.scan.BackgroundScanner;
import com.polidea.rxandroidble2.scan.ScanResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bleshadow.dagger.Binds;
import bleshadow.dagger.BindsInstance;
import bleshadow.dagger.Component;
import bleshadow.dagger.Module;
import bleshadow.dagger.Provides;
import bleshadow.javax.inject.Named;
import bleshadow.javax.inject.Provider;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

@ClientScope
@Component(modules = {ClientComponent.ClientModule.class})
public interface ClientComponent {

    class NamedExecutors {

        public static final String BLUETOOTH_INTERACTION = "executor_bluetooth_interaction";
        public static final String CONNECTION_QUEUE = "executor_connection_queue";
        private NamedExecutors() {

        }
    }

    class NamedSchedulers {

        public static final String COMPUTATION = "computation";
        public static final String TIMEOUT = "timeout";
        public static final String BLUETOOTH_INTERACTION = "bluetooth_interaction";
        public static final String BLUETOOTH_CALLBACKS = "bluetooth_callbacks";
        private NamedSchedulers() {

        }
    }

    class PlatformConstants {

        public static final String INT_TARGET_SDK = "target-sdk";
        public static final String INT_DEVICE_SDK = "device-sdk";
        public static final String BOOL_IS_ANDROID_WEAR = "android-wear";
        public static final String STRING_ARRAY_SCAN_PERMISSIONS = "scan-permissions";
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

    @Component.Builder
    interface Builder {
        ClientComponent build();

        @BindsInstance
        Builder applicationContext(Context context);
    }

    @Module(subcomponents = DeviceComponent.class)
    abstract class ClientModule {

        @Provides
        static BluetoothManager provideBluetoothManager(Context context) {
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
        @Named(PlatformConstants.STRING_ARRAY_SCAN_PERMISSIONS)
        static String[] provideRecommendedScanRuntimePermissionNames(
                @Named(PlatformConstants.INT_DEVICE_SDK) int deviceSdk,
                @Named(PlatformConstants.INT_TARGET_SDK) int targetSdk
        ) {
            int sdkVersion = Math.min(deviceSdk, targetSdk);
            if (sdkVersion < 23 /* pre Android M */) {
                // Before API 23 (Android M) no runtime permissions are needed
                return new String[]{};
            }
            if (sdkVersion < 29 /* pre Android 10 */) {
                // Since API 23 (Android M) ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION allows for getting scan results
                return new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                };
            }
            // Since API 29 (Android 10) only ACCESS_FINE_LOCATION allows for getting scan results
            return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        }

        @Provides
        static ContentResolver provideContentResolver(Context context) {
            return context.getContentResolver();
        }

        @Provides
        static LocationServicesStatus provideLocationServicesStatus(
                @Named(PlatformConstants.INT_DEVICE_SDK) int deviceSdk,
                Provider<LocationServicesStatusApi18> locationServicesStatusApi18Provider,
                Provider<LocationServicesStatusApi23> locationServicesStatusApi23Provider
        ) {
            return deviceSdk < Build.VERSION_CODES.M
                    ? locationServicesStatusApi18Provider.get()
                    : locationServicesStatusApi23Provider.get();
        }

        @Provides
        @Named(NamedBooleanObservables.LOCATION_SERVICES_OK)
        static Observable<Boolean> provideLocationServicesOkObservable(
                @Named(PlatformConstants.INT_DEVICE_SDK) int deviceSdk,
                LocationServicesOkObservableApi23Factory locationServicesOkObservableApi23Factory
        ) {
            return deviceSdk < Build.VERSION_CODES.M
                    ? ObservableUtil.justOnNext(true) // there is no need for one before Marshmallow
                    : locationServicesOkObservableApi23Factory.get();
        }

        @Provides
        @Named(NamedExecutors.CONNECTION_QUEUE)
        @ClientScope
        static ExecutorService provideConnectionQueueExecutorService() {
            return Executors.newCachedThreadPool();
        }

        @Provides
        @Named(NamedExecutors.BLUETOOTH_INTERACTION)
        @ClientScope
        static ExecutorService provideBluetoothInteractionExecutorService() {
            return Executors.newSingleThreadExecutor();
        }

        @Provides
        @Named(NamedSchedulers.BLUETOOTH_INTERACTION)
        @ClientScope
        static Scheduler provideBluetoothInteractionScheduler(@Named(NamedExecutors.BLUETOOTH_INTERACTION) ExecutorService service) {
            return Schedulers.from(service);
        }

        @Provides
        @Named(NamedSchedulers.BLUETOOTH_CALLBACKS)
        @ClientScope
        static Scheduler provideBluetoothCallbacksScheduler() {
            return RxJavaPlugins.createSingleScheduler(new RxBleThreadFactory());
        }

        @Provides
        static ClientComponentFinalizer provideFinalizationCloseable(
                @Named(NamedExecutors.BLUETOOTH_INTERACTION) final ExecutorService interactionExecutorService,
                @Named(NamedSchedulers.BLUETOOTH_CALLBACKS) final Scheduler callbacksScheduler,
                @Named(NamedExecutors.CONNECTION_QUEUE) final ExecutorService connectionQueueExecutorService
        ) {
            return new ClientComponentFinalizer() {
                @Override
                public void onFinalize() {
                    interactionExecutorService.shutdown();
                    callbacksScheduler.shutdown();
                    connectionQueueExecutorService.shutdown();
                }
            };
        }

        @Provides
        static LocationManager provideLocationManager(Context context) {
            return (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        @Provides
        @Named(PlatformConstants.INT_TARGET_SDK)
        static int provideTargetSdk(Context context) {
            try {
                return context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).targetSdkVersion;
            } catch (Throwable catchThemAll) {
                return Integer.MAX_VALUE;
            }
        }

        @Provides
        @Named(PlatformConstants.BOOL_IS_ANDROID_WEAR)
        @SuppressLint("InlinedApi")
        static boolean provideIsAndroidWear(Context context, @Named(PlatformConstants.INT_DEVICE_SDK) int deviceSdk) {
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

        @Provides
        static ScanPreconditionsVerifier provideScanPreconditionVerifier(
                @Named(PlatformConstants.INT_DEVICE_SDK) int deviceSdk,
                Provider<ScanPreconditionsVerifierApi18> scanPreconditionVerifierForApi18,
                Provider<ScanPreconditionsVerifierApi24> scanPreconditionVerifierForApi24
        ) {
            if (deviceSdk < Build.VERSION_CODES.N) {
                return scanPreconditionVerifierForApi18.get();
            } else {
                return scanPreconditionVerifierForApi24.get();
            }
        }

        @Binds
        abstract Observable<RxBleAdapterStateObservable.BleAdapterState> bindStateObs(RxBleAdapterStateObservable stateObservable);

        @Binds
        abstract BackgroundScanner bindBackgroundScanner(BackgroundScannerImpl backgroundScannerImpl);

        @Binds
        @ClientScope
        abstract RxBleClient bindRxBleClient(RxBleClientImpl rxBleClient);

        @Binds
        @ClientScope
        abstract ClientOperationQueue bindClientOperationQueue(ClientOperationQueueImpl clientOperationQueue);

        @Binds
        @Named(NamedSchedulers.TIMEOUT)
        abstract Scheduler bindTimeoutScheduler(@Named(NamedSchedulers.COMPUTATION) Scheduler computationScheduler);

        @Binds
        abstract Function<RxBleInternalScanResult, ScanResult> provideScanResultMapper(InternalToExternalScanResultConverter mapper);
    }

    LocationServicesOkObservable locationServicesOkObservable();

    RxBleClient rxBleClient();

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface ClientComponentFinalizer {

        void onFinalize();
    }
}
