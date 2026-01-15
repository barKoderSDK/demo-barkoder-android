package com.barkoder.demoscanner.fragments

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.barkoder.demoscanner.MainActivity
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.ScannerActivity
import com.barkoder.demoscanner.adapters.MrzInfoAdapter
import com.barkoder.demoscanner.adapters.SessionScanAdapter
import com.barkoder.demoscanner.api.RetrofitIInstance
import com.barkoder.demoscanner.databinding.FragmentResultBottomDialogBinding
import com.barkoder.demoscanner.models.BarcodeScanedData
import com.barkoder.demoscanner.models.MrzItem
import com.barkoder.demoscanner.models.SessionScan
import com.barkoder.demoscanner.repositories.BarcodeDataRepository
import com.barkoder.demoscanner.utils.CommonUtil
import com.barkoder.demoscanner.utils.NetworkUtils
import com.barkoder.demoscanner.utils.getString
import com.barkoder.demoscanner.viewmodels.BarcodeDataViewModel
import com.barkoder.demoscanner.viewmodels.BarcodeDataViewModelFactory
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.Serializable
import java.lang.ref.WeakReference
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ResultBottomDialogFragment : BottomSheetDialogFragment(), SessionScanAdapter.OnSessionScanItemClickListener {

    private lateinit var viewModel : BarcodeDataViewModel
    private lateinit var sharedViewModel: com.barkoder.demoscanner.viewmodels.ScanResultSharedViewModel

    private var firstName : String? = null
    private var lastName : String? = null
    private var documentNumber : String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SessionScanAdapter
    private var expandedBottomSheet = false
    var sessionScanCheck : MutableList<SessionScan> = mutableListOf()

    private var bottomSheetHeightModeOne = false
    private lateinit var csvSaveLauncher: ActivityResultLauncher<String>
    private lateinit var sharedPreferences : SharedPreferences

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    var bottomSheet: View? = null

    private val scannedBarcodesResultList: MutableList<String> = mutableListOf()
    private val resultsFromLastFrame: MutableList<String> = mutableListOf()
    private val scannedBarcodesTypesList:  MutableList<String> = mutableListOf()
    private val scannedBarcodesDateList : MutableList<String> = mutableListOf()

    private var _binding: FragmentResultBottomDialogBinding? = null
    private val binding get() = _binding!!


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

    fun getPeekHeightBehavior() : Int {
        val bottomSheetView = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView!!)
        return bottomSheetBehavior.peekHeight
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog)

    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as? ScannerActivity)?.isBottomSheetDialogShown = false
        
        // Clear ViewModel data to free memory and prevent leaks
        if (::sharedViewModel.isInitialized) {
            sharedViewModel.clearData()
        }
    }



    fun updateBarcodeInfo(
        numResults: List<String>,
        typeResults: List<String>,
        dateResults: List<String>,
        resultsSize: String?,
        image: Bitmap? = null,
        sessionScan : MutableList<SessionScan>
    ) {

        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        var lastResultsOnFrame = sharedPreferences.getInt("lastResultsOnFrame", 0)
        scannedBarcodesResultList.clear()
        scannedBarcodesTypesList.clear()
        scannedBarcodesDateList.clear()
        sessionScanCheck = sessionScan

        resultsFromLastFrame.addAll(numResults)
        scannedBarcodesResultList.addAll(numResults)
        scannedBarcodesTypesList.addAll(typeResults)
        scannedBarcodesDateList.addAll(dateResults)

        if(lastResultsOnFrame == 1) binding.resultsSize.text = lastResultsOnFrame.toString() + " result found (${resultsSize} total)" else binding.resultsSize.text = lastResultsOnFrame.toString() + " results found (${resultsSize} total)"

        if(sessionScan!!.size == 1) {
            val params = binding.constraintLayout4.layoutParams
            val newHeightInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                80f,
                resources.displayMetrics
            ).toInt()
            Log.d("bottomSHeet", "at 80f")
            params.height = newHeightInPixels
            binding.constraintLayout4.layoutParams = params

        }  else if (sessionScan!!.size == 2) {
            val params = binding.constraintLayout4.layoutParams
            val newHeightInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                160f,
                resources.displayMetrics
            ).toInt()
            Log.d("bottomSHeet", "at 160f")
            params.height = newHeightInPixels
            binding.constraintLayout4.layoutParams = params
        }
        else {
            val params = binding.constraintLayout4.layoutParams
            val newHeightInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                240f,
                resources.displayMetrics
            ).toInt()
            Log.d("bottomSHeet", "at 240f")
            params.height = newHeightInPixels
            binding.constraintLayout4.layoutParams = params
        }

        if(sessionScan!!.size < 2) {
            binding.layoutSearchBtn.visibility = View.GONE
            binding.layoutDetailsBtn.visibility = View.GONE
            binding.constraintLayout4.visibility = View.VISIBLE
            binding.layoutExpandBtn.visibility = View.GONE
            binding.layoutSearchBtn.visibility = View.VISIBLE
            binding.textBarcodeNumResult.visibility = View.GONE
            binding.textBarcodeTypeResult.visibility = View.GONE
        } else {

            binding.layoutSearchBtn.visibility = View.GONE
            binding.layoutDetailsBtn.visibility = View.GONE
            binding.constraintLayout4.visibility = View.VISIBLE
            binding.layoutExpandBtn.visibility = View.VISIBLE
            binding.textBarcodeNumResult.visibility = View.GONE
            binding.textBarcodeTypeResult.visibility = View.GONE

        }

        scannedBarcodesResultList.lastOrNull()?.let { lastResult ->
            extractDocumentRawText(lastResult)
            if (scannedBarcodesTypesList.lastOrNull() == "MRZ") {
                binding.textBarcodeNumResult.text = "$firstName $lastName \nDocument number: $documentNumber"
            } else {
                binding.textBarcodeNumResult.text = lastResult
            }
            binding.textBarcodeTypeResult.text = scannedBarcodesTypesList.lastOrNull() ?: ""
        }
        binding.textBarcodeNumResult.movementMethod = ScrollingMovementMethod()
        binding.imageView.setImageBitmap(image)
        recyclerView = requireView().findViewById(R.id.recayclerview_gallery)
        val layoutManager = LinearLayoutManager(context).apply {
            reverseLayout = true
            stackFromEnd = true
        }
        recyclerView.layoutManager = layoutManager
        adapter = SessionScanAdapter(scannedBarcodesResultList!!, scannedBarcodesTypesList!!, lastResultsOnFrame,  sessionScan!!, WeakReference(this))
        recyclerView.adapter = adapter


        Handler(Looper.getMainLooper()).postDelayed({
            adapter.notifyDataSetChanged()
        }, 50)

    }

    companion object {
        fun newInstance(numResult: MutableList<String>, typeResult : List<String>, dateResult : List<String>, image : Bitmap? = null,
                        resultsSize : String? = null, sessionScan : MutableList<SessionScan>): ResultBottomDialogFragment {
            val fragment = ResultBottomDialogFragment()
            val args = Bundle()
            // Only pass minimal data - large data comes from SharedViewModel to prevent TransactionTooLargeException
            args.putString("numResult", numResult.firstOrNull() ?: "")
            args.putString("typeResult", typeResult.firstOrNull() ?: "")
            args.putString("resultsSize", resultsSize)
            // DO NOT pass Bitmap or SessionScan - they cause TransactionTooLargeException!
            // These are retrieved from SharedViewModel instead

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
            dialog?.setCanceledOnTouchOutside(true)
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
        
        // Initialize SharedViewModel to get large data (prevents TransactionTooLargeException)
        sharedViewModel = ViewModelProvider(requireActivity()).get(com.barkoder.demoscanner.viewmodels.ScanResultSharedViewModel::class.java)
        
        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        var lastResultsOnFrame = sharedPreferences.getInt("lastResultsOnFrame", 0)
        var galleryScanMode = sharedPreferences.getBoolean("galleryScan", false)
        var arMode = sharedPreferences.getBoolean("arMode", false)
           val numResult = arguments?.getString("numResult")
           val typeResult = arguments?.getString("typeResult")
        val resultsSize = arguments?.getString("resultsSize")
        
        // Get large data from SharedViewModel instead of arguments to prevent TransactionTooLargeException
        val resultsList = sharedViewModel.resultsList
        val typesList = sharedViewModel.typesList
        val dateList = sharedViewModel.datesList
        val image = sharedViewModel.currentImage
        var sessionScan = sharedViewModel.sessionScans
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        csvSaveLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri != null) {
                // New compliance-safe way to save the file
                saveToCSVViaSAF(uri)
            } else {
                Toast.makeText(requireContext(), "Save cancelled.", Toast.LENGTH_SHORT).show()
            }
        }

        if(sessionScan!!.size == 1) {
            val params = binding.constraintLayout4.layoutParams
            val newHeightInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                80f,
                resources.displayMetrics
            ).toInt()
            Log.d("bottomSHeet onView", "at 80f")
            params.height = newHeightInPixels
            binding.constraintLayout4.layoutParams = params
        }  else if (sessionScan!!.size == 2) {
            val params = binding.constraintLayout4.layoutParams
            val newHeightInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                160f,
                resources.displayMetrics
            ).toInt()
            Log.d("bottomSHeet onView", "at 160f")
            params.height = newHeightInPixels
            binding.constraintLayout4.layoutParams = params
        } else {
            val params = binding.constraintLayout4.layoutParams
            val newHeightInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                240f,
                resources.displayMetrics
            ).toInt()
            Log.d("bottomSHeet onView", "at 240f")
            params.height = newHeightInPixels
            binding.constraintLayout4.layoutParams = params
        }

        if(resultsList!!.size < 3) {
            binding.layoutSearchBtn.visibility = View.GONE
            binding.layoutDetailsBtn.visibility = View.GONE
            binding.constraintLayout4.visibility = View.VISIBLE
            binding.layoutExpandBtn.visibility = View.GONE
            binding.layoutSearchBtn.visibility = View.VISIBLE
            binding.textBarcodeNumResult.visibility = View.GONE
            binding.textBarcodeTypeResult.visibility = View.GONE
        } else {
            binding.layoutSearchBtn.visibility = View.GONE
            binding.layoutDetailsBtn.visibility = View.GONE
            binding.constraintLayout4.visibility = View.VISIBLE
            binding.layoutExpandBtn.visibility = View.VISIBLE
            binding.textBarcodeNumResult.visibility = View.GONE
            binding.textBarcodeTypeResult.visibility = View.GONE

        }

        if(sessionScan.size == 1) {
            binding.layoutExpandBtn.visibility = View.GONE
            binding.layoutSearchBtn.visibility = View.VISIBLE
        }
        updateSearchEngine()

        updateCopyTerminator()

        val lastResult = resultsList?.lastOrNull()

        if (lastResult != null && CommonUtil.isTextURL(lastResult)) {
            binding.btnSearchWeb.setImageResource(R.drawable.ico_webhook)
            binding.txtOpenSearchButton.text = "Open"
        }



        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        var autoSendWebhook = prefs.getBoolean(getString(R.string.key_webhook_autosend), false)
        val webHookFeedBack = prefs.getBoolean(getString(R.string.key_webhook_feedback), false)
        var webHookEncodeData = prefs.getBoolean(getString(R.string.key_webhook_encode_data), false)
        var enabledWebhook = prefs.getBoolean(getString(R.string.key_enable_webhook), true)
        var enabledSearchWeb = prefs.getBoolean(getString(R.string.key_enable_searchweb), true)

