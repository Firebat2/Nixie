package io.github.nightcalls.nixie.service.creating.listener

import io.github.nightcalls.nixie.listeners.VoiceEventListener
import io.github.nightcalls.nixie.repository.UsersInVoicesRepository
import io.github.nightcalls.nixie.service.count.VoiceTimeCountService
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

@Service
class VoiceEventListenerCreatingService(
    private val jda: JDA,
    private val service: VoiceTimeCountService,
    private val usersInVoicesRepository: UsersInVoicesRepository
) : ListenerAdapter() {
    private val listeners = ConcurrentHashMap<Long, VoiceEventListener>()

    /**
     * Создать и запустить VoiceEventListener'ы при поднятии контекста приложения
     */
    @EventListener
    fun onContextRefreshedEvent(event: ContextRefreshedEvent) {
        val joinedGuilds = jda.guilds
        logger.info { "Создаются VoiceEventListener'ы для серверов в количестве ${joinedGuilds.size}..." }
        joinedGuilds.forEach { createListener(it) }
        jda.addEventListener(this)
        logger.info { "VoiceEventListener'ы запущены" }
    }

    /**
     * Отключить данного слушателя изменений контекста и VoiceEventListener'ов при закрытии контекста приложения
     */
    @EventListener
    fun onContextClosedEvent(event: ContextClosedEvent) {
        jda.removeEventListener(this)
        usersInVoicesRepository.deleteAll()
        logger.info { "VoiceEventListener'ы отключены, данные о пользователях в голосовых каналах удалены" }
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        logger.info { "Бот присоединился к серверу ${event.guild.name}, создаётся VoiceEventListener..." }
        createListener(event.guild)
        logger.info { "VoiceEventListener для сервера ${event.guild.name} успешно запущен" }
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        if (removeListener(event.guild)) {
            usersInVoicesRepository.deleteAllByGuildId(event.guild.idLong)
            logger.info { "Бот покинул сервер ${event.guild.name}, VoiceEventListener успешно отключён, данные о пользователях в голосовых каналах этого сервера удалены" }
        }
    }

    private fun createListener(guild: Guild) {
        logger.debug { "Создаётся VoiceEventListener для сервера ${guild.name}..." }
        val listener = VoiceEventListener(guild, service)
        jda.addEventListener(listener)
        listeners[guild.idLong] = listener
        logger.debug { "VoiceEventListener для сервера ${guild.name} запущен" }
    }

    private fun removeListener(guild: Guild): Boolean {
        logger.debug { "Отключается VoiceEventListener для сервера ${guild.name}..." }
        listeners.remove(guild.idLong)?.let { listener ->
            jda.removeEventListener(listener)
            logger.debug { "VoiceEventListener для сервера ${guild.name} отключён" }
            return true
        }
        logger.debug { "VoiceEventListener для сервера ${guild.name} не найден" }
        return false
    }
}