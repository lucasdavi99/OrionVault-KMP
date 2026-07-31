package com.cuboidestudio.orionvault

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform