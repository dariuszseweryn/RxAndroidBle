MockRxAndroidBle
=============

This project allows to mock a Bluetooth LE device. It's supposed to be used with RxAndroidBle library.


### Example

Using MockRxAndroidBle you can start writing and testing your Bluetooth code having just the device's specification.

Here's an example:

```java
RxBleClient rxBleClientMock = new RxBleClientMock.Builder()
    .addDevice(new RxBleClientMock.DeviceBuilder() // <-- creating device mock, there can me multiple of them
        .deviceMacAddress(macAddress)
        .deviceName(deviceName)
        .scanRecord(scanRecordBytes)
        .rssi(rssiValue)
        .addService( // <-- adding service mocks to the device, there can be multiple of them
            serviceUUID,
            new RxBleClientMock.CharacteristicsBuilder()
                .addCharacteristic( // <-- adding characteristic mocks to the service, there can be multiple of them
                    characteristicUUID,
                    characteristicDataBytes,
                    new RxBleClientMock.DescriptorsBuilder()
                        .addDescriptor(descriptorUUID, descriptorDataBytes) // <-- adding descriptor mocks
                    	.build() // to the characteristic, there can be multiple of them
        ).build()
    ).build();

// Now mocked client can be used the same way as RxAndroidBle client
```

### Download

Get MockRxAndroidBle via Maven:

```xml
<dependency>
  <groupId>com.polidea.rxandroidble</groupId>
  <artifactId>mockclient</artifactId>
  <version>1.4.1</version>
  <type>aar</type>
</dependency>
```

or via Gradle

```groovy
compile "com.polidea.rxandroidble:mockclient:1.4.1"
```

### License

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
