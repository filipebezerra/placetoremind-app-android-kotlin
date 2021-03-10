package app.filipebezerra.placetoremind.data

import app.filipebezerra.placetoremind.data.dto.ReminderDTO
import app.filipebezerra.placetoremind.data.dto.Result


/**
 * Main entry point for accessing reminders data.
 */
interface ReminderDataSource {
    suspend fun getReminders(): Result<List<ReminderDTO>>
    suspend fun saveReminder(reminder: ReminderDTO)
    suspend fun getReminder(id: String): Result<ReminderDTO>
    suspend fun deleteAllReminders()
}