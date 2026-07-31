package com.cuboidestudio.orionvault.storage.secure

actual class PlatformContext

actual fun createSecureCredentialStore(context: PlatformContext): SecureCredentialStore =
    IosSecureCredentialStore()
