# RxAndroidBle [![Build Status](https://travis-ci.org/Polidea/RxAndroidBle.svg?branch=master)](https://travis-ci.org/Polidea/RxAndroidBle) [![Maven Central](https://img.shields.io/maven-central/v/com.polidea.rxandroidble/rxandroidble.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.polidea.rxandroidble%22%20AND%20a%3A%22rxandroidble%22)
![Polidea](https://raw.githubusercontent.com/Polidea/RxAndroidBle/master/site/viking-large.jpeg "Tailored software services including concept, design, development and testing")
## Introduction

RxAndroidBle is a powerful painkiller for Android's Bluetooth Low Energy headaches. It is backed by RxJava, implementing complicated APIs as handy reactive observables. The library does for you:

 * Fancy asynchronous operations support (read, write, notifications)
 * Threading management in order to meet Android contracts
 * Connection and operation error handling

For support head to [StackOverflow #rxandroidble](http://stackoverflow.com/questions/tagged/rxandroidble?sort=active)

Read the official announcement at [Polidea Blog](https://www.polidea.com/blog/RXAndroidBLE/).

## RxAndroidBLE @ Mobile Central Europe 2016
[![RxAndroidBLE @ Mobile Central Europe 2016](https://img.youtube.com/vi/0aKfUGCxUDM/0.jpg)](https://www.youtube.com/watch?v=0aKfUGCxUDM)

## Usage
### Obtaining the client
It's your job to maintain single instance of the client. You can use singleton, scoped [Dagger](http://google.github.io/dagger/) component or whatever else you want.

```java
RxBleClient rxBleClient = RxBleClient.create(context);
```

### Device discovery
Scanning devices in the area is simple as that:

```java
Subscription scanSubscription = rxBleClient.scanBleDevices()
	.subscribe(
	    rxBleScanResult -> {
	        // Process scan result here.
	    },
	    throwable -> {
	        // Handle an error here.
	    }
	);

// When done, just unsubscribe.
scanSubscription.unsubscribe();
```

### Connection
For further BLE interactions the connection is required.

```java
String macAddress = "AA:BB:CC:DD:EE:FF";
RxBleDevice device = rxBleClient.getBleDevice(macAddress);

Subscription subscription = device.establishConnection(context, false) // <-- autoConnect flag
	.subscribe(
	    rxBleConnection -> {
		    // All GATT operations are done through the rxBleConnection.
	    },
        throwable -> {
            // Handle an error here.
        }
    );

// When done... unsubscribe and forget about connection teardown :)
subscription.unsubscribe();
```

#### Auto connect
After https://developer.android.com/reference/android/bluetooth/BluetoothDevice.html#connectGatt(android.content.Context, boolean, android.bluetooth.BluetoothGattCallback):
autoConnect	boolean: Whether to directly connect to the remote device (false) or to automatically connect as soon as the remote device becomes available (true).

Auto connect concept may be misleading at first glance. With the autoconnect flag set to false the connection will end up with an error if a BLE device is not advertising when the `RxBleDevice#establishConnection` method is called. From platform to platform timeout after which the error is emitted differs, but in general it is rather tens of seconds than single seconds.

Setting the auto connect flag to true allows you to wait until the BLE device becomes discoverable. The `RxBleConnection` instance won't be emitted until the connection is fully set up. From experience it also handles acquiring wake locks, so it's safe to assume that your Android device will be woken up after the connection has been established - but it is not a documented feature and may change in the future system releases.

Be careful not to overuse the autoConnect flag. On the other side it has negative impact on the connection initialization speed. Scanning window and interval is lowered as it is optimized for background use and depending on Bluetooth parameters it may (and usually do) take more time to establish the connection.

### Read / write operations
#### Read
```java
device.establishConnection(context, false)
	.flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUUID))
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
device.establishConnection(context, false)
	.flatMap(rxBleConnection -> rxBleConnection.writeCharacteristic(characteristicUUID, bytesToWrite))
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
device.establishConnection(context, false)
    .flatMap(rxBleConnection -> Observable.combineLatest(
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
device.establishConnection(context, false)
    .flatMap(rxBleConnection -> rxBleConnection.createNewLongWriteBuilder()
            .setCharacteristicUuid(uuid) // required or the .setCharacteristic()
            // .setCharacteristic() alternative if you have a specific BluetoothGattCharacteristic
            .setBytes(byteArray)
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
 device.establishConnection(context, false)
    .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUuid)
	    .doOnNext(bytes -> {
	        // Process read data.
	    })
	    .flatMap(bytes -> rxBleConnection.writeCharacteristic(characteristicUuid, bytesToWrite)))
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
 device.establishConnection(context, false)
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

### Error handling
Every error you may encounter is provided via onError callback. Each public method has JavaDoc explaining possible errors.

### Helpers
We encourage you to check the package `com.polidea.rxandroidble.helpers` which contains handy reactive wrappers for some typical use-cases.

## More examples

Complete usage examples are located in `/sample` [GitHub repo](https://github.com/Polidea/RxAndroidBle/tree/master/sample/src/main/java/com/polidea/rxandroidble/sample).

## Download
### Gradle

```java
compile "com.polidea.rxandroidble:rxandroidble:1.1.0"
```
### Maven

```xml
<dependency>
  <groupId>com.polidea.rxandroidble</groupId>
  <artifactId>rxandroidble</artifactId>
  <version>1.1.0</version>
  <type>aar</type>
</dependency>
```

## Unit testing
Using RxAndroidBle enables you to unit test your application easily. For examples how to use mocking head to [MockRxAndroidBle](https://github.com/Polidea/RxAndroidBle/tree/master/mockrxandroidble).
## Contributing
If you would like to contribute code you can do so through GitHub by forking the repository and sending a pull request.

When submitting code, please make every effort to follow existing conventions and style in order to keep the code as readable as possible. Please also make sure your code compiles by running ```./gradlew clean checkstyle test```.

## Maintainers
* Dariusz Seweryn (dariusz.seweryn@polidea.com)
* Paweł Urban (pawel.urban@polidea.com)

## Contributors, thank you!
* Michał Zieliński (michal.zielinski@polidea.com)
* Fracturedpsyche (https://github.com/fracturedpsyche)
* Andrea Pregnolato (https://github.com/pregno)
* Matthieu Vachon (https://github.com/maoueh) - custom operations, yay!

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
