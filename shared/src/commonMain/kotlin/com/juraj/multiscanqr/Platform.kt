package com.juraj.multiscanqr

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform