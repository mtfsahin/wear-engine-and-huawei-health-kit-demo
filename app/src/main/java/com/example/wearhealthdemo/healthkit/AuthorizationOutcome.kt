package com.example.wearhealthdemo.healthkit

import android.net.Uri

sealed interface AuthorizationOutcome {
    data class Redirect(val uri: Uri) : AuthorizationOutcome

    data class Failed(val message: String) : AuthorizationOutcome
}
