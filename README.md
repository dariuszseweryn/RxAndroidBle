# RxAndroidBle [![Build Status](https://travis-ci.org/Polidea/RxAndroidBle.svg?branch=master)](https://travis-ci.org/Polidea/RxAndroidBle) [![Maven Central](https://img.shields.io/maven-central/v/com.polidea.rxandroidble3/rxandroidble.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.polidea.rxandroidble3%22%20AND%20a%3A%22rxandroidble%22)
<p align="center">
  <img 
    alt="Tailored software services including concept, design, development and testing"
    src="site/RX-Android.png"
    height="300"
    style="margin-top: 20px; margin-bottom: 20px;"
  />
</p>

## Introduction

RxAndroidBle is a powerful painkiller for Android's Bluetooth Low Energy headaches. It is backed by RxJava, implementing complicated APIs as handy reactive observables. The library does for you:

 * Fancy asynchronous operations support (read, write, notifications)
 * Threading management in order to meet Android contracts
 * Connection and operation error handling

For support head to [StackOverflow #rxandroidble](http://stackoverflow.com/questions/tagged/rxandroidble?sort=active)

Read the official announcement at [Polidea Blog](https://www.polidea.com/blog/RXAndroidBLE/).

## RxAndroidBLE @ Mobile Central Europe 2016
[![RxAndroidBLE @ Mobile Central Europe 2016](https://img.youtube.com/vi/0aKfUGCxUDM/0.jpg)](https://www.youtube.com/watch?v=0aKfUGCxUDM)

## Getting Started

The first step is to include RxAndroidBle into your project.

### Gradle
If you use Gradle to build your project — as a Gradle project implementation dependency:
```groovy
implementation "com.polidea.rxandroidble2:rxandroidble:1.12.1"
```
### Maven
If you use Maven to build your project — as a Maven project dependency:
```xml
<dependency>
  <groupId>com.polidea.rxandroidble2</groupId>
  <artifactId>rxandroidble</artifactId>
  <version>1.12.1</version>
  <type>aar</type>
</dependency>
```

### Snapshot
If your are interested in cutting-edge build you can get a `x.y.z-SNAPSHOT` version of the library.
NOTE: Snapshots are built from the top of the `master` and `develop` branches and a subject to more frequent changes that may break the API and/or change behavior.

To be able to download it you need to add Sonatype Snapshot repository site to your `build.gradle` file:
```groovy
maven { url "https://oss.sonatype.org/content/repositories/snapshots" }
```

### Permissions
Android since API 23 (6.0 / Marshmallow) requires location permissions declared in the manifest for an app to run a BLE scan. RxAndroidBle already provides all the necessary bluetooth permissions for you in AndroidManifest.

Runtime permissions required for running a BLE scan:

| from API | to API (inclusive) | Acceptable runtime permissions |
|:---:|:---:| --- |
| 18 | 22 | (No runtime permissions needed) |
| 23 | 28 | One of below:<br>- `android.permission.ACCESS_COARSE_LOCATION`<br>- `android.permission.ACCESS_FINE_LOCATION` |
| 29 | current | - `android.permission.ACCESS_FINE_LOCATION` |

#### Potential permission issues
Google is checking `AndroidManifest` for declaring permissions when releasing to the Play Store. If you have `ACCESS_COARSE_LOCATION` or `ACCESS_FINE_LOCATION` set manually using tag `uses-permission` (as opposed to `uses-permission-sdk-23`) you may run into an issue where your manifest does not merge with RxAndroidBle's, resulting in a failure to upload to the Play Store. These permissions are only required on SDK 23+. If you need any of these permissions on a lower version of Android replace your statement with:
```xml
<uses-permission
  android:name="android.permission.ACCESS_COARSE_LOCATION"
  android:maxSdkVersion="22"/>
```
```xml
<uses-permission
  android:name="android.permission.ACCESS_FINE_LOCATION"
  android:maxSdkVersion="22"/>
```

## Usage
### Obtaining the client
It's your job to maintain single instance of the client. You can use singleton, scoped [Dagger](http://google.github.io/dagger/) component or whatever else you want.

```java
RxBleClient rxBleClient = RxBleClient.create(context);
```

### Turning the bluetooth on / off
The library does _not_ handle managing the state of the BluetoothAdapter.
<br>Direct managing of the state is not recommended as it violates the application user's right to manage the state of their phone. See `Javadoc` of [BluetoothAdapter.enable()](https://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html#enable()) method.
<br>It is the user's responsibility to inform why the application needs Bluetooth to be turned on and for ask the application's user consent.
<br>It is possible to show a native activity for turning the Bluetooth on by calling:
```java
Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
int REQUEST_ENABLE_BT = 1;
context.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
```

### Device discovery
Scanning devices in the area is simple as that:

```java
Disposable scanSubscription = rxBleClient.scanBleDevices(
        new ScanSettings.Builder()
            // .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // change if needed
            // .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // change if needed
            .build()
        // add filters if needed
)
    .subscribe(
        scanResult -> {
            // Process scan result here.
        },
        throwable -> {
            // Handle an error here.
        }
    );

// When done, just dispose.
scanSubscription.dispose();
```
For devices with API <21 (before Lollipop) the scan API is emulated to get the same behaviour.

### Observing client state
On Android it is not always trivial to determine if a particular BLE operation has a potential to succeed. e.g. to scan on Android 6.0 the device needs to have a `BluetoothAdapter`, the application needs to have a granted [runtime permission](https://github.com/Polidea/RxAndroidBle#permissions) for either `ACCESS_COARSE_LOCATION` or `ACCESS_FINE_LOCATION`, additionally `Location Services` need to be turned on.
To be sure that the scan will work only when everything is ready you could use:

```java
Disposable flowDisposable = rxBleClient.observeStateChanges()
    .switchMap(state -> { // switchMap makes sure that if the state will change the rxBleClient.scanBleDevices() will dispose and thus end the scan
        switch (state) {

            case READY:
                // everything should work
                return rxBleClient.scanBleDevices();
            case BLUETOOTH_NOT_AVAILABLE:
                // basically no functionality will work here
            case LOCATION_PERMISSION_NOT_GRANTED:
                // scanning and connecting will not work
            case BLUETOOTH_NOT_ENABLED:
                // scanning and connecting will not work
            case LOCATION_SERVICES_NOT_ENABLED:
                // scanning will not work
            default:
                return Observable.empty();
        }
    })
    .subscribe(
    	rxBleScanResult -> {
    	    // Process scan result here.
    	},
    	throwable -> {
    	    // Handle an error here.
    	}
    );

// When done, just dispose.
flowDisposable.dispose();
```

### Connection
For further BLE interactions the connection is required.

```java
String macAddress = "AA:BB:CC:DD:EE:FF";
RxBleDevice device = rxBleClient.getBleDevice(macAddress);

Disposable disposable = device.establishConnection(false) // <-- autoConnect flag
    .subscribe(
        rxBleConnection -> {
            // All GATT operations are done through the rxBleConnection.
        },
        throwable -> {
            // Handle an error here.
        }
    );

// When done... dispose and forget about connection teardown :)
disposable.dispose();
```

#### Auto connect
From <a href="https://developer.android.com/reference/android/bluetooth/BluetoothDevice.html#connectGatt(android.content.Context, boolean, android.bluetooth.BluetoothGattCallback)">BluetoothDevice.connectGatt() Javadoc</a>:
> autoConnect	boolean: Whether to directly connect to the remote device (false) or to automatically connect as soon as the remote device becomes available (true).

Auto connect concept may be misleading at first glance. With the autoconnect flag set to false the connection will end up with an error if a BLE device is not advertising when the `RxBleDevice#establishConnection` method is called. From platform to platform timeout after which the error is emitted differs, but in general it is rather tens of seconds than single seconds (~30 s).

Setting the auto connect flag to true allows you to wait until the BLE device becomes discoverable. The `RxBleConnection` instance won't be emitted until the connection is fully set up. From experience it also handles acquiring wake locks, so it's safe to assume that your Android device will be woken up after the connection has been established - but it is not a documented feature and may change in the future system releases. Unlike the native Android API, if `autoConnect=true` while using this library there will be NO attempts to automatically reconnect if the original connection is lost.

Be careful not to overuse the autoConnect flag. On the other side it has negative impact on the connection initialization speed. Scanning window and interval is lowered as it is optimized for background use and depending on Bluetooth parameters it may (and usually do) take more time to establish the connection.

### Read / write operations
#### Read
```java
device.establishConnection(false)
    .flatMapSingle(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUUID))
    .subscribe(
        characteristicValue -> {
            // Read characteristic value.
        },
        throwable -> {
            // Handle an error here.
        }
    );
```
#### Write
```java
device.establishConnection(false)
    .flatMapSingle(rxBleConnection -> rxBleConnection.writeCharacteristic(characteristicUUID, bytesToWrite))
    .subscribe(
        characteristicValue -> {
            // Characteristic value confirmed.
        },
        throwable -> {
            // Handle an error here.
        }
    );
```
#### Multiple reads
```java
device.establishConnection(false)
    .flatMap(rxBleConnection -> Single.zip(
        rxBleConnection.readCharacteristic(firstUUID),
        rxBleConnection.readCharacteristic(secondUUID),
        YourModelCombiningTwoValues::new
    ))
    .subscribe(
        model -> {
            // Process your model.
        },
        throwable -> {
            // Handle an error here.
        }
    );
```
#### Long write
```java
device.establishConnection(false)
    .flatMap(rxBleConnection -> rxBleConnection.createNewLongWriteBuilder()
        .setCharacteristicUuid(uuid) // required or the .setCharacteristic()
        // .setCharacteristic() alternative if you have a specific BluetoothGattCharacteristic
        .setBytes(byteArray)
        // .setWriteOperationRetryStrategy(retryStrategy) // if you'd like to retry batch write operations on failure, provide your own retry strategy
        // .setMaxBatchSize(maxBatchSize) // optional -> default 20 or current MTU
        // .setWriteOperationAckStrategy(ackStrategy) // optional to postpone writing next batch
        .build()
    )
    .subscribe(
        byteArray -> {
            // Written data.
        },
        throwable -> {
            // Handle an error here.
        }
    );
```
#### Read and write combined

```java
device.establishConnection(false)
    .flatMapSingle(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUuid)
        .doOnSuccess(bytes -> {
            // Process read data.
        })
        .flatMap(bytes -> rxBleConnection.writeCharacteristic(characteristicUuid, bytesToWrite))
    )
    .subscribe(
        writeBytes -> {
            // Written data.
        },
        throwable -> {
            // Handle an error here.
        }
    );
```
### Change notifications
```java
device.establishConnection(false)
    .flatMap(rxBleConnection -> rxBleConnection.setupNotification(characteristicUuid))
    .doOnNext(notificationObservable -> {
        // Notification has been set up
    })
    .flatMap(notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
    .subscribe(
        bytes -> {
            // Given characteristic has been changes, here is the value.
        },
        throwable -> {
            // Handle an error here.
        }
    );
```
### Observing connection state
If you want to observe changes in device connection state just subscribe like below. On subscription you will receive the most current state instantly.

```java
device.observeConnectionStateChanges()
    .subscribe(
        connectionState -> {
            // Process your way.
        },
        throwable -> {
            // Handle an error here.
        }
    );
```
### Logging
For connection debugging you can use extended logging

```java
RxBleClient.setLogLevel(RxBleLog.DEBUG);
```

By default `RxBleLog` uses logcat to print the messages. You can provide your own logger implementation to forward it to other logging libraries such as Timber.
```java
RxBleLog.setLogger((level, tag, msg) -> Timber.tag(tag).log(level, msg));
```

### Error handling
Every error you may encounter is provided via `onError` callback. Each public method has JavaDoc explaining possible errors.

### Observable behaviour
From different interfaces, you can obtain different `Observable`s which exhibit different behaviours.
There are two types of `Observable`s that you may encounter.
1. Multiple values - i.e. `RxBleClient.scan()`, `RxBleDevice.observeConnectionStateChanges()` and `Observable` emitted by `RxBleConnection.setupNotification()` / `RxBleConnection.setupIndication()`
2. One value — these usually are meant for auto cleanup upon disposing i.e. `setupNotification()` / `setupIndication()` — when you will dispose the notification / indication will be disabled

`RxBleDevice.establishConnection()` is an `Observable` that will emit a single `RxBleConnection` but will not complete as the connection may be later a subject to an error (i.e. external disconnection). Whenever you are no longer interested in keeping the connection open you should dispose it which will cause disconnection and cleanup of resources.

The below table contains an overview of used `Observable` patterns

| Interface | Function | Number of values | [Hot/Cold](https://medium.com/@benlesh/hot-vs-cold-observables-f8094ed53339)
| --- | --- | --- | --- |
| RxBleClient | scanBleDevices()* | Infinite | cold |
| RxBleClient | observeStateChanges() | Infinite** | hot |
| RxBleDevice | observeConnectionStateChanges() | Infinite | hot |
| RxBleDevice | establishConnection()* | One | cold |
| RxBleConnection | setupNotification()* | One | cold |
| RxBleConnection | setupNotification() emitted Observable | Infinite** | hot |
| RxBleConnection | setupIndication()* | One | cold |
| RxBleConnection | setupIndication() emitted Observable | Infinite** | hot |
| RxBleConnection | queue() | User defined | cold |

\* this `Observable` when disposed closes/cleans up internal resources (i.e. finishes scan, closes a connection, disables notifications)<br>
\** this `Observable` may complete. For example `observeStateChanges()` does emit only a single value and finishes in exactly one situation — when Bluetooth Adapter is not available on the device. There is no reason to monitor other states as the adapter does not appear during runtime. A second example: Observables emitted from `setupNotification` / `setupIndication` may complete when the parent Observable is disposed.

### Helpers
We encourage you to check the package [`com.polidea.rxandroidble3.helpers`](https://github.com/Polidea/RxAndroidBle/tree/master/rxandroidble/src/main/java/com/polidea/rxandroidble3/helpers) and [`com.polidea.rxandroidble3.utils`](https://github.com/Polidea/RxAndroidBle/tree/master/rxandroidble/src/main/java/com/polidea/rxandroidble3/utils) which contain handy reactive wrappers for some typical use-cases.

#### Value interpretation
Bluetooth Specification specifies formats in which `int`/`float`/`String` values may be stored in characteristics. `BluetoothGattCharacteristic` has functions for retrieving those (`.getIntValue()`/`.getFloatValue()`/`.getStringValue()`).
Since `RxAndroidBle` reads and notifications emit `byte[]` you may want to use `ValueIntepreter` helper to retrieve the same data easily.

#### Observing BluetoothAdapter state
If you would like to observe `BluetoothAdapter` state changes you can use `RxBleAdapterStateObservable`.

## More examples

Usage examples are located in:
- [`/sample`](https://github.com/Polidea/RxAndroidBle/tree/master/sample/src/main/java/com/polidea/rxandroidble3/sample)
- [`/sample-kotlin`](https://github.com/Polidea/RxAndroidBle/tree/master/sample-kotlin/src/main/kotlin/com/polidea/rxandroidble3/samplekotlin)

Keep in mind that these are only _samples_ to show how the library can be used. These are not meant for being role model of a good application architecture.

## Testing
Using RxAndroidBle enables you to test your application easily.

### Unit tests
Most of the objects that the library uses are implementations of interfaces which can be mocked.

Alternatively one could use `MockRxAndroidBle` (more info below). Note: Using `MockRxAndroidBle` in unit tests needs [Robolectric](https://github.com/robolectric/robolectric).

### Integration tests
Sometimes there is a need to develop the application without the access to a physical device. We have created [MockRxAndroidBle](https://github.com/Polidea/RxAndroidBle/tree/master/mockrxandroidble) as a drop-in addon for mocking a simple peripheral.

Unfortunately it is not under active development—PRs are welcomed though. ;)

## [Small performance comparison](https://github.com/Polidea/RxAndroidBle/issues/41#issuecomment-333513707)

## Contributing
If you would like to contribute code you can do so through GitHub by forking the repository and sending a pull request.

When submitting code, please make every effort to follow existing conventions and style in order to keep the code as readable as possible. Please also make sure your code compiles by running ```./gradlew clean checkstyle test```.

## FAQ
If you encounter seemingly incorrect behaviour in your application that is regarding this library please check the below list of Frequently Asked Questions:
- [Cannot connect](https://github.com/Polidea/RxAndroidBle/wiki/FAQ:-Cannot-connect)
- [UndeliverableException](https://github.com/Polidea/RxAndroidBle/wiki/FAQ:-UndeliverableException)

## Support
* non-commercial — head to [StackOverflow #rxandroidble](https://stackoverflow.com/questions/tagged/rxandroidble)
* commercial — drop an email to hello@polidea.com for more info

[Contact us](https://www.polidea.com/project/?utm_source=Github&utm_medium=Npaid&utm_campaign=Kontakt&utm_term=Code&utm_content=GH_NOP_KKT_COD_RAB001)

Learn more about Polidea's BLE services [here](https://www.polidea.com/services/ble/?utm_source=Github&utm_medium=Npaid&utm_campaign=Tech_BLE&utm_term=Code&utm_content=GH_NOP_BLE_COD_RAB001)

## Discussions
Want to talk about it? Join our discussion on [Gitter](https://gitter.im/RxBLELibraries/RxAndroidBle)

## Maintainers
* Dariusz Seweryn (github: dariuszseweryn)

## [Contributors](https://github.com/Polidea/RxAndroidBle/graphs/contributors), thank you!

## License

    Copyright 2016 Polidea Sp. z o.o

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.



## Maintained by

[![Polidea](https://raw.githubusercontent.com/Polidea/RxAndroidBle/master/site/polidea_logo.png "Tailored software services including concept, design, development and testing")](http://www.polidea.com)
