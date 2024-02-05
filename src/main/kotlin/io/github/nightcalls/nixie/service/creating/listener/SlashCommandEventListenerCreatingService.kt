package io.github.nightcalls.nixie.service.creating.listener

import io.github.nightcalls.nixie.listeners.SlashCommandEventListener
import io.github.nightcalls.nixie.service.SlashCommandService
import io.github.nightcalls.nixie.service.count.MessageCountService
import io.github.nightcalls.nixie.service.count.VoiceTimeCountService
import io.github.nightcalls.nixie.utils.END_DATE_OPTION
import io.github.nightcalls.nixie.utils.NAME_OPTION
import io.github.nightcalls.nixie.utils.START_DATE_OPTION
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

@Service
class SlashCommandEventListenerCreatingService(
    private val jda: JDA,
    private val slashCommandService: SlashCommandService,
    private val messageCountService: MessageCountService,
    private val voiceTimeCountService: VoiceTimeCountService
) : ListenerAdapter() {
    private val listeners = ConcurrentHashMap<Long, SlashCommandEventListener>()

    /**
     * Создать и запустить SlashCommandEventListener'ы при поднятии контекста приложения
     */
    @EventListener
    fun onContextRefreshedEvent(event: ContextRefreshedEvent) {
        updateGlobalCommands(jda)
        val joinedGuilds = jda.guilds
        logger.info { "Создаются SlashCommandEventListener'ы для серверов в количестве ${joinedGuilds.size}..." }
        joinedGuilds.forEach { createListener(it) }
        jda.addEventListener(this)
        logger.info { "SlashCommandEventListener'ы запущены" }
    }

    /**
     * Отключить данного слушателя изменений контекста и SlashCommandEventListener'ов при закрытии контекста приложения
     */
    @EventListener
    fun onContextClosedEvent(event: ContextClosedEvent) {
        jda.removeEventListener(this)
        logger.info { "SlashCommandEventListener'ы отключены" }
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        logger.info { "Бот присоединился к серверу ${event.guild.name}, создаётся SlashCommandEventListener..." }
        createListener(event.guild)
        logger.info { "SlashCommandEventListener для сервера ${event.guild.name} успешно запущен" }
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        if (removeListener(event.guild)) {
            logger.info { "Бот покинул сервер ${event.guild.name}, SlashCommandEventListener успешно отключён" }
        }
    }

    fun updateGlobalCommands(jda: JDA) {
        jda.updateCommands().addCommands(
            Commands.slash("nixie", "Show info"),
        ).queue()
        logger.info { "Глобальные команды в количестве ${jda.retrieveCommands().complete().size} установлены" }
    }

    private fun createListener(guild: Guild) {
        logger.debug { "Создаётся SlashCommandEventListener для сервера ${guild.name}..." }
        val listener = SlashCommandEventListener(guild, slashCommandService, messageCountService, voiceTimeCountService)
        guild.updateCommands().addCommands(
            Commands.slash("stats-messages", "Show messages stats")
                .addOption(OptionType.STRING, NAME_OPTION, "Уникальное имя пользователя", false)
                .addOption(OptionType.STRING, START_DATE_OPTION, "Начало периода", false)
                .addOption(OptionType.STRING, END_DATE_OPTION, "Конец периода", false),
            Commands.slash("stats-voices", "Show voice time stats")
                .addOption(OptionType.STRING, NAME_OPTION, "Уникальное имя пользователя", false)
                .addOption(OptionType.STRING, START_DATE_OPTION, "Начало периода", false)
                .addOption(OptionType.STRING, END_DATE_OPTION, "Конец периода", false),
        ).queue()
        jda.addEventListener(listener)
        listeners[guild.idLong] = listener
        logger.debug {
            "SlashCommandEventListener для сервера ${guild.name} запущен, локальные команды в количестве ${
                guild.retrieveCommands().complete().size
            } установлены"
        }
    }

    private fun removeListener(guild: Guild): Boolean {
        logger.debug { "Отключается SlashCommandEventListener для сервера ${guild.name}..." }
        listeners.remove(guild.idLong)?.let { listener ->
            jda.removeEventListener(listener)
            logger.debug { "SlashCommandEventListener для сервера ${guild.name} отключён" }
            return true
        }
        logger.debug { "SlashCommandEventListener для сервера ${guild.name} не найден" }
        return false
    }
}