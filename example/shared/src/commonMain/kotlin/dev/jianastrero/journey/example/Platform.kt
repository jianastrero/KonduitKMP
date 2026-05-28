package dev.jianastrero.journey.example

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
