/*
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
import android.content.pm.PackageManager
import android.Manifest
import android.Manifest.permission
import android.Manifest.permission.READ_CALENDAR
import android.Manifest.permission.WRITE_CALENDAR
import android.system.Os.remove
import android.provider.CalendarContract
import android.net.Uri
import android.content.ContentResolver
import android.database.Cursor
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat


class CalendarPlugin(val activity: Activity) : MethodCallHandler {
    var permissionRequests: MutableMap<Int, Result>  = mutableMapOf<Int, Result>();
    val PERMISSION_REQUEST_CODE: Int = 37


    companion object {
        lateinit var channel: MethodChannel


        @JvmStatic
        fun registerWith(registrar: Registrar): Unit {
            channel = MethodChannel(registrar.messenger(), "com.whelksoft.calendar")
            val plugin = CalendarPlugin(activity = registrar.activity())
            channel.setMethodCallHandler(plugin)
        }
    }

    private fun requestCalendarReadWritePermission(result: Result) {
        if (activity == null) {
            result.error("E_ERROR_PERMISSION", "Activity doesn't exist", null)
            return
        }
        PERMISSION_REQUEST_CODE = PERMISSION_REQUEST_CODE+1
        permissionRequests.put(PERMISSION_REQUEST_CODE, result)
        ActivityCompat.requestPermissions(activity, arrayOf<String>(WRITE_CALENDAR, READ_CALENDAR), PERMISSION_REQUEST_CODE)
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (permissionRequests.containsKey(requestCode)) {

            // If request is cancelled, the result arrays are empty.
            val result = permissionRequests.get(requestCode)

            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                result?.success("authorized")
            } else if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                result?.success("denied")
            } else if (permissionRequests.size() === 1) {
                result?.error("permissions - unknown error", if (grantResults.size > 0) grantResults[0].toString() else "Request was cancelled", null)
            }
            permissionRequests.remove(requestCode)
        }
    }


    private fun haveCalendarReadWritePermissions(): Boolean {
        val writePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
        val readPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)

        return writePermission == PackageManager.PERMISSION_GRANTED && readPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun findEventCalendars(): Map<String, dynamic> {

        val cursor: Cursor?
        val cr = getContentResolver()

        val uri = CalendarContract.Calendars.CONTENT_URI

        cursor = cr.query(uri, arrayOf<String>(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.IS_PRIMARY, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.ALLOWED_AVAILABILITY, CalendarContract.Calendars.ACCOUNT_TYPE), null, null, null)

        return serializeEventCalendars(cursor)
    }

    private fun serializeEvents(cursor: Cursor): ArrayList<dynamic> {
        val results = mutableListOf<dynamic>()

        while (cursor.moveToNext()) {
            results.add(serializeEvent(cursor))
        }

        cursor.close()

        return results
    }

    private fun serializeEvent(cursor: Cursor): MutableMap<String, dynamic> {
        val event = mutableMapOf<String, dynamic>()

        val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        val sdf = SimpleDateFormat(dateFormat)
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"))

        val foundStartDate = Calendar.getInstance()
        val foundEndDate = Calendar.getInstance()

        var allDay = false
        var startDateUTC = ""
        var endDateUTC = ""

        if (cursor.getString(3) != null) {
            foundStartDate.setTimeInMillis(java.lang.Long.parseLong(cursor.getString(3)))
            startDateUTC = sdf.format(foundStartDate.getTime())
        }

        if (cursor.getString(4) != null) {
            foundEndDate.setTimeInMillis(java.lang.Long.parseLong(cursor.getString(4)))
            endDateUTC = sdf.format(foundEndDate.getTime())
        }

        if (cursor.getString(5) != null) {
            allDay = cursor.getInt(5) != 0
        }

        if (cursor.getString(7) != null) {
            val recurrenceRule = WritableNativeMap()
            val recurrenceRules = cursor.getString(7).split(";".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            val format = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'")

            event.put("recurrence", recurrenceRules[0].split("=".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[1].toLowerCase())
            recurrenceRule.put("frequency", recurrenceRules[0].split("=".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[1].toLowerCase())

            if (cursor.getColumnIndex(CalendarContract.Events.DURATION) != -1 && cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DURATION)) != null) {
                recurrenceRule.put("duration", cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DURATION)))
            }

            if (recurrenceRules.size >= 2 && recurrenceRules[1].split("=".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0] == "INTERVAL") {
                recurrenceRule.put("interval", Integer.parseInt(recurrenceRules[1].split("=".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[1]))
            }

            if (recurrenceRules.size >= 3) {
                if (recurrenceRules[2].split("=".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0] == "UNTIL") {
                    try {
                        recurrenceRule.put("endDate", sdf.format(format.parse(recurrenceRules[2].split("=".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[1])))
                    } catch (e: ParseException) {
                        e.printStackTrace()
                    }

                } else if (recurrenceRules[2].split("=".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0] == "COUNT") {
                    recurrenceRule.put("occurrence", Integer.parseInt(recurrenceRules[2].split("=".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[1]))
                }

            }

            event.put("recurrenceRule", recurrenceRule)
        }

        event.put("id", cursor.getString(0))
        event.put("calendar", findCalendarById(cursor.getString(cursor.getColumnIndex("calendar_id"))))
        event.put("title", cursor.getString(cursor.getColumnIndex("title")))
        event.put("description", cursor.getString(2))
        event.put("startDate", startDateUTC)
        event.put("endDate", endDateUTC)
        event.put("allDay", allDay)
        event.put("location", cursor.getString(6))
        event.put("availability", availabilityStringMatchingConstant(cursor.getInt(9)))

        if (cursor.getInt(10) > 0) {
            event.put("alarms", findReminderByEventId(cursor.getString(0), java.lang.Long.parseLong(cursor.getString(3))))
        } else {
            val emptyAlarms = WritableNativeArray()
            event.put("alarms", emptyAlarms)
        }

        if (cursor.getColumnIndex(CalendarContract.Events.ORIGINAL_ID) != -1 && cursor.getString(cursor.getColumnIndex(CalendarContract.Events.ORIGINAL_ID)) != null) {
            event.put("originalId", cursor.getString(cursor.getColumnIndex(CalendarContract.Events.ORIGINAL_ID)))
        }

        if (cursor.getColumnIndex(CalendarContract.Instances.ORIGINAL_SYNC_ID) != -1 && cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.ORIGINAL_SYNC_ID)) != null) {
            event.put("syncId", cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.ORIGINAL_SYNC_ID)))
        }

        return event
    }

    private fun serializeEventCalendars(cursor: Cursor): Map<String, dynamic> {
        val results = mutableListOf<dynamic>()

        while (cursor.moveToNext()) {
            results.add(serializeEventCalendar(cursor))
        }

        cursor.close()

        return results
    }

    private fun serializeEventCalendar(cursor: Cursor): Map<String, dynamic> {

        var isPrimary = false

        if (cursor.getString(3) != null) {
            isPrimary = cursor.getString(3) == "1"
        }

        val accesslevel = cursor.getInt(4)

        var allowsModifications = false;
        if (accesslevel == CalendarContract.Calendars.CAL_ACCESS_ROOT ||
                accesslevel == CalendarContract.Calendars.CAL_ACCESS_OWNER ||
                accesslevel == CalendarContract.Calendars.CAL_ACCESS_EDITOR ||
                accesslevel == CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
            allowsModifications = true
        } else {
            allowsModifications = false
        }

        var result = mapOf(
                "id" to cursor.getString(0),
                "title" to cursor.getString(1),
                "source" to cursor.getString(2),
                "allowedAvailabilities" to calendarAllowedAvailabilitiesFromDBString(cursor.getString(5)),
                "type" to cursor.getString(6),
                "isPrimary" to primary,
                "allowsModifications" to allowsModifications);

        return result
    }

    override fun onMethodCall(call: MethodCall, result: Result): Unit {
        when {
            call.method == "getCalendarPermissions" -> {
                val sharedPreferences = reactContext.getSharedPreferences(RNC_PREFS, ReactContext.MODE_PRIVATE)
                val permissionRequested = sharedPreferences.getBoolean("permissionRequested", false)

                if (this.haveCalendarReadWritePermissions()) {
                    result.success("authorized")
                } else if (!permissionRequested) {
                    result.success("undetermined")
                } else {
                    result.success("denied")
                }
            }
            call.method == "requestCalendarPermissions" -> {
                val sharedPreferences = reactContext.getSharedPreferences(RNC_PREFS, ReactContext.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putBoolean("permissionRequested", true)
                editor.apply()

                if (this.haveCalendarReadWritePermissions()) {
                    result.sucess("authorized")
                } else {
                    this.requestCalendarReadWritePermission(result)
                }
            }
            call.method == "findCalendars" -> {
                if (this.haveCalendarReadWritePermissions()) {
                    try {
                        val thread = Thread(object : Runnable {
                            override fun run() {
                                val calendars = findEventCalendars()
                                result.success(calendars)
                            }
                        })
                        thread.start()
                    } catch (e: Exception) {
                        result.error("calendar request error", e.message)
                    }

                } else {
                    result.error("add event error", "you don't have permissions to retrieve an event to the users calendar")
                }
            }
            else -> result.notImplemented()
        }
    }


}
*/