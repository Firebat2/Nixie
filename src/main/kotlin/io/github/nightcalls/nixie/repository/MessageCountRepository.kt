package io.github.nightcalls.nixie.repository

import io.github.nightcalls.nixie.repository.record.MessageCountRecord
import io.github.nightcalls.nixie.service.count.dto.IdCountPair
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface MessageCountRepository : CrudRepository<MessageCountRecord, Long> {

    fun findByGuildIdAndUserIdAndDate(guildId: Long, userId: Long, date: LocalDate): Optional<MessageCountRecord>

    /**
     * Для записи с этим id увеличить счётчик сообщений на 1
     */
    @Modifying
    @Query("UPDATE MessageCountRecord mcr SET mcr.messageCount = mcr.messageCount + 1 WHERE mcr.id = :id")
    fun increaseMessageCountById(id: Long): Int

    /**
     * Сложить счётчики сообщений для всех пользователей конкретного сервера, сгруппировать пары "id пользователя + счётчик сообщений" и отсортировать по убыванию
     */
    @Query("SELECT new io.github.nightcalls.nixie.service.count.dto.IdCountPair(mcr.userId, SUM(mcr.messageCount)) FROM MessageCountRecord AS mcr WHERE mcr.guildId = :guildId GROUP BY mcr.userId ORDER BY SUM(mcr.messageCount) DESC")
    fun sumAllCountsByGuildIdAndGroupByUserIds(guildId: Long): List<IdCountPair>

    /**
     * Сложить счётчики сообщений для конкретного пользователя конкретного сервера
     */
    @Query("SELECT new io.github.nightcalls.nixie.service.count.dto.IdCountPair(mcr.userId, SUM(mcr.messageCount)) FROM MessageCountRecord AS mcr WHERE mcr.guildId = :guildId AND mcr.userId = :userId GROUP BY mcr.userId")
    fun sumCountsByGuildIdAndUserId(guildId: Long, userId: Long): IdCountPair?

    /**
     * Сложить счётчики сообщений для всех пользователей конкретного сервера за конкретный период, сгруппировать пары "id пользователя + счётчик сообщений" и отсортировать по убыванию
     */
    @Query("SELECT new io.github.nightcalls.nixie.service.count.dto.IdCountPair(mcr.userId, SUM(mcr.messageCount)) FROM MessageCountRecord AS mcr WHERE mcr.guildId = :guildId AND mcr.date BETWEEN :startDate AND :endDate GROUP BY mcr.userId ORDER BY SUM(mcr.messageCount) DESC")
    fun sumAllCountsByGuildIdAndPeriodAndGroupByUserIds(
        guildId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<IdCountPair>

    /**
     * Сложить счётчики сообщений для конкретного пользователя конкретного сервера за конкретный период
     */
    @Query("SELECT new io.github.nightcalls.nixie.service.count.dto.IdCountPair(mcr.userId, SUM(mcr.messageCount)) FROM MessageCountRecord AS mcr WHERE mcr.guildId = :guildId AND mcr.userId = :userId AND mcr.date BETWEEN :startDate AND :endDate GROUP BY mcr.userId")
    fun sumCountsByGuildIdAndUserIdAndPeriod(
        guildId: Long,
        userId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): IdCountPair?
}