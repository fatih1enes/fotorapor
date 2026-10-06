package com.fatihenes.photoreport.core.common.manager

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
    WHITE_LABEL_REPORTS,
    CUSTOM_COMPANY_BRANDING,
    HIGH_CONTRAST_FIELD_THEME,
    PHOTO_MARKUP_ANNOTATION,
    UNLIMITED_PROJECTS
}

interface CommercialManager {
    fun getCurrentTier(): UserTier
    fun isFeatureUnlocked(feature: CommercialFeature): Boolean
    fun isWhiteLabelEnabled(): Boolean = isFeatureUnlocked(CommercialFeature.WHITE_LABEL_REPORTS)
}

@Singleton
class LocalCommercialManager @Inject constructor() : CommercialManager {

    override fun getCurrentTier(): UserTier {
        // Defaults to FREE tier for standard installation
        return UserTier.FREE
    }

    override fun isFeatureUnlocked(feature: CommercialFeature): Boolean {
        return when (feature) {
            CommercialFeature.WHITE_LABEL_REPORTS -> getCurrentTier() == UserTier.PRO
            else -> true
        }
    }
}
