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
    val status = MutableStateFlow("Tự động đồng bộ • Sẵn sàng")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val watched = mutableSetOf<String>()
    private val debounce = mutableMapOf<String, Job>()
    private val startLock = Mutex()
    private val immediateGuard = Any()
    private val immediateJobs = mutableMapOf<String, Job>()
    private val immediateRequested = mutableSetOf<String>()
    @Volatile private var foregroundJob: Job? = null
    @Volatile private var isForeground = false
    val lock = Mutex()
    @Volatile private var statusProfile = Profiles.LOCAL

    fun report(profile: String, message: String) {
        if (Profiles.active.value == profile) status.value = message
    }

    private fun constraints() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun now(context: Context, profile: String = Profiles.active.value, force: Boolean = false) {
        if (!profile.startsWith("ACCOUNT:") || profile != Profiles.active.value) return
        val appContext = context.applicationContext
        val request = OneTimeWorkRequestBuilder<SyncWorker>().setInputData(workDataOf("profile" to profile))
            .setConstraints(constraints()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .apply { if (force) setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) }.build()
        // One pending job reads the latest outbox when connectivity returns. Appending a
        // job on every foreground heartbeat would create an unbounded offline queue.
        val policy = if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
        WorkManager.getInstance(appContext).enqueueUniqueWork("sync-${Profiles.key(profile)}", policy, request)
    }

    /** Runs an event-driven foreground sync without WorkManager dispatch latency.
     * Requests received during an active run are conflated into exactly one follow-up run. */
    fun immediate(context: Context, profile: String = Profiles.active.value) {
        if (!profile.startsWith("ACCOUNT:") || profile != Profiles.active.value) return
        val appContext = context.applicationContext
        synchronized(immediateGuard) {
            immediateRequested.add(profile)
            if (immediateJobs[profile]?.isActive == true) return
            val job = scope.launch(start = CoroutineStart.LAZY) {
                while (Profiles.active.value == profile) {
                    synchronized(immediateGuard) { immediateRequested.remove(profile) }
                    if (execute(appContext, profile) == SyncExecution.RETRY) {
                        now(appContext, profile)
                        synchronized(immediateGuard) {
                            immediateRequested.remove(profile)
                            immediateJobs.remove(profile)
                        }
                        return@launch
                    }
                    val repeat = synchronized(immediateGuard) {
                        if (immediateRequested.contains(profile)) true
                        else {
                            immediateJobs.remove(profile)
                            false
                        }
                    }
                    if (!repeat) return@launch
                }
                synchronized(immediateGuard) {
                    immediateRequested.remove(profile)
                    immediateJobs.remove(profile)
                }
            }
            immediateJobs[profile] = job
            job.start()
        }
    }

    fun onForeground(context: Context, profile: String = Profiles.active.value) {
        isForeground = true
        synchronized(this) {
            foregroundJob?.cancel()
            foregroundJob = null
            if (profile.startsWith("ACCOUNT:") && profile == Profiles.active.value) {
                val appContext = context.applicationContext
                RealtimeSyncClient.start(appContext, profile)
                now(appContext, profile)
                foregroundJob = scope.launch {
                    while (isActive && isForeground) {
                        delay(45_000)
                        val active = Profiles.active.value
                        if (active.startsWith("ACCOUNT:") && isForeground) {
                            now(appContext, active)
                        }
                    }
                }
            } else {
                // Logout/profile switches must not leave the previous account's socket alive.
                RealtimeSyncClient.stop()
            }
        }
    }

    fun onBackground() {
        isForeground = false
        RealtimeSyncClient.stop()
        synchronized(this) {
            foregroundJob?.cancel()
            foregroundJob = null
        }
    }

    fun start(context: Context, profile: String) {
        scope.launch {
            startLock.withLock {
                if (profile == Profiles.active.value) startInBackground(context.applicationContext, profile)
            }
        }
    }

    private fun startInBackground(context: Context, profile: String) {
        val scheduled = context.getSharedPreferences("sync_scheduler", Context.MODE_PRIVATE)
        val currentKey = profile.takeIf { it.startsWith("ACCOUNT:") }?.let(Profiles::key)
        val previousKey = scheduled.getString("scheduled_profile_key", null)
        if (previousKey != currentKey) {
            if (previousKey != null) {
                val work = WorkManager.getInstance(context)
                work.cancelUniqueWork("periodic-sync-$previousKey")
                work.cancelUniqueWork("sync-$previousKey")
            }
            scheduled.edit().putString("scheduled_profile_key", currentKey).apply()
        }
        if (statusProfile != profile) {
            statusProfile = profile
            status.value = if (profile.startsWith("ACCOUNT:")) "Tự động đồng bộ • Sẵn sàng" else "Dữ liệu trên thiết bị; đăng nhập để đồng bộ"
        }
        if (!profile.startsWith("ACCOUNT:")) {
            status.value = "Dữ liệu trên thiết bị; đăng nhập để đồng bộ"
            if (isForeground) onForeground(context, profile)
            return
        }
        val app = context.applicationContext
        synchronized(watched) {
            if (watched.add(profile)) {
                com.nocap.app.domain.session.ReadingSessionManager.getInstance(app).recoverStaleSessionsAsync()
                AppDatabase.getInstance(app, profile).invalidationTracker.addObserver(object : InvalidationTracker.Observer(SyncSchema.keys.keys.toTypedArray()) {
                    override fun onInvalidated(tables: Set<String>) {
                        synchronized(debounce) {
                            debounce.remove(profile)?.cancel()
                            debounce[profile] = scope.launch {
                                delay(250)
                                if (Profiles.active.value == profile) {
                                    val pending = AppDatabase.getInstance(app, profile).openHelper.readableDatabase
                                        .query("SELECT 1 FROM sync_outbox LIMIT 1").use { it.moveToFirst() }
                                    if (pending) {
                                        if (isForeground) immediate(app, profile) else now(app, profile)
                                    }
                                }
                            }
                        }
                    }
                })
            }
        }
        val periodic = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).setInputData(workDataOf("profile" to profile))
            .setConstraints(constraints()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        WorkManager.getInstance(app).enqueueUniquePeriodicWork("periodic-sync-${Profiles.key(profile)}", ExistingPeriodicWorkPolicy.KEEP, periodic)
        if (isForeground) {
            onForeground(app, profile)
        } else {
            now(app, profile)
        }
    }

    internal suspend fun execute(context: Context, profile: String): SyncExecution {
        com.nocap.app.data.auth.CloudAuthRepository.getInstance(context).authState.first {
            it != com.nocap.app.domain.model.AuthState.Loading
        }
        return lock.withLock {
            if (profile != Profiles.active.value) return@withLock SyncExecution.SUCCESS
            try {
                report(profile, "Đang đồng bộ…")
                SyncEngine(context, profile).run()
                val db = AppDatabase.getInstance(context, profile).openHelper.readableDatabase
                val unresolved = db.query("SELECT (SELECT COUNT(*) FROM sync_conflicts)+(SELECT COUNT(*) FROM sync_inbox)").use { it.moveToFirst(); it.getLong(0) }
                val pending = db.query("SELECT COUNT(*) FROM sync_outbox WHERE NOT (kind = 'reading_sessions' AND local_key IN (SELECT hex(id) FROM reading_sessions WHERE ended_at IS NULL))").use { it.moveToFirst(); it.getLong(0) }
                val activeSessionPending = db.query("SELECT COUNT(*) FROM sync_outbox WHERE kind = 'reading_sessions' AND local_key IN (SELECT hex(id) FROM reading_sessions WHERE ended_at IS NULL)").use { it.moveToFirst(); it.getLong(0) }
                report(profile, when {
                    unresolved > 0 -> "Một số thay đổi chưa hợp nhất được. Các phiên bản vẫn được giữ; hãy thử đồng bộ lại sau."
                    pending > 0 -> "Còn $pending thay đổi đang chờ tự động gửi."
                    activeSessionPending > 0 -> "Tự động đồng bộ • Đã lưu tiến độ (đang trong phiên đọc)"
                    else -> "Tự động đồng bộ • Đã cập nhật mới nhất"
                })
                SyncExecution.SUCCESS
            } catch (e: CancellationException) {
                throw e
            } catch (e: com.nocap.app.data.billing.ProRequired) {
                report(profile, e.message!!)
                SyncExecution.SUCCESS
            } catch (e: Exception) {
                report(profile, "Chưa đồng bộ được. Kiểm tra kết nối rồi thử lại. Dữ liệu trên máy vẫn được giữ.")
                SyncExecution.RETRY
            }
        }
    }
}

internal enum class SyncExecution { SUCCESS, RETRY }

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val profile = inputData.getString("profile") ?: return Result.failure()
        return when (SyncScheduler.execute(applicationContext, profile)) {
            SyncExecution.SUCCESS -> Result.success()
            SyncExecution.RETRY -> Result.retry()
        }
    }
}
