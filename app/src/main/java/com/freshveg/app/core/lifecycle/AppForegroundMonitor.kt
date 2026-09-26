package com.freshveg.app.core.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors application-level lifecycle transitions using [Application.ActivityLifecycleCallbacks].
 * Detects when the MandiExpress app returns to the foreground from RAM / background
 * without recreating or restarting Activities.
 *
 * Emits [appResumedEvents] / [foregroundResumeEvent] with the resume timestamp to allow active ViewModels
 * to evaluate data freshness and trigger stale-while-revalidate background refreshes.
 */
@Singleton
class AppForegroundMonitor @Inject constructor() : Application.ActivityLifecycleCallbacks {

    private val _isForeground = MutableStateFlow(false)
    val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()

    private val _appResumedEvents = MutableSharedFlow<Long>(replay = 0, extraBufferCapacity = 1)
    val appResumedEvents: SharedFlow<Long> = _appResumedEvents.asSharedFlow()
    val foregroundResumeEvent: SharedFlow<Long> = _appResumedEvents.asSharedFlow()

    private var startedActivityCount = 0
    private var lastBackgroundTimeMs: Long = 0L
    private var lastForegroundTimeMs: Long = 0L

    override fun onActivityStarted(activity: Activity) {
        startedActivityCount++
        if (startedActivityCount == 1) {
            val now = System.currentTimeMillis()
            lastForegroundTimeMs = now
            _isForeground.value = true

            if (lastBackgroundTimeMs > 0L) {
                _appResumedEvents.tryEmit(now)
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivityCount--
        if (startedActivityCount <= 0) {
            startedActivityCount = 0
            lastBackgroundTimeMs = System.currentTimeMillis()
            _isForeground.value = false
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    /**
     * Milliseconds the app spent in background during its last background period.
     */
    fun getDurationInBackgroundMs(): Long {
        if (lastBackgroundTimeMs == 0L || lastForegroundTimeMs == 0L) return 0L
        return (lastForegroundTimeMs - lastBackgroundTimeMs).coerceAtLeast(0L)
    }
}
