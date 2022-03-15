package start.up.tracker.analytics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import start.up.tracker.analytics.principles.Pareto
import start.up.tracker.database.dao.TaskAnalyticsDao
import start.up.tracker.entities.Task
import start.up.tracker.entities.TaskAnalytics
import start.up.tracker.utils.TimeHelper
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/*  Важно реализовать правильное удаление активностей и их отмечание как выполненные.
Проблема в том, что активность можно завершить в срок, но отметить в приложении завершение этой
активности не всегда можно сразу. Нужно учесть, что опоздание может критичным или нет,
но мы не это будем игнорировать из-за сложности реализации.
В итоге прорабатываем 2 ситуации:
1. Отметка результата выполнения активности:
1.1. Выполнено:
1.1.1. В срок
1.1.2. С опозданием (если галочка выставлена в течение не более 24 часов после времен окончания
задания)
1.2. Не выполнено.
2. Удаление активности
При свайпе активности вбок появляется красная кнопка "удалить". Нажимаем на неё, система при этом
спросит пункт 1. После удаления нам нужно сохранить как раз категорию задания и её статус. И всё.
Это понадобится при дальнейшем анализе выполнения заданий из конкретных категорий.
К делу:
1. Получить всю информацию о всех активностях (текущих, завершённых в том числе и удалённых): важны
название, категория, статус завершения, дата. Также можно включить/отключить уведомления как внутри
программы, так и на уровне уведомления самой системы.
2. Создать страничку с названием и описанием принципа, референсами и кнопкой follow/unfollow
3. Для каждого из планируемых принципов создать свой класс. Состояния: метод followed/unfollowed,
увдомления внутри приложения enabled/disabled, уведомления в ОС enabled/disabled, совместимость
с другими (нельзя позволить включить 2 несовместивых принципа). В каждом классе
реализуем методы, в которых будем просто анализировать данные согласно методам. Нужны методы
нотификации (особенно если пользователь довольно серьезно и продолжительно нарушает принцип).
4. Важно заставить эти классы просыпаться при создании новой активности, чтобы они могли
выдать предупреждение или совет, стоит ли создавать активность или нет.
5. Желательно написать к каждому принципу алгоритм, который бы предлагал альтернативу согласно
идеологии принципа. Проблематично. Пользователь при получении предложения системы может
его отклонить или принять. Система должна это учитывать.
1)	принцип Парето (правило 80/20). Рекомендуется утром из 10 дел выбирать 2 наиболее важных и
основной упор делать на выполнении этих дел. Система будет предлагать расставить или исправить
приоритеты на следующий день или дни, если такой принцип на данный момент не совершенно не
выполняется: 30 дел, 27 которых – наивысшего приоритета.
2)	Принцип Эйзенхауэра. Система будет предлагать выделять задания по типу: срочные и важные,
несрочные и неважные, срочные и неважные, а также несрочные и важные путём выбора приоритетов
(от 0 до 3). Хорошо сочетается с принципом Парето.
3)	Эффективным считается добавление “Ненужных дел” – дел, которые выполнять нежелательно
или даже не нужно в случае, если продуктивность человека падает слишком сильно, и человек
тратит время на совершенно ненужные и неважные дела (просмотр TikTok и т.п.).
Такая инверсная методика позволяет поменять своё отношение к таким активностям и
концентрироваться на действительно важных делах и игнорировать хоть желанные, но не нужные
вещи (поиграть в компьютерную игру в четверг после пар в 16:20).
4)	После часа работы нужен отдых в 5 минут. Система при выполнении таски может посылать
уведомление об этом. Необходимость внедрения этой методики стоит под сомнением.
5)	Избежание многозадачной работы. Многозадачная работа временно снижает интеллектуальные
способности: было доказано, что IQ падает в среднем на 10 единиц (USA Today). Система будет
предлагать группировать задачи (привычки, очевидно, нет), если все длятся недлительный период времени (до часу – двух часов) и разбросаны на протяжении нескольких дней.
6)	Дробление задачи на этапы. Тоже полезно: увеличивает продуктивность и вероятность успешного
 выполнения задания.
7)	Метод АБВГД: очередная методика приоритетов. Необходимо сделать – А, следует сделать – Б,
неплохо сделать – В и т.д. Тоже неплохо сочетается с принципом Парето.
 */

@Singleton
class ActiveAnalytics @Inject constructor(
    private val taskAnalyticsDao: TaskAnalyticsDao
) {
    private var allPrinciples = ArrayList<Principle>()

    suspend fun addTask(task: Task) = withContext(Dispatchers.Default) {
        taskAnalyticsDao.insertTaskAnalytics(mapTaskToAnalyticsTask(task))
    }

    suspend fun updateStatus(task: Task) = withContext(Dispatchers.Default) {
        if (task.completed) {
            finishTask(task);
        } else {
            unfinishTask(task);
        }
    }

    private suspend fun finishTask(task: Task) = withContext(Dispatchers.Default) {
        val taskAnalytics = taskAnalyticsDao.getTaskById(task.taskId)
        val inTime = isFinishedInTime(taskAnalytics);
        val newTaskAnalytics = taskAnalytics.copy(completed = true, completedInTime = inTime)
        taskAnalyticsDao.updateTaskAnalytics(newTaskAnalytics)
    }

    private suspend fun unfinishTask(task: Task) = withContext(Dispatchers.Default) {
        val taskAnalytics = taskAnalyticsDao.getTaskById(task.taskId)
        val newTaskAnalytics = taskAnalytics.copy(completed = false, completedInTime = false)
        taskAnalyticsDao.updateTaskAnalytics(newTaskAnalytics)
    }

    suspend fun deleteAllTasks() = withContext(Dispatchers.Default) {
        val tasks = taskAnalyticsDao.getAllTasks()
        for (task in tasks) {
            taskAnalyticsDao.deleteTaskAnalytics(task)
        }
    }

    // TODO think of location to get all existing principles
    fun preparePrinciples() {
        allPrinciples.add(Pareto())
    }

    private fun mapTaskToAnalyticsTask(task: Task): TaskAnalytics {
        return TaskAnalytics(
            id = task.taskId,
            taskId = task.taskId,
            title = task.title,
            date = task.date,
            categoryId = task.categoryId,
            categoryName = task.categoryName,
        )
    }

    private fun isFinishedInTime(taskAnalytics: TaskAnalytics): Boolean {
        val currentDate = TimeHelper.getCurrentDayInMilliseconds()
        return if (taskAnalytics.date != null) {
            val dif: Long = if (currentDate - taskAnalytics.date > 0) {
                currentDate - taskAnalytics.date
            } else {
                (-1) * (currentDate - taskAnalytics.date)
            }
            val days: Long = TimeUnit.MILLISECONDS.toDays(dif)
            days < 1
        } else {
            true
        }
    }

    fun manager() {
        // будем вызывать логику каждого из методов при необходимости: при создании таска
    }

}