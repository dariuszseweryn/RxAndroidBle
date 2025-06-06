Change Log
==========
Version 1.19.1
* Fixed `BluetoothAdapter.ACTION_STATE_CHANGED` BroadcastReceiver leak (https://github.com/dariuszseweryn/RxAndroidBle/pull/864)

Version 1.19.0
* Added `ScanResult#getAdvertisingSid`, thanks @KrzysztofMamak (https://github.com/dariuszseweryn/RxAndroidBle/pull/856)

Version 1.18.1
* Fixed `CharacteristicLongWriteOperation` defaults for API 33 (Android 13) (https://github.com/dariuszseweryn/RxAndroidBle/pull/847)

Version 1.18.0
* Added `RxBleConnection#readPhy` and `RxBleConnection#setPreferredPhy`, thanks @JamesDougherty (https://github.com/dariuszseweryn/RxAndroidBle/pull/840)

Version 1.17.2
* Fixed log tags generation on builds with Proguard enabled (https://github.com/Polidea/RxAndroidBle/pull/814)

Version 1.17.1
* No changes over 1.17.0. Mistake release.

Version 1.17.0
* Changed RxBleConnection.GATT_MTU_MAXIMUM to 515 to fix Android 13, thanks @marsounjan! (https://github.com/Polidea/RxAndroidBle/pull/808)

Version 1.16.0
* Added `ScanSettingsBuilder#setLegacy` option, thanks @danielstuart14! (https://github.com/Polidea/RxAndroidBle/pull/801)

Version 1.15.2
* Fixed `RxBleDevice#toString` crashing when runtime permission BLUETOOTH_CONNECT is not granted on API 31 (Android 12) (https://github.com/Polidea/RxAndroidBle/pull/800)
* Added granted permissions cache (https://github.com/Polidea/RxAndroidBle/pull/802)
* Updated `RxBleClient#getBondedDevices` Javadoc for usage on API 31 (Android 12) (https://github.com/Polidea/RxAndroidBle/pull/804)

Version 1.15.1
* Fixed duplicate 'META-INF/com.google.dagger_dagger.version' file (regression) (https://github.com/Polidea/RxAndroidBle/pull/794)

Version 1.15.0
* No functional changes. First release of RxAndroidBle based on RxJava 3. RxJava 2 version is still supported. (https://github.com/Polidea/RxAndroidBle/pull/793)

Version 1.14.1
* Fixed duplicate 'META-INF/com.google.dagger_dagger.version' file (https://github.com/Polidea/RxAndroidBle/pull/790)

Version 1.14.0
* Added `RxBleClient#getConnectedPeripherals` for retrieving all BLE peripherals connected to the device, thanks @DaBs! (https://github.com/Polidea/RxAndroidBle/pull/765)
* Updated URL for native GATT statuses in `BleGattException`, thanks @ariccio! (https://github.com/Polidea/RxAndroidBle/pull/779)
* Added `BleScanResult#isConnectable` for getting info if the scanned peripheral is connectable, thanks @MartinSadovy! (https://github.com/Polidea/RxAndroidBle/pull/781)
* Added helper functions `RxBleClient#isConnectRuntimePermissionGranted` and `RxBleClient#getRecommendedConnectRuntimePermissions` (https://github.com/Polidea/RxAndroidBle/pull/787)

Version 1.13.1
* Fixed 'BLUETOOTH_SCAN' permission entry in Android Manifest for apps that do not build using Android 12 target (https://github.com/Polidea/RxAndroidBle/pull/767)

Version 1.13.0
* **Changed behaviour** of `RxBleClient#getRecommendedScanRuntimePermissions` (https://github.com/Polidea/RxAndroidBle/pull/762)
* Adjusted checks for scanning in regards to API 31 (Android 12) (https://github.com/Polidea/RxAndroidBle/pull/762)
* Adjusted readme for new BLE permission system introduced with API 31 (https://github.com/Polidea/RxAndroidBle/pull/762)

Version 1.12.1
* Added a new location check for API 28+ (https://github.com/Polidea/RxAndroidBle/pull/747)
* Fixed requirements for location services check (https://github.com/Polidea/RxAndroidBle/pull/748)

Version 1.12.0
* Fixed a leak in `ScanOperationApi21`, thanks @seotrader (https://github.com/Polidea/RxAndroidBle/pull/708)
* Added Service Solicitation UUIDs support, thanks @nrbrook! (https://github.com/Polidea/RxAndroidBle/pull/711)
* [mockclient] Added builder for `RxBleScanRecordMock`, thanks @nrbrook! (https://github.com/Polidea/RxAndroidBle/pull/703)
* [mockclient] Changed `RxBleClientMock.DeviceBuilder()` => `RxBleDeviceMock.Builder`. Structural changes to creating devices, thanks @nrbrook! (https://github.com/Polidea/RxAndroidBle/pull/706)
* [mockclient] Added builder functions to provide callbacks for characteristic/descriptor writes/reads, thanks @nrbrook! (https://github.com/Polidea/RxAndroidBle/pull/707)
* [mockclient] Added constructor for `RxScanRecordMock`, thanks @nrbrook! (https://github.com/Polidea/RxAndroidBle/pull/712)
* [mockclient] Fixed behaviour of `RxBleClientMock.observeStateChanges()` (https://github.com/Polidea/RxAndroidBle/pull/744)

Version 1.11.1
* Fixed `NullPointerException` when logging failed `BluetoothGattCharacteristic` initial reads (https://github.com/Polidea/RxAndroidBle/pull/679)
* [mockclient] Added support for `BluetoothDevice` in `RxBleDeviceMock` (https://github.com/Polidea/RxAndroidBle/pull/676)

Version 1.11.0
* Added workaround for scans with settings match found and/or lost but no filters (https://github.com/Polidea/RxAndroidBle/pull/612)
* Fixed typo in `BleCharacteristicNotFoundException` (https://github.com/Polidea/RxAndroidBle/pull/625)
* Added scan permission helper functions (https://github.com/Polidea/RxAndroidBle/pull/642)

Version 1.10.5
* Fixed possibly incorrect order of notifications and operation completions (https://github.com/Polidea/RxAndroidBle/pull/639)
* Fixed possibility of library being stalled on operation cancelling (https://github.com/Polidea/RxAndroidBle/pull/650)
* Fixed a silenced `IllegalStateException` after a scan is stopped because `BluetoothAdapter` being disabled (https://github.com/Polidea/RxAndroidBle/pull/649)   

Version 1.10.4
* Fixed location permission check for Android 10 (https://github.com/Polidea/RxAndroidBle/pull/640)

Version 1.10.3
* Fixed `RxBleConnection.observeConnectionParametersUpdates()` not working in obfuscated apps. Added consumer `proguard-rules.pro` Proguard config file. (https://github.com/Polidea/RxAndroidBle/pull/634) 
* Fixed log statement on reading RSSI of connection (https://github.com/Polidea/RxAndroidBle/pull/631)
* Fixed log statement on setting `CONNECTION_PRIORITY_HIGH` (https://github.com/Polidea/RxAndroidBle/pull/623)

Version 1.10.2
* Fixed possible `UndeliverableException` when scan subscription is quickly disposed (https://github.com/Polidea/RxAndroidBle/pull/609)
* Minor allocation decrease in `ScanSetupBuilderImplApi21` (https://github.com/Polidea/RxAndroidBle/pull/613)

Version 1.10.1
* Fixed `IllegalStateException` in `RxBleAdapterStateObservable` (https://github.com/Polidea/RxAndroidBle/pull/596)

Version 1.10.0
* Added possibility to disable logs of scanned peripherals via `LogOptions.Builder.setShouldLogScannedPeripherals()` (https://github.com/Polidea/RxAndroidBle/pull/579)
* Added `RxBleConnection.observeConnectionParametersUpdates()` function (https://github.com/Polidea/RxAndroidBle/pull/565)
* Minor allocation decrease in `LocationServicesOkObservable` (https://github.com/Polidea/RxAndroidBle/pull/574)
* Fixed leaking `RxBleAdapterStateObservable` (https://github.com/Polidea/RxAndroidBle/pull/575)

Version 1.9.2
* Fixed `IllegalArgumentException` in `LocationServicesOkObservableApi23Factory` (https://github.com/Polidea/RxAndroidBle/pull/573)

Version 1.9.1
* Fixed `RxBleClient.observeStateChanges()` emissions (https://github.com/Polidea/RxAndroidBle/pull/556)
* Fixed `RxBleDevice.establishConnection(boolean, Timeout)` Javadoc (https://github.com/Polidea/RxAndroidBle/pull/558)

Version 1.9.0
* Added possibility to disable Location Services check before scan (https://github.com/Polidea/RxAndroidBle/pull/533)
* Reworked library logging API and behaviour (https://github.com/Polidea/RxAndroidBle/pull/551) 

Version 1.8.2
* Fixed sporadic NullPointerException in DisconnectionRouter (https://github.com/Polidea/RxAndroidBle/pull/553)

Version 1.8.1
* Added more GATT status descriptions (https://github.com/Polidea/RxAndroidBle/pull/543)

Version 1.8.0
* Added `NotificationSetupMode.QUICK_SETUP` for devices which start notifying right after CCC descriptor write (https://github.com/Polidea/RxAndroidBle/pull/478)
* Migrated to androidx usage (https://github.com/Polidea/RxAndroidBle/pull/497) 

Version 1.7.2
* Fixed stalled library (race condition) when trying to connect while BluetoothAdapter is OFF (https://github.com/Polidea/RxAndroidBle/pull/522)
* Fixed logs in DisconnectionRouter (https://github.com/Polidea/RxAndroidBle/pull/523) 

Version 1.7.1
* Fixed possible `IllegalArgumentException` while parsing UUIDs from advertisements (https://github.com/Polidea/RxAndroidBle/pull/485)
* Fixed `NullPointerException` when calling `BackgroundScanner` start / stop scan while `BluetoothAdapter` was not turned ON (https://github.com/Polidea/RxAndroidBle/pull/487)

Version 1.7.0
* Introduced a new API that allows for background scanning in modern Android OS versions (https://github.com/Polidea/RxAndroidBle/issues/369)
* Fixed LocationServicesOkObservable (https://github.com/Polidea/RxAndroidBle/pull/438)
* Added GATT status code to `BleDisconnectionException` (https://github.com/Polidea/RxAndroidBle/pull/405)
* Fixed possible concurrent access to `DisconnectionRouter` (https://github.com/Polidea/RxAndroidBle/pull/442)
* Fixed race condition in `CharacteristicLongWriteOperation` (https://github.com/Polidea/RxAndroidBle/pull/465) 

Version 1.6.0
* Deprecated ConnectionSharingAdapter (https://github.com/Polidea/RxAndroidBle/pull/397)
* Fixed unexpected behaviour of LocationServicesOkObservable if unsubscribed immediately after first emission (https://github.com/Polidea/RxAndroidBle/pull/430)
* Added possibility to modify CustomOperation priority (https://github.com/Polidea/RxAndroidBle/pull/414)
* Fixed stalled library if a just started operation was already unsubscribed (https://github.com/Polidea/RxAndroidBle/pull/428) 

Version 1.5.0
* Added possibility to change default operation timeout (https://github.com/Polidea/RxAndroidBle/pull/321)
* Fixed Dagger2 compatibility (https://github.com/Polidea/RxAndroidBle/pull/342 https://github.com/Polidea/RxAndroidBle/pull/348)
* Fixed DisconnectionRouter leaking subscription to RxBleAdapterStateObservable (https://github.com/Polidea/RxAndroidBle/pull/353)
* Improved Location Services status check (https://github.com/Polidea/RxAndroidBle/issues/327)
* Added logger that prints out GATT server structure on a successful discovery. The log is generated when the logger is in a VERBOSE level (https://github.com/Polidea/RxAndroidBle/pull/355)
* Enhanced operation logger so it displays how long the operation performed. (https://github.com/Polidea/RxAndroidBle/pull/356)
* Added retry strategies for long write operations (https://github.com/Polidea/RxAndroidBle/pull/357)
* Introduced API in RxJava2
* Removed deprecated establishConnection method
* Removed deprecated writeCharacteristic method
* Introduced BleDescriptorNotFoundException

Version 1.5.0 (RxJava1)
* Added possibility to change default operation timeout (https://github.com/Polidea/RxAndroidBle/pull/321)
* Fixed Dagger2 compatibility (https://github.com/Polidea/RxAndroidBle/pull/342 https://github.com/Polidea/RxAndroidBle/pull/348)
* Fixed DisconnectionRouter leaking subscription to RxBleAdapterStateObservable (https://github.com/Polidea/RxAndroidBle/pull/353)
* Improved Location Services status check (https://github.com/Polidea/RxAndroidBle/issues/327)
* Added logger that prints out GATT server structure on a successful discovery. The log is generated when the logger is in a VERBOSE level (https://github.com/Polidea/RxAndroidBle/pull/355)
* Enhanced operation logger so it displays how long the operation performed. (https://github.com/Polidea/RxAndroidBle/pull/356)
* Added retry strategies for long write operations (https://github.com/Polidea/RxAndroidBle/pull/357)

Version 1.4.3 (RxJava1)
* Log informing that the underlying semaphore in a QueueSemaphore has been interrupted will be printed only when the situation was unexpected.(https://github.com/Polidea/RxAndroidBle/issues/317)
* Fixed possible race condition when calling `.doOnSubscribe()` and `.doOnUnsubscribe()` which lead to calling `ConnectionOperationQueueImpl.onConnectionUnsubscribed()` before the `.onConnectionSubscribed()` has returned. (https://github.com/Polidea/RxAndroidBle/issues/308)
* Updated RxJava dependency (https://github.com/Polidea/RxAndroidBle/issues/312)
* Updated to Gradle 3.0.0/Android Studio 3.0 (https://github.com/Polidea/RxAndroidBle/issues/302)
* Nicer exception messages (https://github.com/Polidea/RxAndroidBle/issues/303)

Version 1.4.2 (RxJava1)
* Fixed MTU value not being updated when changed by the peripheral (https://github.com/Polidea/RxAndroidBle/issues/293)
* Added info logs regarding start/stop of scans (https://github.com/Polidea/RxAndroidBle/pull/295) 
* Fixed routing of the actual disconnection error to all queued operations (https://github.com/Polidea/RxAndroidBle/issues/297)

Version 1.4.1 (RxJava1)
* Fixed issue hasObservers conditional for Output class (https://github.com/Polidea/RxAndroidBle/issues/283)

Version 1.4.0 (RxJava1)
* Added native callback usage support in custom operations. You may consider this API if your implementation is performance critical. (https://github.com/Polidea/RxAndroidBle/issues/165)
* Added pre-scan verification for excessive scan (undocumented Android 7.0 "feature") (https://github.com/Polidea/RxAndroidBle/issues/227)
* Adjusted `BleCannotSetCharacteristicNotificationException` to contain the cause exception if available. `RxBleConnection.setupNotification()`/`RxBleConnection.setupIndication()` will now throw the cause of disconnection if subscribed after connection was disconnected. (https://github.com/Polidea/RxAndroidBle/issues/225) 
* _Changed Behaviour_ of `RxBleDevice.observeConnectionStateChanges()` - does not emit initial state and reflects best `BluetoothGatt` state. (https://github.com/Polidea/RxAndroidBle/issues/50)
* Added support for a custom Logger `RxBleLog.setLogger(Logger)` as alternative to Logcat (https://github.com/Polidea/RxAndroidBle/pull/248)
* Added a warning log if user tries to use a characteristic against it's properties (https://github.com/Polidea/RxAndroidBle/issues/224)
* _Changed Behaviour_ — `BluetoothGatt` is now called on a single background thread instead of the main thread (https://github.com/Polidea/RxAndroidBle/pull/255)
* Decoupled command queues for different connections. (https://github.com/Polidea/RxAndroidBle/issues/250)

Version 1.3.4 (RxJava1)
* Added @Nullable annotation to `RxBleDevice.getName()`. (https://github.com/Polidea/RxAndroidBle/issues/263)
* Fixed connection not being disconnected when `DeadObjectException` was raised. (https://github.com/Polidea/RxAndroidBle/issues/275)

Version 1.3.3 (RxJava1)
* Fixed scan filtering by name on API <21 (https://github.com/Polidea/RxAndroidBle/pull/243)
* Fixed race condition (which would cause the library to hang) when using `.first()` on calls to `RxBleConnection` that emit a single result. (https://github.com/Polidea/RxAndroidBle/issues/244) 

Version 1.3.2 (RxJava1)
* Fixed completing the `Observable<byte[]>` emitted by `RxBleConnection.setupNotification()`/`RxBleConnection.setupIndication()` when unsubscribed (https://github.com/Polidea/RxAndroidBle/issues/231)

Version 1.3.1 (RxJava1)
* Fixed unsubscribing from operations before `onComplete()`/`onError()` causing the library to hang. (https://github.com/Polidea/RxAndroidBle/issues/218)

Version 1.3.0 (RxJava1)
* _Changed Behaviour_ of `RxBleConnection` - connection is no longer closed on individual operation errors. (https://github.com/Polidea/RxAndroidBle/issues/26) 
* Added partial support for API 21 BLE scan in a backwards compatible manner. (https://github.com/Polidea/RxAndroidBle/issues/16)
* Added support for filtering by Manufacturer Data (https://github.com/Polidea/RxAndroidBle/issues/127)
* Added RxBleClient State observing for determining what functionality of the library may work (https://github.com/Polidea/RxAndroidBle/issues/55)
* Added `ValueInterpreter` for interpreting standardized (Bluetooth Specification) int/float/String values from byte[] (https://github.com/Polidea/RxAndroidBle/issues/199)
* Added support for requesting connection priority on API 21+ (https://github.com/Polidea/RxAndroidBle/issues/111)

Version 1.2.4 (RxJava1)
* Lowered memory pressure caused by `RxBleLog` when logs are disabled (https://github.com/Polidea/RxAndroidBle/issues/212)

Version 1.2.3 (RxJava1)
* Fixed scan when filter with 32-bit UUID was specified (https://github.com/Polidea/RxAndroidBle/issues/207)
* Fixed memory leak of scan operation (and potentially any other that would emit an infinite stream of events) (https://github.com/Polidea/RxAndroidBle/issues/194)
* Lowered memory pressure when using functions that accept `UUID`.
* Changed `RxBleConnectionState` from a class to an enum for convenience.
* Minor optimisations to `RxBleRadio` queue processing time.
* Updated `RxJava` to 1.3.0

Version 1.2.2 (RxJava1)
* Fixed visibility of `UUIDUtil`. Deprecated it. Introduced `AdvertisedServiceUUIDExtractor` helper, thanks @marciogranzotto! (https://github.com/Polidea/RxAndroidBle/pull/184)

Version 1.2.1 (RxJava1)
* Added `ByteArrayBatchObservable` helper for splitting long byte arrays
* Fixed behaviour in non-Bluetooth environments. (https://github.com/Polidea/RxAndroidBle/issues/158)
* Fixed `RxBleConnectionMock` overwriting `BluetoothCharacteristic` value on setting notification. (https://github.com/Polidea/RxAndroidBle/issues/160)
* Fixed default payload size of Long Write operation when non-standard MTU was negotiated (https://github.com/Polidea/RxAndroidBle/issues/161)
* Added possibility to get the current MTU value of `RxBleConnection` (https://github.com/Polidea/RxAndroidBle/issues/166)
* Fixed retrying of `RxBleClient.scanBleDevices(UUID...)`, thanks @BharathMG! (https://github.com/Polidea/RxAndroidBle/pull/174)
* Fixed connection not being noticed as established due to a race condition (https://github.com/Polidea/RxAndroidBle/issues/178)
* Fixed `BleBluetoothGattCallbackTimeout` macAddress being null on connection (https://github.com/Polidea/RxAndroidBle/issues/178)
* Fixed disconnect operation behaviour in an edge case situation (https://github.com/Polidea/RxAndroidBle/issues/178)

Version 1.2.0 (RxJava1)
* Added Proguard rules for the library. (https://github.com/Polidea/RxAndroidBle/issues/104)
* Added support for MTU negotiation, thanks @pregno!
* Fixed connecting with autoConnect = true on Android 7.0.0+, thanks @JIUgia!
* Fixed notifications for characteristics with the same UUID
* Adjusted scan location services check for various scenarios (https://github.com/Polidea/RxAndroidBle/issues/106)
* Fixed occasional out-of-order emissions from notifications (https://github.com/Polidea/RxAndroidBle/issues/75)
* Fixed stalled operations by adding timeouts (https://github.com/Polidea/RxAndroidBle/issues/118)
* Added `LocationServicesOkObservable` helper for observing if scan can be successfully started
* Added Jack compatibility (https://github.com/Polidea/RxAndroidBle/issues/123)
* Added compatibility mode for notifications on characteristic that do not contain a Client Characteristic Config descriptor
* Fixed logic of UUID filtering during scan
* Added long write support (https://github.com/Polidea/RxAndroidBle/issues/68)
* Fixed issue with a stalled library when write operations were too quick (https://github.com/Polidea/RxAndroidBle/issues/135)
* Optimised logging
* Added support for custom `BluetoothGatt` operations (https://github.com/Polidea/RxAndroidBle/issues/137)
* Updated `RxJava` to 1.2.9
* Added support for scanning on Android Wear
* Internal refactoring introducing Dagger2 support

Version 1.1.0 (RxJava1)
* Fixed issue that sometimes happened where `RxBleRadioOperationConnect` was not yet subscribed while running. (https://github.com/Polidea/RxAndroidBle/issues/94)
* Fixed issue with descriptor writing using parent characteristic write type. (https://github.com/Polidea/RxAndroidBle/issues/93)
* Added `BleScanException.toString()` for a more descriptive stacktrace.
* Added a workaround for a bug while discovering services. (https://github.com/Polidea/RxAndroidBle/issues/86)
* Added a timeout for discovering services. (https://github.com/Polidea/RxAndroidBle/issues/86)
* Fixed calling `BluetoothGatt.disconnect()` on a correct thread. (https://github.com/Polidea/RxAndroidBle/issues/84)
* Fixed library stuck if disconnection happened during operation execution. (https://github.com/Polidea/RxAndroidBle/issues/81)
* Removed reflection call to `BluetoothGatt.connect()` on Android 7.0.0+. (https://github.com/Polidea/RxAndroidBle/issues/83)
* Removed android.support.v4 dependency.
* Added cancelling of connection establishing process. 
* Reduced method count.
* Fixed `RejectedExecutionException` when processing `BluetoothGattCallback`. (https://github.com/Polidea/RxAndroidBle/issues/25) (https://github.com/Polidea/RxAndroidBle/issues/75)

Version 1.0.2 (RxJava1)

* Added Mock RxAndroidBle to the repository
* Added indications handling on RxBleConnection
* Fixed scan operation concurrency issue (https://github.com/Polidea/RxAndroidBle/issues/17)
* Exposed android.bluetooth.BluetoothDevice in RxBleDevice (https://github.com/Polidea/RxAndroidBle/issues/23)
* Fixed stale RxBleRadio on RxBleOperation unhandled throw (https://github.com/Polidea/RxAndroidBle/issues/18)
* Fixed possible BluetoothCharacteristic value overwrites with multiple writes (https://github.com/Polidea/RxAndroidBle/issues/27)
* Updated `RxJava` (1.1.0 -> 1.1.7) and `RxAndroid` (1.1.0 -> 1.2.1) libraries dependency
* Added interface methods for usage with BluetoothCharacteristic object (https://github.com/Polidea/RxAndroidBle/issues/38)
* Fixed lost connection when BluetoothAdapter disabled before the connection established (https://github.com/Polidea/RxAndroidBle/issues/45)
* Added RxBleClient.getBondedDevices() method, thanks @fracturedpsyche! (https://github.com/Polidea/RxAndroidBle/pull/46)

Version 1.0.1 (RxJava1)

* Fixed scan operation concurrency issue, thanks @artem-zinnatullin! (https://github.com/Polidea/RxAndroidBle/issues/5)
* Fixed location permission requirement check (Android >=6.0)

Version 1.0.0 (RxJava1)

* Changed RxBleClient factory method name.
* After this version the public API will be maintained to avoid conflicts.

Version 0.0.4 (RxJava1)

* Removed duplicated API for connection state from RxBleConnection
* Renamed API for connection state observation in RxBleDevice
* Renamed API for notification setup, not it is RxBleConnection#setupNotification(UUID)
* Added convenience method to check current connection state
* Added ability to filter scan results with one service more easily
* Reject establishConnection calls if connection is already established
* Added adapter for sharing connections

Version 0.0.3 (RxJava1)

* Added location permission for APIs >=23
* Check if location permission is granted and location services are enabled on Android 6.0
* Fixed error callback notifying about disconnects

Version 0.0.2 (RxJava1)

* Bugfixes
* Changed API for instantiation of the client
* Added caches in sensitive places

Version 0.0.1 (RxJava1)

Initial release
* Support for main bluetooth operations (discovery, connection, read, write, notifications)
