package start.up.tracker.ui.extensions.list

import android.view.MotionEvent
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import start.up.tracker.entities.Task
import start.up.tracker.mvvm.view_models.tasks.base.BaseTasksOperationsViewModel
import start.up.tracker.ui.data.entities.ListItem
import start.up.tracker.ui.list.adapters.base.BaseAdapter
import start.up.tracker.ui.list.view_holders.base.BaseViewHolder

/**
 * Разширение для инициализации @RecyclerView и задания параметров
 */
class ListExtension(private var list: RecyclerView?) {

    /**
     * Необходимость блокировать список
     * true - список заблокирован
     */
    private var shouldLockList = false

    init {
        setLayoutManager()
        addOnItemTouchListener()
    }

    /**
     * Задать @Adapter
     */
    fun setAdapter(adapter: Adapter<BaseViewHolder>) {
        list?.adapter = adapter
    }

    /**
     * Заблокировать список
     */
    fun lock() {
        shouldLockList = true
    }

    /**
     * Разблокировать список
     */
    fun unlock() {
        shouldLockList = false
    }

    /**
     * Задать @LayoutManager
     * По умолчанию задается @LinearLayoutManager
     */
    fun setLayoutManager(layoutManager: LayoutManager = getDefaultLayoutManager()) {
        list?.layoutManager = layoutManager
    }

    /**
     * Добавляет возможность свайпать вправо или влево элемент списка
     */
    fun attachSwipeToAdapter(
        adapter: BaseAdapter<ListItem, BaseViewHolder>,
        viewModel: BaseTasksOperationsViewModel
    ) {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: ViewHolder,
                target: ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
                val listItem = adapter.getItems().elementAt(viewHolder.adapterPosition)
                viewModel.onTaskSwiped(listItem.data as Task)
            }
        }).attachToRecyclerView(list)
    }

    /**
     * Получить @LayoutManager по умолчанию
     */
    private fun getDefaultLayoutManager(): LayoutManager {
        return LinearLayoutManager(
            null,
            LinearLayoutManager.VERTICAL,
            false
        )
    }

    /**
     * Добавление слушателя для возможности блокировки списка
     */
    private fun addOnItemTouchListener() {
        list?.addOnItemTouchListener(object : SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return shouldLockList
            }
        })
    }
}
