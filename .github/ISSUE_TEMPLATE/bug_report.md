---
name: Bug report
about: Create a report to help us improve
title: ''
labels: bug
assignees: ''

---

**Describe the bug**
A clear and concise description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Go to '...'
2. Click on '....'
3. Scroll down to '....'
4. See error

**Expected behavior**
A clear and concise description of what you expected to happen.

**Smartphone (please complete the following information):**
 - Device: [e.g. Google Pixel]
 - OS: [e.g. Android 10]
 - Library version: [e.g. 1.10.5]

**Logs from the application when bug occurs (this will greatly help in quick understanding the problem)**
To turn on logs use:
```
RxBleClient.updateLogOptions(new LogOptions.Builder()
        .setLogLevel(LogConstants.DEBUG)
        .setMacAddressLogSetting(LogConstants.MAC_ADDRESS_FULL)
        .setUuidsLogSetting(LogConstants.UUIDS_FULL)
        .setShouldLogAttributeValues(true)
        .build()
);
```
```
// please paste the logs here
```

**Additional context**
Add any other context about the problem here.
