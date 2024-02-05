package io.github.nightcalls.nixie.repository.record

import io.github.nightcalls.nixie.listeners.dto.MessageEventDto
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "message_counts",
    indexes = [Index(
        name = "message_counts_guild_user_date_index",
        columnList = "guildId, userId, date",
        unique = true
    )]
)
data class MessageCountRecord(
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
     * Счётчик сообщений за минимальный период агрегации
     */
    @Column(nullable = false)
    val messageCount: Int = 1
) {
    constructor(eventDto: MessageEventDto) : this(
        guildId = eventDto.guildId,
        userId = eventDto.userId,
        date = eventDto.date
    )
}