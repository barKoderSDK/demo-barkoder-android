package com.barkoder.demoscanner.utils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.text.TextUtils
import androidx.fragment.app.FragmentManager
import com.barkoder.Barkoder
import com.barkoder.demoscanner.fragments.ResultBottomDialogFragment
import com.barkoder.demoscanner.models.RecentScan
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

object ScannedResultsUtil {

    private const val SCANNED_RESULTS_SHARED_PREFS_SUFFIX = "_preferences_recent_scans"
    private const val SCANNED_RESULTS_KEY = "recentScans"

    private var gson: Gson? = null //TODO use Hilt maybe ??
    private var resultsSharedPrefs: SharedPreferences? = null

    suspend fun handleScannedResults(
        context: Context,
        results: Array<Barkoder.Result>,
        fragmentManager : FragmentManager,
        image : Bitmap,
        showContinueButton: Boolean = true

    ): Boolean {
        lateinit var result: Continuation<Boolean>

        val barcodeDataList = mutableListOf<String>()
        val barcodeTypeList = mutableListOf<String>()

        var mutableListResults = getResultsResultList(results, barcodeDataList)
        var mutableListTypes = getResultsTypeList(results, barcodeTypeList)

        val title = getResultsTitle(results)
        val message = getResultsReadString(results)

        storeResultsInPref(context.applicationContext, results)

//        val bottomSheetFragment = ResultBottomDialogFragment.newInstance(mutableListResults,
//            mutableListTypes, image, null)
//        bottomSheetFragment.show(fragmentManager, bottomSheetFragment.tag)

        return suspendCoroutine { continuation -> result = continuation }
    }

    fun storeResultsInPref(
        context: Context,
        results: Array<out Barkoder.Result>?
    ) {
        if (results!!.isNotEmpty()) {
            val recents = getResultsFromPref(context)

            val scannedDate =
                SimpleDateFormat(
                    "yyyy/MM/dd/HH:mm:ss",
                    Locale.getDefault()
                ).format(Date())


            for (result in results) {
                recents.add(0, RecentScan(scannedDate, result))
            }

            resultsSharedPrefs(context).edit()
                .putString(SCANNED_RESULTS_KEY, gson().toJson(recents))
                .apply()
        }

    }

    fun deleteResultsFromPref(
        context: Context,
        position: Int = -1
    ) {
        if (position == -1) {
            resultsSharedPrefs(context).edit()
                .remove(SCANNED_RESULTS_KEY)
                .apply()
        } else {
            val recents = getResultsFromPref(context)
            recents.removeAt(position)

            resultsSharedPrefs(context).edit()
                .putString(SCANNED_RESULTS_KEY, gson().toJson(recents))
                .apply()
        }
    }

    fun getResultsFromPref(
        context: Context
    ): ArrayList<RecentScan> {
        return gson().fromJson(
            resultsSharedPrefs(context).getString(
                SCANNED_RESULTS_KEY,
                JsonArray().toString()
            ), TypeToken.getParameterized(MutableList::class.java, RecentScan::class.java).type
        )
    }

    fun getResultsTitle(results: Array<out Barkoder.Result>?): ArrayList<String> {
        return ArrayList(results?.take(50)?.map {  if(it.extra != null) formatBarcodeName(it.barcodeTypeName, it.extra.toList()) else it.barcodeTypeName } ?: listOf("No Read"))

    }


    fun getResultsResultList(results: Array<out Barkoder.Result>?, list: MutableList<String>) : MutableList<String> {
        for(i in results!!){
            list.add(i.textualData)
        }
        return list
    }

    fun getResultsTypeList(results: Array<out Barkoder.Result>?, list: MutableList<String>) : MutableList<String> {
        for(i in results!!){
            list.add(i.barcodeTypeName)
        }
        return list
    }


    fun getResultsResultListContinious(results: Array<out Barkoder.Result>?, list: MutableList<String>) : MutableList<String> {
        for(i in results!!){
            list.add(i.textualData)
        }
        return list
    }

    fun getResultsTypeListContinious(results: Array<out Barkoder.Result>?, list: MutableList<String>) : MutableList<String> {
        for(i in results!!){
            list.add(i.barcodeTypeName)
        }
        return list
    }

    fun getResultsReadString(results: Array<out Barkoder.Result>?): ArrayList<String> {
        return ArrayList(results?.take(50)?.map { it.textualData } ?: listOf("No Read"))
    }

    private fun getResultReadString(
        resultExtra: Array<Barkoder.BKKeyValue>?,
        resultTextualData: String
    ) = getResultReadString(resultExtra?.associate { it.key to it.value }, resultTextualData)

    fun getResultReadString(
        resultExtra: Map<String, String>?,
        resultTextualData: String
    ): String {
        resultExtra?.let {
            for (item in it) {
                if (item.key == "formattedText" && !TextUtils.isEmpty(item.value))
                    return item.value
            }
        }

        return resultTextualData
    }

    fun isBarcode2D(barcodeType: String): Boolean {
        return barcodeType == "Aztec" ||
                barcodeType == "Aztec Compact" ||
                barcodeType == "QR" ||
                barcodeType == "QRMicro" ||
                barcodeType == "Datamatrix"
    }

    fun isMRZ(barcodeType: String): Boolean {
        return barcodeType == "ID Document"
    }

    private fun gson(): Gson {
        return gson ?: GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
    }

    private fun resultsSharedPrefs(context: Context): SharedPreferences {
        return resultsSharedPrefs ?: context.getSharedPreferences(
            context.packageName + SCANNED_RESULTS_SHARED_PREFS_SUFFIX,
            Context.MODE_PRIVATE
        )
    }


    fun formatBarcodeName(barcodeTypeName: String, extra: List<Barkoder.BKKeyValue>?): String {
        if (extra.isNullOrEmpty()) {
            // If extra is null or empty, return the original barcodeTypeName
            return barcodeTypeName
        }

        // Flags to track conditions
        var isGS1 = false
        var isComposite = false

        // Iterate over the list and check for keys and values
        for (item in extra) {
            when (item.key) {
                "gs1" -> isGS1 = item.value?.toIntOrNull() == 1
                "composite" -> isComposite = item.value?.toIntOrNull() == 1
            }
        }

        // Return the formatted name based on conditions
        return when {
            isGS1 && isComposite -> "GS1 $barcodeTypeName Composite"
            isGS1 -> "GS1 $barcodeTypeName"
            isComposite -> "$barcodeTypeName Composite"
            else -> barcodeTypeName
        }
    }

}
