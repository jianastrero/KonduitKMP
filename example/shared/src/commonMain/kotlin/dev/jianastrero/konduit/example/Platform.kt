package dev.jianastrero.konduit.example

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform