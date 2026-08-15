package ba.noniboy.struja

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class.
 * Hilt will generate the DI graph at compile time.
 * Initializes the local database and seeds it with default rates.
 */
@HiltAndroidApp
class StrujaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // App-level initialization (if needed)
    }
}
