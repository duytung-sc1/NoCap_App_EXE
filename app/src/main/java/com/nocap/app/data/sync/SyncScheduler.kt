package com.nocap.app.data.sync

import android.content.Context
import androidx.room.InvalidationTracker
import androidx.work.*
import com.nocap.app.core.database.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

object SyncScheduler {
    val status = MutableStateFlow("Chưa đồng bộ")
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val watched=mutableSetOf<String>()
    private val debounce=mutableMapOf<String,Job>()
    val lock=Mutex()
    @Volatile private var statusProfile = Profiles.LOCAL
    fun report(profile: String, message: String) { if(Profiles.active.value==profile)status.value=message }
    private fun constraints()=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    fun now(context: Context,profile: String=Profiles.active.value) {
        if(!profile.startsWith("ACCOUNT:"))return
        val request=OneTimeWorkRequestBuilder<SyncWorker>().setInputData(workDataOf("profile" to profile))
            .setConstraints(constraints()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork("sync-${Profiles.key(profile)}",ExistingWorkPolicy.APPEND_OR_REPLACE,request)
    }
    fun start(context: Context,profile: String) {
        scope.launch { startInBackground(context.applicationContext, profile) }
    }
    private fun startInBackground(context: Context,profile: String) {
        if(statusProfile!=profile) { statusProfile=profile;status.value="Chưa đồng bộ" }
        if(!profile.startsWith("ACCOUNT:")){status.value="Dữ liệu trên thiết bị; đăng nhập để đồng bộ";return}
        val app=context.applicationContext
        synchronized(watched) {
            if(watched.add(profile)) {
                com.nocap.app.domain.session.ReadingSessionManager.getInstance(app).recoverStaleSessionsAsync()
                AppDatabase.getInstance(app,profile).invalidationTracker.addObserver(object: InvalidationTracker.Observer(SyncSchema.keys.keys.toTypedArray()) {
                    override fun onInvalidated(tables: Set<String>) {
                        synchronized(debounce) {
                            debounce.remove(profile)?.cancel()
                            debounce[profile]=scope.launch { delay(2000);if(Profiles.active.value==profile) now(app,profile) }
                        }
                    }
                })
            }
        }
        val periodic=PeriodicWorkRequestBuilder<SyncWorker>(15,TimeUnit.MINUTES).setInputData(workDataOf("profile" to profile))
            .setConstraints(constraints()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build()
        WorkManager.getInstance(app).enqueueUniquePeriodicWork("periodic-sync-${Profiles.key(profile)}",ExistingPeriodicWorkPolicy.KEEP,periodic)
        now(app,profile)
    }
}

class SyncWorker(context: Context,params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val profile=inputData.getString("profile") ?: return Result.failure()
        com.nocap.app.data.auth.CloudAuthRepository.getInstance(applicationContext).authState.first {
            it != com.nocap.app.domain.model.AuthState.Loading
        }
        return SyncScheduler.lock.withLock {
            if(profile!=Profiles.active.value)return@withLock Result.success()
            try {
                SyncScheduler.report(profile,"Đang đồng bộ…")
                SyncEngine(applicationContext,profile).run()
                val db=AppDatabase.getInstance(applicationContext,profile).openHelper.readableDatabase
                val unresolved=db.query("SELECT (SELECT COUNT(*) FROM sync_conflicts)+(SELECT COUNT(*) FROM sync_inbox)").use { it.moveToFirst();it.getLong(0) }
                val pending=db.query("SELECT COUNT(*) FROM sync_outbox").use { it.moveToFirst();it.getLong(0) }
                SyncScheduler.report(profile,when {
                    unresolved>0 -> "Một số thay đổi chưa hợp nhất được. Các phiên bản vẫn được giữ; hãy thử đồng bộ lại sau."
                    pending>0 -> "Còn thay đổi đang chờ. Hãy kết thúc phiên đọc và đồng bộ lại."
                    else -> "Đã đồng bộ"
                })
                Result.success()
            } catch(e: CancellationException){throw e}
            catch(e: com.nocap.app.data.billing.ProRequired) {
                SyncScheduler.report(profile,e.message!!)
                Result.success() // Keep outbox intact; login/foreground/periodic sync rechecks entitlement.
            }
            catch(e: Exception) {
                SyncScheduler.report(profile,"Chưa đồng bộ được. Kiểm tra kết nối rồi thử lại. Dữ liệu trên máy vẫn được giữ.")
                Result.retry()
            }
        }
    }
}
