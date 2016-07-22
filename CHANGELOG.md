Change Log
=============

Version 0.0.1
-------------

Initial release
* Support for main bluetooth operations (discovery, connection, read, write, notifications)

Version 0.0.2

* Bugfixes
* Changed API for instantiation of the client
* Added caches in sensitive places

Version 0.0.3
* Added location permission for APIs >=23
* Check if location permission is granted and location services are enabled on Android 6.0
* Fixed error callback notifying about disconnects

Version 0.0.4
* Removed duplicated API for connection state from RxBleConnection
* Renamed API for connection state observation in RxBleDevice
* Renamed API for notification setup, not it is RxBleConnection#setupNotification(UUID)
* Added convenience method to check current connection state
* Added ability to filter scan results with one service more easily
* Reject establishConnection calls if connection is already established
* Added adapter for sharing connections

Version 1.0.0
* Changed RxBleClient factory method name.
* After this version the public API will be maintained to avoid conflicts.

Version 1.0.1
* Fixed scan operation concurrency issue, thanks artem-zinnatullin!  (https://github.com/Polidea/RxAndroidBle/issues/5)
* Fixed location permission requirement check (Android >=6.0)

Version 1.0.2
* Added Mock RxAndroidBle to the repository
* Added indications handling on RxBleConnection
* Fixed scan operation concurrency issue (https://github.com/Polidea/RxAndroidBle/issues/17)
* Exposed android.bluetooth.BluetoothDevice in RxBleDevice (https://github.com/Polidea/RxAndroidBle/issues/23)
* Fixed stale RxBleRadio on RxBleOperation unhandled throw (https://github.com/Polidea/RxAndroidBle/issues/18)
* Fixed possible BluetoothCharacteristic value overwrites with multiple writes (https://github.com/Polidea/RxAndroidBle/issues/27)
* Updated rxJava (1.1.0 -> 1.1.7) and rxAndroid (1.1.0 -> 1.2.1) libraries dependency
* Added interface methods for usage with BluetoothCharacteristic object (https://github.com/Polidea/RxAndroidBle/issues/38)
* Fixed lost connection when BluetoothAdapter disabled before the connection established (https://github.com/Polidea/RxAndroidBle/issues/45)
* Added RxBleClient.getBondedDevices() method, thanks fracturedpsyche! (https://github.com/Polidea/RxAndroidBle/pull/46)