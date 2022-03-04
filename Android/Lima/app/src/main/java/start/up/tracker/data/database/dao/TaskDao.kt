package start.up.tracker.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import start.up.tracker.data.entities.Task
import start.up.tracker.data.relations.CategoryWithTasks
import start.up.tracker.data.relations.TaskWithCategories

/**
 * A suspending function is simply a function that can be paused and resumed at a later time.
 * They can execute a long running operation and wait for it to complete without blocking.
 *
 * A flow is a stream of multiple, asynchronously computed values. Flows emit values as soon as
 * they are done computing them. A flow consists of a producer and a consumer. As the names suggest,
 * a producer emits values while the consumer receives the values. A flow can only be used or collected
 * inside a coroutine. That's why we don't need suspend modifier.
 */
@Dao
interface TaskDao {

    @Query(
        """
       SELECT * 
       FROM task_table
       JOIN cross_ref ON task_table.taskId = cross_ref.taskId
       WHERE categoryId = :categoryId AND
       (completed != :hideCompleted OR completed = 0) AND 
       task_table.taskName LIKE '%' || :searchQuery || '%' 
       ORDER BY priority 
       ASC, created"""
    )
    fun getTasksOfCategory(searchQuery: String, hideCompleted: Boolean, categoryId: Int): Flow<List<Task>>

    @Query(
        """
        SELECT COUNT(*) 
        FROM task_table 
        JOIN cross_ref ON task_table.taskId = cross_ref.taskId
        WHERE categoryId = :categoryId AND
        (completed != :hideCompleted OR completed = 0)
    """
    )
    suspend fun countTasksOfCategory(categoryId: Int, hideCompleted: Boolean): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM task_table 
        JOIN cross_ref ON task_table.taskId = cross_ref.taskId
        WHERE categoryId = 1 AND
        completed = 0
    """
    )
    fun countTasksOfInbox(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM category WHERE categoryId = :categoryId")
    suspend fun getTasksOfCategory(categoryId: Int): List<CategoryWithTasks>

    @Transaction
    @Query("SELECT * FROM task_table WHERE taskId = :taskId")
    fun getCategoriesOfTask(taskId: Int): Flow<TaskWithCategories?>

    @Query("SELECT MAX(taskId) FROM task_table")
    suspend fun getTaskMaxId(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM task_table WHERE completed = 1")
    suspend fun deleteCompletedTasks()
}
