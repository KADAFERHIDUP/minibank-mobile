package io.github.kadaferhidup.minibank.mobile.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kadaferhidup.minibank.mobile.data.ApiError
import io.github.kadaferhidup.minibank.mobile.data.BankRepository
import io.github.kadaferhidup.minibank.mobile.data.RegisterRequest
import io.github.kadaferhidup.minibank.mobile.data.Validation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AuthViewModel(private val repo: BankRepository) : ViewModel() {

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var fieldErrors by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    /** Muncul kalau server lama merespons (sedang bangun dari tidur). */
    var slowHint by mutableStateOf(false)
        private set

    fun clearField(name: String) {
        if (name in fieldErrors) fieldErrors = fieldErrors - name
        error = null
    }

    fun login(email: String, password: String) {
        val errs = buildMap {
            if (email.isBlank()) put("email", "Isi email")
            if (password.isBlank()) put("password", "Isi password")
        }
        if (errs.isNotEmpty()) {
            fieldErrors = errs
            return
        }
        submit { repo.login(email, password).exceptionOrNull() }
    }

    fun register(fullName: String, email: String, phone: String, password: String, pin: String) {
        val errs = Validation.register(fullName, email, phone, password, pin)
        if (errs.isNotEmpty()) {
            fieldErrors = errs
            return
        }
        submit { repo.register(RegisterRequest(fullName, email, phone, password, pin)).exceptionOrNull() }
    }

    private fun submit(block: suspend () -> Throwable?) {
        if (loading) return
        loading = true
        error = null
        fieldErrors = emptyMap()
        val hint: Job = viewModelScope.launch {
            delay(6_000)
            slowHint = true
        }
        viewModelScope.launch {
            val failure = block() as? ApiError
            hint.cancel()
            slowHint = false
            loading = false
            if (failure != null) {
                fieldErrors = failure.fields
                error = if (failure.fields.isEmpty()) failure.message else null
            }
        }
    }
}
