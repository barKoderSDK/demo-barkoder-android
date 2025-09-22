package com.barkoder.demoscanner.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.Context.CLIPBOARD_SERVICE
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.FileProvider
import com.barkoder.demoscanner.R
import com.google.android.material.internal.ViewUtils.dpToPx
import java.io.File
import java.io.IOException

object CommonUtil {

    fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    fun isTextURL(text: String?): Boolean {
        return if (!text.isNullOrBlank()) {
            Patterns.WEB_URL.matcher(text).matches()
        } else
            false
    }

    fun cleanResultString(result: String): String {
        return result.filter {
            // Keep only printable characters: letters, digits, punctuation, and whitespace
            !it.isISOControl() && it != '?' && it.code in 32..126 || it.isWhitespace()
        }
    }


    fun openURLInBrowser(url: String, activity: Activity) {
        try {
            var urlToOpen = url

            if (!urlToOpen.startsWith("http"))
                urlToOpen = "https://$urlToOpen"

            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen)))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.about_learn_more_no_browser, Toast.LENGTH_LONG)
                .show()
        }
    }

   fun copyBarcodeResultText(context : Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = android.content.ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
    }



    fun setAlertDialogMessageAsURL(
        dialog: AlertDialog,
        context: Context
    ) {
        dialog.findViewById<TextView>(android.R.id.message)?.let { messageView ->
            messageView.paintFlags = messageView.paintFlags or Paint.UNDERLINE_TEXT_FLAG

            if (context is Activity) {
                messageView.setOnClickListener {
                    openURLInBrowser(
                        messageView.text!!.toString(),
                        context
                    )
                }
            }
        }
    }



    fun copyTextToClipboard(textToCopy: String?, context: Context?) {
        if (!textToCopy.isNullOrBlank()) {
            context?.run {
                val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.setPrimaryClip(ClipData.newPlainText("", textToCopy))
                // Only show a toast for Android 12 and lower.
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S)
                    Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun openSearchInBrowser(url: String, query : String, activity: Activity) {
        try {
            var urlToOpen = url + query

            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen)))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.about_learn_more_no_browser, Toast.LENGTH_LONG)
                .show()
        }
    }

    fun getBitmapFromInternalStorage(imagePath: String): Bitmap? {
        val file = File(imagePath)
        if (file.exists()) {
            return BitmapFactory.decodeFile(file.absolutePath)
        } else {
            Log.e("Bitmap Conversion", "File does not exist at $imagePath")
            return null
        }
    }

    fun createPdf(
        context: Context,
        imageList: List<Pair<Bitmap, String>>,  // List of Bitmaps and their names
        text: String
    ) {
        val pdfDocument = PdfDocument()

        val paint = Paint()
        val titlePaint = Paint()
        titlePaint.textSize = 20f

        // Create the page description (A4 size)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points (595x842)
        val page = pdfDocument.startPage(pageInfo)

        // Get the canvas to draw on
        val canvas: Canvas = page.canvas

        // Handle multi-line text by splitting on '\n'
        val lines = text.split("\n")

        // Define the starting Y position for the text
        var textYPosition = 100f  // Starting Y position for the first line of text

        // Loop through each line and draw it on the canvas
        lines.forEach { line ->
            canvas.drawText(line, 50f, textYPosition, titlePaint)
            textYPosition += titlePaint.textSize + 10f // Move Y position down for the next line (10 points spacing)
        }

        // Define the starting Y position for the images
        var imageYPosition = 50f  // Starting Y position for the first image
        val defaultImageXPosition = 400f  // Default X position for images (aligned to the right side)

        // Loop through the image list and draw each image based on its name
        imageList.forEach { (bitmap, name) ->

            // Check the name of the image and scale accordingly
            val scaledBitmap = when (name) {
                "signature" -> {
                    // For "main" and "signature", scale to width 200 and height 100
                    Bitmap.createScaledBitmap(bitmap, 200, 100, true)
                }
                "main" -> {
                    Bitmap.createScaledBitmap(bitmap, 450, 100, true)
                }
                "document" -> {
                    Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                }
                else -> {
                    // For all other images, scale to a default size (e.g., 150x150)
                    Bitmap.createScaledBitmap(bitmap, 150, 150, true)
                }
            }

            // Adjust the X position for "main" and "signature"
            val imageXPosition = when (name) {
                "signature" -> defaultImageXPosition - 50f  // Move 50 points to the left
                "main" -> defaultImageXPosition - 300f
                "document" -> defaultImageXPosition - 50f
                else -> defaultImageXPosition  // Default X position for other images
            }

            // Draw the bitmap at the calculated position
            canvas.drawBitmap(scaledBitmap, imageXPosition, imageYPosition, paint)

            // Update Y position for the next image (to stack them vertically)
            imageYPosition += scaledBitmap.height + 20f // Add a margin of 20 points between images
        }

        // Finish the page
        pdfDocument.finishPage(page)

        // Save the PDF to the PDF directory
        val filePath = getExternalFilesDir(context, "shared_pdf")
        val file = File(filePath, "shared_pdf_${System.currentTimeMillis()}.pdf")
        Log.d("File Path", file.absolutePath)

        try {
            file.outputStream().use { outputStream ->
                pdfDocument.writeTo(outputStream)
                Log.d("PDF Creation", "PDF generated at ${file.absolutePath}")
            }
            Log.d("File Size", "PDF file size: ${file.length()} bytes")
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        // Check if the file size is valid before sharing
        if (file.length() > 0) {
            Log.d("File Status", "File is ready to be shared")
            sharePdfFile(context, file)
        } else {
            Log.e("File Status", "File is empty, not sharing")
        }
    }


    fun getExternalFilesDir(context: Context, folder: String): File {
        val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), folder)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    fun sharePdfFile(context: Context, file: File) {
        Log.d("path absolute", file.absolutePath)
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Ensure this matches the authority in the manifest
            file
        )

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        context.startActivity(Intent.createChooser(intent, "Share PDF via"))
    }
}
