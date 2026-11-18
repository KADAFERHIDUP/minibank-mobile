package io.github.kadaferhidup.minibank.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kadaferhidup.minibank.mobile.data.Format
import io.github.kadaferhidup.minibank.mobile.ui.theme.MB
import java.math.BigDecimal

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = 52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = MB.Ink, contentColor = Color.White, disabledContainerColor = MB.Ink3, disabledContentColor = MB.Paper),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MB.Ink),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MB.Ink),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supporting: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    secret: Boolean = false,
    prefix: String? = null,
    enabled: Boolean = true,
) {
    var shown by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        isError = error != null,
        prefix = prefix?.let { { Text(it, fontWeight = FontWeight.SemiBold) } },
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }, onGo = { onDone?.invoke() }),
        visualTransformation = if (secret && !shown) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (secret) {
            {
                IconButton(onClick = { shown = !shown }) {
                    Icon(
                        if (shown) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (shown) "Sembunyikan" else "Tampilkan",
                    )
                }
            }
        } else null,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MB.Paper,
            unfocusedContainerColor = MB.Paper,
            disabledContainerColor = MB.Card2,
            focusedBorderColor = MB.Ink,
            unfocusedBorderColor = MB.Line,
            focusedLabelColor = MB.Ink,
            unfocusedLabelColor = MB.Ink2,
        ),
    )
}

/** Kartu dasar dengan sudut membulat seperti versi web. */
@Composable
fun BankCard(modifier: Modifier = Modifier, color: Color = MB.Card, content: @Composable () -> Unit) {
    Surface(modifier = modifier, color = color, shape = RoundedCornerShape(22.dp), content = content)
}

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Color(0xFFF6DEDA), RoundedCornerShape(14.dp))
            .border(1.dp, MB.RedInk.copy(alpha = .25f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MB.RedInk)
        Text(message, color = MB.RedInk, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Nominal besar dengan "Rp" dan sen dikecilkan, seperti `.money` di web. */
@Composable
fun MoneyText(
    amount: BigDecimal,
    modifier: Modifier = Modifier,
    size: TextUnit = 40.sp,
    hidden: Boolean = false,
    color: Color = MB.Ink,
    subColor: Color = MB.Ink2,
) {
    if (hidden) {
        Text(
            "Rp ••••••",
            fontSize = size,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = modifier.semantics { contentDescription = "Saldo disembunyikan" },
        )
        return
    }
    val full = Format.rupiah(amount).removePrefix("Rp")
    val (whole, cents) = full.split(',').let { it[0] to it.getOrNull(1) }
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontSize = size * .42f, fontWeight = FontWeight.Medium, color = subColor)) { append("Rp ") }
            withStyle(SpanStyle(fontSize = size, fontWeight = FontWeight.Bold, color = color)) { append(whole) }
            if (cents != null) withStyle(SpanStyle(fontSize = size * .38f, fontWeight = FontWeight.Medium, color = subColor)) { append(",$cents") }
        },
        letterSpacing = (-1).sp,
        modifier = modifier,
    )
}

@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MB.Ink, strokeWidth = 3.dp)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

/** Dua baris judul besar seperti `.headline` web: baris kedua abu. */
@Composable
fun Headline(first: String, second: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(first, style = MaterialTheme.typography.headlineMedium)
        Text(second, style = MaterialTheme.typography.headlineMedium, color = MB.Ink3)
    }
}

@Composable
fun Pill(text: String, color: Color = MB.Card2, contentColor: Color = MB.Ink, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.background(color, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
        color = contentColor,
        style = MaterialTheme.typography.labelMedium,
    )
}
