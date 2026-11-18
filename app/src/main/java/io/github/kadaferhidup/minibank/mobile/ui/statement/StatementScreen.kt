package io.github.kadaferhidup.minibank.mobile.ui.statement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kadaferhidup.minibank.mobile.data.Account
import io.github.kadaferhidup.minibank.mobile.data.Format
import io.github.kadaferhidup.minibank.mobile.ui.components.BankCard
import io.github.kadaferhidup.minibank.mobile.ui.components.ErrorBanner
import io.github.kadaferhidup.minibank.mobile.ui.components.Headline
import io.github.kadaferhidup.minibank.mobile.ui.components.Loading
import io.github.kadaferhidup.minibank.mobile.ui.home.TransactionRow
import io.github.kadaferhidup.minibank.mobile.ui.theme.MB

@Composable
fun StatementScreen(vm: StatementViewModel, account: Account?, refreshKey: Int) {
    LaunchedEffect(account?.accountNumber, refreshKey) {
        account?.let { vm.load(it.accountNumber, force = refreshKey > 0) }
    }
    val grouped = vm.items.groupBy { Format.dayKey(it.date) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Headline("Mutasi", account?.let { "•••• ${it.accountNumber.takeLast(4)}" } ?: "rekening")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                Range.entries.forEach { r ->
                    val on = r == vm.range
                    Text(
                        r.label,
                        modifier = Modifier
                            .background(if (on) MB.Ink else MB.Card, CircleShape)
                            .clickable(role = Role.Tab, enabled = account != null) { account?.let { vm.load(it.accountNumber, r) } }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        color = if (on) Color.White else MB.Ink,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Summary("Masuk", Format.rupiah(vm.totalIn), MB.Green, Modifier.weight(1f))
                Summary("Keluar", Format.rupiah(vm.totalOut), MB.RedInk, Modifier.weight(1f))
            }
        }
        vm.error?.let { item { ErrorBanner(it) } }
        if (vm.loading) item { Loading() }
        if (!vm.loading && vm.error == null && vm.items.isEmpty()) item {
            BankCard(Modifier.fillMaxWidth()) {
                Text("Tidak ada transaksi dalam ${vm.range.label} terakhir.", color = MB.Ink2, modifier = Modifier.padding(18.dp))
            }
        }
        if (!vm.loading) grouped.forEach { (_, lines) ->
            item(key = "h-" + Format.dayKey(lines.first().date)) {
                Text(Format.dayHeader(lines.first().date), color = MB.Ink2, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            }
            items(lines, key = { it.reference + it.direction }) { TransactionRow(it) }
        }
    }
}

@Composable
private fun Summary(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    BankCard(modifier, color = MB.Paper) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = MB.Ink2, style = MaterialTheme.typography.labelMedium)
            Text(value, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}
