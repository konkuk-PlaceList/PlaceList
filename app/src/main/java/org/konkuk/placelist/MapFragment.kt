package org.konkuk.placelist

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.konkuk.placelist.databinding.FragmentMapBinding

class MapFragment : Fragment(), OnMapReadyCallback {
    lateinit var mMap: GoogleMap
    val model : MyViewModel by activityViewModels()
    var binding : FragmentMapBinding? = null
    lateinit var mLayout : View
    var mLocationManager : LocationManager? = null
    var mLocationListener : LocationListener? = null
    var marker : Marker? =null
    val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == Activity.RESULT_OK){ }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding= FragmentMapBinding.inflate(layoutInflater,container,false)
        val mapFragment = childFragmentManager.findFragmentById(org.konkuk.placelist.R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mLocationManager = this.requireActivity().getSystemService(LOCATION_SERVICE) as LocationManager
        mLocationListener = LocationListener { location ->
            val lat = location.latitude
            val lng = location.longitude
            Log.d("GmapViewFragment", "Lat: $lat, lon: $lng")
            val currentLocation = LatLng(lat, lng)
            marker = mMap.addMarker(MarkerOptions().position(currentLocation))
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
            model.setLiveData(currentLocation)
        }
        model.location.observe(viewLifecycleOwner, Observer {
            marker?.remove()
            marker = mMap.addMarker(MarkerOptions().position(LatLng(it.latitude,it.longitude)))!!
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker!!.position, 15.0f))
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding=null
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
        var mark : LatLng
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mark, 15.0f))

        mMap.setOnMapClickListener {
            Log.d(ContentValues.TAG, "onMapClick :" + it.latitude + it.longitude)
            mark = LatLng(it.latitude, it.longitude)
            marker?.remove()
            marker = mMap.addMarker(MarkerOptions().position(mark).title("이름"))!!
            model.setLiveData(mark)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker!!.position, 15.0f))
        }

        if (ActivityCompat.checkSelfPermission(
                this.requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this.requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        mLocationManager!!.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            3000L,
            30f,
            mLocationListener!!
        )

    }
}
