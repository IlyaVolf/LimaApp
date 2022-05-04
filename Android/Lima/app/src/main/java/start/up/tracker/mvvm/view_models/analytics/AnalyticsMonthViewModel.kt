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
import start.up.tracker.entities.DayStat
import java.lang.StringBuilder
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@HiltViewModel
class AnalyticsMonthViewModel @Inject constructor(
    private val dao: AnalyticsDao,
) : ViewModel() {

    inner class ChartData(
        da: MutableList<DataEntry>,
        ti: String,
        av: String,
        de: String,
        fo: String,
        smax: Boolean,
        smin: Boolean,
        sh: Int,
        dn: String,
    ) {
        var data = da
        val title = ti
        var average = av
        var date = de
        val format = fo
        val isSoftMaximum = smax
        val isSoftMinimum = smin
        var shift = sh
        val description = dn
    }

    fun update(id: Int, sh: Int) = viewModelScope.launch {
        when (chartDataList[id].title) {
            "All tasks" -> {
                chartDataList[id] = loadAllTasks(sh + chartDataList[id].shift)
            }
            "Completed tasks" -> {
                chartDataList[id] = loadCompletedTasks(sh + chartDataList[id].shift)
            }
            "Productivity" -> {
                chartDataList[id] = loadProductivity(sh + chartDataList[id].shift)
            }
            "Productivity Tendency" ->
                chartDataList[id] = loadProductivityTendency(sh + chartDataList[id].shift)
        }

        _statMonth2.value = true
    }

    val chartDataList: MutableList<ChartData> = ArrayList()

    private var _statMonth: MutableLiveData<Boolean> = MutableLiveData(false)
    val statMonth: LiveData<Boolean>
        get() = _statMonth

    private var _statMonth2: MutableLiveData<Boolean> = MutableLiveData(false)
    val statMonth2: LiveData<Boolean>
        get() = _statMonth2

    init {
        loadTasks()
    }

    private fun loadTasks() = viewModelScope.launch {
        chartDataList.add(loadAllTasks(0))
        chartDataList.add(loadCompletedTasks(0))
        chartDataList.add(loadProductivity(0))
        chartDataList.add(loadProductivityTendency(0))

        _statMonth.value = true
    }

    private suspend fun loadAllTasks(shift: Int): ChartData {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + shift)
        val currentYear: Int = calendar.get(Calendar.YEAR)
        val currentMonth: Int = calendar.get(Calendar.MONTH) + 1
        val stats = dao.getStatMonth(currentYear, currentMonth)
        val data: MutableList<DataEntry> = ArrayList()
        val currentMonthName = SimpleDateFormat("MMMM").format(calendar.time)
        val currentDate =
            StringBuilder().append(currentMonthName).append(" ").append(currentYear).toString()

        calendar.set(currentYear, currentMonth, 1)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthList: MutableMap<Int, Int> = mutableMapOf()

        for (i in 1..maxDay) {
            monthList[i] = 0
        }

        var sum = 0

        stats.forEach {
            monthList[it.day] = it.allTasks
            sum += it.allTasks
        }

        val average = sum.toDouble() / maxDay.toDouble()

        monthList.forEach {
            data.add(ValueDataEntry(it.key.toString(), it.value))
        }

        return (ChartData(data, "All tasks", formatDouble(average),
            currentDate, "{%value}", false, false, shift,
            "Number of all your tasks in the month"
        ))
    }

    private suspend fun loadCompletedTasks(shift: Int): ChartData {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + shift)
        val currentYear: Int = calendar.get(Calendar.YEAR)
        val currentMonth: Int = calendar.get(Calendar.MONTH) + 1
        val stats = dao.getStatMonth(currentYear, currentMonth)

        val data: MutableList<DataEntry> = ArrayList()
        val currentMonthName = SimpleDateFormat("MMMM").format(calendar.time)
        val currentDate =
            StringBuilder().append(currentMonthName).append(" ").append(currentYear).toString()

        calendar.set(currentYear, currentMonth, 1)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthList: MutableMap<Int, Int> = mutableMapOf()

        for (i in 1..maxDay) {
            monthList[i] = 0
        }

        var sum = 0

        stats.forEach {
            monthList[it.day] = it.completedTasks
            sum += it.completedTasks
        }

        val average = sum.toDouble() / maxDay.toDouble()

        monthList.forEach {
            data.add(ValueDataEntry(it.key.toString(), it.value))
        }

        return (ChartData(data, "Completed tasks", formatDouble(average),
            currentDate, "{%value}", false, false, shift,
            "Number of your completed tasks in the month"
        ))
    }

    private suspend fun loadProductivity(shift: Int): ChartData {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + shift)
        val currentYear: Int = calendar.get(Calendar.YEAR)
        val currentMonth: Int = calendar.get(Calendar.MONTH) + 1
        val stats = dao.getStatMonth(currentYear, currentMonth)
        val data: MutableList<DataEntry> = ArrayList()
        val currentMonthName = SimpleDateFormat("MMMM").format(calendar.time)
        val currentDate =
            StringBuilder().append(currentMonthName).append(" ").append(currentYear).toString()

        calendar.set(currentYear, currentMonth, 1)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthList: MutableMap<Int, Double> = mutableMapOf()

        for (i in 1..maxDay) {
            monthList[i] = 0.0
        }

        var sum = 0.0
        var nonEmptyCounter = 0

        stats.forEach {
            if (it.completedTasks == 0 || it.allTasks == 0) {
                monthList[it.day] = 0.0
                sum += 1.0
            } else {
                monthList[it.day] = it.completedTasks.toDouble() / it.allTasks.toDouble() * 100
                sum += monthList[it.day]!!
                nonEmptyCounter++
            }
        }

        val average: Double = if (sum == 0.0 || nonEmptyCounter == 0) {
            0.0
        } else {
            sum / nonEmptyCounter
        }

        monthList.forEach {
            data.add(ValueDataEntry(it.key.toString(), it.value))
        }

        return (ChartData(data, "Productivity",
            formatDouble(average) + "%",
            currentDate, "{%value}%", true, false, shift,
            "The ratio of all tasks you completed in the month to all created tasks"
        ))
    }

    private suspend fun loadProductivityTendency(shift: Int): ChartData {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + shift)
        val currentYear: Int = calendar.get(Calendar.YEAR)
        val currentMonth: Int = calendar.get(Calendar.MONTH) + 1
        val stats = dao.getStatMonth(currentYear, currentMonth)
        val data: MutableList<DataEntry> = ArrayList()
        val currentMonthName = SimpleDateFormat("MMMM").format(calendar.time)
        val currentDate =
            StringBuilder().append(currentMonthName).append(" ").append(currentYear).toString()

        calendar.set(currentYear, currentMonth, 1)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthList: MutableMap<Int, Double> = mutableMapOf()

        for (i in 1..maxDay) {
            monthList[i] = 0.0
        }

        var sum = 0.0
        var nonEmptyCounter = 0
        var buf = 0.0

        stats.forEach {
            if (it.completedTasks == 0 || it.allTasks == 0) {
                monthList[it.day] = 0.0
                buf = 0.0
            } else {
                val new = it.completedTasks.toDouble() / it.allTasks.toDouble() * 100
                if (new == 0.0) {
                    monthList[it.day] = 0.0
                    buf = 0.0
                } else {
                    monthList[it.day] = (new - buf) / new * 100
                    buf = new
                }
                sum += monthList[it.day]!!
                nonEmptyCounter++
            }
        }

        val average: Double = if (sum == 0.0 || nonEmptyCounter == 0) {
            0.0
        } else {
            sum / nonEmptyCounter
        }

        monthList.forEach {
            data.add(ValueDataEntry(it.key.toString(), it.value))
        }

        return (ChartData(data, "Productivity Tendency",
            formatDouble(average) + "%",
            currentDate, "{%value}%", true, true, shift,
            "The ratio of your productivity compared to the previous day of the month"
        ))

    }

    private fun formatDouble(number: Double): String {
        val str = BigDecimal(number).setScale(1, RoundingMode.HALF_EVEN).toString()
        val lastSymbol = str.substring(str.length - 1)
        return if (lastSymbol == "0") {
            str.substring(0, str.length - 2);
        } else {
            str
        }
    }
}