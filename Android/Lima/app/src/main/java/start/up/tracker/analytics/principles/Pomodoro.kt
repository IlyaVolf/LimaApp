package start.up.tracker.analytics.principles

import start.up.tracker.R
import start.up.tracker.analytics.entities.AnalyticsMessage
import start.up.tracker.analytics.principles.base.Principle
import start.up.tracker.database.TechniquesIds
import start.up.tracker.entities.Task
import start.up.tracker.utils.TimeHelper
import start.up.tracker.utils.resources.ResourcesUtils

class Pomodoro : Principle {

    override suspend fun checkComplianceOnAddTask(task: Task): AnalyticsMessage? {
        return logicAddEditTask(task)
    }

    override suspend fun checkComplianceOnEditTask(task: Task): AnalyticsMessage? {
        return logicAddEditTask(task)
    }

    /**
     * Первый, тестовый принцип
     * Добавление/редактирование новой таски:
     * Получаем список всех тасков, выполнение которых запланировано на день добавляемой/
     * редактируемой таски. Сканируем приоритетность этих тасков. 80% тасков не должны иметь
     * приоритета, 20% остальных тасков должны иметь приоритет (любой).
     * Для реагирования необходимо как минимум 3 таски.
     *
     * @param task активность
     */
    private fun logicAddEditTask(task: Task): AnalyticsMessage? {
        val currentDate = TimeHelper.getCurrentTimeInMilliseconds()
        val startDate = TimeHelper.computeStartDate(task)
        if (task.date == null || task.startTimeInMinutes == null || task.endTimeInMinutes == null) {
            return AnalyticsMessage(
                principleId = TechniquesIds.POMODORO,
                title = ResourcesUtils.getString(R.string.pomodoro_message_title),
                message = ResourcesUtils.getString(R.string.pomodoro_message_body),
                messageDetailed = ResourcesUtils.getString(R.string.pomodoro_message_detailed)
            )
        } else {
            // TODO write method
            if (currentDate >= startDate) {
                return null
            }
        }
        return null
    }
}
