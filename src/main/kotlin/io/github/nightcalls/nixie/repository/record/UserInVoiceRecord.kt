package io.github.nightcalls.nixie.repository.record

import io.github.nightcalls.nixie.listeners.dto.VoiceEventDto
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "users_in_voices",
    indexes = [Index(name = "users_in_voices_guild_user_index", columnList = "guildId, userId", unique = true)]
)
data class UserInVoiceRecord(
    /**
     * Синтетический id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    /**
     * Id сервера
     */
    @Column(nullable = false)
    val guildId: Long,
    /**
     * Id пользователя
     */
    @Column(nullable = false)
    val userId: Long,
    /**
     * Время входа в голосовой канал
     */
    @Column(nullable = false)
    val entryTime: LocalDateTime
) {
    constructor(eventDto: VoiceEventDto) : this(
        guildId = eventDto.guildId,
        userId = eventDto.userId,
        entryTime = eventDto.time
    )
}