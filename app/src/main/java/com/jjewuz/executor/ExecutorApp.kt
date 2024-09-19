package com.jjewuz.executor

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.android.material.color.DynamicColors

class ExecutorApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
    }
}