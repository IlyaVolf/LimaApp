package start.up.tracker.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import start.up.tracker.data.db.Task
import start.up.tracker.data.repository.TaskRepositoryImpl
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(private val repository: TaskRepositoryImpl) : ViewModel() {

    suspend fun addTask(task: Task) = repository.addTask(task)

    suspend fun updateTask(task: Task) = repository.updateTask(task)

    suspend fun deleteTask(task: Task) = repository.deleteTask(task)

    suspend fun deleteAllTasks() = repository.deleteAllTasks()

    fun getAllTasks() = repository.getAllTasks()
}