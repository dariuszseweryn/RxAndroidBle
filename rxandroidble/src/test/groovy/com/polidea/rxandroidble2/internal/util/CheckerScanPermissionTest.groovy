package com.polidea.rxandroidble2.internal.util


import spock.lang.Specification
import spock.lang.Unroll

class CheckerScanPermissionTest extends Specification {
    CheckerScanPermission objectUnderTest

    @Unroll
    def "return input data concatenated if needed"() {

        given:
        objectUnderTest = new CheckerScanPermission(null, repackToArray(permissions))

        expect:
        objectUnderTest.getRecommendedScanRuntimePermissions() == (expectedPermissions.toArray(new String[0]))

        where:
        permissions                    | expectedPermissions
        []                             | []
        [[], []]                       | []
        [["p0", "p1"]]                 | ["p0", "p1"]
        [["p0", "p1"], []]             | ["p0", "p1"]
        [["p0"], ["p1"]]               | ["p0", "p1"]
        [["p0", "p1"], ["p2"]]         | ["p0", "p1", "p2"]
        [["p0", "p1"], ["p2"], ["p3"]] | ["p0", "p1", "p2", "p3"]
    }

    String[][] repackToArray(ArrayList<ArrayList<String>> permissions) {
        def result = new String[permissions.size()][]
        for (i in 0..<permissions.size()) {
            ArrayList<String> permissionsArrayInner = permissions.get(i)
            result[i] = permissionsArrayInner.toArray(new String[0])
        }
        return result
    }
}
