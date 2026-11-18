package io.github.kadaferhidup.minibank.mobile.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.kadaferhidup.minibank.mobile.data.BankRepository
import io.github.kadaferhidup.minibank.mobile.data.Session
import io.github.kadaferhidup.minibank.mobile.ui.account.AccountScreen
import io.github.kadaferhidup.minibank.mobile.ui.account.ReceiveDialog
import io.github.kadaferhidup.minibank.mobile.ui.auth.AuthViewModel
import io.github.kadaferhidup.minibank.mobile.ui.auth.LoginScreen
import io.github.kadaferhidup.minibank.mobile.ui.auth.RegisterScreen
import io.github.kadaferhidup.minibank.mobile.ui.home.BankViewModel
import io.github.kadaferhidup.minibank.mobile.ui.home.HomeScreen
import io.github.kadaferhidup.minibank.mobile.ui.statement.StatementScreen
import io.github.kadaferhidup.minibank.mobile.ui.statement.StatementViewModel
import io.github.kadaferhidup.minibank.mobile.ui.theme.MB
import io.github.kadaferhidup.minibank.mobile.ui.transfer.TransferScreen
import io.github.kadaferhidup.minibank.mobile.ui.transfer.TransferViewModel

/** Belum masuk -> layar masuk/daftar. Sudah masuk -> empat tab. Token hilang (401, logout, idle) -> kembali ke layar masuk. */
@Composable
fun MiniBankRoot(session: Session, repo: BankRepository) {
    val token by session.token.collectAsState()
    val current = token
    if (current == null) {
        AuthFlow(session, repo)
    } else {
        // Wadah ViewModel khusus satu sesi. Dibuang saat keluar, jadi data nasabah sebelumnya
        // tidak tertinggal di memori dan tidak terbawa ke akun berikutnya.
        val owner = remember(current) {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
        DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { MainFlow(repo) }
    }
}

@Composable
private fun AuthFlow(session: Session, repo: BankRepository) {
    val nav = rememberNavController()
    val vm: AuthViewModel = viewModel { AuthViewModel(repo) }
    val reason by session.logoutReason.collectAsState()
    NavHost(nav, startDestination = "login", modifier = Modifier.fillMaxSize()) {
        composable("login") {
            LoginScreen(vm, notice = reason, onRegister = {
                session.consumeLogoutReason()
                nav.navigate("register")
            })
        }
        composable("register") { RegisterScreen(vm, onBack = { nav.popBackStack() }) }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Beranda", Icons.Outlined.Home),
    Transfer("Transfer", Icons.Outlined.SwapHoriz),
    Statement("Mutasi", Icons.AutoMirrored.Outlined.ReceiptLong),
    Account("Akun", Icons.Outlined.AccountCircle),
}

@Composable
private fun MainFlow(repo: BankRepository) {
    val bank: BankViewModel = viewModel { BankViewModel(repo) }
    val transfer: TransferViewModel = viewModel { TransferViewModel(repo) }
    val statement: StatementViewModel = viewModel { StatementViewModel(repo) }
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var showReceive by rememberSaveable { mutableStateOf(false) }
    var statementRefresh by rememberSaveable { mutableIntStateOf(0) }

    // Pindah tab = pesan dari tab Akun tidak ditampilkan lagi.
    LaunchedEffect(tab) { if (tab != Tab.Account) bank.message = null }

    Scaffold(
        containerColor = MB.Bg,
        bottomBar = {
            NavigationBar(containerColor = MB.Paper) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == tab,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            indicatorColor = MB.Ink,
                            selectedTextColor = MB.Ink,
                            unselectedIconColor = MB.Ink2,
                            unselectedTextColor = MB.Ink2,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        androidx.compose.foundation.layout.Box(modifier) {
            when (tab) {
                Tab.Home -> HomeScreen(
                    bank,
                    onTransfer = { tab = Tab.Transfer },
                    onStatement = { tab = Tab.Statement },
                    onReceive = { showReceive = true },
                )
                Tab.Transfer -> TransferScreen(
                    transfer,
                    from = bank.selected,
                    onSent = {
                        bank.refresh()
                        statementRefresh++
                    },
                    onFinish = { tab = Tab.Home },
                )
                Tab.Statement -> StatementScreen(statement, bank.selected, statementRefresh)
                Tab.Account -> AccountScreen(bank)
            }
        }
    }

    val selected = bank.selected
    if (showReceive && selected != null) {
        ReceiveDialog(bank.profile?.fullName, selected) { showReceive = false }
    }
}
