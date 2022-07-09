package com.polidea.rxandroidble2.internal.util

import android.content.Context
import android.content.pm.PackageManager
import spock.lang.Specification

class CheckerPermissionTest extends Specification {

    String testPermissionString = "random.package.RANDOM_PERMISSION"
    def testPermissionArray = new String[] { testPermissionString }
    def mockContext = Mock Context
    CheckerPermission objectUnderTest

    def "should cache granted permissions"() {

        given:
        objectUnderTest = new CheckerPermission(mockContext)

        when:
        def result0 = objectUnderTest.isAnyPermissionGranted(testPermissionArray)
        def result1 = objectUnderTest.isAnyPermissionGranted(testPermissionArray)

        then:
        1 * mockContext.checkPermission(testPermissionString, _, _) >> PackageManager.PERMISSION_GRANTED

        and:
        result0
        result1
    }

    def "should call context each time if permission is not granted"() {

        given:
        objectUnderTest = new CheckerPermission(mockContext)

        when:
        def result0 = objectUnderTest.isAnyPermissionGranted(testPermissionArray)
        def result1 = objectUnderTest.isAnyPermissionGranted(testPermissionArray)

        then:
        2 * mockContext.checkPermission(testPermissionString, _, _) >> PackageManager.PERMISSION_DENIED

        and:
        !result0
        !result1
    }
}
