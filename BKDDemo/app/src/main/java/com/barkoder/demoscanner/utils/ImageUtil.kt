package com.barkoder.demoscanner.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlin.math.max
import kotlin.math.truncate

object ImageUtil {

    fun bitmapFromUri(
        contentResolver: ContentResolver?,
        uri: Uri?,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        if (contentResolver != null && uri != null) {
            return if (Build.VERSION.SDK_INT < 28) {
                // For Android versions lower than 28
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)

                // Log original bitmap size
                Log.d("Decoding BitmapInfo", "Original Bitmap Size: ${bitmap.width}x${bitmap.height}")

                // Scale down the bitmap if necessary
                val scaledBitmap = scaleDownBitmap(bitmap, reqWidth, reqHeight)

                // Log scaled bitmap size
                Log.d("Decoding BitmapInfo", "Scaled Bitmap Size: ${scaledBitmap.width}x${scaledBitmap.height}")

                scaledBitmap
            } else {
                // For Android 28 (Pie) and above
                val source = ImageDecoder.createSource(contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }

                // Log original bitmap size
                Log.d("Decoding BitmapInfo", "Original Bitmap Size: ${bitmap.width}x${bitmap.height}")

                // Scale down the bitmap if necessary
                val scaledBitmap = scaleDownBitmap(bitmap, reqWidth, reqHeight)

                // Log scaled bitmap size
                Log.d("Decoding BitmapInfo", "Scaled Bitmap Size: ${scaledBitmap.width}x${scaledBitmap.height}")

                scaledBitmap
            }
        }
        return null
    }

    fun scaleDownBitmap(bitmap: Bitmap, reqWidth: Int, reqHeight: Int): Bitmap {
        // Get the larger dimension of the bitmap (either width or height)
        val largerDimension = max(bitmap.width, bitmap.height)

        // Check if scaling is needed (if the larger dimension is greater than the requested size)
        if (largerDimension <= max(reqWidth, reqHeight)) {
            // No scaling needed, return the original bitmap
            return bitmap
        }

        // Calculate the scaling factor
        val scaleDownFactor =
            (truncate(largerDimension / max(reqWidth, reqHeight).toDouble()) + 1).toInt()

        // Calculate new dimensions
        val newWidth = bitmap.width / scaleDownFactor
        val newHeight = bitmap.height / scaleDownFactor

        // Scale and return the new bitmap
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
