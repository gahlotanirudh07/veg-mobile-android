package com.freshveg.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.freshveg.app.core.lifecycle.AppForegroundMonitor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VegApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var appForegroundMonitor: AppForegroundMonitor

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(appForegroundMonitor)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