//        binding.bottomDialogLayoutButtons.visibility = View.GONE
//        binding.resultsSize.visibility = View.GONE
        binding.imageView.visibility = View.GONE
//        binding.textBarcodeTypeResult.visibility = View.GONE




        recyclerView = requireView().findViewById(R.id.recayclerview_gallery)
        val layoutManager = LinearLayoutManager(context).apply {
            reverseLayout = true
            stackFromEnd = true
        }

        recyclerView.layoutManager = layoutManager
        adapter = SessionScanAdapter(resultsList!!, typesList!!, lastResultsOnFrame,  sessionScan!!, WeakReference(this))
        recyclerView.adapter = adapter

        binding.imageView.setImageBitmap(image)

        Handler(Looper.getMainLooper()).postDelayed({
            adapter.notifyDataSetChanged()
        }, 50)
        val keyWebHook = sharedPreferences.getString(getString(R.string.key_secret_word_webhook), "")

        val urlWebHook = sharedPreferences.getString(getString(R.string.key_url_webhook), "")

        var endPointUrl = extractEndpointFromUrl(urlWebHook!!)
//        sharedPreferenceRegisterListener()

        scannedBarcodesResultList.addAll(resultsList!!)
        scannedBarcodesTypesList.addAll(typesList!!)
        scannedBarcodesDateList.addAll(dateList!!)
        resultsFromLastFrame.addAll(resultsList)


        if(resultsSize != null) {
            if(galleryScanMode || arMode) {
                if(lastResultsOnFrame == 1) binding.resultsSize.text = lastResultsOnFrame.toString() + " result found" else binding.resultsSize.text = lastResultsOnFrame.toString() + " results found"
            } else {
                if(lastResultsOnFrame == 1) binding.resultsSize.text = lastResultsOnFrame.toString() + " result found (${resultsSize} total)" else binding.resultsSize.text = lastResultsOnFrame.toString() + " results found (${resultsSize} total)"
            }

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

//            if(!expandedBottomSheet) {
//                if(sessionScan!!.size == 1) {
//                    val params = binding.constraintLayout4.layoutParams
//                    val newHeightInPixels = TypedValue.applyDimension(
//                        TypedValue.COMPLEX_UNIT_DIP,
//                        80f,
//                        resources.displayMetrics
//                    ).toInt()
//                    Log.d("bottomSHeet created", "at 80f")
//                    params.height = newHeightInPixels
//                    binding.constraintLayout4.layoutParams = params
//                }
//                else if (sessionScan!!.size == 2) {
//                    val params = binding.constraintLayout4.layoutParams
//                    val newHeightInPixels = TypedValue.applyDimension(
//                        TypedValue.COMPLEX_UNIT_DIP,
//                        160f,
//                        resources.displayMetrics
//                    ).toInt()
//                    Log.d("bottomSHeet created", "at 160f")
//                    params.height = newHeightInPixels
//                    binding.constraintLayout4.layoutParams = params
//                }
//            }


        }

        binding.layoutBottomSheet.setBackgroundResource(R.drawable.bottomsheet_rounded_bg)

        binding.btnTapAnyhere.setOnClickListener{
            stateListener?.onStartScanningClicked()
            if(bottomSheetBehavior.peekHeight == 1200) {

                (activity as? MainActivity)?.hideImageView()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                updatePeekHeightInstant(1200, 0, bottomSheetBehavior)
                binding.layoutTapAnywhere.visibility = View.INVISIBLE
            }
        }

        binding.layoutTapAnywhere.setOnClickListener{
            stateListener?.onStartScanningClicked()

            if(bottomSheetBehavior.peekHeight == 1200) {
                (activity as? MainActivity)?.hideImageView()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                updatePeekHeightInstant(1200, 0, bottomSheetBehavior)
                binding.layoutTapAnywhere.visibility = View.INVISIBLE
            }
        }

        (dialog as? BottomSheetDialog)?.window?.findViewById<View>(com.google.android.material.R.id.touch_outside)?.setOnClickListener {
            if(!expandedBottomSheet){
                stateListener?.onStartScanningClicked()
            }

            if(bottomSheetBehavior.peekHeight == 1200) {
                (activity as? MainActivity)?.hideImageView()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                updatePeekHeightInstant(1200, 0, bottomSheetBehavior)
                binding.layoutTapAnywhere.visibility = View.INVISIBLE

            }
        }

        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                (activity as? MainActivity)?.hideImageView()
               stateListener?.onStartScanningClicked()
                if(bottomSheetBehavior.peekHeight == 1200) {
                    updatePeekHeight(1200, 0, bottomSheetBehavior)
                    binding.layoutTapAnywhere.visibility = View.INVISIBLE

                }
                dismiss()
                true
            } else {
                false
            }
        }


        scannedBarcodesResultList.lastOrNull()?.let { lastResult ->
            extractDocumentRawText(lastResult)
            if (scannedBarcodesTypesList.lastOrNull() == "MRZ") {
                binding.textBarcodeNumResult.text = "$firstName $lastName \nDocument number: $documentNumber"
            } else {
                binding.textBarcodeNumResult.text = lastResult
            }
            binding.textBarcodeTypeResult.text = scannedBarcodesTypesList.lastOrNull() ?: ""
        }
        binding.textBarcodeNumResult.movementMethod = ScrollingMovementMethod()

        val uri = Uri.parse(urlWebHook)
        val baseUrl = "${uri.scheme}://${uri.host}/"

        val repository = BarcodeDataRepository()
        val viewModelFactory = BarcodeDataViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory).get(BarcodeDataViewModel::class.java)

