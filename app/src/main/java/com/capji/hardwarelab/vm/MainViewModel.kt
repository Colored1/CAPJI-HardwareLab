package com.capji.hardwarelab.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.capji.hardwarelab.core.AppPrefs
import com.capji.hardwarelab.data.BenchResult
import com.capji.hardwarelab.data.StorageBench
import com.capji.hardwarelab.data.TestId
import com.capji.hardwarelab.data.TestStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Main : Screen
    data class Test(val id: TestId) : Screen
    data object Settings : Screen
}

data class BenchState(
    val running: Boolean = false,
    val progress: Float = 0f,
    val result: BenchResult? = null,
    val error: Boolean = false,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val ar = MutableStateFlow(AppPrefs.isArabic(app))
    val screen = MutableStateFlow<Screen>(Screen.Main)
    val tab = MutableStateFlow(0)
    val results = MutableStateFlow(loadResults())
    /** -1 = not in guided mode */
    val guidedIndex = MutableStateFlow(-1)
    val bench = MutableStateFlow(BenchState())

    private var benchJob: Job? = null

    private fun loadResults(): Map<TestId, TestStatus> {
        val app = getApplication<Application>()
        val m = LinkedHashMap<TestId, TestStatus>()
        for (id in TestId.entries) {
            val s = AppPrefs.getStatus(app, id.name)
            val st = try {
                if (s != null) TestStatus.valueOf(s) else TestStatus.PENDING
            } catch (_: IllegalArgumentException) {
                TestStatus.PENDING
            }
            m[id] = st
        }
        return m
    }

    fun setLanguage(arabic: Boolean) {
        ar.value = arabic
        AppPrefs.setArabic(getApplication(), arabic)
    }

    fun selectTab(i: Int) {
        tab.value = i
    }

    fun openTest(id: TestId) {
        guidedIndex.value = -1
        screen.value = Screen.Test(id)
    }

    fun openSettings() {
        screen.value = Screen.Settings
    }

    fun startGuided() {
        guidedIndex.value = 0
        screen.value = Screen.Test(TestId.entries.first())
    }

    /** Store the result without leaving the test screen (used for auto-detected results). */
    fun mark(id: TestId, status: TestStatus) {
        if (results.value[id] == status) return
        val m = LinkedHashMap(results.value)
        m[id] = status
        results.value = m
        AppPrefs.setStatus(getApplication(), id.name, status.name)
    }

    /** Store (optional) result, then go to the next guided step or back to the main screen. */
    fun finish(id: TestId, status: TestStatus?) {
        if (status != null) mark(id, status)
        val gi = guidedIndex.value
        if (gi >= 0) {
            val next = gi + 1
            if (next < TestId.entries.size) {
                guidedIndex.value = next
                screen.value = Screen.Test(TestId.entries[next])
            } else {
                guidedIndex.value = -1
                tab.value = 3
                screen.value = Screen.Main
            }
        } else {
            screen.value = Screen.Main
        }
    }

    fun back() {
        guidedIndex.value = -1
        screen.value = Screen.Main
    }

    fun resetResults() {
        AppPrefs.clearStatuses(getApplication())
        results.value = loadResults()
    }

    fun runBenchmark() {
        if (bench.value.running) return
        benchJob?.cancel()
        benchJob = viewModelScope.launch {
            bench.value = bench.value.copy(running = true, progress = 0f, error = false)
            try {
                val r = StorageBench.run(getApplication(), 96) { p ->
                    bench.value = bench.value.copy(progress = p)
                }
                bench.value = BenchState(running = false, progress = 1f, result = r)
            } catch (_: Throwable) {
                bench.value = bench.value.copy(running = false, error = true)
            }
        }
    }
}
