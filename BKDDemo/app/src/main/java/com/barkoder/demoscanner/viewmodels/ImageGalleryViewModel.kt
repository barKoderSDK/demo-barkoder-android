package com.barkoder.demoscanner.viewmodels

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel

class ImageGalleryViewModel : ViewModel() {
    var imageUri: Uri? = null
    var bitmap: Bitmap? = null
}