//        if (autoSendWebhook && enabledWebhook) {
//
//            if (!NetworkUtils.isInternetAvailable(requireContext())) {
//                Toast.makeText(
//                    requireContext(),
//                    getString(R.string.toast_network_error_autosend),
//                    Toast.LENGTH_SHORT
//                ).show()
//            } else {
//                if (!urlWebHook.isNullOrBlank()) {
//                    RetrofitIInstance.rebuild(baseUrl)
//
//                    val secretWord = keyWebHook.orEmpty()
//                    val timestamp  = generate10BitTimestamp()
//                    val securityHash = generateMD5Hash(timestamp, secretWord)
//
//                    val jsonArray = ArrayList<Map<String, String>>()
//
//                    // use ALL items, not just last()
////                    val count = minOf(scannedBarcodesResultList.size, scannedBarcodesTypesList.size)
//                    for (i in 0 until resultsFromLastFrame.size) {
//                        val result = resultsFromLastFrame[i]
//                        val symbology = scannedBarcodesTypesList[i]
//
//                        val encodedResult    = if (webHookEncodeData) encodeStringToBase64(result) else result
//                        val encodedSymbology = if (webHookEncodeData) encodeStringToBase64(symbology) else symbology
//
//                        val jsonData = mapOf(
//                            "base64" to if (webHookEncodeData) "true" else "false",   // stays string since Map<String,String>
//                            getString(R.string.webhook_value_title) to encodedResult,
//                            getString(R.string.webhook_date_title)  to timestamp,
//                            getString(R.string.webhook_symobology_title) to encodedSymbology
//                        )
//                        jsonArray.add(jsonData)
//                    }
//                    resultsFromLastFrame.clear()
//                    val payload = BarcodeScanedData(timestamp, securityHash, jsonArray)
//                    viewModel.createPost(endPointUrl, payload)
//                }
//            }
//        }

