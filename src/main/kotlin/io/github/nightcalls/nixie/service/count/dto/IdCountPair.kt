package io.github.nightcalls.nixie.service.count.dto

data class IdCountPair(
    val id: Long,
    val count: Int
) {
    constructor(id: Long, count: Long) : this(id, count.toInt())
}