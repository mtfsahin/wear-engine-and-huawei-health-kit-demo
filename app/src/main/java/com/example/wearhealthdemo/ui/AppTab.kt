package com.example.wearhealthdemo.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(val label: String, val title: String, val icon: ImageVector) {
    WEAR_ENGINE(
        label = "Wear Engine",
        title = "Wear Engine: messages & files",
        icon = Icons.AutoMirrored.Filled.Send,
    ),
    HEALTH_KIT(
        label = "Login-free",
        title = "Health Kit: login-free",
        icon = Icons.Default.FavoriteBorder,
    ),
}
