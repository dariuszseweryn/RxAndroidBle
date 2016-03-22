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