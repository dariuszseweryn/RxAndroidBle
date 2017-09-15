Change Log
==========

Version 1.4.1
* Fixed issue hasObservers conditional for Output class (https://github.com/Polidea/RxAndroidBle/issues/283)

Version 1.4.0
* Added native callback usage support in custom operations. You may consider this API if your implementation is performance critical. (https://github.com/Polidea/RxAndroidBle/issues/165)
* Added pre-scan verification for excessive scan (undocumented Android 7.0 "feature") (https://github.com/Polidea/RxAndroidBle/issues/227)
* Adjusted `BleCannotSetCharacteristicNotificationException` to contain the cause exception if available. `RxBleConnection.setupNotification()`/`RxBleConnection.setupIndication()` will now throw the cause of disconnection if subscribed after connection was disconnected. (https://github.com/Polidea/RxAndroidBle/issues/225) 
* _Changed Behaviour_ of `RxBleDevice.observeConnectionStateChanges()` - does not emit initial state and reflects best `BluetoothGatt` state. (https://github.com/Polidea/RxAndroidBle/issues/50)
* Added support for a custom Logger `RxBleLog.setLogger(Logger)` as alternative to Logcat (https://github.com/Polidea/RxAndroidBle/pull/248)
* Added a warning log if user tries to use a characteristic against it's properties (https://github.com/Polidea/RxAndroidBle/issues/224)
* _Changed Behaviour_ — `BluetoothGatt` is now called on a single background thread instead of the main thread (https://github.com/Polidea/RxAndroidBle/pull/255)
* Decoupled command queues for different connections. (https://github.com/Polidea/RxAndroidBle/issues/250)

Version 1.3.4
* Added @Nullable annotation to `RxBleDevice.getName()`. (https://github.com/Polidea/RxAndroidBle/issues/263)
* Fixed connection not being disconnected when `DeadObjectException` was raised. (https://github.com/Polidea/RxAndroidBle/issues/275)

Version 1.3.3
* Fixed scan filtering by name on API <21 (https://github.com/Polidea/RxAndroidBle/pull/243)
* Fixed race condition (which would cause the library to hang) when using `.first()` on calls to `RxBleConnection` that emit a single result. (https://github.com/Polidea/RxAndroidBle/issues/244) 

Version 1.3.2
* Fixed completing the `Observable<byte[]>` emitted by `RxBleConnection.setupNotification()`/`RxBleConnection.setupIndication()` when unsubscribed (https://github.com/Polidea/RxAndroidBle/issues/231)

Version 1.3.1
* Fixed unsubscribing from operations before `onComplete()`/`onError()` causing the library to hang. (https://github.com/Polidea/RxAndroidBle/issues/218)

Version 1.3.0
* _Changed Behaviour_ of `RxBleConnection` - connection is no longer closed on individual operation errors. (https://github.com/Polidea/RxAndroidBle/issues/26) 
* Added partial support for API 21 BLE scan in a backwards compatible manner. (https://github.com/Polidea/RxAndroidBle/issues/16)
* Added support for filtering by Manufacturer Data (https://github.com/Polidea/RxAndroidBle/issues/127)
* Added RxBleClient State observing for determining what functionality of the library may work (https://github.com/Polidea/RxAndroidBle/issues/55)
* Added `ValueInterpreter` for interpreting standardized (Bluetooth Specification) int/float/String values from byte[] (https://github.com/Polidea/RxAndroidBle/issues/199)
* Added support for requesting connection priority on API 21+ (https://github.com/Polidea/RxAndroidBle/issues/111)

Version 1.2.4
* Lowered memory pressure caused by `RxBleLog` when logs are disabled (https://github.com/Polidea/RxAndroidBle/issues/212)

Version 1.2.3
* Fixed scan when filter with 32-bit UUID was specified (https://github.com/Polidea/RxAndroidBle/issues/207)
* Fixed memory leak of scan operation (and potentially any other that would emit an infinite stream of events) (https://github.com/Polidea/RxAndroidBle/issues/194)
* Lowered memory pressure when using functions that accept `UUID`.
* Changed `RxBleConnectionState` from a class to an enum for convenience.
* Minor optimisations to `RxBleRadio` queue processing time.
* Updated `RxJava` to 1.3.0

Version 1.2.2
* Fixed visibility of `UUIDUtil`. Deprecated it. Introduced `AdvertisedServiceUUIDExtractor` helper, thanks marciogranzotto! (https://github.com/Polidea/RxAndroidBle/pull/184)

Version 1.2.1
* Added `ByteArrayBatchObservable` helper for splitting long byte arrays
* Fixed behaviour in non-Bluetooth environments. (https://github.com/Polidea/RxAndroidBle/issues/158)
* Fixed `RxBleConnectionMock` overwriting `BluetoothCharacteristic` value on setting notification. (https://github.com/Polidea/RxAndroidBle/issues/160)
* Fixed default payload size of Long Write operation when non-standard MTU was negotiated (https://github.com/Polidea/RxAndroidBle/issues/161)
* Added possibility to get the current MTU value of `RxBleConnection` (https://github.com/Polidea/RxAndroidBle/issues/166)
* Fixed retrying of `RxBleClient.scanBleDevices(UUID...)`, thanks BharathMG! (https://github.com/Polidea/RxAndroidBle/pull/174)
* Fixed connection not being noticed as established due to a race condition (https://github.com/Polidea/RxAndroidBle/issues/178)
* Fixed `BleBluetoothGattCallbackTimeout` macAddress being null on connection (https://github.com/Polidea/RxAndroidBle/issues/178)
* Fixed disconnect operation behaviour in an edge case situation (https://github.com/Polidea/RxAndroidBle/issues/178)

Version 1.2.0
* Added Proguard rules for the library. (https://github.com/Polidea/RxAndroidBle/issues/104)
* Added support for MTU negotiation, thanks pregno!
* Fixed connecting with autoConnect = true on Android 7.0.0+, thanks JIUgia!
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

Version 1.1.0
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

Version 1.0.2

* Added Mock RxAndroidBle to the repository
* Added indications handling on RxBleConnection
* Fixed scan operation concurrency issue (https://github.com/Polidea/RxAndroidBle/issues/17)
* Exposed android.bluetooth.BluetoothDevice in RxBleDevice (https://github.com/Polidea/RxAndroidBle/issues/23)
* Fixed stale RxBleRadio on RxBleOperation unhandled throw (https://github.com/Polidea/RxAndroidBle/issues/18)
* Fixed possible BluetoothCharacteristic value overwrites with multiple writes (https://github.com/Polidea/RxAndroidBle/issues/27)
* Updated `RxJava` (1.1.0 -> 1.1.7) and `RxAndroid` (1.1.0 -> 1.2.1) libraries dependency
* Added interface methods for usage with BluetoothCharacteristic object (https://github.com/Polidea/RxAndroidBle/issues/38)
* Fixed lost connection when BluetoothAdapter disabled before the connection established (https://github.com/Polidea/RxAndroidBle/issues/45)
* Added RxBleClient.getBondedDevices() method, thanks fracturedpsyche! (https://github.com/Polidea/RxAndroidBle/pull/46)

Version 1.0.1

* Fixed scan operation concurrency issue, thanks artem-zinnatullin! (https://github.com/Polidea/RxAndroidBle/issues/5)
* Fixed location permission requirement check (Android >=6.0)

Version 1.0.0

* Changed RxBleClient factory method name.
* After this version the public API will be maintained to avoid conflicts.

Version 0.0.4

* Removed duplicated API for connection state from RxBleConnection
* Renamed API for connection state observation in RxBleDevice
* Renamed API for notification setup, not it is RxBleConnection#setupNotification(UUID)
* Added convenience method to check current connection state
* Added ability to filter scan results with one service more easily
* Reject establishConnection calls if connection is already established
* Added adapter for sharing connections

Version 0.0.3

* Added location permission for APIs >=23
* Check if location permission is granted and location services are enabled on Android 6.0
* Fixed error callback notifying about disconnects

Version 0.0.2

* Bugfixes
* Changed API for instantiation of the client
* Added caches in sensitive places

Version 0.0.1

Initial release
* Support for main bluetooth operations (discovery, connection, read, write, notifications)