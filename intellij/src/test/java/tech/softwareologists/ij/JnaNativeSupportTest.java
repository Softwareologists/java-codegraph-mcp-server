package tech.softwareologists.ij;

import com.sun.jna.Native;
import org.junit.Test;

public class JnaNativeSupportTest {
    @Test
    public void jnaLibrary_available() {
        int size = Native.getNativeSize(int.class);
        if (size <= 0) {
            throw new AssertionError("Invalid native size: " + size);
        }
    }
}
