package com.barkoder.demoscanner

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.barkoder.demoscanner.enums.ScanMode
import com.barkoder.demoscanner.fragments.SettingsFragment
import androidx.activity.OnBackPressedCallback

class SettingsActivity : AppCompatActivity() {
    private lateinit var scanMode: ScanMode
    private var openedFromSettings: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.toolBarColor)
        }
        scanMode = ScanMode.values()[this.intent.extras!!.getInt(SettingsFragment.ARGS_MODE_KEY)]
        Log.d("scanMode", scanMode.ordinal.toString());
        openedFromSettings = intent.getBooleanExtra("opened_from_settings", false)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()


        val root = findViewById<View>(android.R.id.content)

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

                val fm = supportFragmentManager

                if (fm.backStackEntryCount > 0) {
                    fm.popBackStack()
                } else {
                    if (openedFromSettings ||
                        scanMode == ScanMode.GALLERY_SCAN ||
                        scanMode == ScanMode.GLOBAL
                    ) {
                        finish()
                    } else {
                        val intent = Intent(
                            this@SettingsActivity,
                            ScannerActivity::class.java
                        )
                        intent.putExtra(
                            ScannerActivity.ARGS_MODE_KEY,
                            scanMode.ordinal
                        )
                        startActivity(intent)
                        finish()
                    }
                }
            }
        })

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            val fm = supportFragmentManager
            if (fm.backStackEntryCount > 0) {
                fm.popBackStack() // Go back to previous fragment
            } else if (openedFromSettings || scanMode == ScanMode.GALLERY_SCAN || scanMode == ScanMode.GLOBAL) {
                // Just go back normally
                finish()
            } else {
                // Original behavior
                val intent = Intent(this@SettingsActivity, ScannerActivity::class.java)
                intent.putExtra(ScannerActivity.ARGS_MODE_KEY, scanMode.ordinal)
                startActivity(intent)
                finish()
            }
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }



}