//        binding.btnWebHook.setOnClickListener {
//
//            lifecycleScope.launch {
//
//                if (urlWebHook.isNullOrBlank()) {
//                    var notConfiguredWebHookDialog = NotConfiguredWebHookDialog()
//                    notConfiguredWebHookDialog.show(requireFragmentManager(), "NotConfiguredWebHookDialog")
//
//                } else {
//
//                    if (!NetworkUtils.isInternetAvailable(requireContext())) {
//                        dismiss()
//                        materialDialogError(
//                            getString(R.string.material_dialog_server_eror_title),
//                            getString(R.string.material_dialog_network_error)
//                        )
//                    } else {
//                        RetrofitIInstance.rebuild(baseUrl)
//                        val secretWord = keyWebHook
//                        val timestamp = generate10BitTimestamp()
//                        val securityHash = generateMD5Hash(timestamp, secretWord!!)
//
//                            val jsonArray = ArrayList<Map<String, String>>()
//                            if(scannedBarcodesResultList.size == scannedBarcodesTypesList.size) {
//
//                                for(i in 0 until scannedBarcodesResultList.size) {
//                                    val result = scannedBarcodesResultList[i]
//                                    val symbology = scannedBarcodesTypesList[i]
//                                    val encodedResult = encodeStringToBase64(result)
//                                    val encodedSymbology = encodeStringToBase64(symbology)
//
//                                    val jsonData = mapOf(
//                                        getString(R.string.webhook_symobology_title) to if(webHookEncodeData) encodedSymbology else symbology,
//                                        getString(R.string.webhook_value_title) to if(webHookEncodeData) encodedResult else result,
//                                        getString(R.string.webhook_date_title) to timestamp,
//                                        "encoded" to if(webHookEncodeData) "true" else "false"
//                                    )
//                                    jsonArray.add(jsonData)
//                                }
//                            }
//
//                            val barcodeData = BarcodeScanedData(timestamp, securityHash, jsonArray)
//
//                            viewModel.createPost(endPointUrl, barcodeData)
//
//
//                        viewModel.barcodeDataResponse.observe(
//                            viewLifecycleOwner,
//                            Observer { response ->
//                                if (response.isSuccessful) {
//                                    Toast.makeText(
//                                        requireContext(),
//                                        "You data was sent to endpoint",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
//                                } else {
//                                    if (webHookFeedBack) {
//                                            materialDialogError(
//                                                "Server error", "Response status code was unacceptable: ${
//                                                            response.code().toString()
//                                                        }."
//                                            )
//
//                                    }
//
//                                }
//                            })
//                    }
//                }
//            }
//        }




        binding.btnCsvDialog.setOnClickListener {
            // ✅ Compliant: Start the system's "Create Document" intent
            val defaultFileName = "scans_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            csvSaveLauncher.launch(defaultFileName)
        }

        binding.btnExpand.setOnClickListener {
            expandedBottomSheet = !expandedBottomSheet

            if(expandedBottomSheet) {
                binding.layoutTapAnywhere.visibility = View.GONE
                bottomSheet?.let {
                    bottomSheetBehavior = BottomSheetBehavior.from(it)
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    bottomSheetBehavior.peekHeight = 6200
                }

                val params = binding.constraintLayout4.layoutParams
                val newHeightInPixels = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    550f,
                    resources.displayMetrics
                ).toInt()

                params.height = newHeightInPixels
                binding.txtBtnExpand.text = "Collapse"
                binding.constraintLayout4.layoutParams = params

                val btnColapseColor = ContextCompat.getColor(requireContext(), R.color.btnColapseColor)
                val btnColapseBackground = ContextCompat.getDrawable(requireContext(), R.drawable.btn_expand_rounded_corners_background)
                binding.layoutExpandBtn.setBackgroundDrawable(btnColapseBackground)
                binding.btnExpand.backgroundTintList = ColorStateList.valueOf(btnColapseColor)

            } else {
                binding.layoutTapAnywhere.visibility = View.VISIBLE
                bottomSheet?.let {
                    bottomSheetBehavior = BottomSheetBehavior.from(it)
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    bottomSheetBehavior.peekHeight = 1200
                }

                val params = binding.constraintLayout4.layoutParams
                val newHeightInPixels = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    225f,
                    resources.displayMetrics
                ).toInt()
                binding.txtBtnExpand.text = "Expand"
                params.height = newHeightInPixels

                binding.constraintLayout4.layoutParams = params

                val btnColapseColor = ContextCompat.getColor(requireContext(), R.color.white)
                val btnColapseBackground = ContextCompat.getDrawable(requireContext(), R.drawable.btn_colapse_rounded_corners_background)
                binding.layoutExpandBtn.setBackgroundDrawable(btnColapseBackground)
                binding.btnExpand.backgroundTintList = ColorStateList.valueOf(btnColapseColor)
            }

        }

    }



    private fun saveToCSVViaSAF(uri: Uri) {
        try {
            // Get the CSV content
            val csvContent = generateCSVContent(
                scannedBarcodesTypesList,
                scannedBarcodesResultList,
                scannedBarcodesDateList
            )

            // Write to the URI using ContentResolver.openOutputStream()
            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csvContent.toByteArray())
            }
            Toast.makeText(requireContext(), "Scans saved to CSV successfully!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("CSV_SAVE", "Failed to save CSV via SAF: ${e.message}", e)
            Toast.makeText(requireContext(), "Failed to save CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
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

    private fun updatePeekHeightInstant(
        originalHeight: Int,
        updatedHeight: Int,
        behavior: BottomSheetBehavior<*>
    ) {
        behavior.peekHeight = updatedHeight  // ✅ Instant change

        if (updatedHeight == 1200) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val continuousMode = prefs.getBoolean(getString(R.string.key_continuous_scaning), false)

            if (!continuousMode) {
                binding.layoutTapAnywhere.post {
                    binding.layoutTapAnywhere.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun generateCSVContent(types: List<String>, results: List<String>, dates: List<String>): String {
        val csv = StringBuilder()
        csv.append("Date,Barcode Type,Barcode Result\n")
        for (i in types.indices) {
            // Using quotes to handle potential commas or special characters in data
            csv.append("${dates[i]},\"${types[i]}\",\"${results[i]}\"\n")
        }
        return csv.toString()
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


    private fun updateCopyTerminator() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val copyTerminator = prefs.getString(getString(R.string.key_result_copyTerminator), "Comma (,)")
        when(copyTerminator) {

            "Comma (,)" -> binding.btnCopyValue.setOnClickListener {
                copyScannedBarcodesWithComma()
                Toast.makeText(requireContext(), "Values was copied to clipboard!", Toast.LENGTH_SHORT).show()
            }

            "Semicolon (;)" -> binding.btnCopyValue.setOnClickListener {
                copyScannedBarcodesWithSemiColon()
                Toast.makeText(requireContext(), "Values was copied to clipboard!", Toast.LENGTH_SHORT).show()
            }

            "New line" -> binding.btnCopyValue.setOnClickListener {
                copyScannedBarcodesInNewLine()
                Toast.makeText(requireContext(), "Values was copied to clipboard!", Toast.LENGTH_SHORT).show()
            }

        }
    }



    private fun updateSearchEngine() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        // Get data from scannedBarcodesResultList (populated from SharedViewModel in onViewCreated)
        val numResult = if (scannedBarcodesResultList.isNotEmpty()) {
            scannedBarcodesResultList.last()
        } else {
            "" // Fallback to empty string if no results
        }
        val searchEngineWeb = prefs.getString(getString(R.string.key_result_searchEngine))
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


    private fun updateSearchEngineOnBarcodeDetails(btn : MaterialButton? = null, result : String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val searchEngineWeb = prefs.getString(getString(R.string.key_result_searchEngine))
        when (searchEngineWeb) {

            "Google" ->  btn?.setOnClickListener {
                if (CommonUtil.isTextURL(result)) {
                    CommonUtil.openURLInBrowser(result, requireActivity())
                } else {
                    var encodedURL = URLEncoder.encode(result, "UTF-8")
                    CommonUtil.openSearchInBrowser("https://www.google.com/search?q=",encodedURL, requireActivity() )
                }
            }
            "Yahoo" -> btn?.setOnClickListener {
                if (CommonUtil.isTextURL(result)) {
                    CommonUtil.openURLInBrowser(result, requireActivity())
                } else {
                    var encodedURL = URLEncoder.encode(result, "UTF-8")
                    CommonUtil.openSearchInBrowser(
                        "https://search.yahoo.com/search?p=",
                        encodedURL,
                        requireActivity()
                    )
                }
            }
            "DuckDuckGo" -> btn?.setOnClickListener {
                if (CommonUtil.isTextURL(result)) {
                    CommonUtil.openURLInBrowser(result, requireActivity())
                } else {
                    var encodedURL = URLEncoder.encode(result, "UTF-8")
                    CommonUtil.openSearchInBrowser(
                        "https://duckduckgo.com/?q=",
                        encodedURL,
                        requireActivity()
                    )
                }
            }
            "Yandex" -> btn?.setOnClickListener {
                if (CommonUtil.isTextURL(result)) {
                    CommonUtil.openURLInBrowser(result, requireActivity())
                } else {
                    var encodedURL = URLEncoder.encode(result, "UTF-8")
                    CommonUtil.openSearchInBrowser(
                        "https://yandex.com/search/?text=",
                        encodedURL,
                        requireActivity()
                    )
                }
            }
            "Bing" -> btn?.setOnClickListener {
                if (CommonUtil.isTextURL(result)) {
                    CommonUtil.openURLInBrowser(result, requireActivity())
                } else {
                    var encodedURL = URLEncoder.encode(result, "UTF-8")
                    CommonUtil.openSearchInBrowser(
                        "https://www.bing.com/search?q=",
                        encodedURL,
                        requireActivity()
                    )
                }
            }
            "Brave" -> btn?.setOnClickListener {
                if (CommonUtil.isTextURL(result)) {
                    CommonUtil.openURLInBrowser(result, requireActivity())
                } else {
                    var encodedURL = URLEncoder.encode(result, "UTF-8")
                    CommonUtil.openSearchInBrowser(
                        "https://search.brave.com/search?q=",
                        encodedURL,
                        requireActivity()
                    )
                }
            }
        }
    }





    private fun copyScannedBarcodesWithComma() {
        val textToCopy = scannedBarcodesResultList.joinToString(separator = ", ")
        CommonUtil.copyBarcodeResultText(requireContext(),textToCopy)
    }

    private fun copyScannedBarcodesInNewLine() {
        val textToCopy = scannedBarcodesResultList.joinToString(separator = "\n")
        CommonUtil.copyBarcodeResultText(requireContext(),textToCopy)
    }

    private fun copyScannedBarcodesWithSemiColon() {
        val textToCopy = scannedBarcodesResultList.joinToString(separator = "; ")
        CommonUtil.copyBarcodeResultText(requireContext(),textToCopy)
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

    fun extractDocumentRawText(rawData: String){

        // Split the raw string into lines
        val lines = rawData.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        // Iterate over each line to find the required information
        for (line in lines) {
            when {
                line.startsWith("first_name:") -> {
                    firstName = line.split("first_name:")[1].trim()
                }
                line.startsWith("last_name:") -> {
                    lastName = line.split("last_name:")[1].trim()
                }
                line.startsWith("document_number:") -> {
                    documentNumber = line.split("document_number:")[1].trim()
                }
                // You can add other fields if needed
            }
        }

    }

    override fun onSessionScanItemClick(item: SessionScan) {
            if(item.scanTypeName == "MRZ") {
              showFullScreenDialog(requireContext(), item.pictureBitmap, item.documentBitmap, item.signatureBitmap,item.mainBitmap,item.scanText)

            } else {
                showBarcodeDetailsDialog(
                    requireContext(),
                    item.thumbnailBitmap?.toString() ?: "", // If null, pass empty string
                    item.scanText,
                    item.scanTypeName,
                    item.formattedText,
                    item.formattedJsonText,
                    item.scannedTimesInARow,
                    item.sadlImageRawBase64
                )

            }


    }

    override fun onSessionScanItemLongClick(item: SessionScan, position: Int) {
       Log.d("qweqe", "asdsad")
    }


    fun formatDateString(inputDate: String?): String? {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val date: Date?
        try {
            date = inputFormat.parse(inputDate)
            return if (date != null) {
                outputFormat.format(date)
            } else {
                null // or handle parsing error
            }
        } catch (e: ParseException) {
            e.printStackTrace()
            return null // or handle parsing error
        }
    }



    public fun showFullScreenDialog(context: Context , picutreImage : String?, documentImage : String?, signatureImage : String?, mainImage : String?, results: String?) {
            // or use `this` if in an Activity
            val builder =
                AlertDialog.Builder(context, com.barkoder.R.style.FullScreenDialogStyle)
            // Inflate the custom layout
            val inflater = LayoutInflater.from(context)
            val dialogView = inflater.inflate(R.layout.custom_dialog_results, null)

        builder.setView(dialogView)
        builder.setCancelable(true)

        Log.d("resultseqewq212", results!!)

        val dialog = builder.create()

        val window = dialog.window
        if (window != null) {
            // Make the status bar visible
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            window.setWindowAnimations(R.style.DialogAnimationDetailsDialog)
            // Set the status bar background color to white
            window.statusBarColor = Color.WHITE

            // Make the icons dark (grey)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

            // Find the ImageView and set the bitmap image
            val dialogImageView =
                dialogView.findViewById<ImageView>(R.id.imageViewDialog)
            val firstNameUser = dialogView.findViewById<TextView>(R.id.firstNameUser)
            val recayclerViewMrzItem = dialogView.findViewById<RecyclerView>(R.id.mrzInfoRecayclerView)

        val textView5 =
            dialogView.findViewById<TextView>(R.id.textView5)

            val imageDocument =
                dialogView.findViewById<ImageView>(R.id.imageDocument)
            val imageMain =
                dialogView.findViewById<ImageView>(R.id.imageMain)
            val imagePicture =
                dialogView.findViewById<ImageView>(R.id.imagePicture)
            val imageSignature =
                dialogView.findViewById<ImageView>(R.id.imageSignature)

            val viewCardPicture = dialogView.findViewById<LinearLayout>(R.id.imagePictureLayout)
            val viewCardDocument = dialogView.findViewById<LinearLayout>(R.id.imageDocumentLayout)
            val viewCardSignature = dialogView.findViewById<LinearLayout>(R.id.imageSignatureLayout)
            val viewCardMain = dialogView.findViewById<LinearLayout>(R.id.imageMainLayout)

        textView5.text = "MRZ Data"
            // Split the raw string into lines
            val lines = results?.split("\n".toRegex())
                ?.dropLastWhile { it.isEmpty() }
                ?.toTypedArray()

            // Initialize variables for first name and last name
            var firstName: String? = null
            var lastName: String? = null
            var fullName: String? = null


            // Iterate over each line to find the required information
            for (line in lines!!) {
                if (line.startsWith("first_name:")) {
                    firstName = line.split("first_name:".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1].trim { it <= ' ' }
                } else if (line.startsWith("last_name:")) {
                    lastName =
                        line.split("last_name:".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim { it <= ' ' }
                }
            }

            if (firstName == null) firstName = ""
            if (lastName == null) lastName = ""



            fullName = "$firstName $lastName"
            firstNameUser.text = firstName + " " + lastName


        val mrzItems = mutableListOf<MrzItem>()

// Regex to match "label: value" patterns
        val pattern = Regex("""(\w+):\s*([^\n\r]+)""")

        pattern.findAll(results ?: "").forEach { matchResult ->
            val rawLabel = matchResult.groups[1]?.value ?: ""
            val rawValue = matchResult.groups[2]?.value ?: ""

            // Skip "first_name" or "last_name"
            if (rawLabel.equals("first_name", ignoreCase = true) ||
                rawLabel.equals("last_name", ignoreCase = true)) {
                return@forEach
            }

            val formattedLabel = rawLabel
                .replace("_", " ")
                .lowercase()
                .replaceFirstChar { it.titlecase() }

            mrzItems.add(MrzItem(formattedLabel, rawValue.trim()))
        }


// Attach to RecyclerView
        recayclerViewMrzItem.layoutManager = LinearLayoutManager(context)
        recayclerViewMrzItem.adapter = MrzInfoAdapter(mrzItems)



        var bitmapsArray =  mutableListOf<Pair<Bitmap, String>>()


            if (picutreImage != null) {
                // Set visibility and display the image
                CommonUtil.getBitmapFromInternalStorage(picutreImage)?.let { bitmapsArray.add(Pair(it, "picture")) }
                viewCardPicture.visibility = View.VISIBLE
                Glide.with(context)
                    .load(File(picutreImage))
                    .into(imagePicture)


                Glide.with(context)
                    .load(File(picutreImage))
                    .into(dialogImageView)


                // Set the OnClickListener only if pictureBitmap is not null
                viewCardPicture.setOnClickListener {
                    showFullScreenImage(requireContext(),  getBitmapFromInternalStorage(picutreImage))
                }
            } else {
                // Hide the view if the picture is null
                viewCardPicture.visibility = View.GONE
            }

            if (documentImage != null) {
                CommonUtil.getBitmapFromInternalStorage(documentImage)?.let { bitmapsArray.add(Pair(it, "document")) }
                viewCardDocument.visibility = View.VISIBLE
                Glide.with(context)
                    .load(File(documentImage))
                    .into(imageDocument)

                viewCardDocument.setOnClickListener {
                    showFullScreenImage(requireContext(),  getBitmapFromInternalStorage(documentImage))
                }
            } else {
                viewCardDocument.visibility = View.GONE
            }

            if (signatureImage != null) {
                CommonUtil.getBitmapFromInternalStorage(signatureImage)?.let { bitmapsArray.add(Pair(it, "signature")) }
                viewCardSignature.visibility = View.VISIBLE
                Glide.with(context)
                    .load(File(signatureImage))
                    .into(imageSignature)
                viewCardSignature.setOnClickListener {
                    showFullScreenImage(requireContext(),  getBitmapFromInternalStorage(signatureImage))
                }
            } else {
                viewCardSignature.visibility = View.GONE
            }

            if (mainImage != null) {
                CommonUtil.getBitmapFromInternalStorage(mainImage)?.let { bitmapsArray.add(Pair(it, "main")) }
                viewCardMain.visibility = View.VISIBLE
                Glide.with(context)
                    .load(File(mainImage))
                    .into(imageMain)
                viewCardMain.setOnClickListener {
                    showFullScreenImage(requireContext(),  getBitmapFromInternalStorage(mainImage))
                }
            } else {
                viewCardMain.visibility = View.GONE
            }

//            imageDocument.setImageBitmap(documentImage)
//            imageSignature.setImageBitmap(signatureBitmap)
//            imageMain.setImageBitmap(mainBitmap)

            val closeButton = dialogView.findViewById<ImageButton>(R.id.buttonClose)


        val btnCopy = dialogView.findViewById<MaterialButton>(R.id.btnCopy)
        val btnSearch = dialogView.findViewById<MaterialButton>(R.id.btnSearch)
        val btnPDF = dialogView.findViewById<MaterialButton>(R.id.btnPDF)


        btnPDF.setOnClickListener {
        //    CommonUtil.createPdf(requireContext(), bitmapsArray, "Full Name: $fullName\nNationality: $nationality\nDate of birth: $dateOfBirth\nDocument Number: $documentNumber\nIssuing country: $issuing_country\nDate of expiry $expirationDate")
        }

        updateSearchEngineOnBarcodeDetails(btnSearch, "$fullName")

        btnCopy.setOnClickListener {
            CommonUtil.copyBarcodeResultText(requireContext(), results)
            Toast.makeText(requireContext(), "Values was copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

            closeButton.setOnClickListener { dialog.dismiss() }

            dialog.setOnShowListener {
                val window = dialog.window
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            dialog.show()


        if(expandedBottomSheet) {
            binding.layoutTapAnywhere.visibility = View.GONE
            bottomSheet?.let {
                bottomSheetBehavior = BottomSheetBehavior.from(it)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                bottomSheetBehavior.peekHeight = 6200
            }

            val params = binding.constraintLayout4.layoutParams
            val newHeightInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                550f,
                resources.displayMetrics
            ).toInt()

            params.height = newHeightInPixels
            binding.txtBtnExpand.text = "Collapse"
            binding.constraintLayout4.layoutParams = params
        } else {
            binding.layoutTapAnywhere.visibility = View.VISIBLE
            bottomSheet?.let {
                bottomSheetBehavior = BottomSheetBehavior.from(it)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                bottomSheetBehavior.peekHeight = 1200
            }

//            val params = binding.constraintLayout4.layoutParams
//            val newHeightInPixels = TypedValue.applyDimension(
//                TypedValue.COMPLEX_UNIT_DIP,
//                225f,
//                resources.displayMetrics
//            ).toInt()
//            binding.txtBtnExpand.text = "Expand"
//            params.height = newHeightInPixels

//            binding.constraintLayout4.layoutParams = params
        }

        }

    @SuppressLint("MissingInflatedId")
    public fun showBarcodeDetailsDialog(context: Context, mainImage: String, result: String, typname: String, formattedTextValue : String,formattedTextJson : String, scannedTimes: Int, sadlImageRawBase64 : String) {
        val dialog = Dialog(requireContext(), com.barkoder.R.style.FullScreenDialogStyle)

        // Inflate the custom layout
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.custom_dialog_barcode_result, null)

        dialog.setContentView(dialogView)

        val window = dialog.window
        if (window != null) {
            // Make the status bar visible
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            window.setWindowAnimations(R.style.DialogAnimationDetailsDialog)
            // Set the status bar background color to white
            window.statusBarColor = Color.WHITE // Or ContextCompat.getColor(this, R.color.white)

            // Make the icons dark (grey)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        val barcodeValueText = dialogView.findViewById<TextView>(R.id.barcodeValueText)
        val barcodeTypeText = dialogView.findViewById<TextView>(R.id.barcodeTypeText)
        val barcodeBitmap = dialogView.findViewById<ImageView>(R.id.barcodeImage)
        val formattedText = dialogView.findViewById<TextView>(R.id.FormattedValueText)
        val formattedLayout = dialogView.findViewById<LinearLayout>(R.id.formattedTextLayout)
        val formattedTextJsonLayout = dialogView.findViewById<LinearLayout>(R.id.formattedTextJsonLayout)
        val formattedJsonValueText = dialogView.findViewById<TextView>(R.id.FormattedJsonValueText)
        val scannedTimesLayout = dialogView.findViewById<LinearLayout>(R.id.timesScannedLayout)
        val scannedTimesText = dialogView.findViewById<TextView>(R.id.timesScannedText)
        val sadlImage = dialogView.findViewById<ImageView>(R.id.sadlImage)
        val textCapturedMedia = dialogView.findViewById<TextView>(R.id.textCapturedMedia)
        val sadlImagesLayout = dialogView.findViewById<LinearLayout>(R.id.sadlImagesLayout)
        val buttonCopyJson = dialogView.findViewById<ImageButton>(R.id.buttonCopyJson)

        if(scannedTimes > 1) {
            scannedTimesLayout.visibility = View.VISIBLE
        } else {
            scannedTimesLayout.visibility = View.GONE
        }

        if (sadlImageRawBase64 != null && sadlImageRawBase64.length > 1) {
            try {
                val grayscalePixels: ByteArray = Base64.decode(sadlImageRawBase64, Base64.NO_WRAP)
                Log.d("DecodeDebug", "Decoded bytes: " + grayscalePixels.size)

                val width = 200
                val height = 250


                // OPTION 1: Try ARGB_8888 with grayscale values
                val argbPixels = IntArray(width * height)
                for (i in grayscalePixels.indices) {
                    val gray = grayscalePixels[i].toInt() and 0xFF // Convert to unsigned
                    argbPixels[i] = -0x1000000 or (gray shl 16) or (gray shl 8) or gray
                }

                val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                grayscaleBitmap.setPixels(argbPixels, 0, width, 0, 0, width, height)
                sadlImage.setImageBitmap(grayscaleBitmap)
            }  catch (e: Exception) {
                Log.e("ImageError", "Failed to decode grayscale pixels", e)
            }
        } else {
            textCapturedMedia.visibility = View.GONE
            sadlImagesLayout.visibility = View.GONE
        }

        if(formattedTextValue.length > 0) {
            formattedLayout.visibility = View.VISIBLE
        } else {
            formattedLayout.visibility = View.GONE
        }

        if(formattedTextJson.length > 0) {
            formattedTextJsonLayout.visibility = View.VISIBLE
        } else {
            formattedTextJsonLayout.visibility = View.GONE
        }


        // Ensure `mainImage` conversion works properly
        val bitmap = getBitmapFromInternalStorage(mainImage)
        if (bitmap != null) {
            barcodeBitmap.setImageBitmap(bitmap)

        } else {
            barcodeBitmap.setImageResource(R.drawable.container__2_)
            // Handle error case when bitmap is null
            Log.e("Error", "Bitmap is null for the given image string.")
        }

        val cleanedResult = CommonUtil.cleanResultString(result)
        barcodeValueText.text = cleanedResult
        Log.d("results", result)
        barcodeTypeText.text = typname
        formattedText.text = formattedTextValue
        formattedJsonValueText.text = prettyPrintJson(formattedTextJson)
        scannedTimesText.text = scannedTimes.toString()


        buttonCopyJson.setOnClickListener {
            // Get the JSON text
            val jsonText = prettyPrintJson(formattedTextJson) // your formatted JSON string

            // Copy to clipboard
            val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("JSON Data", jsonText)
            clipboard.setPrimaryClip(clip)

            // Show confirmation
            Toast.makeText(context, "JSON copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        val closeButton = dialogView.findViewById<ImageButton>(R.id.buttonClose)
        val btnCopy = dialogView.findViewById<MaterialButton>(R.id.btnCopy)
        val btnSearch = dialogView.findViewById<MaterialButton>(R.id.btnSearch)
        val btnPDF = dialogView.findViewById<MaterialButton>(R.id.btnPDF)
        val txtSearch = dialogView.findViewById<TextView>(R.id.txtSearch)
        var bitmapsArray = mutableListOf<Pair<Bitmap, String>>()
        val rowsLayout = dialogView.findViewById<LinearLayout>(R.id.rowsLayout)


        if (formattedTextValue.isNotEmpty()) {
            formattedTextValue.lines().forEach { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()

                    // **Skip adding the row if value is empty**
                    if (value.isEmpty()) {
                        return@forEach  // Skip this iteration
                    }

                    // Create parent horizontal LinearLayout
                    val rowLayout = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setBackgroundColor(Color.WHITE)
                        setPadding(15, 15, 15, 15)

                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.topMargin = 2
                        layoutParams = params
                    }

                    // Key TextView (left side)
                    val keyView = TextView(context).apply {
                        text = key
                        setTextColor(Color.parseColor("#666666"))
                        textSize = 14f
                        setPadding(15, 20, 15, 20)
                    }

                    // Value TextView (right side)
                    val valueView = TextView(context).apply {
                        text = value
                        setTextColor(Color.parseColor("#000000"))
                        textSize = 14f
                        gravity = Gravity.END
                        setPadding(15, 20, 15, 20)
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    }

                    formattedLayout.visibility = View.GONE

                    // Add views
                    rowLayout.addView(keyView)
                    rowLayout.addView(valueView)
                    rowsLayout.addView(rowLayout)

                    // Divider line
                    val divider = View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 2
                        )
                        setBackgroundColor(Color.parseColor("#FFF0EF"))
                    }
                    rowsLayout.addView(divider)
                }
            }
        }

        if (mainImage != null) {
            CommonUtil.getBitmapFromInternalStorage(mainImage)?.let { bitmapsArray.add(Pair(it,"barcode")) }
        }

//        btnPDF.setOnClickListener {
//            CommonUtil.createPdf(requireContext(), bitmapsArray, "${typname}\n${result}")
//        }

        if(CommonUtil.isTextURL(result)) {
            btnSearch.setIconResource(R.drawable.ico_webhook) // Replace with your new icon
            binding.txtOpenSearchButton.text = "Open"
            txtSearch.text = "Open"
        }
        updateSearchEngineOnBarcodeDetails(btnSearch, result)

        btnCopy.setOnClickListener {
            CommonUtil.copyBarcodeResultText(requireContext(), result)
            Toast.makeText(requireContext(), "Values was copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

        closeButton.setOnClickListener {
            dialog.dismiss()

        }
        dialog.setOnShowListener {
            val window = dialog.window
            if (window != null) {
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                window.setBackgroundDrawableResource(android.R.color.transparent) // Optional: Transparent background
            }
        }
        dialog.show()

        if(expandedBottomSheet) {
            binding.layoutTapAnywhere.visibility = View.GONE
            bottomSheet?.let {
                bottomSheetBehavior = BottomSheetBehavior.from(it)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                bottomSheetBehavior.peekHeight = 6200
            }

            val params = binding.constraintLayout4.layoutParams
            val newHeightInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                550f,
                resources.displayMetrics
            ).toInt()

            params.height = newHeightInPixels
            binding.txtBtnExpand.text = "Collapse"
            binding.constraintLayout4.layoutParams = params
        } else {
            binding.layoutTapAnywhere.visibility = View.VISIBLE
            bottomSheet?.let {
                bottomSheetBehavior = BottomSheetBehavior.from(it)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                bottomSheetBehavior.peekHeight = 1200
            }

//            val params = binding.constraintLayout4.layoutParams
//            val newHeightInPixels = TypedValue.applyDimension(
//                TypedValue.COMPLEX_UNIT_DIP,
//                225f,
//                resources.displayMetrics
//            ).toInt()
//            binding.txtBtnExpand.text = "Expand"
//            params.height = newHeightInPixels
//
//            binding.constraintLayout4.layoutParams = params
        }
    }

    fun prettyPrintJson(jsonString: String): String {
        return try {
            val jsonObject = JSONObject(jsonString)
            jsonObject.toString(4) // 4 spaces for indentation
        } catch (e: JSONException) {
            // If it's not a valid JSON, just return the original string
            jsonString
        }
    }

    fun cleanResultString(result: String): String {
        return result.filter {
            // Keep only printable characters: letters, digits, punctuation, and whitespace
            !it.isISOControl() && it != '?' && it.code in 32..126 || it.isWhitespace()
        }
    }

    fun showFullScreenImage(context: Context, bitmap: Bitmap?) {
        // Create a Dialog to display the image
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_fullscreen_image)  // Inflate the dialog layout

        // Find the ImageView in the dialog layout and set the image from the clicked ImageView
        val fullScreenImageView = dialog.findViewById<ImageView>(R.id.fullScreenImageView)

        // Set the bitmap directly into the ImageView
        fullScreenImageView.setImageBitmap(bitmap)

        // Show the dialog
        dialog.show()

        // Optionally, dismiss the dialog when the image is clicked
        fullScreenImageView.setOnClickListener {
            dialog.dismiss()
        }

    }

    fun getBitmapFromInternalStorage(imagePath: String): Bitmap? {
        return try {
            // Use BitmapFactory to decode the file at the given path into a Bitmap
            BitmapFactory.decodeFile(imagePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null // Return null if the conversion fails
        }
    }




}
