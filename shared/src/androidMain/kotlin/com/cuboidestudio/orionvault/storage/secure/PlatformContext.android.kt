package com.cuboidestudio.orionvault.storage.secure

actual class PlatformContext(val context: android.content.Context)

actual fun createSecureCredentialStore(context: PlatformContext): SecureCredentialStore =
    AndroidSecureCredentialStore(context.context)
