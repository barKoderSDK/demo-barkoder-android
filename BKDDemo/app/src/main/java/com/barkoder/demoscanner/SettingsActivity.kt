package com.barkoder.demoscanner

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.barkoder.demoscanner.enums.ScanMode
import com.barkoder.demoscanner.fragments.SettingsFragment

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


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            if (openedFromSettings || scanMode.ordinal == 14) {
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

    override fun onBackPressed() {
        if (openedFromSettings || scanMode.ordinal == 14) {
            // Just finish if opened from within settings
            finish()
        } else {
            // Go back to scanner activity with scan mode
            val intent = Intent(this@SettingsActivity, ScannerActivity::class.java)
            intent.putExtra(ScannerActivity.ARGS_MODE_KEY, scanMode.ordinal)
            startActivity(intent)
            finish()
        }
    }

}
