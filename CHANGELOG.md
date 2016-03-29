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

Version 0.0.5
