package io.github.nightcalls.nixie.listeners

import io.github.nightcalls.nixie.listeners.dto.VoiceEventDto
import io.github.nightcalls.nixie.service.count.VoiceTimeCountService
import io.github.nightcalls.nixie.utils.toCommonLocalDateTime
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.time.OffsetDateTime

private val logger = KotlinLogging.logger {}

open class VoiceEventListener(
    private val guild: Guild,
    private val service: VoiceTimeCountService
) : ListenerAdapter() {

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        if (event.guild != guild) {
            return
        }
        val eventTime = OffsetDateTime.now().toCommonLocalDateTime()
        val eventDto = VoiceEventDto(event, eventTime)
        logger.info { "Получен ${event.javaClass.simpleName}: $eventDto" }
        service.increaseOrCreateCount(eventDto)
    }
}