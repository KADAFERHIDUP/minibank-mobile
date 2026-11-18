package io.github.kadaferhidup.minibank.mobile

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.github.kadaferhidup.minibank.mobile.data.BankRepository
import io.github.kadaferhidup.minibank.mobile.data.Session

class MiniBankApp : Application() {

    lateinit var session: Session
        private set
    lateinit var repository: BankRepository
        private set

    /** Waktu aplikasi terakhir masuk ke latar belakang. */
    private var backgroundedAt: Long? = null

    override fun onCreate() {
        super.onCreate()
        session = Session()
        repository = BankRepository.create(BuildConfig.API_BASE_URL, session, BuildConfig.DEBUG)

        // Seperti aplikasi bank pada umumnya: ditinggal di latar belakang lebih dari 5 menit = keluar otomatis.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                backgroundedAt = System.currentTimeMillis()
            }

            override fun onStart(owner: LifecycleOwner) {
                val since = backgroundedAt ?: return
                backgroundedAt = null
                if (System.currentTimeMillis() - since > IDLE_LIMIT_MS) {
                    session.end("Kamu otomatis keluar karena aplikasi lama tidak dipakai.")
                }
            }
        })
    }

    companion object {
        const val IDLE_LIMIT_MS = 5 * 60 * 1000L
    }
}
