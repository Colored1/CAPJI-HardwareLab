package com.capji.hardwarelab.data

data class ScoreResult(val score: Int?, val grade: String, val tested: Int, val passed: Int, val failed: Int, val na: Int)

object HealthScore {
    fun compute(results: Map<TestId, TestStatus>, battery: BatteryReading?, bench: BenchResult?): ScoreResult {
        val passed = results.values.count { it == TestStatus.PASSED }
        val failed = results.values.count { it == TestStatus.FAILED }
        val na = results.values.count { it == TestStatus.NA }
        val tested = passed + failed

        var weight = 0f
        var sum = 0f
        if (tested > 0) {
            weight += 70f
            sum += 70f * passed / tested
        }
        if (battery != null) {
            weight += 20f
            val tempPenalty = when {
                battery.tempC >= 50f -> 0.6f
                battery.tempC >= 43f -> 0.85f
                else -> 1f
            }
            sum += 20f * (BatteryLab.healthScore(battery.health) / 100f) * tempPenalty
        }
        if (bench != null) {
            weight += 10f
            val speedScore = when {
                bench.writeMBs >= 150f -> 1f
                bench.writeMBs >= 60f -> 0.85f
                bench.writeMBs >= 25f -> 0.65f
                else -> 0.4f
            }
            sum += 10f * speedScore
        }
        if (tested == 0 && bench == null) {
            // nothing meaningful measured yet
            return ScoreResult(null, "-", tested, passed, failed, na)
        }
        val score = if (weight > 0f) (sum / weight * 100f).toInt().coerceIn(0, 100) else 0
        return ScoreResult(score, gradeOf(score), tested, passed, failed, na)
    }

    fun gradeOf(score: Int): String = when {
        score >= 95 -> "A+"
        score >= 85 -> "A"
        score >= 70 -> "B"
        else -> "C"
    }
}
