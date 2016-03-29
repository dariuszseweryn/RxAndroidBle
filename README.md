# RxAndroidBle [![Build Status](https://travis-ci.org/Polidea/RxAndroidBle.svg?branch=master)](https://travis-ci.org/Polidea/RxAndroidBle) [![Maven Central](https://img.shields.io/maven-central/v/com.polidea.rxandroidble/rxandroidble.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.polidea.rxandroidble%22%20AND%20a%3A%22rxandroidble%22)
## Introduction

RxAndroidBle is a powerful painkiller for Android's Bluetooth Low Energy headaches. It is backed by RxJava, implementing complicated APIs as handy reactive observables. The library does for you:

 * Fancy asynchronous operations support (read, write, notifications)
 * Threading management in order to meet Android contracts
 * Connection and operation error handling

For support head to [StackOverflow #rxandroidble](http://stackoverflow.com/questions/tagged/rxandroidble?sort=active)

## Usage
### Obtaining the client
It's your job to maintain single instance of the client. You can use singleton, scopped [Dagger](http://google.github.io/dagger/) component or whatever else you want.

```java
RxBleClient rxBleClient = RxBleClient.getInstance(context);
```

### Device discovery
Scanning devices in the area is simple as that:

```java
Subscription scanSubscription = rxBleClient.scanBleDevices()
	.subscribe(rxBleScanResult -> {
	    // Process scan result here.
	});
	
// When done, just unsubscribe.
scanSubscription.unsubscribe();
```

### Connection
For further BLE interactions the connection is required.

```java
String macAddress = "AA:BB:CC:DD:EE:FF";
RxBleDevice device = rxBleClient.getBleDevice(macAddress);

Subscription subscription = device.establishConnection(context, false) // <-- autoConnect flag
	.subscribe(rxBleConnection -> {
		// All GATT operations are done through the rxBleConnection.
	});
	
// When done... unsubscribe and forget about connection teardown :)
subscription.unsubscribe();
```

### Read / write operations
#### Read
```java
device.establishConnection(context, false)
	.flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUUID))
	.subscribe(characteristicValue -> {
		// Read characteristic value.
	});

```
#### Write
```java
device.establishConnection(context, false)
	.flatMap(rxBleConnection -> rxBleConnection.writeCharacteristic(characteristicUUID, bytesToWrite))
	.subscribe(characteristicValue -> {
		// Characteristic value confirmed.
	});
```
#### Multiple reads
```java
 device.establishConnection(context, false)
    .flatMap(rxBleConnection -> Observable.combineLatest(
        rxBleConnection.readCharacteristic(firstUUID),
        rxBleConnection.readCharacteristic(secondUUID),
        YourModelCombiningTwoValues::new
    ))
	.subscribe(model -> {
	    // Process your model.
	});
```
#### Read and write combined

```java
 device.establishConnection(context, false)
    .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUuid)
	    .doOnNext(bytes -> {
	        // Process read data.
	    })
	    .flatMap(bytes -> rxBleConnection.writeCharacteristic(characteristicUuid, bytesToWrite))
	.subscribe(writeBytes -> {
		// Written data.
	});
```
### Change notifications
```java
 device.establishConnection(context, false)
    .flatMap(rxBleConnection -> rxBleConnection.setupNotification(characteristicUuid))
    .doOnNext(notificationObservable -> {
    	// Notification has been set up
    })
    .flatMap(notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
    .subscribe(bytes -> {
    	// Given characteristic has been changes, here is the value.
    });

```
### Observing connection state
If you want to observe changes in device connection state just subscribe like below. On subscription you will receive the most current state instantly.

```java
device.observeConnectionStateChanges()
    .subscribe(connectionState -> {
    	// Process your way.
    });
```
### Logging
For connection debugging you can use extended logging

```java
RxBleClient.setLogLevel(RxBleLog.DEBUG);
```

### Error handling
Every error you may encounter is provided via onError callback. Each public method has JavaDoc explaining possible errors.


## More examples

Complete usage examples are located in `/sample` [GitHub repo](https://github.com/Polidea/RxAndroidBle/tree/master/sample/src/main/java/com/polidea/rxandroidble/sample).

## Download
### Gradle

```java
compile "com.polidea.rxandroidble:rxandroidble:0.0.4"
```
### Maven

```xml
<dependency>
  <groupId>com.polidea.rxandroidble</groupId>
  <artifactId>rxandroidble</artifactId>
  <version>0.0.4</version>
  <type>aar</type>
</dependency>
```
## Contributing
If you would like to contribute code you can do so through GitHub by forking the repository and sending a pull request.

When submitting code, please make every effort to follow existing conventions and style in order to keep the code as readable as possible. Please also make sure your code compiles by running ```gradle clean checkstyle test```.

## Maintainers
Dariusz Seweryn (dariusz.seweryn@polidea.com)

Paweł Urban (pawel.urban@polidea.com)

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
