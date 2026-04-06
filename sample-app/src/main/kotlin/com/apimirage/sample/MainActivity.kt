package com.apimirage.sample

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.view.ViewGroup
import android.widget.TextView
import com.apimirage.core.ApiMirage
import com.apimirage.core.ApiMirageConfig
import com.apimirage.core.ApiMirageDiagnostics
import com.apimirage.sample.network.SampleApiEnvironment
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

public class MainActivity : Activity() {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

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
        val titleView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            text = getString(R.string.sample_title)
            textSize = 24f
            setPadding(0, 0, 0, 24)
        }

        val summaryView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            text = getString(
                R.string.sample_summary,
                currentConfig.enabled.toString(),
                currentConfig.seed?.toString() ?: "none",
                currentConfig.diagnostics.name,
            )
            textSize = 16f
            setPadding(0, 0, 0, 32)
        }

        val runButton = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            text = getString(R.string.sample_run_button)
        }

        val resultView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            text = getString(R.string.sample_loading)
            setTextIsSelectable(true)
            textSize = 15f
            setPadding(0, 32, 0, 0)
        }

        val content = LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            addView(titleView)
            addView(summaryView)
            addView(runButton)
            addView(resultView)
        }

        setContentView(
            ScrollView(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                addView(content)
            },
        )

        runButton.setOnClickListener {
            runSampleDemo(resultView, runButton)
        }

        runSampleDemo(resultView, runButton)
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }

    private fun runSampleDemo(
        resultView: TextView,
        runButton: Button,
    ) {
        val config = ApiMirage.currentConfig()
        resultView.text = getString(R.string.sample_loading)
        runButton.isEnabled = false

        executor.execute {
            val report = SampleApiScenarioRunner(
                service = SampleApiEnvironment.createService(config),
            ).runDemoReport(config)

            runOnUiThread {
                resultView.text = report
                runButton.isEnabled = true
            }
        }
    }
}
