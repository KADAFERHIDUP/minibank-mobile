package io.github.kadaferhidup.minibank.mobile.ui.account

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kadaferhidup.minibank.mobile.BuildConfig
import io.github.kadaferhidup.minibank.mobile.data.Account
import io.github.kadaferhidup.minibank.mobile.data.Format
import io.github.kadaferhidup.minibank.mobile.ui.components.BankCard
import io.github.kadaferhidup.minibank.mobile.ui.components.ErrorBanner
import io.github.kadaferhidup.minibank.mobile.ui.components.Headline
import io.github.kadaferhidup.minibank.mobile.ui.components.Pill
import io.github.kadaferhidup.minibank.mobile.ui.components.PrimaryButton
import io.github.kadaferhidup.minibank.mobile.ui.components.SecondaryButton
import io.github.kadaferhidup.minibank.mobile.ui.components.SectionTitle
import io.github.kadaferhidup.minibank.mobile.ui.home.BankViewModel
import io.github.kadaferhidup.minibank.mobile.ui.theme.MB

const val MAX_ACCOUNTS = 3

@Composable
fun AccountScreen(vm: BankViewModel) {
    var confirmLogout by remember { mutableStateOf(false) }
    val p = vm.profile

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Headline("Rekening", "dan profil.") }
        if (p != null) item {
            BankCard(Modifier.fillMaxWidth(), color = MB.Paper) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(52.dp).background(MB.Ink, CircleShape), contentAlignment = Alignment.Center) {
                        Text(p.fullName.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Column {
                        Text(p.fullName, style = MaterialTheme.typography.titleMedium)
                        Text(p.email, color = MB.Ink2)
                        p.phone?.let { Text(it, color = MB.Ink2) }
                    }
                }
            }
        }
        vm.message?.let { item { BankCard(Modifier.fillMaxWidth()) { Text(it, modifier = Modifier.padding(16.dp)) } } }
        item { SectionTitle("Rekening saya (${vm.accounts.size}/$MAX_ACCOUNTS)") }
        items(vm.accounts, key = { it.accountNumber }) { a ->
            AccountRow(a, selected = a.accountNumber == vm.selected?.accountNumber, hidden = vm.balanceHidden) { vm.select(a.accountNumber) }
        }
        if (vm.accounts.size < MAX_ACCOUNTS) item {
            SecondaryButton(
                if (vm.opening) "Membuka rekening…" else "Buka rekening baru",
                vm::openAccount, Modifier.fillMaxWidth(), enabled = !vm.opening,
            )
        }
        vm.error?.let { item { ErrorBanner(it) } }
        item {
            BankCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Keamanan", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Sesi hanya disimpan di memori: menutup aplikasi atau tidak dipakai 5 menit di latar belakang = keluar otomatis. Tangkapan layar diblokir.",
                        color = MB.Ink2, style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        item {
            PrimaryButton("Keluar", { confirmLogout = true }, Modifier.fillMaxWidth())
        }
        item {
            Text(
                "MiniBank ${BuildConfig.VERSION_NAME} · server ${BuildConfig.API_BASE_URL.removePrefix("https://").trimEnd('/')}",
                color = MB.Ink2, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
            title = { Text("Keluar dari MiniBank?") },
            text = { Text("Kamu perlu masuk lagi dengan email dan password.") },
            confirmButton = { TextButton(onClick = { confirmLogout = false; vm.logout() }) { Text("Keluar", color = MB.RedInk) } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Batal", color = MB.Ink) } },
            containerColor = MB.Paper,
        )
    }
}

@Composable
private fun AccountRow(a: Account, selected: Boolean, hidden: Boolean, onClick: () -> Unit) {
    BankCard(
        Modifier.fillMaxWidth().clickable(role = Role.RadioButton, onClick = onClick),
        color = if (selected) MB.Ink else MB.Paper,
    ) {
        val fg = if (selected) Color.White else MB.Ink
        val sub = if (selected) Color.White.copy(alpha = .7f) else MB.Ink2
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Format.accountNumber(a.accountNumber), color = fg, style = MaterialTheme.typography.titleMedium)
                Text(if (hidden) "Rp ••••••" else Format.rupiah(a.balance), color = sub)
            }
            when (a.status) {
                "ACTIVE" -> if (selected) Pill("Dipakai", color = MB.Red, contentColor = Color.White)
                "FROZEN" -> Pill("Dibekukan", color = MB.Red, contentColor = Color.White)
                else -> Pill(a.status)
            }
        }
    }
}

/** Info untuk menerima transfer: nomor rekening dan nama, bisa disalin atau dibagikan. */
@Composable
fun ReceiveDialog(name: String?, account: Account, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val text = buildString {
        append("Transfer ke MiniBank\n")
        append("No. rekening: ${account.accountNumber}\n")
        if (name != null) append("Atas nama: $name")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Terima transfer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bagikan nomor rekening ini ke pengirim.", color = MB.Ink2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(Format.accountNumber(account.accountNumber), style = MaterialTheme.typography.headlineSmall)
                        if (name != null) Text(name, color = MB.Ink2)
                    }
                    IconButton(onClick = { clipboard.setText(AnnotatedString(account.accountNumber)) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Salin nomor rekening")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(send, "Bagikan nomor rekening"))
            }) {
                Icon(Icons.Outlined.Share, contentDescription = null, tint = MB.Ink)
                Text("  Bagikan", color = MB.Ink)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup", color = MB.Ink) } },
        containerColor = MB.Paper,
    )
}
