package org.donttreadonmysmartphone.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class SplitRepairCommandTest {
    @Test public void validatesAndRestoresFlagsWithoutWindowManagerOverride() {
        String script = SplitRepairCommand.verifiedFlags();
        assertTrue(script.indexOf("a=$(settings get") < script.indexOf("settings put global force_resizable_activities 1"));
        assertTrue(script.contains("ROLLBACK_VERIFICADO"));
        assertTrue(script.contains("ROLLBACK_NAO_CONFIRMADO"));
        assertTrue(script.contains("settings delete global"));
        assertTrue(script.contains("FLAGS_VERIFICADAS"));
        assertFalse(script.contains("wm set-multi-window-config"));
    }

    @Test public void windowManagerOverrideChecksAvailabilityBeforeMutation() {
        String script = SplitRepairCommand.windowManagerOverride();
        assertTrue(script.startsWith("wm get-multi-window-config || exit 1"));
        assertTrue(script.contains("wm set-multi-window-config --supportsNonResizable 1 --respectsActivityMinWidthHeight -1 || exit 1"));
    }
}
