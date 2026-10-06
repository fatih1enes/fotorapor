package com.fatihenes.photoreport.core.common.manager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommercialManagerTest {

    @Test
    fun testDefaultFreeTierFeatures() {
        val manager = LocalCommercialManager()
        assertEquals(UserTier.FREE, manager.getCurrentTier())
        assertFalse(manager.isWhiteLabelEnabled())
        assertFalse(manager.isFeatureUnlocked(CommercialFeature.WHITE_LABEL_REPORTS))
        assertTrue(manager.isFeatureUnlocked(CommercialFeature.HIGH_CONTRAST_FIELD_THEME))
        assertTrue(manager.isFeatureUnlocked(CommercialFeature.PHOTO_MARKUP_ANNOTATION))
    }
}
