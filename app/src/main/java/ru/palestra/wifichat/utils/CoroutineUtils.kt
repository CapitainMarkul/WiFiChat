package ru.palestra.wifichat.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

object CoroutineUtils {

    fun tickerFlow(period: Duration) =
        flow {
            while (true) {
                emit(Unit)
                delay(period)
            }
        }
}