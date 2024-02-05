package io.github.nightcalls.nixie.repository

import io.github.nightcalls.nixie.repository.record.VoiceTimeRecord
import io.github.nightcalls.nixie.service.count.dto.IdCountPair
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface VoiceTimeRepository : CrudRepository<VoiceTimeRecord, Long> {

    fun findByGuildIdAndUserIdAndDate(guildId: Long, userId: Long, date: LocalDate): Optional<VoiceTimeRecord>

    /**
     * Для записи с этим id увеличить время в голосовых каналах на величину времени последней сессии в голосовом канале
     */
    @Modifying
    @Query("UPDATE VoiceTimeRecord vtr SET vtr.time = vtr.time + :voiceTime WHERE vtr.id = :id")
    fun increaseVoiceTimeById(id: Long, voiceTime: Int): Int

    /**
     * Сложить время в голосовых каналах для всех пользователей конкретного сервера, сгруппировать пары "id пользователя + время в голосовых каналах" и отсортировать по убыванию
     */
    @Query("SELECT new io.github.nightcalls.nixie.service.count.dto.IdCountPair(vtr.userId, SUM(vtr.time)) FROM VoiceTimeRecord AS vtr WHERE vtr.guildId = :guildId GROUP BY vtr.userId ORDER BY SUM(vtr.time) DESC")
    fun sumAllTimeByGuildIdAndGroupByUserIds(guildId: Long): List<IdCountPair>

    /**
     * Сложить время в голосовых каналах для конкретного пользователя конкретного сервера
     */
    @Query("SELECT new io.github.nightcalls.nixie.service.count.dto.IdCountPair(vtr.userId, SUM(vtr.time)) FROM VoiceTimeRecord AS vtr WHERE vtr.guildId = :guildId AND vtr.userId = :userId GROUP BY vtr.userId")
    fun sumTimeByGuildIdAndUserId(guildId: Long, userId: Long): IdCountPair?

    /**
     * Сложить время в голосовых каналах для всех пользователей конкретного сервера за конкретный период, сгруппировать пары "id пользователя + время в голосовых каналах" и отсортировать по убыванию
     */
    @Query("SELECT new io.github.nightcalls.nixie.service.count.dto.IdCountPair(vtr.userId, SUM(vtr.time)) FROM VoiceTimeRecord AS vtr WHERE vtr.guildId = :guildId AND vtr.date BETWEEN :startDate AND :endDate GROUP BY vtr.userId ORDER BY SUM(vtr.time) DESC")
    fun sumAllTimeByGuildIdAndPeriodAndGroupByUserIds(
        guildId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<IdCountPair>

    /**
     * Сложить время в голосовых каналах для конкретного пользователя конкретного сервера за конкретный период
     */
    @Query("SELECT new io.github.nightcalls.nixie.service.count.dto.IdCountPair(vtr.userId, SUM(vtr.time)) FROM VoiceTimeRecord AS vtr WHERE vtr.guildId = :guildId AND vtr.userId = :userId AND vtr.date BETWEEN :startDate AND :endDate GROUP BY vtr.userId")
    fun sumTimeByGuildIdAndUserIdAndPeriod(
        guildId: Long,
        userId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): IdCountPair?
}