package com.barkoder.demoscanner.utils

import android.content.Context
import android.util.Log
import com.google.android.play.agesignals.AgeSignalsManagerFactory
import com.google.android.play.agesignals.AgeSignalsRequest
import com.google.android.play.agesignals.model.AgeSignalsVerificationStatus

/**
 * Helper class for integrating the Play Age Signals API (beta) v0.0.3
 * to comply with Texas SB2420 (App Store Accountability Act).
 */
class AgeSignalsHelper(private val context: Context) {

    companion object {
        private const val TAG = "AgeSignalsHelper"
    }

    interface AgeSignalsCallback {
        fun onAgeSignalsReceived(result: AgeSignalsInfo)
        fun onAgeSignalsError(errorCode: Int, message: String)
    }

    data class AgeSignalsInfo(
        val userStatus: Int?,
        val ageLower: Int?,
        val ageUpper: Int?,
        val installId: String?,
        val isMinor: Boolean,
        val isAccessDenied: Boolean
    )

    /**
     * Requests age signals from Google Play.
     * The API only returns data for users in applicable jurisdictions (e.g., Texas).
     * For users outside those regions, userStatus will be null.
     */
    fun requestAgeSignals(callback: AgeSignalsCallback) {
        try {
            val ageSignalsManager = AgeSignalsManagerFactory.create(context)

            ageSignalsManager
                .checkAgeSignals(AgeSignalsRequest.builder().build())
                .addOnSuccessListener { ageSignalsResult ->
                    val userStatus = ageSignalsResult.userStatus()
                    val ageLower = ageSignalsResult.ageLower()
                    val ageUpper = ageSignalsResult.ageUpper()
                    val installId = ageSignalsResult.installId()

                    Log.d(TAG, "Age signals received - status: $userStatus, " +
                            "ageLower: $ageLower, ageUpper: $ageUpper")

                    val isMinor = ageLower != null && (ageUpper != null && ageUpper < 18)
                    val isAccessDenied = userStatus == AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED

                    val info = AgeSignalsInfo(
                        userStatus = userStatus,
                        ageLower = ageLower,
                        ageUpper = ageUpper,
                        installId = installId,
                        isMinor = isMinor,
                        isAccessDenied = isAccessDenied
                    )

                    callback.onAgeSignalsReceived(info)
                }
                .addOnFailureListener { exception ->
                    val errorCode = parseErrorCode(exception)
                    Log.e(TAG, "Age signals request failed - code: $errorCode, " +
                            "message: ${exception.message}")
                    callback.onAgeSignalsError(errorCode, exception.message ?: "Unknown error")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Age Signals API", e)
            callback.onAgeSignalsError(-100, e.message ?: "Initialization failed")
        }
    }

    private fun parseErrorCode(exception: Exception): Int {
        // The Play Age Signals API wraps error codes in the exception
        return try {
            val message = exception.message ?: ""
            val codeMatch = Regex("\\((-?\\d+)\\)").find(message)
            codeMatch?.groupValues?.get(1)?.toInt() ?: -100
        } catch (e: Exception) {
            -100
        }
    }

    /**
     * Returns a human-readable description of the user status.
     */
    fun getUserStatusDescription(userStatus: Int?): String {
        return when (userStatus) {
            AgeSignalsVerificationStatus.VERIFIED -> "Age verified by Google"
            AgeSignalsVerificationStatus.DECLARED -> "Age declared by user"
            AgeSignalsVerificationStatus.SUPERVISED -> "Supervised account"
            AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING -> "Parental approval pending"
            AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED -> "Parental approval denied"
            AgeSignalsVerificationStatus.UNKNOWN -> "Age unknown"
            else -> "Not applicable (user not in regulated jurisdiction)"
        }
    }

    /**
     * Returns a human-readable error description based on the error code.
     */
    fun getErrorDescription(errorCode: Int): String {
        return when (errorCode) {
            -1 -> "Play Age Signals API not available. Please update the Play Store."
            -2 -> "Play Store not found on this device."
            -3 -> "No network connection available."
            -4 -> "Google Play Services not found or outdated."
            -5 -> "Cannot bind to Play Store service. Please update the Play Store."
            -6 -> "Play Store version is outdated. Please update."
            -7 -> "Google Play Services version is outdated. Please update."
            -8 -> "Transient error. Please try again."
            -9 -> "App was not installed from Google Play."
            -10 -> "Age Signals SDK version is outdated. Please update the app."
            -100 -> "Internal error. Please try again later."
            else -> "Unknown error (code: $errorCode)."
        }
    }
}
