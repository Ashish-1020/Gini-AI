package com.aashu.offlinechatbot.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aashu.offlinechatbot.ui.chat.ChatRoute
import com.aashu.offlinechatbot.ui.onboarding.OnboardingRoute
import com.aashu.offlinechatbot.ui.setup.ModelSetupRoute
import com.aashu.offlinechatbot.ui.voice.VoiceRoute

object Routes {
    const val ONBOARDING = "onboarding"
    const val SETUP = "setup"
    const val MANAGE_MODEL = "manage_model"
    const val CHAT = "chat"
    const val VOICE = "voice"
}

@Composable
fun AppNavHost(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingRoute(
                onContinue = { modelReady ->
                    navController.navigateClearingStack(if (modelReady) Routes.CHAT else Routes.SETUP)
                },
            )
        }
        composable(Routes.SETUP) {
            ModelSetupRoute(
                onStartChat = { navController.navigateClearingStack(Routes.CHAT) },
                onBack = null,
            )
        }
        composable(Routes.MANAGE_MODEL) {
            ModelSetupRoute(
                onStartChat = { navController.navigateClearingStack(Routes.CHAT) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.CHAT) {
            ChatRoute(
                onManageModel = { navController.navigate(Routes.MANAGE_MODEL) },
                onOpenVoice = { navController.navigate(Routes.VOICE) { launchSingleTop = true } },
            )
        }
        composable(Routes.VOICE) {
            VoiceRoute(
                onBack = { navController.popBackStack() },
                onOpenChat = { navController.popBackStack() },
            )
        }
    }
}

private fun NavHostController.navigateClearingStack(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
