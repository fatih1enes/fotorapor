package com.fatihenes.photoreport.manager

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the user's tier in the application.
 * Currently defaults to FREE with all non-commercial features active.
 * Extensible for Google Play Billing (Subscriptions / One-time purchases) in commercial release.
 */
enum class UserTier {
    FREE,
    PRO
}

/**
 * Feature flags for commercial tier capabilities.
 */
enum class CommercialFeature {
    // Future features: CUSTOM_WATERMARK_BRANDING, UNLIMITED_HIGH_RES_EXPORT, etc.
}

interface CommercialManager {
    fun getCurrentTier(): UserTier
    fun isFeatureUnlocked(feature: CommercialFeature): Boolean
}

@Singleton
class LocalCommercialManager @Inject constructor() : CommercialManager {

    override fun getCurrentTier(): UserTier {
        // Non-commercial stage: Default to FREE tier with standard features unlocked
        return UserTier.FREE
    }

    override fun isFeatureUnlocked(feature: CommercialFeature): Boolean {
        // In current non-commercial stage, all baseline features are unlocked and accessible
        return true
    }
}
