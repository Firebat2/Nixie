package io.github.nightcalls.nixie.listeners

import io.github.nightcalls.nixie.listeners.dto.MessageEventDto
import io.github.nightcalls.nixie.service.count.MessageCountService
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

private val logger = KotlinLogging.logger {}

open class MessageEventListener(
    private val guild: Guild,
    private val service: MessageCountService
) : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.guild != guild) {
            return
        }
        val eventDto = MessageEventDto(event)
        logger.info { "Получен ${event.javaClass.simpleName}: $eventDto" }
        service.increaseOrCreateCount(eventDto)
    }
}