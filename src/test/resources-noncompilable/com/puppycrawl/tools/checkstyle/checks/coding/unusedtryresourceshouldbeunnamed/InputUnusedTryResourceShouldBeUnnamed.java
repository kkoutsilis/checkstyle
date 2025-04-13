/*
UnusedTryResourceShouldBeUnnamed

*/

//non-compiled with javac: Compilable with Java21
package com.puppycrawl.tools.checkstyle.checks.coding.unusedtryresourceshouldbeunnamed;

public class InputUnusedTryResourceShouldBeUnnamed {
    void test() {
        // violation below, 'Unused try resource 'a' should be unnamed'
        try (AutoCloseable a = lock()) {

        } catch (Exception e) {

        }

        try (AutoClosable _ = lock()) {

        } catch (Exception e) {

        }

        try {

        } catch (Exception e) {

        }
    }

    AutoCloseable lock() {
        return null;
    }
}
