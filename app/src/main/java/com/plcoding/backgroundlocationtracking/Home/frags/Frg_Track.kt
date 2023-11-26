package com.plcoding.backgroundlocationtracking.Home.frags

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.plcoding.backgroundlocationtracking.LocationService
import com.plcoding.backgroundlocationtracking.R
import com.plcoding.backgroundlocationtracking.SessionManager
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class Frg_Track : Fragment(), OnMapReadyCallback {
    var clock_btn: RelativeLayout? = null
    var clock_layout: LinearLayout? = null
    var isClockin = false
    var clock_msg: TextView? = null
    var clock_msg2: TextView? = null
    var time: TextView? = null
    var clock_status: TextView? = null
    var clock_in_anim: LottieAnimationView? = null
    var clock_out_anim: LottieAnimationView? = null
    var session: SessionManager? = null
    var loading : LinearLayout? = null


    //////
    private var mapView: MapView? = null
    private var mMap: GoogleMap? = null
    private var mark: Marker? = null
    var mFusedLocationClient: FusedLocationProviderClient? = null
    private var user_Latitude = 0.0
    private var user_Longitude = 0.0

    //////////////////////

    override fun onCreateView(
        _inflater: LayoutInflater,
        _container: ViewGroup?,
        _savedInstanceState: Bundle?
    ): View {
        val _view = _inflater.inflate(R.layout.frg_track, _container, false)
        initialize(_view)
        initializeLogic()
        return _view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      ///////////////////
        mapView!!.onCreate(savedInstanceState)
        mapView!!.getMapAsync(this)

        //////////////////
        super.onViewCreated(view, savedInstanceState)
    }



    private fun initialize(_view: View) {

        loading = _view.findViewById(R.id.loading222)
////////////////////////////
        mapView = _view.findViewById(R.id.mapView3)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

///////////////////////
        session = SessionManager(activity)

        clock_btn = _view.findViewById(R.id.clock_btn)
        clock_layout = _view.findViewById(R.id.clock_layout)
        clock_msg = _view.findViewById(R.id.clock_msg)
        clock_msg2 = _view.findViewById(R.id.clock_msg2)
        time = _view.findViewById(R.id.time)
        clock_status = _view.findViewById(R.id.clock_status)
        clock_in_anim = _view.findViewById(R.id.animationView)
        clock_out_anim = _view.findViewById(R.id.animationView2)
        clock_in_anim!!.setAnimation(R.raw.clock_in)
        clock_out_anim!!.setAnimation(R.raw.clock_in)
        clock_layout!!.setVisibility(View.GONE)
        if (session!!.clock_status_check == "true") {
            isClockin = false
            clock_out_show()
        } else {
            isClockin = true
            clock_in_show()
        }
    }



    private fun initializeLogic() {
        clock_btn!!.setOnLongClickListener {
            _clickAnimation(clock_layout)
            if (isClockin) {
                clock_in_fun()
            } else {
                clock_out_fun()
            }
            true
        }
    }

    fun _clickAnimation(_view: View?) {
        val fade_in = ScaleAnimation(
            0.8f,
            1f,
            0.8f,
            1f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.7f
        )
        fade_in.duration = 300
        fade_in.fillAfter = true
        _view!!.startAnimation(fade_in)
    }

    fun clock_in_fun() {
        session!!.clock_status_check = "true"
        clock_btn!!.alpha = 0f
        clock_btn!!.visibility = View.GONE
        clock_layout!!.visibility = View.VISIBLE
        clock_btn!!.isEnabled = false
        time!!.text = current_time()
        //UtilsClass.showToast("Tracking started!",getActivity());
        val intent = Intent(activity, LocationService::class.java)
        intent.action = LocationService.ACTION_START
        requireActivity().startService(intent)
    }

    fun clock_out_fun() {
        session!!.clock_status_check = "false"
        clock_btn!!.alpha = 0f
        clock_btn!!.visibility = View.GONE
        clock_layout!!.visibility = View.VISIBLE
        clock_btn!!.isEnabled = false
        time!!.text = current_time()
        val intent = Intent(activity, LocationService::class.java)
        intent.action = LocationService.ACTION_STOP
        requireActivity().startService(intent)
    }

    fun clock_out_show() {

        //LottieAnimationView statusView = (LottieAnimationView) findViewById(R.id.check_animation_view);
         //anim.setColorFilter(new PorterDuffColorFilter(0XFF00a5ff, PorterDuff.Mode.SRC_ATOP));
          //anim.setColorFilter(0xff00a5ff);

        clock_out_anim!!.visibility = View.VISIBLE
        clock_in_anim!!.visibility = View.GONE
        clock_status!!.text = "Clock out"
        time!!.setTextColor(resources.getColor(R.color.primaryTextColor_orange))
        clock_msg!!.text = "Clocked out at"
        clock_msg2!!.text = "Loaction tracking stopped"

    }

    fun clock_in_show() {

        clock_out_anim!!.visibility = View.GONE
        clock_in_anim!!.visibility = View.VISIBLE
        clock_status!!.text = "Clock in"
        time!!.setTextColor(resources.getColor(R.color.primaryTextColor))
        clock_msg!!.text = "Clocked in at"
        clock_msg2!!.text = "Tracking your loaction"

    }

    fun current_time(): String {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("hh:mm aaa")
        return sdf.format(cal.time)
    }


    //////////////////// MAP //////


    override fun onMapReady(map: GoogleMap) {
        mMap = map
        //UtilsClass.showToast("Please Accept Location.sadasdad",requireActivity())
        enableLocation()
    }


    fun add_marker(lat: Double, lng: Double) {
        val location = LatLng(lat, lng)
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))

        // Custom marker icon (assuming you named your custom marker icon as "custom_marker_icon")
       /* if (mark != null) {
            mark!!.remove()
        }*/
        mark = mMap!!.addMarker(
            MarkerOptions().position(location)
                .title("Your location")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.user_ic))
        )

        mark!!.showInfoWindow()


    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {

        // Initializing LocationRequest
        // object with appropriate methods
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 5000
        mLocationRequest.fastestInterval = 1000
        mLocationRequest.numUpdates = 1

        // setting LocationRequest
        // on FusedLocationClient
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        mFusedLocationClient!!.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()
        )
    }



    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation = locationResult.lastLocation
            user_Latitude =  mLastLocation!!.latitude
            user_Longitude = mLastLocation.longitude
            loading!!.visibility = View.GONE
            //progress!!.dismiss()
            add_marker(user_Latitude, user_Longitude)

        }
    }

    private fun getMyLocation() {


        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            mMap!!.isMyLocationEnabled = true

            Log.d("2323","212121")

            mFusedLocationClient!!.lastLocation.addOnCompleteListener { task ->
                val location = task.result
                if (location == null) {
                    requestNewLocationData()
                } else {
                    //OLD LOCATION
                    add_marker(location.latitude, location.longitude)
                }
            }
            requestNewLocationData()
            /*   mMap!!.setOnMyLocationChangeListener { location: Location ->


                   val ltlng = LatLng(location.latitude, location.longitude)
                   val cameraUpdate = CameraUpdateFactory.newLatLngZoom(ltlng, 16f)
                   mMap!!.animateCamera(cameraUpdate)
                   add_marker(location.latitude, location.longitude)
                   // mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(ltlng, 15f))
               }*/
        }




    }


    private fun enableLocation() {


        val locationRequest = LocationRequest.create()
        locationRequest.apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 30 * 1000.toLong()
            fastestInterval = 5 * 1000.toLong()
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result=
            activity?.let { LocationServices.getSettingsClient(it).checkLocationSettings(builder.build()) }
        result!!.addOnCompleteListener {
            try {
                val response: LocationSettingsResponse = it.getResult(ApiException::class.java)
                println("location>>>>>>> ${response.locationSettingsStates!!.isGpsPresent}")
                if(response.locationSettingsStates!!.isGpsPresent)
                {
                    getMyLocation()
                }

            }catch (e: ApiException){
                when (e.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->
                        try {
                            val intentSenderRequest =
                                e.status.resolution?.let { it1 -> IntentSenderRequest.Builder(it1).build() }
                            launcher.launch(intentSenderRequest)
                        } catch (e: IntentSender.SendIntentException) {
                        }
                }
            }
        }
    }


    private var launcher=  registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()){ result->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("TAG", "OK")

            getMyLocation()

        } else {
            Log.d("TAG", "CANCEL")
            enableLocation()
            // UtilsClass.showToast("Please Accept Location.",this@MainActivity)
        }
    }






    override fun onResume() {
        mapView!!.onResume();
        super.onResume()
    }

    override fun onStart() {
        mapView!!.onStart();
        super.onStart()
    }

    override fun onPause() {
        mapView!!.onPause();
        super.onPause()
    }

    override fun onDestroy() {
        mapView!!.onDestroy();
        super.onDestroy()
    }

    override fun onLowMemory() {
        mapView!!.onLowMemory();
        super.onLowMemory()
    }




}