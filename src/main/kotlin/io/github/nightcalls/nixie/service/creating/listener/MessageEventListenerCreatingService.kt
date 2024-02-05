package io.github.nightcalls.nixie.service.creating.listener

import io.github.nightcalls.nixie.listeners.MessageEventListener
import io.github.nightcalls.nixie.service.count.MessageCountService
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
class MessageEventListenerCreatingService(
    private val jda: JDA,
    private val service: MessageCountService
) : ListenerAdapter() {
    private val listeners = ConcurrentHashMap<Long, MessageEventListener>()

    /**
     * Создать и запустить MessageEventListener'ы при поднятии контекста приложения
     */
    @EventListener
    fun onContextRefreshedEvent(event: ContextRefreshedEvent) {
        val joinedGuilds = jda.guilds
        logger.info { "Создаются MessageEventListener'ы для серверов в количестве ${joinedGuilds.size}..." }
        joinedGuilds.forEach { createListener(it) }
        jda.addEventListener(this)
        logger.info { "MessageEventListener'ы запущены" }
    }

    /**
     * Отключить данного слушателя изменений контекста и MessageEventListener'ов при закрытии контекста приложения
     */
    @EventListener
    fun onContextClosedEvent(event: ContextClosedEvent) {
        jda.removeEventListener(this)
        logger.info { "MessageEventListener'ы отключены" }
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        logger.info { "Бот присоединился к серверу ${event.guild.name}, создаётся MessageEventListener..." }
        createListener(event.guild)
        logger.info { "MessageEventListener для сервера ${event.guild.name} успешно запущен" }
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        if (removeListener(event.guild)) {
            logger.info { "Бот покинул сервер ${event.guild.name}, MessageEventListener успешно отключён" }
        }
    }

    private fun createListener(guild: Guild) {
        logger.debug { "Создаётся MessageEventListener для сервера ${guild.name}..." }
        val listener = MessageEventListener(guild, service)
        jda.addEventListener(listener)
        listeners[guild.idLong] = listener
        logger.debug { "MessageEventListener для сервера ${guild.name} запущен" }
    }

    private fun removeListener(guild: Guild): Boolean {
        logger.debug { "Отключается MessageEventListener для сервера ${guild.name}..." }
        listeners.remove(guild.idLong)?.let { listener ->
            jda.removeEventListener(listener)
            logger.debug { "MessageEventListener для сервера ${guild.name} отключён" }
            return true
        }
        logger.debug { "MessageEventListener для сервера ${guild.name} не найден" }
        return false
    }
}