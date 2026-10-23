package com.aashu.offlinechatbot.data.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(private val prefs: SharedPreferences) {
    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_DONE, value) }

    var activeDownloadId: Long
        get() = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
        set(value) = prefs.edit(commit = true) { putLong(KEY_DOWNLOAD_ID, value) }

    private companion object {
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_DOWNLOAD_ID = "model_download_id"
    }
}
