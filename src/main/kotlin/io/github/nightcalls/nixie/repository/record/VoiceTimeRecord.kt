package io.github.nightcalls.nixie.repository.record

import io.github.nightcalls.nixie.listeners.dto.VoiceEventDto
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "voice_time",
    indexes = [Index(name = "voice_time_guild_user_date_index", columnList = "guildId, userId, date", unique = true)]
)
data class VoiceTimeRecord(
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
     * Дата минимального периода агрегации (день)
     */
    @Column(nullable = false)
    val date: LocalDate,
    /**
     * Проведенное в голосовых каналах время в секундах за минимальный период агрегации
     */
    @Column(nullable = false)
    val time: Int
) {
    constructor(eventDto: VoiceEventDto, time: Int) : this(
        guildId = eventDto.guildId,
        userId = eventDto.userId,
        date = eventDto.date,
        time = time
    )
}