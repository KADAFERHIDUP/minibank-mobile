package io.github.kadaferhidup.minibank.mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kadaferhidup.minibank.mobile.data.Account
import io.github.kadaferhidup.minibank.mobile.data.Format
import io.github.kadaferhidup.minibank.mobile.data.StatementLine
import io.github.kadaferhidup.minibank.mobile.ui.components.BankCard
import io.github.kadaferhidup.minibank.mobile.ui.components.ErrorBanner
import io.github.kadaferhidup.minibank.mobile.ui.components.Loading
import io.github.kadaferhidup.minibank.mobile.ui.components.MoneyText
import io.github.kadaferhidup.minibank.mobile.ui.components.Pill
import io.github.kadaferhidup.minibank.mobile.ui.components.SectionTitle
import io.github.kadaferhidup.minibank.mobile.ui.theme.MB

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: BankViewModel, onTransfer: () -> Unit, onStatement: () -> Unit, onReceive: () -> Unit) {
    PullToRefreshBox(isRefreshing = vm.loading && vm.accounts.isNotEmpty(), onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                val first = vm.profile?.fullName?.substringBefore(' ').orEmpty()
                Text(if (first.isEmpty()) "Halo" else "Halo, $first", style = MaterialTheme.typography.headlineSmall)
                Text("Ini ringkasan rekeningmu hari ini.", color = MB.Ink2)
            }
            vm.error?.let { item { ErrorBanner(it) } }
            if (vm.accounts.isEmpty() && vm.loading) item { Loading() }

            if (vm.accounts.size > 1) item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vm.accounts, key = { it.accountNumber }) { a ->
                        val on = a.accountNumber == vm.selected?.accountNumber
                        Text(
                            "•••• ${a.accountNumber.takeLast(4)}",
                            modifier = Modifier
                                .background(if (on) MB.Ink else MB.Card, CircleShape)
                                .clickable(role = Role.Tab) { vm.select(a.accountNumber) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            color = if (on) Color.White else MB.Ink,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            vm.selected?.let { acc -> item { AccountCard(acc, vm.balanceHidden) { vm.balanceHidden = !vm.balanceHidden } } }

            if (vm.accounts.isNotEmpty()) item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("Transfer", Icons.Outlined.SwapHoriz, onTransfer, Modifier.weight(1f))
                    QuickAction("Mutasi", Icons.AutoMirrored.Outlined.ReceiptLong, onStatement, Modifier.weight(1f))
                    QuickAction("Terima", Icons.Outlined.QrCode2, onReceive, Modifier.weight(1f))
                }
            }

            if (vm.accounts.isNotEmpty()) item {
                SectionTitle("Transaksi terakhir") { TextButton(onClick = onStatement) { Text("Lihat semua", color = MB.Ink) } }
            }
            if (vm.accounts.isNotEmpty() && vm.recent.isEmpty() && !vm.loading) item {
                BankCard(Modifier.fillMaxWidth()) {
                    Text("Belum ada transaksi 30 hari terakhir.", color = MB.Ink2, modifier = Modifier.padding(18.dp))
                }
            }
            items(vm.recent, key = { it.reference + it.direction }) { TransactionRow(it, vm.balanceHidden) }
        }
    }
}

@Composable
private fun AccountCard(acc: Account, hidden: Boolean, onToggle: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    BankCard(Modifier.fillMaxWidth(), color = MB.Ink) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tabungan", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                if (!acc.isActive) Pill(if (acc.status == "FROZEN") "Dibekukan" else acc.status, color = MB.Red, contentColor = Color.White)
                Box(Modifier.padding(start = 8.dp).size(10.dp).background(MB.Red, CircleShape))
            }
            Spacer(Modifier.height(18.dp))
            Text("Saldo", color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoneyText(acc.balance, Modifier.weight(1f), size = 36.sp, hidden = hidden, color = Color.White, subColor = Color.White.copy(alpha = .7f))
                IconButton(onClick = onToggle) {
                    Icon(
                        if (hidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = if (hidden) "Tampilkan saldo" else "Sembunyikan saldo",
                        tint = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Format.accountNumber(acc.accountNumber), color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { clipboard.setText(AnnotatedString(acc.accountNumber)) }) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Salin nomor rekening", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(MB.Paper, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(40.dp).background(MB.Ink, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun TransactionRow(line: StatementLine, hidden: Boolean = false) {
    val credit = line.isCredit
    val title = when (line.type) {
        "DEPOSIT" -> "Setor tunai"
        "WITHDRAWAL" -> "Tarik tunai"
        else -> if (credit) "Transfer masuk" else "Transfer keluar"
    }
    val who = line.counterpartyName ?: line.counterparty?.takeIf { it != "TUNAI" }?.let(Format::accountNumber)
    BankCard(Modifier.fillMaxWidth(), color = MB.Paper) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(38.dp).background(if (credit) MB.Card else MB.Card2, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (credit) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                    contentDescription = if (credit) "Masuk" else "Keluar",
                    tint = if (credit) MB.Green else MB.RedInk,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(who, line.description?.takeIf { it.isNotBlank() }).joinToString(" · ").ifEmpty { Format.dateTime(line.date) },
                    color = MB.Ink2, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                if (hidden) "••••" else Format.rupiah(line.amount, withSign = true, negative = !credit),
                color = if (credit) MB.Green else MB.RedInk,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
