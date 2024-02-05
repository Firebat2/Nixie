package io.github.nightcalls.nixie.repository

import io.github.nightcalls.nixie.repository.record.UserInVoiceRecord
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UsersInVoicesRepository : CrudRepository<UserInVoiceRecord, Long> {

    fun findByGuildIdAndUserId(guildId: Long, userId: Long): Optional<UserInVoiceRecord>

    /**
     * Записать время входа в голосовой канал конкретного пользователя конкретного сервера; не обновлять существующую запись в случае перехода между голосовыми каналами одного и того же сервера
     */
    @Modifying
    @Query(
        value = "INSERT INTO users_in_voices (guild_id, user_id, entry_time) values (:#{#record.guildId}, :#{#record.userId}, :#{#record.entryTime}) ON CONFLICT (guild_id, user_id) DO NOTHING",
        nativeQuery = true
    )
    fun saveIfNotSameGuildJoining(record: UserInVoiceRecord): Int

    fun deleteAllByGuildId(guildId: Long)
}