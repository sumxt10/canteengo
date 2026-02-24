package com.example.canteengo.utils

import com.example.canteengo.models.AdminProfile
import com.example.canteengo.models.MenuItem
import com.example.canteengo.models.StudentProfile
import com.example.canteengo.repository.SpendingStats

/**
 * In-memory cache manager for smoother navigation.
 * Caches frequently accessed data to avoid visible loading states when navigating between screens.
 * Data is refreshed in background while cached data is displayed immediately.
 */
object CacheManager {

    // User profile caches
    private var cachedStudentProfile: StudentProfile? = null
    private var cachedAdminProfile: AdminProfile? = null
    private var profileCacheTime: Long = 0

    // Menu items cache
    private var cachedMenuItems: List<MenuItem>? = null
    private var menuCacheTime: Long = 0

    // Spending stats cache
    private var cachedSpendingStats: SpendingStats? = null
    private var statsCacheTime: Long = 0

    // Cache validity duration (5 minutes)
    private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L

    // Quick cache validity (30 seconds) for more dynamic data
    private const val QUICK_CACHE_VALIDITY_MS = 30 * 1000L

    // Student Profile
    fun cacheStudentProfile(profile: StudentProfile) {
        cachedStudentProfile = profile
        profileCacheTime = System.currentTimeMillis()
    }

    fun getCachedStudentProfile(): StudentProfile? {
        return if (isCacheValid(profileCacheTime)) cachedStudentProfile else null
    }

    fun getStudentProfileEvenIfStale(): StudentProfile? = cachedStudentProfile

    // Admin Profile
    fun cacheAdminProfile(profile: AdminProfile) {
        cachedAdminProfile = profile
        profileCacheTime = System.currentTimeMillis()
    }

    fun getCachedAdminProfile(): AdminProfile? {
        return if (isCacheValid(profileCacheTime)) cachedAdminProfile else null
    }

    fun getAdminProfileEvenIfStale(): AdminProfile? = cachedAdminProfile

    // Menu Items
    fun cacheMenuItems(items: List<MenuItem>) {
        cachedMenuItems = items
        menuCacheTime = System.currentTimeMillis()
    }

    fun getCachedMenuItems(): List<MenuItem>? {
        return if (isCacheValid(menuCacheTime)) cachedMenuItems else null
    }

    fun getMenuItemsEvenIfStale(): List<MenuItem>? = cachedMenuItems

    // Spending Stats
    fun cacheSpendingStats(stats: SpendingStats) {
        cachedSpendingStats = stats
        statsCacheTime = System.currentTimeMillis()
    }

    fun getCachedSpendingStats(): SpendingStats? {
        return if (isQuickCacheValid(statsCacheTime)) cachedSpendingStats else null
    }

    fun getSpendingStatsEvenIfStale(): SpendingStats? = cachedSpendingStats

    // Clear all caches (on logout)
    fun clearAll() {
        cachedStudentProfile = null
        cachedAdminProfile = null
        cachedMenuItems = null
        cachedSpendingStats = null
        profileCacheTime = 0
        menuCacheTime = 0
        statsCacheTime = 0
    }

    // Clear user-specific caches
    fun clearUserData() {
        cachedStudentProfile = null
        cachedAdminProfile = null
        cachedSpendingStats = null
        profileCacheTime = 0
        statsCacheTime = 0
    }

    private fun isCacheValid(cacheTime: Long): Boolean {
        return System.currentTimeMillis() - cacheTime < CACHE_VALIDITY_MS
    }

    private fun isQuickCacheValid(cacheTime: Long): Boolean {
        return System.currentTimeMillis() - cacheTime < QUICK_CACHE_VALIDITY_MS
    }
}

