package start.up.tracker.mvvm.view_models.analytics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import start.up.tracker.database.dao.AnalyticsDao
import start.up.tracker.utils.TimeHelper
import java.lang.StringBuilder
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@HiltViewModel
class AnalyticsWeekViewModel @Inject constructor(
    private val dao: AnalyticsDao,
) : ViewModel() {

    inner class ChartData(d: MutableList<DataEntry>, t: String, a: Int, i: String) {
        val data = d
        val title = t
        val average = a
        val date = i
    }

    val chartDataList: MutableList<ChartData> = ArrayList()
    private val daysName = listOf("Mon", "Tue", "Wen", "Thu", "Fri", "Sat", "Sun")

    private var _statWeek: MutableLiveData<Boolean> = MutableLiveData(false)
    val statMonth: LiveData<Boolean>
        get() = _statWeek

    init {
        loadTasks()
    }

    private fun loadTasks() = viewModelScope.launch {
        loadCompletedTasks()
        loadAllTasks()

        _statWeek.value = true
    }

    private suspend fun loadCompletedTasks() {
        val calendar = Calendar.getInstance()
        val currentYear: Int = calendar.get(Calendar.YEAR)
        val currentMonth: Int = calendar.get(Calendar.MONTH) + 1
        val currentWeek: Int = calendar.get(Calendar.WEEK_OF_YEAR) + 1
        val currentDay: Int = calendar.get(Calendar.DAY_OF_MONTH)
        val stats = dao.getStatWeek(currentYear, currentWeek)

        val data: MutableList<DataEntry> = ArrayList()
        val currentDate =
            StringBuilder().append(TimeHelper.getStartOfWeekDayFromMillis(calendar.timeInMillis,
                (calendar.get(Calendar.DAY_OF_WEEK) - 1) % 7)).append(" ")
                .append(TimeHelper.getStartOfWeekMonthNameFromMillis(calendar.timeInMillis,
                    (calendar.get(Calendar.DAY_OF_WEEK) - 1) % 7)).append(" ")
                .append(TimeHelper.getEndOfWeekDayFromMillis(calendar.timeInMillis,
                    (calendar.get(Calendar.DAY_OF_WEEK) - 1) % 7)).append(" - ")
                .append(TimeHelper.getEndOfWeekMonthNameFromMillis(calendar.timeInMillis,
                    (calendar.get(Calendar.DAY_OF_WEEK) - 1) % 7)).append(" ")
                .toString()

        calendar.set(currentYear, currentMonth, currentDay)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_WEEK)

        val weekList: MutableMap<String, Int> = mutableMapOf()

        for (i in 0 until maxDay) {
            weekList[daysName[i]] = 0
        }

        var sum = 0

        stats.forEach {
            weekList[daysName[it.dayOfWeek - 1]] = it.completedTasks
            sum += it.completedTasks
        }

        val average = sum / maxDay

        weekList.forEach {
            data.add(ValueDataEntry(it.key, it.value))
        }

        chartDataList.add(ChartData(data, "Completed tasks", average, currentDate))
    }

    private suspend fun loadAllTasks() {
        val calendar = Calendar.getInstance()
        val currentYear: Int = calendar.get(Calendar.YEAR)
        val currentMonth: Int = calendar.get(Calendar.MONTH) + 1
        val currentWeek: Int = calendar.get(Calendar.WEEK_OF_YEAR) + 1
        val currentDay: Int = calendar.get(Calendar.DAY_OF_MONTH)
        val stats = dao.getStatWeek(currentYear, currentWeek)

        val data: MutableList<DataEntry> = ArrayList()
        val currentDate =
            StringBuilder().append(TimeHelper.getStartOfWeekDayFromMillis(calendar.timeInMillis,
                (calendar.get(Calendar.DAY_OF_WEEK) - 1) % 7)).append(" ")
                .append(TimeHelper.getStartOfWeekMonthNameFromMillis(calendar.timeInMillis,
                    (calendar.get(Calendar.DAY_OF_WEEK) - 1) % 7)).append(" ")
                .append(TimeHelper.getEndOfWeekDayFromMillis(calendar.timeInMillis,
                    (calendar.get(Calendar.DAY_OF_WEEK) - 1) % 7)).append(" - ")
                .append(TimeHelper.getEndOfWeekMonthNameFromMillis(calendar.timeInMillis,
                    (calendar.get(Calendar.DAY_OF_WEEK) - 1) % 7)).append(" ")
                .toString()

        calendar.set(currentYear, currentMonth, currentDay)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_WEEK)

        val weekList: MutableMap<String, Int> = mutableMapOf()

        for (i in 0 until maxDay) {
            weekList[daysName[i]] = 0
        }

        var sum = 0

        stats.forEach {
            weekList[daysName[it.dayOfWeek - 1]] = it.allTasks
            sum += it.allTasks
        }

        val average = sum / maxDay

        weekList.forEach {
            data.add(ValueDataEntry(it.key, it.value))
        }

        chartDataList.add(ChartData(data, "All tasks", average, currentDate))
    }
}
