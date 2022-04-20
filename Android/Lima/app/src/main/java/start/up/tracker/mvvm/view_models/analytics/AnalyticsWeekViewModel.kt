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
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@HiltViewModel
class AnalyticsWeekViewModel @Inject constructor(
    private val dao: AnalyticsDao
) : ViewModel() {

    inner class ChartData(d: MutableList<DataEntry>, t: String) {
        val data = d
        val title = t
    }

    val chartDataList : MutableList<ChartData> = ArrayList()

    private var _statWeek: MutableLiveData<Boolean> = MutableLiveData(false)
    val statMonth: LiveData<Boolean>
        get() = _statWeek

    private val calendar = Calendar.getInstance()
    private val currentYear: Int = calendar.get(Calendar.YEAR)
    private val currentMonth: Int = calendar.get(Calendar.MONTH) + 1
    val currentMonthName: String = SimpleDateFormat("MMMM").format(calendar.time)

    init {
        loadTasks()
    }

    private fun loadTasks() = viewModelScope.launch {
        val stats = dao.getStatMonth(currentYear, currentMonth)

        loadCompletedTasks(stats)
        loadAllTasks(stats)
        loadCompletedTasks(stats)

        _statWeek.value = true
    }

    private fun loadCompletedTasks(stats: List<DayStat>) {
        val data: MutableList<DataEntry> = ArrayList()

        calendar.set(currentYear, currentMonth, 1)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthList: MutableMap<Int, Int> = mutableMapOf()

        for (i in 1..maxDay) {
            monthList[i] = 0
        }

        stats.forEach {
            monthList[it.day] = it.completedTasks
        }

        monthList.forEach {
            data.add(ValueDataEntry(it.key.toString(), it.value))
        }

        chartDataList.add(ChartData(data, "Completed tasks", ))
    }

    private fun loadAllTasks(stats: List<DayStat>) {
        val data: MutableList<DataEntry> = ArrayList()

        calendar.set(currentYear, currentMonth, 1)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthList: MutableMap<Int, Int> = mutableMapOf()

        for (i in 1..maxDay) {
            monthList[i] = 0
        }

        stats.forEach {
            monthList[it.day] = it.allTasks
        }

        monthList.forEach {
            data.add(ValueDataEntry(it.key.toString(), it.value))
        }

        chartDataList.add(ChartData(data, "All tasks", ))
    }
}
