package org.traccar.client

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class SyncWorker(private val appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

//    private val apiService = RetrofitClient.retrofit.create(SyncApiService::class.java)
    private lateinit var apiService: SyncApiService

    private val dbHelper = DatabaseHelper(appContext)

    override suspend fun doWork(): Result {
        val sharedPreferences = appContext.getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            return Result.failure()
        }
        apiService = RetrofitClient.getApiKeyClient(token).create(SyncApiService::class.java)

        val unsyncedSubmissions = dbHelper.selectAllFormSubmissions()
        for (submission in unsyncedSubmissions) {
            try {
                apiService.sendFormData(submission)
                dbHelper.deleteFormSubmission(submission.id.toLong())
            } catch (e: HttpException) {
                if (e.response()?.code() == 401) {
                    dbHelper.clearUserData()
                    withContext(Dispatchers.Main) {
                        appContext.startActivity(
                            Intent(appContext, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        )
                    }
                    return Result.failure()
                }
                return Result.retry()
            } catch (e: Exception) {
                return Result.retry()
            }
        }
        return Result.success()
    }
}