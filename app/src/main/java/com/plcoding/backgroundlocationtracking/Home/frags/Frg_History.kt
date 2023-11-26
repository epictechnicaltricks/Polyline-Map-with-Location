package com.plcoding.backgroundlocationtracking.Home.frags


import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.logicbeanzs.uberpolylineanimation.MapAnimator
import com.plcoding.backgroundlocationtracking.Api.ApiList
import com.plcoding.backgroundlocationtracking.Api.RequestNetwork
import com.plcoding.backgroundlocationtracking.Api.RequestNetworkController
import com.plcoding.backgroundlocationtracking.R
import com.plcoding.backgroundlocationtracking.RequestNetwork.RequestListener
import com.plcoding.backgroundlocationtracking.SessionManager
import com.plcoding.backgroundlocationtracking.UtilsPackage.UtilsClass
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class Frg_History : Fragment(), OnMapReadyCallback {

    var history_api: RequestNetwork? = null
    var history_api_listener: RequestNetwork.RequestListener? = null

    //////
    private var mapView: MapView? = null
    private var mMap: GoogleMap? = null
    private lateinit var polyline: Polyline

    private var marker: Marker? = null

    var mFusedLocationClient: FusedLocationProviderClient? = null
    var PERMISSION_ID = 44

    var progress: ProgressDialog? = null


    private var user_Latitude = 0.0
    private var user_Longitude = 0.0

    private val wayPoints = mutableListOf<LatLng>()

    private val wayPoints_temp = mutableListOf<LatLng>()


    var session : SessionManager?= null

    var log_from_date = "2023-11-27" // current date
    var log_to_date = "2023-11-30"

    ///

    private var time: TimerTask? = null
    private val _timer = Timer()

var play :LinearLayout?  =null

    override fun onCreateView(
        _inflater: LayoutInflater,
        _container: ViewGroup?,
        _savedInstanceState: Bundle?
    ): View {
        val _view = _inflater.inflate(R.layout.frg_history_layout, _container, false)
        initialize(_view)
        initializeLogic()
        return _view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        mapView!!.onCreate(savedInstanceState)
        mapView!!.getMapAsync(this)



        super.onViewCreated(view, savedInstanceState)
    }

    private fun initialize(_view: View) {

        mapView = _view.findViewById(R.id.mapView3)
        progress = ProgressDialog(requireActivity())
        progress!!.setMessage("Finding all routes..")
        session = SessionManager(activity)
        play = _view.findViewById(R.id.play)
        play!!.visibility =View.GONE

        play!!.setOnClickListener {

            play!!.alpha  = 0.3f
            play!!.isEnabled = false
            animate_loc()
        }

        //progress!!.show()

        history_api = RequestNetwork(activity)

        history_api_listener = object : RequestListener, RequestNetwork.RequestListener {
            override fun onResponse(
                tag: String?,
                response: String?,
                responseHeaders: HashMap<String, Any>?
            ) {


                if (response!!.contains("200")) {

                    try {
                        val obj = JSONObject(response)
                        val array = JSONArray(obj.getString("resultSet"))


                        //wayPoints.clear()
                        for (i in 0 until array.length())
                        {
                            val data = JSONObject(array.getString(i))

                            wayPoints.add(
                                LatLng(
                                    data.getString("lat").toDouble(),
                                    data.getString("lng").toDouble()
                                )
                            )
                        }


                        play!!.visibility =View.VISIBLE

                        stratAnimation()

                        Log.d("api_data",wayPoints.toString())


                    }catch (e :Exception) {
                        e.printStackTrace()
                        UtilsClass.showAlert(
                            "Please try again later.. "+e,
                            false,
                            "Error Failed to get data",
                            activity
                        )
                    }


                } else {
                    UtilsClass.showAlert(
                        "Please try again later..",
                        false,
                        "Failed to get data",
                        activity
                    )
                }




            }

            override fun onErrorResponse(tag: String?, message: String?) {
                UtilsClass.show_no_connection(activity!!);
            }

        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());


    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {

        // Initializing LocationRequest
        // object with appropriate methods
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 5
        mLocationRequest.fastestInterval = 0
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
            //progress!!.dismiss()
            add_marker(user_Latitude, user_Longitude)

        }
    }





    private fun initializeLogic() {


        log_from_date = getCurrentDateTime().toString()
        log_to_date = getNextDate(log_from_date).toString()

       /* _l_location_listener = LocationListener { _param1: Location ->
            val _lat = _param1.latitude
            val _lng = _param1.longitude
            val _acc = _param1.accuracy.toDouble()
            user_Latitude = _lat
            user_Longitude = _lng




        }*/


    }

    private fun getNextDate(inputDate_: String): String? {
        var inputDate = inputDate_
      //  inputDate = "15-12-2015" // for example
        val format = SimpleDateFormat("yyyy-MM-dd")
        try {
            val date: Date = format.parse(inputDate)
            val c = Calendar.getInstance()
            c.time = date
            c.add(Calendar.DATE, 1)
            inputDate = format.format(c.time)
            Log.d("asd", "selected date : $inputDate")
            System.out.println(date)
        } catch (e: java.lang.Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            inputDate = ""
        }
        return inputDate
    }

    private fun getCurrentDateTime(): String? {
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        val date = Date()
        return dateFormat.format(date)
    }

    private fun get_history_from_api()
    {
        val api_url = "${ApiList().BASE_URL}method=locationHistoryByUserAndDate&user_id=" +
                "${session!!.userID}&log_from=${log_from_date}&log_to=${log_to_date}"


        history_api!!.startRequestNetwork(
            RequestNetworkController.GET,
            api_url,
            "",
            history_api_listener
        )
        Log.d("api_call",api_url)
     //   UtilsClass.showToast("api_call :${api_url}",activity)
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



    private fun getMyLocation() {


        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                ACCESS_FINE_LOCATION
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
                    add_marker(location.latitude, location.longitude)
                }
            }
            requestNewLocationData()

             /*
                mMap!!.setOnMyLocationChangeListener { location: Location ->
                val ltlng = LatLng(location.latitude, location.longitude)
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(ltlng, 16f)
                mMap!!.animateCamera(cameraUpdate)
                add_marker(location.latitude, location.longitude)
                // mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(ltlng, 15f))}
            */

        }




    }


  /*  private fun enableLocation() {


        val locationRequest = create()
        locationRequest.apply {
            priority = PRIORITY_HIGH_ACCURACY
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
*/

    private var launcher=  registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()){ result->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("TAG", "OK")

            getMyLocation()

        } else {
            Log.d("TAG", "CANCEL")
           // enableLocation()
            // UtilsClass.showToast("Please Accept Location.",this@MainActivity)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        Log.d("2323343","212121")

        get_history_from_api()




    }



    fun add_marker(lat: Double, lng: Double) {
        val location = LatLng(lat, lng)
             mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))

        // Custom marker icon (assuming you named your custom marker icon as "custom_marker_icon")
        if (marker != null) {
            marker!!.remove()
        }

        marker = mMap!!.addMarker(
            MarkerOptions().position(location)
                .title("You")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.track))
        )
    // mMap.addMarker(markerOptions);
    }


    fun animate_loc() {


        marker!!.remove()
       //MapAnimator.setPolylineWidth(15f)// default is 8f
       //MapAnimator.animateRoute(mMap!!, wayPoints_temp)

  var point_position = 0

        time = object : TimerTask() {
        override fun run() {
            activity!!.runOnUiThread(Runnable {
                ///////////////////////////////////////


                if(point_position<wayPoints.size)
                {


                    wayPoints_temp.add(wayPoints[point_position])


                   if(point_position % 2 == 0)
                   {
                       marker = mMap!!.addMarker(
                           MarkerOptions()
                               .position(wayPoints_temp[point_position])
                               .title(wayPoints[point_position].toString())
                               .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                       )
                       marker!!.showInfoWindow()
                   } else
                   {
                       marker = mMap!!.addMarker(
                           MarkerOptions()
                               .position(wayPoints_temp[point_position])
                               .title(wayPoints[point_position].toString())
                               .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                       )
                       marker!!.showInfoWindow()
                   }


                    if(wayPoints_temp.size>2)
                    {
                        MapAnimator.setPolylineWidth(15f)// default is 8f
                        MapAnimator.animateRoute(mMap!!, wayPoints_temp)
                    }

                    mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(wayPoints_temp[point_position],
                        15f))

                    Log.d("GF34",wayPoints_temp[point_position].toString())

                    point_position++


                } else
                {
                    marker!!.remove()
                    play!!.alpha  = 1f
                    play!!.isEnabled = true
                    wayPoints_temp.clear()
                    time!!.cancel()
                    stratAnimation()
                }

                //////////////////////////////////////

            })
        }
        }
        _timer.scheduleAtFixedRate(time, 1500,3500)
    }

    fun stop_animate_loc() {

       if(time!=null)
       {
           wayPoints_temp.clear()
           time!!.cancel()
       }


    }

    fun stratAnimation()
    {
        marker = mMap!!.addMarker(
            MarkerOptions()
                .position(wayPoints.first())
                .title("Clocked in location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )

        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(wayPoints.last(), 15f))
        marker = mMap!!.addMarker(
            MarkerOptions()
                .position(wayPoints.last())
                .title("Clocked out location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        )
        marker!!.showInfoWindow()

        // MapAnimator.setPolylineWidth(15f)// default is 8f
        // MapAnimator.animateRoute(mMap!!, wayPoints)
        MapAnimator.setPolylineWidth(15f)// default is 8f
        MapAnimator.animateRoute(mMap!!, wayPoints)
    }


}