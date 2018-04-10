package com.whelksoft.timezone

import android.app.Activity
import android.content.Intent
import android.location.Location
import java.util.TimeZone
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import com.google.android.gms.location.places.Place



class TimezonePlugin(val activity: Activity) : MethodCallHandler, ActivityResultListener {
    var placeResult: Result? = null
    val REQUEST_GOOGLE_PLAY_SERVICES = 1000
    var PLACE_PICKER_REQUEST: Int = 42

    companion object {
        lateinit var channel: MethodChannel


        @JvmStatic
        fun registerWith(registrar: Registrar): Unit {
            channel = MethodChannel(registrar.messenger(), "com.whelksoft.timezone")
            val plugin = TimezonePlugin(activity = registrar.activity())
            channel.setMethodCallHandler(plugin)
            registrar.addActivityResultListener(plugin)
        }
     }

    override fun onMethodCall(call: MethodCall, result: Result): Unit {
        when {
            call.method == "getLocalTimezone" -> {
                result.success(TimeZone.getDefault().getID())
                return
            }
            call.method == "showPlacesPicker" -> {
                val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)
                if (GoogleApiAvailability.getInstance().showErrorDialogFragment(activity, code, REQUEST_GOOGLE_PLAY_SERVICES)) {
                    return
                }

                //val intent = Intent(activity, PlacesActivity::class.java)
                //activity.startActivity(intent)

                var intentBuilder = PlacePicker.IntentBuilder()
                activity.startActivityForResult(intentBuilder.build(activity), PLACE_PICKER_REQUEST)

                placeResult = result
                return
            }
             else -> result.notImplemented()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        System.out.println("onActivityResult " + requestCode + " "+ resultCode);
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    System.out.println("data is null!!!!");
                }
                var selectedPlace = PlacePicker.getPlace(this.activity, data)
                var northeast = mapOf(
                        "latitude" to selectedPlace.getViewport().northeast.latitude,
                        "longitude" to selectedPlace.getViewport().northeast.longitude
                )
                var southwest = mapOf(
                        "latitude" to selectedPlace.getViewport().southwest.latitude,
                        "longitude" to selectedPlace.getViewport().southwest.longitude
                )
                var bounds = mapOf(
                        "northeast" to northeast,
                        "southwest" to southwest
                )

                var result = mapOf(
                        "address" to selectedPlace.getAddress().toString(),
                        "placeid" to selectedPlace.getId(),
                        "latitude" to selectedPlace.getLatLng().latitude,
                        "longitude" to selectedPlace.getLatLng().longitude,
                        "name" to selectedPlace.getName().toString(),
                        "phoneNumber" to selectedPlace.getPhoneNumber().toString(),
                        "priceLevel" to selectedPlace.getPriceLevel(),
                        "rating" to selectedPlace.getRating(),
                        "bounds" to bounds
                )

                placeResult?.success(result)

                return true
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (data == null) {
                    System.out.println("data is null!!!!");
                }
                placeResult?.error("PICK_FAILED", "Error getting place", null);
                return true
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                if (data == null) {
                    System.out.println("data is null!!!!");
                }
                System.out.println(PlaceAutocomplete.getStatus(this.activity, data));
                placeResult?.error("PICK_FAILED", "Invalid API Code: "
                        + PlaceAutocomplete.getStatus(this.activity, data), null);
            }
        }
        return false
    }
}