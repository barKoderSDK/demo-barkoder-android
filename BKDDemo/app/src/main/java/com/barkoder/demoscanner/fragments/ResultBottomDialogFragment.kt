package com.barkoder.demoscanner.fragments

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
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
import android.text.method.ScrollingMovementMethod
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.barkoder.demoscanner.MainActivity
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.ScannerActivity
import com.barkoder.demoscanner.adapters.RecentScansAdapter
import com.barkoder.demoscanner.adapters.SessionScanAdapter
import com.barkoder.demoscanner.api.RetrofitIInstance
import com.barkoder.demoscanner.databinding.FragmentResultBottomDialogBinding
import com.barkoder.demoscanner.models.BarcodeScanedData
import com.barkoder.demoscanner.models.RecentScan2
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

    private var firstName : String? = null
    private var lastName : String? = null
    private var documentNumber : String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SessionScanAdapter
    private var expandedBottomSheet = false
    var sessionScanCheck : MutableList<SessionScan> = mutableListOf()

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
        image: Bitmap? = null,
        sessionScan : MutableList<SessionScan>
    ) {
        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        var lastResultsOnFrame = sharedPreferences.getInt("lastResultsOnFrame", 0)
        scannedBarcodesResultList.clear()
        scannedBarcodesTypesList.clear()
        scannedBarcodesDateList.clear()
        sessionScanCheck = sessionScan
        binding.resultsSize.text = lastResultsOnFrame.toString() + " results found (${resultsSize} total)"
        scannedBarcodesResultList.addAll(numResults)
        scannedBarcodesTypesList.addAll(typeResults)
        scannedBarcodesDateList.addAll(dateResults)

        if(sessionScan!!.size == 1) {
            val params = binding.constraintLayout4.layoutParams
            val newHeightInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                80f,
                resources.displayMetrics
            ).toInt()
            params.height = newHeightInPixels
            binding.constraintLayout4.layoutParams = params
        }  else if (sessionScan!!.size == 2) {
            val params = binding.constraintLayout4.layoutParams
            val newHeightInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                160f,
                resources.displayMetrics
            ).toInt()
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

        extractDocumentRawText(scannedBarcodesResultList.last())
        if(scannedBarcodesTypesList.last() == "MRZ") {
            binding.textBarcodeNumResult.text = "Full name: ${firstName} ${lastName} \n" +
                    "Document number: ${documentNumber}"
        } else {
            binding.textBarcodeNumResult.text = scannedBarcodesResultList.last()
        }
        binding.textBarcodeTypeResult.text = scannedBarcodesTypesList.last()
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




    }

    companion object {
        fun newInstance(numResult: MutableList<String>, typeResult : List<String>, dateResult : List<String>, image : Bitmap? = null,
                        resultsSize : String? = null, sessionScan : MutableList<SessionScan>): ResultBottomDialogFragment {
            val fragment = ResultBottomDialogFragment()
            val args = Bundle()
            args.putString("numResult", numResult[0])
            args.putString("typeResult", typeResult[0])
            args.putString("resultsSize", resultsSize)
            args.putParcelable("bitmapImage", image)
            args.putStringArrayList("resultsList", ArrayList(numResult))
            args.putStringArrayList("resultsTypes", ArrayList(typeResult))
            args.putStringArrayList("dateResultsList", ArrayList(dateResult))
            args.putSerializable("sessionScan", sessionScan as Serializable)

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
        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        var lastResultsOnFrame = sharedPreferences.getInt("lastResultsOnFrame", 0)
           val numResult = arguments?.getString("numResult")
           val typeResult = arguments?.getString("typeResult")
        val resultsSize = arguments?.getString("resultsSize")
        val resultsList = arguments?.getStringArrayList("resultsList")?.toMutableList()
        val typesList = arguments?.getStringArrayList("resultsTypes")?.toMutableList()
        val dateList = arguments?.getStringArrayList("dateResultsList")?.toMutableList()
        val image = arguments?.getParcelable<Bitmap>("bitmapImage")
        var sessionScan = arguments?.getSerializable("sessionScan") as? MutableList<SessionScan>
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)


        if(sessionScan!!.size == 1) {
            val params = binding.constraintLayout4.layoutParams
            val newHeightInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                75f,
                resources.displayMetrics
            ).toInt()
            params.height = newHeightInPixels
            binding.constraintLayout4.layoutParams = params
        }  else if (sessionScan!!.size == 2) {
            val params = binding.constraintLayout4.layoutParams
            val newHeightInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                150f,
                resources.displayMetrics
            ).toInt()
            params.height = newHeightInPixels
            binding.constraintLayout4.layoutParams = params
        } else {
            val params = binding.constraintLayout4.layoutParams
            val newHeightInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                225f,
                resources.displayMetrics
            ).toInt()
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
        updateSearchEngine()

        updateCopyTerminator()

        if(CommonUtil.isTextURL(resultsList?.last())) {
            binding.btnSearchWeb.setImageResource(R.drawable.ico_webhook) // Replace with your new icon
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


        val keyWebHook = sharedPreferences.getString(getString(R.string.key_secret_word_webhook), "")

        val urlWebHook = sharedPreferences.getString(getString(R.string.key_url_webhook), "")

        var endPointUrl = extractEndpointFromUrl(urlWebHook!!)
//        sharedPreferenceRegisterListener()

        scannedBarcodesResultList.addAll(resultsList!!)
        scannedBarcodesTypesList.addAll(typesList!!)
        scannedBarcodesDateList.addAll(dateList!!)


        if(resultsSize != null) {
            binding.resultsSize.text = lastResultsOnFrame.toString() + " results found (${resultsSize} total)"
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
            if(sessionScan!!.size == 1) {
                val params = binding.constraintLayout4.layoutParams
                val newHeightInPixels = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    75f,
                    resources.displayMetrics
                ).toInt()
                params.height = newHeightInPixels
                binding.constraintLayout4.layoutParams = params
            }  else if (sessionScan!!.size == 2) {
                val params = binding.constraintLayout4.layoutParams
                val newHeightInPixels = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    150f,
                    resources.displayMetrics
                ).toInt()
                params.height = newHeightInPixels
                binding.constraintLayout4.layoutParams = params
            }

        }

        binding.layoutBottomSheet.setBackgroundResource(R.drawable.bottomsheet_rounded_bg)

        binding.btnTapAnyhere.setOnClickListener{
            stateListener?.onStartScanningClicked()
            if(bottomSheetBehavior.peekHeight == 1200) {
                (activity as? MainActivity)?.hideImageView()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                updatePeekHeight(1200, 0, bottomSheetBehavior)
                binding.layoutTapAnywhere.visibility = View.INVISIBLE
            }
        }

        binding.layoutTapAnywhere.setOnClickListener{
            stateListener?.onStartScanningClicked()

            if(bottomSheetBehavior.peekHeight == 1200) {
                (activity as? MainActivity)?.hideImageView()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                updatePeekHeight(1200, 0, bottomSheetBehavior)
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
                updatePeekHeight(1200, 0, bottomSheetBehavior)
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


        extractDocumentRawText(scannedBarcodesResultList.last())
        if(scannedBarcodesTypesList.last() == "MRZ") {
            binding.textBarcodeNumResult.text = "Full name: ${firstName} ${lastName} \n" +
                    "Document number: ${documentNumber}"
        } else {
            binding.textBarcodeNumResult.text = scannedBarcodesResultList.last()
        }
        binding.textBarcodeTypeResult.text = scannedBarcodesTypesList.last()
        binding.textBarcodeNumResult.movementMethod = ScrollingMovementMethod()

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
                showBarcodeDetailsDialog(requireContext(), item.thumbnailBitmap!!, item.scanText, item.scanTypeName, item.formattedText)

            }


    }

    override fun onSessionScanItemLongClick(item: SessionScan, position: Int) {
        TODO("Not yet implemented")
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
                AlertDialog.Builder(context, R.style.FullScreenDialogStyle)
            // Inflate the custom layout
            val inflater = LayoutInflater.from(context)
            val dialogView = inflater.inflate(R.layout.custom_dialog_results, null)

            // Find the ImageView and set the bitmap image
            val dialogImageView =
                dialogView.findViewById<ImageView>(R.id.imageViewDialog)
            val firstNameUser = dialogView.findViewById<TextView>(R.id.firstNameUser)
            val dateOfBirthUser = dialogView.findViewById<TextView>(R.id.dateOfBirthUser)
            val issuingCountry = dialogView.findViewById<TextView>(R.id.issuingCountry)
            val genderUser = dialogView.findViewById<TextView>(R.id.genderUser)
            val expirationDateUser =
                dialogView.findViewById<TextView>(R.id.expirationDateUser)
            val nationalityUser = dialogView.findViewById<TextView>(R.id.nationalityUser)
            val documentNumberUser =
                dialogView.findViewById<TextView>(R.id.documentNumberUser)
            val documentTypeUser = dialogView.findViewById<TextView>(R.id.documentType)
            val imageDocument =
                dialogView.findViewById<ImageView>(R.id.imageDocument)
            val imageMain =
                dialogView.findViewById<ImageView>(R.id.imageMain)
            val imagePicture =
                dialogView.findViewById<ImageView>(R.id.imagePicture)
            val imageSignature =
                dialogView.findViewById<ImageView>(R.id.imageSignature)
            val verificationLayout =
                dialogView.findViewById<LinearLayout>(R.id.layoutVerificationUser)
            val verificationChecksLayout =
                dialogView.findViewById<LinearLayout>(R.id.layout_verification_checks)
            val iconVerification =
                dialogView.findViewById<ImageView>(R.id.icon_verification_user)
            val textVerification =
                dialogView.findViewById<TextView>(R.id.text_verification_user)
            val iconVerificationExpire =
                dialogView.findViewById<ImageView>(R.id.icon_verification_expires)
            val iconVerificationOver21 =
                dialogView.findViewById<ImageView>(R.id.icon_verification_over21)
            val textVerificationOver21 =
                dialogView.findViewById<TextView>(R.id.text_verification_over21)
            val textVerificationExpire =
                dialogView.findViewById<TextView>(R.id.text_verification_expires)
            val viewCardPicture = dialogView.findViewById<LinearLayout>(R.id.imagePictureLayout)
            val viewCardDocument = dialogView.findViewById<LinearLayout>(R.id.imageDocumentLayout)
            val viewCardSignature = dialogView.findViewById<LinearLayout>(R.id.imageSignatureLayout)
            val viewCardMain = dialogView.findViewById<LinearLayout>(R.id.imageMainLayout)


            // Split the raw string into lines
            val lines = results?.split("\n".toRegex())
                ?.dropLastWhile { it.isEmpty() }
                ?.toTypedArray()

            // Initialize variables for first name and last name
            var firstName: String? = null
            var lastName: String? = null
            var documentNumber: String? = null
            var dateOfBirth: String? = null
            var expirationDate: String? = null
            var nationality: String? = null
            var fullName: String? = null
            var documentType: String? = null
            var issuing_country: String? = null
            var gender_user: String? = null

            // Iterate over each line to find the required information
            for (line in lines!!) {
                if (line.startsWith("first_name:")) {
                    firstName = line.split("first_name:".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1].trim { it <= ' ' }
                } else if (line.startsWith("last_name:")) {
                    lastName =
                        line.split("last_name:".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim { it <= ' ' }
                } else if (line.startsWith("document_number:")) {
                    documentNumber =
                        line.split("document_number:".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim { it <= ' ' }
                } else if (line.startsWith("date_of_birth:")) {
                    dateOfBirth =
                        line.split("date_of_birth:".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim { it <= ' ' }
                } else if (line.startsWith("nationality:")) {
                    nationality =
                        line.split("nationality:".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim { it <= ' ' }
                } else if (line.startsWith("date_of_expiry:")) {
                    expirationDate =
                        line.split("date_of_expiry:".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim { it <= ' ' }
                } else if (line.startsWith("document_type:")) {
                    documentType =
                        line.split("document_type:".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim { it <= ' ' }
                } else if (line.startsWith("issuing_country:")) {
                    issuing_country =
                        line.split("issuing_country:".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim { it <= ' ' }
                } else if (line.startsWith("gender:")) {
                    gender_user =
                        line.split("gender:".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim { it <= ' ' }
                }
            }

            if (firstName == null) firstName = ""
            if (lastName == null) lastName = ""

            val formattedDateBirth =
                formatDateString(dateOfBirth) // Ensure this method returns a formatted date string
            val formattedDateExpiry = formatDateString(expirationDate)



            fullName = "$firstName $lastName"
            firstNameUser.text = firstName + " " + lastName
            dateOfBirthUser.text = formattedDateBirth
            expirationDateUser.text = formattedDateExpiry
            nationalityUser.text = nationality
            documentNumberUser.text = documentNumber
            documentTypeUser.text = documentType
            issuingCountry.text = issuing_country
            genderUser.text = gender_user


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
            CommonUtil.createPdf(requireContext(), bitmapsArray, "Full Name: $fullName\nNationality: $nationality\nDate of birth: $dateOfBirth\nDocument Number: $documentNumber\nIssuing country: $issuing_country\nDate of expiry $expirationDate")
        }

        updateSearchEngineOnBarcodeDetails(btnSearch, "$fullName")

        btnCopy.setOnClickListener {
            CommonUtil.copyBarcodeResultText(requireContext(), results)
            Toast.makeText(requireContext(), "Values was copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

            builder.setView(dialogView)
            builder.setCancelable(true)

            val dialog = builder.create()

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

            val params = binding.constraintLayout4.layoutParams
            val newHeightInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                225f,
                resources.displayMetrics
            ).toInt()
            binding.txtBtnExpand.text = "Expand"
            params.height = newHeightInPixels

            binding.constraintLayout4.layoutParams = params
        }

        }

    public fun showBarcodeDetailsDialog(context: Context, mainImage: String , result: String, typname: String, formattedTextValue : String) {
        val dialog = Dialog(requireContext(), R.style.FullScreenDialogStyle)

        // Inflate the custom layout
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.custom_dialog_barcode_result, null)

        dialog.setContentView(dialogView)

        val barcodeValueText = dialogView.findViewById<TextView>(R.id.barcodeValueText)
        val barcodeTypeText = dialogView.findViewById<TextView>(R.id.barcodeTypeText)
        val barcodeBitmap = dialogView.findViewById<ImageView>(R.id.barcodeImage)
        val formattedText = dialogView.findViewById<TextView>(R.id.FormattedValueText)
        val formattedLayout = dialogView.findViewById<LinearLayout>(R.id.formattedTextLayout)
        if(formattedTextValue.length > 0) {
            formattedLayout.visibility = View.VISIBLE
        } else {
            formattedLayout.visibility = View.GONE
        }


        // Ensure `mainImage` conversion works properly
        val bitmap = getBitmapFromInternalStorage(mainImage)
        if (bitmap != null) {
            barcodeBitmap.setImageBitmap(bitmap)
        } else {
            // Handle error case when bitmap is null
            Log.e("Error", "Bitmap is null for the given image string.")
        }


        barcodeValueText.text = result
        barcodeTypeText.text = typname
        formattedText.text = formattedTextValue

        val closeButton = dialogView.findViewById<ImageButton>(R.id.buttonClose)
        val btnCopy = dialogView.findViewById<MaterialButton>(R.id.btnCopy)
        val btnSearch = dialogView.findViewById<MaterialButton>(R.id.btnSearch)
        val btnPDF = dialogView.findViewById<MaterialButton>(R.id.btnPDF)
        val txtSearch = dialogView.findViewById<TextView>(R.id.txtSearch)
        var bitmapsArray = mutableListOf<Pair<Bitmap, String>>()

        if (mainImage != null) {
            CommonUtil.getBitmapFromInternalStorage(mainImage)?.let { bitmapsArray.add(Pair(it,"barcode")) }
        }

        btnPDF.setOnClickListener {
            CommonUtil.createPdf(requireContext(), bitmapsArray, "${typname}\n${result}")
        }

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

            val params = binding.constraintLayout4.layoutParams
            val newHeightInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                225f,
                resources.displayMetrics
            ).toInt()
            binding.txtBtnExpand.text = "Expand"
            params.height = newHeightInPixels

            binding.constraintLayout4.layoutParams = params
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
