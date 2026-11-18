package io.github.kadaferhidup.minibank.mobile.ui.transfer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kadaferhidup.minibank.mobile.data.Account
import io.github.kadaferhidup.minibank.mobile.data.Format
import io.github.kadaferhidup.minibank.mobile.data.Validation
import io.github.kadaferhidup.minibank.mobile.ui.components.BankCard
import io.github.kadaferhidup.minibank.mobile.ui.components.ErrorBanner
import io.github.kadaferhidup.minibank.mobile.ui.components.Field
import io.github.kadaferhidup.minibank.mobile.ui.components.Headline
import io.github.kadaferhidup.minibank.mobile.ui.components.MoneyText
import io.github.kadaferhidup.minibank.mobile.ui.components.PrimaryButton
import io.github.kadaferhidup.minibank.mobile.ui.components.SecondaryButton
import io.github.kadaferhidup.minibank.mobile.ui.theme.MB
import java.math.BigDecimal

@Composable
fun TransferScreen(vm: TransferViewModel, from: Account?, onSent: () -> Unit, onFinish: () -> Unit) {
    BackHandler(enabled = vm.step == Step.Amount || vm.step == Step.Confirm) { vm.back() }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StepBar(vm.step)
        when (vm.step) {
            Step.Recipient -> RecipientStep(vm, from)
            Step.Amount -> AmountStep(vm, from)
            Step.Confirm -> ConfirmStep(vm, from, onSent)
            Step.Receipt -> ReceiptStep(vm, onNew = vm::reset, onFinish = { vm.reset(); onFinish() })
        }
    }
}

@Composable
private fun StepBar(step: Step) {
    val labels = listOf("Tujuan", "Nominal", "Konfirmasi", "Struk")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, label ->
            val done = i <= step.ordinal
            Column(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth().height(4.dp).background(if (done) MB.Ink else MB.Line, CircleShape))
                Text(label, style = MaterialTheme.typography.labelMedium, color = if (i == step.ordinal) MB.Ink else MB.Ink2, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun RecipientStep(vm: TransferViewModel, from: Account?) {
    Headline("Kirim uang", "ke siapa?")
    BankCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Field(
                vm.toAccount, vm::onToAccount, "Nomor rekening tujuan",
                error = vm.fieldError["toAccount"],
                supporting = "10 digit. Nama pemilik dicek dulu sebelum kirim.",
                keyboardType = KeyboardType.Number, imeAction = ImeAction.Go,
                onDone = { vm.checkRecipient(from?.accountNumber) },
            )
            vm.error?.let { ErrorBanner(it) }
            PrimaryButton("Cek nama penerima", { vm.checkRecipient(from?.accountNumber) }, Modifier.fillMaxWidth(), loading = vm.busy)
        }
    }
    if (from == null) ErrorBanner("Belum ada rekening untuk mengirim.")
}

@Composable
private fun AmountStep(vm: TransferViewModel, from: Account?) {
    val r = vm.recipient ?: return
    Headline("Berapa", "yang dikirim?")
    RecipientCard(r.holderName, r.accountNumber)
    BankCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Field(
                vm.amountText, vm::onAmount, "Nominal", prefix = "Rp ",
                error = vm.fieldError["amount"],
                supporting = "Min ${Format.rupiah(BigDecimal(Validation.MIN_TRANSFER))} · maks ${Format.rupiah(BigDecimal(Validation.MAX_TRANSFER))} per transaksi",
                keyboardType = KeyboardType.Number,
            )
            Field(vm.description, vm::onDescription, "Catatan (opsional)", imeAction = ImeAction.Done)
            from?.let {
                Text("Saldo ${Format.rupiah(it.balance)} · dari ${Format.accountNumber(it.accountNumber)}", color = MB.Ink2, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Ubah tujuan", vm::back, Modifier.weight(1f))
                PrimaryButton("Lanjut", { vm.toConfirm(from?.balance) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ConfirmStep(vm: TransferViewModel, from: Account?, onSent: () -> Unit) {
    val r = vm.recipient ?: return
    Headline("Cek lagi", "sebelum kirim.")
    BankCard(color = MB.Paper) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Nominal", color = MB.Ink2)
            MoneyText(BigDecimal.valueOf(vm.amount ?: 0L), size = 34.sp)
            HorizontalDivider(color = MB.Line2)
            Line("Ke", "${r.holderName}\n${Format.accountNumber(r.accountNumber)}")
            Line("Dari", from?.let { Format.accountNumber(it.accountNumber) } ?: "-")
            if (vm.description.isNotBlank()) Line("Catatan", vm.description.trim())
            Line("Biaya", "Gratis")
        }
    }
    BankCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Field(
                vm.pin, vm::onPin, "PIN transaksi", error = vm.fieldError["pin"],
                keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Go, secret = true,
                onDone = { from?.let { vm.send(it.accountNumber, onSent) } },
            )
            vm.error?.let { ErrorBanner(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Kembali", vm::back, Modifier.weight(1f), enabled = !vm.busy)
                PrimaryButton("Kirim", { from?.let { vm.send(it.accountNumber, onSent) } }, Modifier.weight(1f), loading = vm.busy, enabled = from != null)
            }
        }
    }
}

@Composable
private fun ReceiptStep(vm: TransferViewModel, onNew: () -> Unit, onFinish: () -> Unit) {
    val res = vm.result ?: return
    val tx = res.transaction
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(64.dp).background(MB.Ink, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Text("Transfer berhasil", style = MaterialTheme.typography.headlineSmall)
        if (res.replayed) {
            Text("Transfer ini sudah tercatat sebelumnya, saldo tidak dipotong lagi.", color = MB.Ink2, textAlign = TextAlign.Center)
        }
    }
    BankCard(color = MB.Paper) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MoneyText(tx.amount, size = 34.sp)
            HorizontalDivider(color = MB.Line2)
            Line("Ke", "${vm.recipient?.holderName.orEmpty()}\n${tx.destinationAccount?.let(Format::accountNumber) ?: "-"}")
            Line("Dari", tx.sourceAccount?.let(Format::accountNumber) ?: "-")
            tx.description?.takeIf { it.isNotBlank() }?.let { Line("Catatan", it) }
            Line("Waktu", Format.dateTime(tx.createdAt))
            Line("Referensi", tx.reference)
            Line("Status", if (tx.status == "COMPLETED") "Berhasil" else tx.status)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SecondaryButton("Transfer lagi", onNew, Modifier.weight(1f))
        PrimaryButton("Selesai", onFinish, Modifier.weight(1f))
    }
}

@Composable
private fun RecipientCard(name: String, number: String) {
    BankCard(color = MB.Paper) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).background(MB.Ink, CircleShape), contentAlignment = Alignment.Center) {
                Text(name.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text(Format.accountNumber(number), color = MB.Ink2)
            }
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = MB.Ink2, modifier = Modifier.weight(.4f))
        Text(value, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(.6f))
    }
}
