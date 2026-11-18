package io.github.kadaferhidup.minibank.mobile.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.kadaferhidup.minibank.mobile.ui.components.BankCard
import io.github.kadaferhidup.minibank.mobile.ui.components.ErrorBanner
import io.github.kadaferhidup.minibank.mobile.ui.components.Field
import io.github.kadaferhidup.minibank.mobile.ui.components.Headline
import io.github.kadaferhidup.minibank.mobile.ui.components.PrimaryButton
import io.github.kadaferhidup.minibank.mobile.ui.theme.MB

@Composable
private fun AuthFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("minibank", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Box(Modifier.padding(start = 3.dp).size(7.dp).background(MB.Red, CircleShape))
        }
        content()
    }
}

@Composable
fun LoginScreen(vm: AuthViewModel, notice: String?, onRegister: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val submit = { vm.login(email, password) }

    AuthFrame {
        Spacer(Modifier.height(24.dp))
        Headline("Selamat datang", "kembali.")
        Text("Masuk untuk cek saldo, transfer, dan lihat mutasi.", color = MB.Ink2)
        if (notice != null) ErrorBanner(notice)
        BankCard(color = MB.Card) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(email, { email = it; vm.clearField("email") }, "Email", error = vm.fieldErrors["email"], keyboardType = KeyboardType.Email)
                Field(
                    password, { password = it; vm.clearField("password") }, "Password",
                    error = vm.fieldErrors["password"], keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Go, onDone = submit, secret = true,
                )
                vm.error?.let { ErrorBanner(it) }
                if (vm.slowHint) Text("Membangunkan server demo, biasanya butuh sekitar satu menit…", color = MB.Ink2, style = MaterialTheme.typography.bodyMedium)
                PrimaryButton("Masuk", submit, Modifier.fillMaxWidth(), loading = vm.loading)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Belum punya rekening?", color = MB.Ink2)
            TextButton(onClick = onRegister) { Text("Buka rekening", color = MB.Ink, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun RegisterScreen(vm: AuthViewModel, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    val submit = { vm.register(name, email, phone, password, pin) }

    AuthFrame {
        Headline("Buka rekening", "dalam semenit.")
        Text("Rekening tabungan langsung dibuat setelah daftar.", color = MB.Ink2)
        BankCard {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(name, { name = it; vm.clearField("fullName") }, "Nama lengkap", error = vm.fieldErrors["fullName"])
                Field(email, { email = it; vm.clearField("email") }, "Email", error = vm.fieldErrors["email"], keyboardType = KeyboardType.Email)
                Field(phone, { phone = it.filter { c -> c.isDigit() || c == '+' }; vm.clearField("phone") }, "Nomor HP",
                    error = vm.fieldErrors["phone"], supporting = "Contoh 081234567890", keyboardType = KeyboardType.Phone)
                Field(password, { password = it; vm.clearField("password") }, "Password", error = vm.fieldErrors["password"],
                    supporting = "Minimal 8 karakter, huruf dan angka", keyboardType = KeyboardType.Password, secret = true)
                Field(pin, { pin = it.filter(Char::isDigit).take(6); vm.clearField("pin") }, "PIN transaksi",
                    error = vm.fieldErrors["pin"], supporting = "6 angka, dipakai setiap transfer",
                    keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Go, onDone = submit, secret = true)
                vm.error?.let { ErrorBanner(it) }
                if (vm.slowHint) Text("Membangunkan server demo, biasanya butuh sekitar satu menit…", color = MB.Ink2, style = MaterialTheme.typography.bodyMedium)
                PrimaryButton("Buka rekening", submit, Modifier.fillMaxWidth(), loading = vm.loading)
            }
        }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Sudah punya rekening? Masuk", color = MB.Ink, fontWeight = FontWeight.Bold)
        }
    }
}
