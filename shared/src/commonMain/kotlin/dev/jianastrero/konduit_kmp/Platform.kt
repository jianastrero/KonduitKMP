package dev.jianastrero.konduit_kmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform