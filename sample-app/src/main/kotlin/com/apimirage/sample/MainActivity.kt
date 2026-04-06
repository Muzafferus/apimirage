package com.apimirage.sample

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import com.apimirage.core.ApiMirage
import com.apimirage.core.ApiMirageConfig
import com.apimirage.core.ApiMirageDiagnostics

public class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ApiMirage.install(
            ApiMirageConfig(
                enabled = BuildConfig.DEBUG,
                seed = 20260407L,
                diagnostics = if (BuildConfig.DEBUG) {
                    ApiMirageDiagnostics.LOGS
                } else {
                    ApiMirageDiagnostics.NONE
                },
            ),
        )

        val currentConfig = ApiMirage.currentConfig()
        val statusView = TextView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            text = getString(
                R.string.sample_status_placeholder,
                currentConfig.enabled.toString(),
                currentConfig.seed?.toString() ?: "none",
                currentConfig.diagnostics.name,
            )
            textSize = 18f
        }

        setContentView(statusView)
    }
}
