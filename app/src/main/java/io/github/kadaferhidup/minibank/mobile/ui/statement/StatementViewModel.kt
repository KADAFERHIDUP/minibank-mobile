package io.github.kadaferhidup.minibank.mobile.ui.statement

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kadaferhidup.minibank.mobile.data.BankRepository
import io.github.kadaferhidup.minibank.mobile.data.Format
import io.github.kadaferhidup.minibank.mobile.data.StatementLine
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

/** Rentang mutasi. Server membatasi maksimal 90 hari. */
enum class Range(val label: String, val days: Long) { Week("7 hari", 7), Month("30 hari", 30), Quarter("90 hari", 90) }

class StatementViewModel(private val repo: BankRepository) : ViewModel() {

    var range by mutableStateOf(Range.Month)
        private set
    var items by mutableStateOf<List<StatementLine>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var loadedFor: Pair<String, Range>? = null
    private var job: Job? = null

    val totalIn: BigDecimal get() = items.filter { it.isCredit }.fold(BigDecimal.ZERO) { a, l -> a + l.amount }
    val totalOut: BigDecimal get() = items.filterNot { it.isCredit }.fold(BigDecimal.ZERO) { a, l -> a + l.amount }

    fun load(accountNumber: String, newRange: Range = range, force: Boolean = false) {
        if (!force && loadedFor == accountNumber to newRange) return
        range = newRange
        loadedFor = accountNumber to newRange
        job?.cancel()
        loading = true
        error = null
        job = viewModelScope.launch {
            val to = LocalDate.now(Format.jakarta)
            val from = to.minusDays(newRange.days - 1)
            repo.statement(accountNumber, from, to)
                .onSuccess { items = it.items }
                .onFailure {
                    error = it.message
                    loadedFor = null
                }
            loading = false
        }
    }
}
