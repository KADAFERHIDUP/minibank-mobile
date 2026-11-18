package io.github.kadaferhidup.minibank.mobile.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kadaferhidup.minibank.mobile.data.Account
import io.github.kadaferhidup.minibank.mobile.data.BankRepository
import io.github.kadaferhidup.minibank.mobile.data.Format
import io.github.kadaferhidup.minibank.mobile.data.Profile
import io.github.kadaferhidup.minibank.mobile.data.StatementLine
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Data bersama untuk semua tab setelah masuk: profil, daftar rekening, rekening terpilih, transaksi terakhir. */
class BankViewModel(private val repo: BankRepository) : ViewModel() {

    var profile by mutableStateOf<Profile?>(null)
        private set
    var accounts by mutableStateOf<List<Account>>(emptyList())
        private set
    var selectedNumber by mutableStateOf<String?>(null)
        private set
    var recent by mutableStateOf<List<StatementLine>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var balanceHidden by mutableStateOf(false)
    var opening by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    val selected: Account? get() = accounts.firstOrNull { it.accountNumber == selectedNumber } ?: accounts.firstOrNull()

    init {
        refresh()
    }

    fun refresh() {
        if (loading) return
        loading = true
        error = null
        viewModelScope.launch {
            val p = async { repo.profile() }
            val a = repo.accounts()
            p.await().onSuccess { profile = it }
            a.onSuccess { list ->
                accounts = list
                if (list.none { it.accountNumber == selectedNumber }) selectedNumber = list.firstOrNull()?.accountNumber
                loadRecent()
            }.onFailure { error = it.message }
            loading = false
        }
    }

    fun select(number: String) {
        if (number == selectedNumber) return
        selectedNumber = number
        recent = emptyList()
        viewModelScope.launch { loadRecent() }
    }

    private suspend fun loadRecent() {
        val number = selected?.accountNumber ?: return
        val today = LocalDate.now(Format.jakarta)
        repo.statement(number, today.minusDays(30), today)
            .onSuccess { recent = it.items.take(5) }
    }

    fun openAccount() {
        if (opening) return
        opening = true
        viewModelScope.launch {
            repo.openAccount()
                .onSuccess {
                    message = "Rekening baru ${Format.accountNumber(it.accountNumber)} berhasil dibuka."
                    selectedNumber = it.accountNumber
                    opening = false
                    refresh()
                }
                .onFailure {
                    message = it.message
                    opening = false
                }
        }
    }

    fun logout() = repo.logout()
}
