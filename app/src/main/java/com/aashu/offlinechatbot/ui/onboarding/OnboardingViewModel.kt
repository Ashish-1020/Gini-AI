package com.aashu.offlinechatbot.ui.onboarding

import androidx.lifecycle.ViewModel
import com.aashu.offlinechatbot.data.prefs.AppPreferences
import com.aashu.offlinechatbot.data.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val modelRepository: ModelRepository,
) : ViewModel() {
    fun completeOnboarding(): Boolean {
        prefs.onboardingDone = true
        return modelRepository.isModelReady()
    }
}
