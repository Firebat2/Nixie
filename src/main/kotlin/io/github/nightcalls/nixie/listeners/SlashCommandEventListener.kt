package io.github.nightcalls.nixie.listeners

import io.github.nightcalls.nixie.service.SlashCommandService
import io.github.nightcalls.nixie.service.count.MessageCountService
import io.github.nightcalls.nixie.service.count.VoiceTimeCountService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.minus
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

private val logger = KotlinLogging.logger {}

class SlashCommandEventListener(
    private val guild: Guild,
    private val slashCommandService: SlashCommandService,
    private val messageCountService: MessageCountService,
    private val voiceTimeCountService: VoiceTimeCountService
) : ListenerAdapter() {
    private var lastCommandTime: Instant = Clock.System.now().minus(10, DateTimeUnit.SECOND)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.guild != guild) {
            return
        }
        logger.info { "Получен ${event.javaClass.simpleName}: ${event.name}" }
        when (event.name) {
            "nixie" -> slashCommandService.showInfo(event)
            "stats-messages" -> checkCoolDown(event) { messageCountService.showStats(event) }
            "stats-voices" -> checkCoolDown(event) { voiceTimeCountService.showStats(event) }
        }
    }

    private fun checkCoolDown(
        event: SlashCommandInteractionEvent,
        function: (event: SlashCommandInteractionEvent) -> Unit
    ) {
        if ((Clock.System.now() - lastCommandTime).inWholeSeconds < 10) {
            return slashCommandService.coolDownReply(event)
        }
        lastCommandTime = Clock.System.now()
        return function(event)
    }
}