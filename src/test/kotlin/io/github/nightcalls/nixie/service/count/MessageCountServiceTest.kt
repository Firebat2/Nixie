package io.github.nightcalls.nixie.service.count

import io.github.nightcalls.nixie.listeners.dto.MessageEventDto
import io.github.nightcalls.nixie.repository.MessageCountRepository
import io.github.nightcalls.nixie.repository.record.MessageCountRecord
import io.github.nightcalls.nixie.service.UserIdService
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

class MessageCountServiceTest {
    @MockK
    private lateinit var repository: MessageCountRepository

    @MockK
    private lateinit var userIdService: UserIdService

    @InjectMockKs
    private lateinit var service: MessageCountService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @AfterEach
    fun tearDown() {
        // TODO?
    }

    @Test
    fun increaseCount() {
        val receivedEvent = mockk<MessageReceivedEvent>()
        val guild = mockk<Guild>()
        every { receivedEvent.guild } returns guild
        every { guild.idLong } returns 1
        val author = mockk<User>()
        every { receivedEvent.author } returns author
        every { author.idLong } returns 2
        val message = mockk<Message>()
        every { receivedEvent.message } returns message
        val date = OffsetDateTime.of(2024, 1, 11, 12, 30, 10, 40, ZoneOffset.UTC)
        every { message.timeCreated } returns date

        val eventDto = MessageEventDto(receivedEvent)
        val record = MessageCountRecord(eventDto)
        every { repository.findByGuildIdAndUserIdAndDate(1, 2, date.toLocalDate()) } returns Optional.empty()
        every { repository.save(record) } returns record

        service.increaseOrCreateCount(eventDto)

        verify(exactly = 1) { repository.findByGuildIdAndUserIdAndDate(1, 2, date.toLocalDate()) }
        verify(exactly = 1) { repository.save(record) }
    }

    @Test
    fun createCount() {
        val event = mockk<MessageReceivedEvent>()
        val guild = mockk<Guild>()
        every { event.guild } returns guild
        every { guild.idLong } returns 1
        val author = mockk<User>()
        every { event.author } returns author
        every { author.idLong } returns 2
        val message = mockk<Message>()
        every { event.message } returns message
        val date = OffsetDateTime.of(2024, 1, 11, 12, 30, 10, 40, ZoneOffset.UTC)
        every { message.timeCreated } returns date

        val eventDto = MessageEventDto(event)
        val record = MessageCountRecord(eventDto)
        every { repository.findByGuildIdAndUserIdAndDate(1, 2, date.toLocalDate()) } returns Optional.of(record)
        every { repository.increaseMessageCountById(0) } returns 1

        service.increaseOrCreateCount(eventDto)

        verify(exactly = 1) { repository.findByGuildIdAndUserIdAndDate(1, 2, date.toLocalDate()) }
        verify(exactly = 1) { repository.increaseMessageCountById(0) }
    }

    @Test
    fun getStatsDefault() {
        val event = mockk<SlashCommandInteractionEvent>()
    }

    @Test
    fun getStatsForUser() {
    }

    @Test
    fun getStatsForPeriod() {
    }

    @Test
    fun getStatsForUserAndPeriod() {
    }
}