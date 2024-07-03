package com.barkoder.demoscanner.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

object ImageUtil {

    fun bitmapFromUri(contentResolver: ContentResolver?, uri: Uri?): Bitmap? {
        if (contentResolver != null && uri != null) {
            return if (Build.VERSION.SDK_INT < 28) {
                val bitmap = MediaStore.Images.Media.getBitmap(
                    contentResolver,
                    uri
                )
                bitmap
            } else {
                val source = ImageDecoder.createSource(contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source)
                { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
                bitmap
            }
        }

        return null
    }
}
