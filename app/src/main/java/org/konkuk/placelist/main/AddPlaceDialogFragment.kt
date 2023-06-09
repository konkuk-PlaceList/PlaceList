package org.konkuk.placelist.main

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.maps.model.LatLng
import org.konkuk.placelist.MyViewModel
import org.konkuk.placelist.R
import org.konkuk.placelist.databinding.FragmentAddPlaceBinding
import org.konkuk.placelist.domain.Place
import java.util.Locale
import android.os.Bundle as Bundle1

class AddPlaceDialogFragment : DialogFragment() {
    lateinit var binding: FragmentAddPlaceBinding
    lateinit var addPlaceListener: AddPlaceListener
    val model : MyViewModel by activityViewModels()
    var text = ""

    var place : Place? = null

    override fun onCreate(savedInstanceState: Bundle1?) {
        super.onCreate(savedInstanceState)
        try{
            addPlaceListener = context as AddPlaceListener
        } catch (e: ClassCastException) { Log.e("E", "Cast Failed")}
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle1?
    ): View {
        binding = FragmentAddPlaceBinding.inflate(inflater, container, false)
        dialog?.window?.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM)
        with (dialog?.window?.attributes){
            this?.width = ViewGroup.LayoutParams.MATCH_PARENT
            this?.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            this?.verticalMargin = 0f
            this?.horizontalMargin = 0f
        }
        initButtons()
        initGeocoder()

        if (tag == "EditPlace"){
            binding.titleText.text = "내 장소 수정"
            binding.mapFragment.tag = "EditPlace"
        }
        if (arguments != null){
            place = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                arguments?.getSerializable("place", Place::class.java)!!
            else arguments?.getSerializable("place") as Place
            binding.placename.setText(place?.name.toString())
            binding.radiusSeekbar.progress = when(place?.detectRange){
                100f -> 0
                200f -> 1
                500f -> 2
                else -> 0
            }
        }

        return binding.root
    }

    private fun initButtons() {
        with(binding){
            this.radiusSeekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val range = when(progress){
                        0 -> 100.0f
                        1 -> 200.0f
                        2 -> 500.0f
                        else -> 100.0f
                    }
                    model.setRange(range)
                    Log.i("Radius", "Radius changed into $range")
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            this.closeBtn.setOnClickListener {
                dismiss()
            }
            this.submitBtn.setOnClickListener {
                if(binding.placename.text.isBlank()) {
                    binding.placename.setHintTextColor(resources.getColor(R.color.red, null))
                    return@setOnClickListener
                }
                val placeId = if (place != null) place!!.id else 0
                if(model.location.value != null) {
                    val pos = model.location.value!!
                    addPlaceListener.addPlace(
                        placeId,
                        binding.placename.text.toString(),
                        pos.latitude.toString(),
                        pos.longitude.toString(),
                        model.detectRange.value!!
                    )
                    dismiss()
                }else {
                    Toast.makeText(requireContext(),"장소가 선택되지 않았습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }
    private fun initGeocoder() {
        val geocoder = Geocoder(requireActivity(), Locale.KOREA)

        binding.location.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KEYCODE_ENTER) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName(binding.location.text.toString(), 1){ addresses ->
                        Log.i("M", "${addresses[0].latitude}, ${addresses[0].longitude}")
                        model.setLiveData(LatLng(addresses[0].latitude, addresses[0].longitude))
                    }
                }
                else{
                    val address = geocoder.getFromLocationName(binding.location.text.toString(), 1)
                    if (address?.size != 0) model.setLiveData(LatLng(address!![0].latitude, address!![0].longitude))
                    else Toast.makeText(requireContext(),"주소를 다시 확인해 주세요.", Toast.LENGTH_SHORT).show()
                }
            }
            false
        }
    }

    override fun onResume() {
        super.onResume()
        context?.dialogFragmentResize(1f, 0.7f)
        val window = dialog!!.window
        window!!.setBackgroundDrawable(ColorDrawable(Color.WHITE))
    }
    private fun Context.dialogFragmentResize(w: Float, h: Float) {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rect = windowManager.currentWindowMetrics.bounds
        val window = dialog?.window
        val x = (rect.width() * w).toInt()
        val y = (rect.height() * h).toInt()
        window?.setLayout(x, y)
    }

    companion object{
        fun toInstance(place: Place) : AddPlaceDialogFragment{
            val obj = AddPlaceDialogFragment()
            val args = Bundle1()
            args.putSerializable("place", place)
            obj.arguments = args
            return obj
        }
    }
}
