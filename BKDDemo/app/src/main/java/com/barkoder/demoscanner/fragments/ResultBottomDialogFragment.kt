package com.barkoder.demoscanner.fragments

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.api.RetrofitIInstance
import com.barkoder.demoscanner.databinding.FragmentResultBottomDialogBinding
import com.barkoder.demoscanner.models.BarcodeScanedData
import com.barkoder.demoscanner.repositories.BarcodeDataRepository
import com.barkoder.demoscanner.utils.CommonUtil
import com.barkoder.demoscanner.utils.DemoDefaults
import com.barkoder.demoscanner.utils.NetworkUtils
import com.barkoder.demoscanner.utils.getString
import com.barkoder.demoscanner.viewmodels.BarcodeDataViewModel
import com.barkoder.demoscanner.viewmodels.BarcodeDataViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ResultBottomDialogFragment : BottomSheetDialogFragment() {

    private lateinit var viewModel : BarcodeDataViewModel

    private lateinit var sharedPreferences : SharedPreferences

    private val REQUEST_CODE = 1001

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    var bottomSheet: View? = null


    private val scannedBarcodesResultList: MutableList<String> = mutableListOf()
    private val scannedBarcodesTypesList:  MutableList<String> = mutableListOf()
    private val scannedBarcodesDateList : MutableList<String> = mutableListOf()


    private var _binding: FragmentResultBottomDialogBinding? = null
    private val binding get() = _binding!!

    private val writeStoragePermission by lazy {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    interface BottomSheetStateListener {

        fun onStartScanningClicked()
    }

    private var stateListener: BottomSheetStateListener? = null

    fun changeBottomSheetState() {
        val bottomSheetView = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView!!)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        updatePeekHeight(bottomSheetBehavior.peekHeight , 1200, bottomSheetBehavior)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog)
    }

    fun updateBarcodeInfo(
        numResults: List<String>,
        typeResults: List<String>,
        dateResults: List<String>,
        resultsSize: String?,
        image: Bitmap? = null
    ) {

        scannedBarcodesResultList.clear()
        scannedBarcodesTypesList.clear()
        scannedBarcodesDateList.clear()

        binding.resultsSize.text = resultsSize + " results found"
        scannedBarcodesResultList.addAll(numResults)
        scannedBarcodesTypesList.addAll(typeResults)
        scannedBarcodesDateList.addAll(dateResults)
        binding.textBarcodeNumResult.text = scannedBarcodesResultList.last()
        binding.textBarcodeTypeResult.text = scannedBarcodesTypesList.last()
        binding.imageView.setImageBitmap(image)
    }

    companion object {
        fun newInstance(numResult: List<String>, typeResult : List<String>, dateResult : List<String>, image : Bitmap? = null,
                        resultsSize : String? = null): ResultBottomDialogFragment {
            val fragment = ResultBottomDialogFragment()
            val args = Bundle()
            args.putString("numResult", numResult[0])
            args.putString("typeResult", typeResult[0])
            args.putString("resultsSize", resultsSize)
            args.putParcelable("bitmapImage", image)
            args.putStringArrayList("resultsList", ArrayList(numResult))
            args.putStringArrayList("resultsTypes", ArrayList(typeResult))
            args.putStringArrayList("dateResultsList", ArrayList(dateResult))

            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentResultBottomDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setDimAmount(0.2f)
        dialog?.setCanceledOnTouchOutside(false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BottomSheetStateListener) {
            stateListener = context
        }
    }

    override fun onStart() {
        super.onStart()
        view?.let { v ->
            val sheetBehavior = BottomSheetBehavior.from(v.parent as View)
            bottomSheetBehavior = sheetBehavior
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            bottomSheetBehavior.peekHeight = 1200
            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            var continiousMode = prefs.getBoolean(getString(R.string.key_continuous_scaning), false)
            if(!continiousMode) {
                binding.layoutTapAnywhere.postDelayed({
                    binding.layoutTapAnywhere.visibility = View.VISIBLE
                }, 200)
            }
        }
    }



    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

           val numResult = arguments?.getString("numResult")
           val typeResult = arguments?.getString("typeResult")
        val resultsSize = arguments?.getString("resultsSize")
        val resultsList = arguments?.getStringArrayList("resultsList")?.toMutableList()
        val typesList = arguments?.getStringArrayList("resultsTypes")?.toMutableList()
        val dateList = arguments?.getStringArrayList("dateResultsList")?.toMutableList()
        val image = arguments?.getParcelable<Bitmap>("bitmapImage")

        updateSearchEngine()

        binding.bottomDialogLayoutGrid.columnCount = 3

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        var autoSendWebhook = prefs.getBoolean(getString(R.string.key_webhook_autosend), false)
        val webHookFeedBack = prefs.getBoolean(getString(R.string.key_webhook_feedback), false)
        var webHookEncodeData = prefs.getBoolean(getString(R.string.key_webhook_encode_data), false)
        var enabledWebhook = prefs.getBoolean(getString(R.string.key_enable_webhook), true)
        var enabledSearchWeb = prefs.getBoolean(getString(R.string.key_enable_searchweb), true)



        binding.imageView.setImageBitmap(image)

        val keyWebHook = sharedPreferences.getString(getString(R.string.key_secret_word_webhook), "")

        val urlWebHook = sharedPreferences.getString(getString(R.string.key_url_webhook), "")

        var endPointUrl = extractEndpointFromUrl(urlWebHook!!)
//        sharedPreferenceRegisterListener()
        binding.bottomDialogLayoutGrid.removeAllViews()

        if(enabledWebhook && enabledSearchWeb) {
            binding.bottomDialogLayoutGrid.columnCount = 2
            addButtonToGridLayout(binding.bottomDialogLayoutGrid, binding.btnCopyValue)
            addButtonToGridLayout(binding.bottomDialogLayoutGrid, binding.btnCsvDialog)
            addButtonToGridLayout(binding.bottomDialogLayoutGrid, binding.btnWebHook)
            addButtonToGridLayout(binding.bottomDialogLayoutGrid, binding.btnSearchWeb)
        } else if (enabledWebhook && !enabledSearchWeb) {
            binding.bottomDialogLayoutGrid.columnCount = 3
            addButtonToGridLayout(binding.bottomDialogLayoutGrid, binding.btnCopyValue)
            addButtonToGridLayout(binding.bottomDialogLayoutGrid, binding.btnCsvDialog)
            addButtonToGridLayout(binding.bottomDialogLayoutGrid, binding.btnWebHook)
        } else if(!enabledWebhook && enabledSearchWeb) {
            binding.bottomDialogLayoutGrid.columnCount = 3
            addButtonToGridLayout(binding.bottomDialogLayoutGrid, binding.btnCopyValue)
            addButtonToGridLayout(binding.bottomDialogLayoutGrid, binding.btnCsvDialog)
            addButtonToGridLayout(binding.bottomDialogLayoutGrid, binding.btnSearchWeb)
        } else {
            binding.bottomDialogLayoutGrid.columnCount = 2
            addButtonToGridLayout(binding.bottomDialogLayoutGrid, binding.btnCopyValue)
            addButtonToGridLayout(binding.bottomDialogLayoutGrid, binding.btnCsvDialog)
        }





        scannedBarcodesResultList.addAll(resultsList!!)
        scannedBarcodesTypesList.addAll(typesList!!)
        scannedBarcodesDateList.addAll(dateList!!)


        if(resultsSize != null) {
            binding.resultsSize.text = resultsSize + " results found"
        } else {
            binding.resultsSize.text = ""
        }

        if(image == null) {
            binding.imageView.visibility = View.GONE
        }

        val parentView = view.parent as View
        parentView.viewTreeObserver.addOnGlobalLayoutListener {
            bottomSheet = parentView.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                bottomSheetBehavior = BottomSheetBehavior.from(it)
            }
            bottomSheet!!.setBackgroundColor(Color.TRANSPARENT)
            bottomSheetBehavior.isDraggable = false
            if(bottomSheetBehavior.peekHeight == 0) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

                parentView.isClickable = true
            }
        }

        binding.layoutBottomSheet.setBackgroundResource(R.drawable.bottomsheet_rounded_bg)

        binding.btnTapAnyhere.setOnClickListener{
            stateListener?.onStartScanningClicked()
            if(bottomSheetBehavior.peekHeight == 1200) {
                updatePeekHeight(1200, 0, bottomSheetBehavior)
                binding.layoutTapAnywhere.visibility = View.INVISIBLE
            }
        }

        binding.layoutTapAnywhere.setOnClickListener{
            stateListener?.onStartScanningClicked()
            if(bottomSheetBehavior.peekHeight == 1200) {
                updatePeekHeight(1200, 0, bottomSheetBehavior)
                binding.layoutTapAnywhere.visibility = View.INVISIBLE
            }
        }

        (dialog as? BottomSheetDialog)?.window?.findViewById<View>(com.google.android.material.R.id.touch_outside)?.setOnClickListener {
            stateListener?.onStartScanningClicked()
            if(bottomSheetBehavior.peekHeight == 1200) {
                updatePeekHeight(1200, 0, bottomSheetBehavior)
                binding.layoutTapAnywhere.visibility = View.INVISIBLE

            }
        }

        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
               stateListener?.onStartScanningClicked()
                if(bottomSheetBehavior.peekHeight == 1200) {
                    updatePeekHeight(1200, 0, bottomSheetBehavior)
                    binding.layoutTapAnywhere.visibility = View.INVISIBLE

                }
                true
            } else {
                false
            }
        }

        binding.textBarcodeNumResult.text = scannedBarcodesResultList.last()
        binding.textBarcodeTypeResult.text = scannedBarcodesTypesList.last()

        val uri = Uri.parse(urlWebHook)
        val baseUrl = "${uri.scheme}://${uri.host}/"

        val repository = BarcodeDataRepository()
        val viewModelFactory = BarcodeDataViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory).get(BarcodeDataViewModel::class.java)

        if (autoSendWebhook && enabledWebhook) {

            if (!NetworkUtils.isInternetAvailable(requireContext())) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_network_error_autosend),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                if(!urlWebHook.isNullOrBlank()) {
                    RetrofitIInstance.rebuild(baseUrl)
                    val secretWord = keyWebHook
                    val timestamp = generate10BitTimestamp()
                    val securityHash = generateMD5Hash(timestamp, secretWord!!)

                    val jsonArray = ArrayList<Map<String, String>>()
                    if(scannedBarcodesResultList.size == scannedBarcodesTypesList.size) {

//                        for(i in 0 until scannedBarcodesResultList.size) {
//                            val result = scannedBarcodesResultList[i]
//                            val symbology = scannedBarcodesTypesList[i]
                        val result = scannedBarcodesResultList.last()
                        val symbology = scannedBarcodesTypesList.last()
                            val encodedResult = encodeStringToBase64(result)
                            val encodedSymbology = encodeStringToBase64(symbology)

                            val jsonData = mapOf(
                                getString(R.string.webhook_symobology_title) to if(webHookEncodeData) encodedSymbology else symbology,
                                getString(R.string.webhook_value_title) to if(webHookEncodeData) encodedResult else result,
                                getString(R.string.webhook_date_title) to timestamp,
                                "encoded" to if(webHookEncodeData) "true" else "false"
                            )
                            jsonArray.add(jsonData)
//                        }
                    }

                    val barcodeData = BarcodeScanedData(timestamp, securityHash, jsonArray)

                    viewModel.createPost(endPointUrl, barcodeData)
                }
            }
        }

        binding.btnCopyValue.setOnClickListener {
            copyScannedBarcodes()
            Toast.makeText(requireContext(), "Values was copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

        binding.btnWebHook.setOnClickListener {

            lifecycleScope.launch {

                if (urlWebHook.isNullOrBlank()) {
                    var notConfiguredWebHookDialog = NotConfiguredWebHookDialog()
                    notConfiguredWebHookDialog.show(requireFragmentManager(), "NotConfiguredWebHookDialog")

                } else {

                    if (!NetworkUtils.isInternetAvailable(requireContext())) {
                        dismiss()
                        materialDialogError(
                            getString(R.string.material_dialog_server_eror_title),
                            getString(R.string.material_dialog_network_error)
                        )
                    } else {
                        RetrofitIInstance.rebuild(baseUrl)
                        val secretWord = keyWebHook
                        val timestamp = generate10BitTimestamp()
                        val securityHash = generateMD5Hash(timestamp, secretWord!!)

                            val jsonArray = ArrayList<Map<String, String>>()
                            if(scannedBarcodesResultList.size == scannedBarcodesTypesList.size) {

                                for(i in 0 until scannedBarcodesResultList.size) {
                                    val result = scannedBarcodesResultList[i]
                                    val symbology = scannedBarcodesTypesList[i]
                                    val encodedResult = encodeStringToBase64(result)
                                    val encodedSymbology = encodeStringToBase64(symbology)

                                    val jsonData = mapOf(
                                        getString(R.string.webhook_symobology_title) to if(webHookEncodeData) encodedSymbology else symbology,
                                        getString(R.string.webhook_value_title) to if(webHookEncodeData) encodedResult else result,
                                        getString(R.string.webhook_date_title) to timestamp,
                                        "encoded" to if(webHookEncodeData) "true" else "false"
                                    )
                                    jsonArray.add(jsonData)
                                }
                            }

                            val barcodeData = BarcodeScanedData(timestamp, securityHash, jsonArray)

                            viewModel.createPost(endPointUrl, barcodeData)


                        viewModel.barcodeDataResponse.observe(
                            viewLifecycleOwner,
                            Observer { response ->
                                if (response.isSuccessful) {
                                    Toast.makeText(
                                        requireContext(),
                                        "You data was sent to endpoint",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    if (webHookFeedBack) {
                                            materialDialogError(
                                                "Server error", "Response status code was unacceptable: ${
                                                            response.code().toString()
                                                        }."
                                            )

                                    }

                                }
                            })
                    }
                }
            }
        }



        binding.btnCsvDialog.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    writeStoragePermission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(writeStoragePermission),
                    REQUEST_CODE
                )
            } else {
                saveToCSV(scannedBarcodesTypesList, scannedBarcodesResultList, scannedBarcodesDateList)
            }
        }
    }

    private fun addButtonToGridLayout(gridLayout: GridLayout, button: MaterialButton) {

        val params = GridLayout.LayoutParams()
        params.width = 0
        params.height = GridLayout.LayoutParams.WRAP_CONTENT
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.setMargins(8, 8, 8, 8)
        gridLayout.addView(button, params)
    }

    private fun updatePeekHeight(originalHeight: Int, updatedHeight: Int, behavior : BottomSheetBehavior<*>) {
        ValueAnimator.ofInt(originalHeight, updatedHeight).apply {
            addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Int
                behavior!!.peekHeight = value
            }
            if(updatedHeight == 1200) {
                val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                var continiousMode = prefs.getBoolean(getString(R.string.key_continuous_scaning), false)
                if(!continiousMode) {
                    binding.layoutTapAnywhere.postDelayed({
                        binding.layoutTapAnywhere.visibility = View.VISIBLE
                    }, 200)
                }
            }
            duration = 500 //
            start()
        }
    }
        private fun saveToCSV(barcodeList: MutableList<String>, typeList: MutableList<String>, dateList : MutableList<String>) {
            val fileName = "ScannedBarcode.csv"

            try {
                val cacheDir = File(requireContext().cacheDir, "csv")
                cacheDir.mkdirs()
                val file = File(cacheDir, fileName)
                val fileWriter = FileWriter(file)

                if (barcodeList.size == typeList.size) {
                    for (i in barcodeList.indices) {
                        fileWriter.append("${barcodeList[i]},${typeList[i]},${dateList[i]}")
                        fileWriter.append('\n')
                    }
                } else {
                    throw IllegalArgumentException("Lists must have the same size")
                }

                fileWriter.flush()
                fileWriter.close()

                shareCSV(file)
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error: CSV could not be saved.", Toast.LENGTH_SHORT)
                    .show()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error: Lists must have the same size", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        private fun shareCSV(file: File) {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().applicationContext.packageName + ".provider",
                file
            )
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "text/csv"
            }
            sendIntent.setPackage("com.google.android.gm")
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(sendIntent)
        }



    private fun updateSearchEngine() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val resultsList = arguments?.getStringArrayList("resultsList")?.toMutableList()
        val searchEngineWeb = prefs.getString(getString(R.string.key_result_searchEngine))
        val numResult = resultsList!!.last()
        when (searchEngineWeb) {

            "Google" ->  binding.btnSearchWeb.setOnClickListener {
                if (CommonUtil.isTextURL(numResult)) {
                    CommonUtil.openURLInBrowser(numResult, requireActivity())
                } else {
                    var encodedURL = URLEncoder.encode(numResult, "UTF-8")
                    CommonUtil.openSearchInBrowser("https://www.google.com/search?q=",encodedURL, requireActivity() )
                }
            }
            "Yahoo" -> binding.btnSearchWeb.setOnClickListener {
                if (CommonUtil.isTextURL(numResult)) {
                    CommonUtil.openURLInBrowser(numResult, requireActivity())
                } else {
                    var encodedURL = URLEncoder.encode(numResult, "UTF-8")
                    CommonUtil.openSearchInBrowser(
                        "https://search.yahoo.com/search?p=",
                        encodedURL,
                        requireActivity()
                    )
                }
            }
            "DuckDuckGo" -> binding.btnSearchWeb.setOnClickListener {
                if (CommonUtil.isTextURL(numResult)) {
                    CommonUtil.openURLInBrowser(numResult, requireActivity())
                } else {
                    var encodedURL = URLEncoder.encode(numResult, "UTF-8")
                    CommonUtil.openSearchInBrowser(
                        "https://duckduckgo.com/?q=",
                        encodedURL,
                        requireActivity()
                    )
                }
            }
            "Yandex" -> binding.btnSearchWeb.setOnClickListener {
                if (CommonUtil.isTextURL(numResult)) {
                    CommonUtil.openURLInBrowser(numResult, requireActivity())
                } else {
                    var encodedURL = URLEncoder.encode(numResult, "UTF-8")
                    CommonUtil.openSearchInBrowser(
                        "https://yandex.com/search/?text=",
                        encodedURL,
                        requireActivity()
                    )
                }
            }
            "Bing" -> binding.btnSearchWeb.setOnClickListener {
                if (CommonUtil.isTextURL(numResult)) {
                    CommonUtil.openURLInBrowser(numResult, requireActivity())
                } else {
                    var encodedURL = URLEncoder.encode(numResult, "UTF-8")
                    CommonUtil.openSearchInBrowser(
                        "https://www.bing.com/search?q=",
                        encodedURL,
                        requireActivity()
                    )
                }
            }
            "Brave" -> binding.btnSearchWeb.setOnClickListener {
                if (CommonUtil.isTextURL(numResult)) {
                    CommonUtil.openURLInBrowser(numResult, requireActivity())
                } else {
                    var encodedURL = URLEncoder.encode(numResult, "UTF-8")
                    CommonUtil.openSearchInBrowser(
                        "https://search.brave.com/search?q=",
                        encodedURL,
                        requireActivity()
                    )
                }
            }
        }
    }

    private fun copyBarcodeResultText(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = android.content.ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun copyScannedBarcodes() {
        val textToCopy = scannedBarcodesResultList.joinToString(separator = ", ")
        copyBarcodeResultText(textToCopy)
    }

    private fun generate10BitTimestamp(): String {
        val currentTimestamp = System.currentTimeMillis()
        val timestamp10Bit = currentTimestamp / 1000

        return timestamp10Bit.toString()
    }

    private fun generateMD5Hash(timestamp: String, secretWord: String): String {
        val input = timestamp + secretWord
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun materialDialogError(title : String, message : String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("Continue") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun encodeStringToBase64(stringToEncode: String): String {
        val data = stringToEncode.toByteArray(Charsets.UTF_8)
        return Base64.encodeToString(data, Base64.DEFAULT)
    }

    private fun extractEndpointFromUrl(url: String): String {
        val trimmedUrl = url.trimEnd('/')
        val uriParts = trimmedUrl.split('/')
        return uriParts.last()
    }



}