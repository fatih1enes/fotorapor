package com.elektrik.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow

// Helper extension to group Flow by key for per-item debouncing
fun <T, K> Flow<T>.groupBy(keySelector: (T) -> K): Flow<Flow<T>> = flow {
    val groups = mutableMapOf<K, MutableSharedFlow<T>>()
    collect { item ->
        val key = keySelector(item)
        var groupFlow = groups[key]
        if (groupFlow == null) {
            groupFlow = MutableSharedFlow(extraBufferCapacity = 10)
            groups[key] = groupFlow
            emit(groupFlow)
        }
        groupFlow.emit(item)
    }
}
