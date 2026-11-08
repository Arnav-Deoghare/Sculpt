package com.arnav.sculpt

import android.app.Application
import android.content.Context
import android.util.Log
import com.arnav.sculpt.data.PhotoRepository
import com.arnav.sculpt.data.PoseRepository
import com.arnav.sculpt.data.db.SculptDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SculptApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Holds the shared database and repositories. Reach it from any screen with context.appContainer. */
class AppContainer(private val app: Application) {

    /** For work that must finish even if a screen leaves the screen, such as saving a photo row. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database = SculptDatabase.get(app)
    val poses = PoseRepository(database.poseDao())
    val photos = PhotoRepository(app, database.photoDao())

    private val _ready = MutableStateFlow(false)

    /** True once the built-in poses exist and the photo records match the photos folder. */
    val ready: StateFlow<Boolean> = _ready

    init {
        appScope.launch {
            try {
                startup()
            } catch (e: Exception) {
                Log.e("SculptStartup", "Startup failed", e)
            } finally {
                _ready.value = true
            }
        }
    }

    private suspend fun startup() {
        poses.ensureBuiltIns()
        // Make sure the database and the photos folder agree.
        photos.reconcile()
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as SculptApplication).container
