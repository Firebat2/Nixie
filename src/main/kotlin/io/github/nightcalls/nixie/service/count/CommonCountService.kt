package io.github.nightcalls.nixie.service.count

import io.github.nightcalls.nixie.service.UserIdService
import io.github.nightcalls.nixie.service.count.dto.IdCountPair
import io.github.nightcalls.nixie.service.count.dto.NameValuePair
import io.github.nightcalls.nixie.utils.*
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.utils.FileUpload
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

private val logger = KotlinLogging.logger {}

@Service
abstract class CommonCountService(
    protected val userIdService: UserIdService
) {
    /**
     * Сформировать и вывести статистику
     */
    fun showStats(event: SlashCommandInteractionEvent) {
        event.deferReply(true).queue()

        val name: String? = event.getOption(NAME_OPTION)?.asString
        val startDate: String? = event.getOption(START_DATE_OPTION)?.asString
        val endDate: String? = event.getOption(END_DATE_OPTION)?.asString
        if (startDate != null && endDate != null) {
            if (name != null) {
                return getStatsForUserAndPeriod(event)
            }
            return getStatsForPeriod(event)
        }
        if (startDate != null || endDate != null) {
            return sendMissingDateEmbed(event.hook)
        }
        if (name != null) {
            return getStatsForUser(event)
        }
        return getStatsDefault(event)
    }

    protected abstract fun getStatsDefault(event: SlashCommandInteractionEvent)

    protected abstract fun getStatsForUser(event: SlashCommandInteractionEvent)

    protected abstract fun getStatsForPeriod(event: SlashCommandInteractionEvent)

    protected abstract fun getStatsForUserAndPeriod(event: SlashCommandInteractionEvent)

    protected fun convertUserNameToUserId(userName: String, hook: InteractionHook): Long? {
        val userId = userIdService.getUserIdByUserName(userName)
        if (userId == null) {
            sendUserNotFoundEmbed(hook)
        }
        return userId
    }

    protected fun validateAndConvertDate(date: String, hook: InteractionHook): LocalDate? {
        return try {
            LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: DateTimeParseException) {
            sendWrongDateFormatEmbed(hook)
            null
        }
    }

    protected fun sendUserNotFoundEmbed(hook: InteractionHook) {
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.setDescription("Пользователь не найден")
        try {
            hook.sendMessageEmbeds(embedBuilder.build()).queue()
        } catch (e: Exception) {
            logger.error { "Не удалось отправить сообщение о том, что пользователь не найден! $e" }
        }
    }

    protected fun sendMissingDateEmbed(hook: InteractionHook) {
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.setDescription("Не заполнена одна из дат")
        try {
            hook.sendMessageEmbeds(embedBuilder.build()).queue()
        } catch (e: Exception) {
            logger.error { "Не удалось отправить сообщение о том, что не заполнена одна из дат! $e" }
        }
    }

    protected fun sendWrongDateFormatEmbed(hook: InteractionHook) {
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.setDescription("Неправильный формат даты. Пример правильного формата: 2024-01-31")
        try {
            hook.sendMessageEmbeds(embedBuilder.build()).queue()
        } catch (e: Exception) {
            logger.error { "Не удалось отправить сообщение о неправильном формате даты! $e" }
        }
    }

    protected fun sendTitleEmbed(event: SlashCommandInteractionEvent, title: String) {
        // Перезапись "Thinking..." сообщения
        val deferEmbedBuilder = getCommonEmbedBuilder()
        deferEmbedBuilder.setDescription("Было запущено формирование статистики")
        // С упоминанием пользователя перед выводом результата
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.setDescription(title)
        try {
            event.hook.sendMessageEmbeds(deferEmbedBuilder.build()).queue()
            event.member?.asMention?.let { event.hook.sendMessage(it).addEmbeds(embedBuilder.build()).queue() }
        } catch (e: Exception) {
            logger.error { "Не удалось отправить сообщение об успешном формировании статистики! $e" }
        }
    }

    protected fun sendEmptyDataEmbed(hook: InteractionHook) {
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.setDescription("Данные отсутствуют")
        try {
            hook.sendMessageEmbeds(embedBuilder.build()).queue()
        } catch (e: Exception) {
            logger.error { "Не удалось отправить сообщение об отсутствующих данных! $e" }
        }
    }

    protected fun createCountView(userIdAndCount: IdCountPair): NameValuePair {
        return NameValuePair(
            userIdService.getUserNameByUserId(userIdAndCount.id),
            userIdAndCount.count.toString()
        )
    }

    protected fun createCountView(userIdAndCount: IdCountPair, userName: String): NameValuePair {
        return NameValuePair(
            userName,
            userIdAndCount.count.toString()
        )
    }

    protected fun createCountViewWithTimeFormat(userIdAndTimeCount: IdCountPair): NameValuePair {
        val voiceTime = userIdAndTimeCount.count
        return NameValuePair(
            userIdService.getUserNameByUserId(userIdAndTimeCount.id),
            String.format("%02d:%02d:%02d", voiceTime / 3600, (voiceTime % 3600) / 60, voiceTime % 60)
        )
    }

    protected fun createCountViewWithTimeFormat(userIdAndTimeCount: IdCountPair, userName: String): NameValuePair {
        val voiceTime = userIdAndTimeCount.count
        return NameValuePair(
            userName,
            String.format("%02d:%02d:%02d", voiceTime / 3600, (voiceTime % 3600) / 60, voiceTime % 60)
        )
    }

    /**
     * Закончить формировать и вывести однострочную статистику
     */
    protected fun createSingleOutput(
        event: SlashCommandInteractionEvent,
        view: NameValuePair?,
        fileTitle: String
    ) {
        if (view == null) {
            sendEmptyDataEmbed(event.hook)
            return
        }
        val formationTime = OffsetDateTime.now().toCommonLocalDateTime().truncatedTo(ChronoUnit.SECONDS)
        val result = StringBuilder()
        result.append("${view.name} ${view.value}\n")
        sendStatsFile(result, event, formationTime, fileTitle)
    }

    /**
     * Закончить формировать и вывести многострочную статистику
     */
    protected fun createTableOutput(
        event: SlashCommandInteractionEvent,
        viewsList: List<NameValuePair>,
        fileTitle: String
    ) {
        if (viewsList.isEmpty()) {
            sendEmptyDataEmbed(event.hook)
            return
        }
        val formationTime = OffsetDateTime.now().toCommonLocalDateTime().truncatedTo(ChronoUnit.SECONDS)
        val firstColumnWidth = viewsList.size.toString().length + 3
        val secondColumnWidth = viewsList.maxBy { it.name.length }.name.length + 2
        logger.debug { "Рассчитаны ширины первого ($firstColumnWidth) и второго ($secondColumnWidth) столбцов" }
        var i = 1
        val result = StringBuilder()
        viewsList.forEach {
            val firstColumn = StringUtils.rightPad("${i++}. -", firstColumnWidth, "-")
            val secondColumn = StringUtils.rightPad("${it.name} -", secondColumnWidth, "-")
            result.append("$firstColumn $secondColumn ${it.value}\n")
        }
        sendStatsFile(result, event, formationTime, fileTitle)
    }

    private fun sendStatsFile(
        result: StringBuilder,
        event: SlashCommandInteractionEvent,
        formationTime: LocalDateTime?,
        fileTitle: String
    ) {
        result.append("\nGuild: ${event.guild?.name}\n")
        result.append("Initiator: ${event.user.name}\n")

        val periodStart = event.getOption(START_DATE_OPTION)?.asString
        if (periodStart == null) {
            val timeJoined =
                event.guild?.selfMember?.timeJoined?.toCommonLocalDateTime()?.truncatedTo(ChronoUnit.SECONDS)
            result.append("Period: $timeJoined - $formationTime")
        } else {
            val periodEnd = event.getOption(END_DATE_OPTION)?.asString
            result.append("Period: $periodStart - $periodEnd")
        }

        try {
            event.hook.sendFiles(
                FileUpload.fromData(
                    result.toString().byteInputStream(StandardCharsets.UTF_8),
                    "Stats_${fileTitle}_$formationTime.txt"
                )
            ).queue()
        } catch (e: Exception) {
            logger.error { "Не удалось отправить файл статистики! $e" }
        }
    }
}