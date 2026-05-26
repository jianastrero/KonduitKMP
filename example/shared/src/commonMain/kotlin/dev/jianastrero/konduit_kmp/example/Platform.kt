package dev.jianastrero.konduit_kmp.example

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform