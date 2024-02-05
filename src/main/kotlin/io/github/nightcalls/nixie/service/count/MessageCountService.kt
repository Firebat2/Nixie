package io.github.nightcalls.nixie.service.count

import io.github.nightcalls.nixie.listeners.dto.MessageEventDto
import io.github.nightcalls.nixie.repository.MessageCountRepository
import io.github.nightcalls.nixie.repository.record.MessageCountRecord
import io.github.nightcalls.nixie.service.UserIdService
import io.github.nightcalls.nixie.utils.END_DATE_OPTION
import io.github.nightcalls.nixie.utils.NAME_OPTION
import io.github.nightcalls.nixie.utils.START_DATE_OPTION
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class MessageCountService(
    private val repository: MessageCountRepository,
    userIdService: UserIdService
) : CommonCountService(userIdService) {

    /**
     * Создать новый или увеличить существующий счётчик отправленных сообщений
     */
    @Transactional
    fun increaseOrCreateCount(eventDto: MessageEventDto) {
        val record = repository.findByGuildIdAndUserIdAndDate(eventDto.guildId, eventDto.userId, eventDto.date)
        if (record.isPresent) {
            logger.debug { "Счётчик, соответствующий $eventDto, уже есть в базе" }
            val result = repository.increaseMessageCountById(record.get().id)
            logger.debug { "Счётчик, соответствующий $eventDto, увеличен: ${result == 1}" }
            return
        }
        logger.debug { "Счётчик, соответствующий $eventDto, отсутствует в базе" }
        val result = repository.save(MessageCountRecord(eventDto))
        logger.debug { "Счётчик, соответствующий $eventDto, создан: $result" }
    }

    /**
     * Обработать счётчики сообщений этого сервера и вывести в виде списка "порядковый номер + имя пользователя + суммарное кол-во сообщений"
     */
    @Transactional
    override fun getStatsDefault(event: SlashCommandInteractionEvent) {
        val messageViewsList = event.guild!!.let {
            val result = repository.sumAllCountsByGuildIdAndGroupByUserIds(it.idLong)
            logger.debug { "При сборе статистики сообщений было сформировано записей: ${result.size}" }
            result
        }.map { createCountView(it) }

        sendTitleEmbed(event, "Сформирована статистика по количеству отправленных сообщений")
        createTableOutput(event, messageViewsList, "messages")
        logger.info { "Отправлена статистика сообщений на сервер ${event.guild?.name}" }
    }

    /**
     * Обработать счётчики сообщений конкретного пользователя этого сервера и вывести в виде строки "имя пользователя + суммарное кол-во сообщений"
     */
    @Transactional
    override fun getStatsForUser(event: SlashCommandInteractionEvent) {
        val userName = event.getOption(NAME_OPTION)!!.asString
        val userId = convertUserNameToUserId(userName, event.hook) ?: return

        val messageView = event.guild!!.let {
            val result = repository.sumCountsByGuildIdAndUserId(it.idLong, userId)
            logger.debug { "При сборе статистики сообщений конкретного пользователя была сформирована запись: ${result != null}" }
            result
        }?.let { createCountView(it, userName) }

        sendTitleEmbed(event, "Сформирована статистика по количеству отправленных сообщений пользователем $userName")
        createSingleOutput(event, messageView, "messages_user")
        logger.info { "Отправлена статистика сообщений пользователя $userName на сервер ${event.guild?.name}" }
    }

    /**
     * Обработать счётчики сообщений этого сервера за конкретный период и вывести в виде списка "порядковый номер + имя пользователя + суммарное кол-во сообщений"
     */
    @Transactional
    override fun getStatsForPeriod(event: SlashCommandInteractionEvent) {
        val startDate = validateAndConvertDate(event.getOption(START_DATE_OPTION)!!.asString, event.hook) ?: return
        val endDate = validateAndConvertDate(event.getOption(END_DATE_OPTION)!!.asString, event.hook) ?: return

        val messageViewsList = event.guild!!.let {
            val result = repository.sumAllCountsByGuildIdAndPeriodAndGroupByUserIds(it.idLong, startDate, endDate)
            logger.debug { "При сборе статистики сообщений за конкретный период было сформировано записей: ${result.size}" }
            result
        }.map { createCountView(it) }

        sendTitleEmbed(
            event, "Сформирована статистика по количеству отправленных сообщений за период $startDate - $endDate"
        )
        createTableOutput(event, messageViewsList, "messages_period")
        logger.info { "Отправлена статистика сообщений периода $startDate-$endDate на сервер ${event.guild?.name}" }
    }

    /**
     * Обработать счётчики сообщений конкретного пользователя этого сервера за конкретный период и вывести в виде строки "имя пользователя + суммарное кол-во сообщений"
     */
    @Suppress("DuplicatedCode")
    @Transactional
    override fun getStatsForUserAndPeriod(event: SlashCommandInteractionEvent) {
        val userName = event.getOption(NAME_OPTION)!!.asString
        val userId = convertUserNameToUserId(userName, event.hook) ?: return
        val startDate = validateAndConvertDate(event.getOption(START_DATE_OPTION)!!.asString, event.hook) ?: return
        val endDate = validateAndConvertDate(event.getOption(END_DATE_OPTION)!!.asString, event.hook) ?: return

        val messageView = event.guild!!.let {
            val result = repository.sumCountsByGuildIdAndUserIdAndPeriod(it.idLong, userId, startDate, endDate)
            logger.debug { "При сборе статистики сообщений конкретного пользователя за конкретный период была сформирована запись: ${result != null}" }
            result
        }?.let { createCountView(it, userName) }

        sendTitleEmbed(
            event,
            "Сформирована статистика по количеству отправленных сообщений пользователем $userName за период $startDate - $endDate"
        )
        createSingleOutput(event, messageView, "messages_user_period")
        logger.info { "Отправлена статистика сообщений пользователя $userName периода $startDate-$endDate на сервер ${event.guild?.name}" }
    }
}