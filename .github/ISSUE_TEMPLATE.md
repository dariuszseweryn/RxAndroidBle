### Disclaimer
Before you will post a new issue to the RxAndroidBle library consider if you are filling a bug/feature request or a general question 
about:
* Usage of RxJava
* Usage of Bluetooth Low Energy
* Usage of RxAndroidBle
* Weird behaviour of the peripheral you are using
* Other not directly related to a new feature request or a bug

If any of the above points seems to match your use case — consider using Google or creating a question on www.stackoverflow.com with 
a tag `rxandroidble` where it will be easier to access by other people with similar questions. Issues that are not bugs or feature requests
will be closed.

### Summary

### Library version
`X.X.X`

#### Preconditions

#### Steps to reproduce actual result
<br />1.
<br />2.
<br />3.

#### Actual result
// What you have experienced

#### Expected result
// A clear and concise description of what you expected to happen

#### Minimum code snippet reproducing the issue

#### Logs from the application running with settings:
```java
RxBleClient.updateLogOptions(new LogOptions.Builder()
        .setLogLevel(LogConstants.DEBUG)
        .setMacAddressLogSetting(LogConstants.MAC_ADDRESS_FULL)
        .setUuidsLogSetting(LogConstants.UUIDS_FULL)
        .setShouldLogAttributeValues(true)
        .build()
);
```

```
// paste logs here
```
