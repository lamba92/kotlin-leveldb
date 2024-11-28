@file:Suppress("unused")

package com.github.lamba92.levelkt.tests

import kotlin.jvm.JvmInline

// Extension properties for binary units
val Number.bytes: MemorySize
    get() = MemorySize(this.toLong())
val Number.kilobytes: MemorySize
    get() = MemorySize(this.toLong() * 1_024)
val Number.megabytes: MemorySize
    get() = MemorySize(this.toLong() * 1_048_576)
val Number.gigabytes: MemorySize
    get() = MemorySize(this.toLong() * 1_073_741_824)
val Number.terabytes: MemorySize
    get() = MemorySize(this.toLong() * 1_099_511_627_776)

@JvmInline
value class MemorySize(val bytes: Long) : Comparable<MemorySize> {
    operator fun plus(other: MemorySize): MemorySize = MemorySize(bytes + other.bytes)
    operator fun minus(other: MemorySize): MemorySize = MemorySize(bytes - other.bytes)
    operator fun times(factor: Number): MemorySize = MemorySize(bytes * factor.toLong())
    operator fun div(factor: Number): MemorySize = MemorySize(bytes / factor.toLong())

    override fun compareTo(other: MemorySize): Int = bytes.compareTo(other.bytes)

    fun toKilobytes(): Double = bytes / 1_024.0
    fun toMegabytes(): Double = bytes / 1_048_576.0
    fun toGigabytes(): Double = bytes / 1_073_741_824.0
    fun toTerabytes(): Double = bytes / 1_099_511_627_776.0

    fun toReadableString(decimals: Int = 2): String {
        val (unit, size) = when {
            bytes >= 1_099_511_627_776L -> "TB" to toTerabytes()
            bytes >= 1_073_741_824L -> "GB" to toGigabytes()
            bytes >= 1_048_576L -> "MB" to toMegabytes()
            bytes >= 1_024L -> "KB" to toKilobytes()
            else -> "B" to bytes.toDouble()
        }
        return "%.${decimals}f $unit".format(size)
    }

    override fun toString() = toReadableString()
}