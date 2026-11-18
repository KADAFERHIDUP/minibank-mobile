package io.github.kadaferhidup.minibank.mobile

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.kadaferhidup.minibank.mobile.ui.MiniBankRoot
import io.github.kadaferhidup.minibank.mobile.ui.theme.MiniBankTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Saldo dan nomor rekening tidak boleh ikut tangkapan layar atau terlihat di daftar aplikasi terakhir.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        val app = application as MiniBankApp
        setContent {
            MiniBankTheme {
                MiniBankRoot(app.session, app.repository)
            }
        }
    }
}
