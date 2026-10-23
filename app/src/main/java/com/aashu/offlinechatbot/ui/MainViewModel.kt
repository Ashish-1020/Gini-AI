package com.aashu.offlinechatbot.ui

import androidx.lifecycle.ViewModel
import com.aashu.offlinechatbot.data.prefs.AppPreferences
import com.aashu.offlinechatbot.data.repository.ModelRepository
import com.aashu.offlinechatbot.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    prefs: AppPreferences,
    modelRepository: ModelRepository,
) : ViewModel() {
    val startDestination: String = when {
        !prefs.onboardingDone -> Routes.ONBOARDING
        modelRepository.isModelReady() -> Routes.CHAT
        else -> Routes.SETUP
    }
}
