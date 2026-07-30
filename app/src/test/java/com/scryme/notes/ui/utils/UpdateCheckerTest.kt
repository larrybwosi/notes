package com.scryme.notes.ui.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun testIsNewerVersion() {
        // Simple major.minor.patch upgrade
        assertTrue(UpdateChecker.isNewerVersion("1.0.0", "1.0.1"))
        assertTrue(UpdateChecker.isNewerVersion("1.0.0", "1.1.0"))
        assertTrue(UpdateChecker.isNewerVersion("1.0.0", "2.0.0"))

        // Handles 'v' prefix
        assertTrue(UpdateChecker.isNewerVersion("v1.0.0", "v1.0.1"))
        assertTrue(UpdateChecker.isNewerVersion("1.0.0", "v1.0.1"))
        assertTrue(UpdateChecker.isNewerVersion("v1.0.0", "1.0.1"))

        // Different segment lengths
        assertTrue(UpdateChecker.isNewerVersion("1.0", "1.0.1"))
        assertFalse(UpdateChecker.isNewerVersion("1.0.1", "1.0"))

        // Same version
        assertFalse(UpdateChecker.isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(UpdateChecker.isNewerVersion("v1.0.0", "1.0.0"))
        assertFalse(UpdateChecker.isNewerVersion("1.0.0", "v1.0.0"))

        // Older version check
        assertFalse(UpdateChecker.isNewerVersion("1.0.1", "1.0.0"))
        assertFalse(UpdateChecker.isNewerVersion("1.1.0", "1.0.0"))
        assertFalse(UpdateChecker.isNewerVersion("2.0.0", "1.0.0"))

        // Empty / weird strings
        assertFalse(UpdateChecker.isNewerVersion("", ""))
        assertFalse(UpdateChecker.isNewerVersion("invalid", "invalid"))
    }
}
