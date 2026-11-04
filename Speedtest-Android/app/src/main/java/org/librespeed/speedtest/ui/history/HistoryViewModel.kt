package org.librespeed.speedtest.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.librespeed.speedtest.data.GeoDistance
import org.librespeed.speedtest.data.HistoryDatabase
import org.librespeed.speedtest.data.HistoryEntry

data class HistoryFilter(
    val network: String? = null,
    val server: String? = null,
    val days: Int? = null
) {
    val active: Boolean get() = network != null || server != null || days != null
}

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = HistoryDatabase(application)
    private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())

    private val _filter = MutableStateFlow(HistoryFilter())
    val filter: StateFlow<HistoryFilter> = _filter

    val entries: StateFlow<List<HistoryEntry>> =
        combine(_entries, _filter) { entries, filter ->
            entries.filter { entry ->
                (filter.network == null || entry.networkType?.startsWith(filter.network) == true) &&
                    (filter.server == null || GeoDistance.cleanName(entry.server) == filter.server) &&
                    (filter.days == null || entry.date >= System.currentTimeMillis() - filter.days * 86_400_000L)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Distinct values available for the filter dialog. */
    val allNetworks: StateFlow<List<String>> =
        combine(_entries, _filter) { entries, _ ->
            entries.mapNotNull { it.networkType?.substringBefore(" ") }.distinct().sorted()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allServers: StateFlow<List<String>> =
        combine(_entries, _filter) { entries, _ ->
            entries.map { GeoDistance.cleanName(it.server) }.distinct().sorted()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setFilter(filter: HistoryFilter) {
        _filter.value = filter
    }

    fun load() {
        viewModelScope.launch {
            _entries.value = withContext(Dispatchers.IO) { database.readAll() }
        }
    }

    fun delete(entry: HistoryEntry) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { database.delete(entry.id) }
            load()
        }
    }

    fun clear() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { database.clear() }
            load()
        }
    }

}
