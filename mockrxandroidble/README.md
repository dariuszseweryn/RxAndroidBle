MockRxAndroidBle
=============

This project allows to mock a Bluetooth LE device. It's supposed to be used with RxAndroidBle library.


### Example

Using MockRxAndroidBle you can start writing and testing your Bluetooth code having just the device's specification.

Here's an example:

```java
RxBleClient rxBleClientMock = new RxBleClientMock.Builder()
    .addDevice(new RxBleDeviceMock.Builder() // <-- creating device mock, there can me multiple of them
        .deviceMacAddress(macAddress)
        .deviceName(deviceName)
        .scanRecord(new RxBleScanRecordMock.Builder()
            .setAdvertiseFlags(1)
            .addServiceUuid(new ParcelUuid(serviceUUID))
            .addServiceUuid(new ParcelUuid(serviceUUID2))
            .addManufacturerSpecificData(0x2211, [0x33, 0x44] as byte[])
            .addServiceData(new ParcelUuid(serviceUUID), [0x11, 0x22] as byte[])
            .setTxPowerLevel(12)
            .setDeviceName("TestDeviceAdv")
            .build()
        )
        .connection(new RxBleConnectionMock.Builder() // <-- creating connection mock
            .rssi(rssi)
            .notificationSource(characteristicNotifiedUUID, characteristicNotificationSubject)
            .addService( // <-- adding service mocks to the device, there can be multiple of them
                serviceUUID,
                new RxBleClientMock.CharacteristicsBuilder()
                    .addCharacteristic( // <-- adding characteristic mocks to the service, there can be multiple of them
                        characteristicUUID,
                        characteristicData,
                        new RxBleClientMock.DescriptorsBuilder()
                            .addDescriptor(descriptorUUID, descriptorData) // <-- adding descriptor mocks
                            .build() // to the characteristic, there can be multiple of them
                    ).build()
            ).characteristicReadCallback(characteristicUUID, (device, characteristic, result) -> {
                result.success(characteristicValue);
            })
            .characteristicWriteCallback(characteristicUUID, (device, characteristic, bytes, result) -> {
                if(writeData(characteristic, bytes)) {
                    result.success();
                } else {
                    result.failure(0x80);
                    // can also use result.disconnect(0x80); 
                }
            }).build()
        ).build()
    ).build();

// Now mocked client can be used the same way as RxAndroidBle client
```

### Download

Get MockRxAndroidBle via Maven:

```xml
<dependency>
  <groupId>com.polidea.rxandroidble3</groupId>
  <artifactId>mockclient</artifactId>
  <version>1.12.1</version>
  <type>aar</type>
</dependency>
```

or via Gradle

```groovy
implementation "com.polidea.rxandroidble3:mockclient:1.12.1"
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
