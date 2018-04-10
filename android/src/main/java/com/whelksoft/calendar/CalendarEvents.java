package com.whelksoft.calendar;

import android.os.Bundle;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.Manifest;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.database.Cursor;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;
import io.flutter.app.FlutterActivity;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.Map;
import android.util.Log;

public class CalendarEvents extends FlutterActivity implements MethodCallHandler {
    private static final String CHANNEL = "com.whelksoft.calendar";

    private static int PERMISSION_REQUEST_CODE = 37;
    private static final String RNC_PREFS = "FLUTTER_CALENDAR_PREFERENCES";
    private static final HashMap<Integer, Result> permissionsResults = new HashMap<>();
    private static final SimpleDateFormat FORMAT_DATE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        new MethodChannel(getFlutterView(), CHANNEL).setMethodCallHandler(this);
    }

    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "uriForCalendar":
                uriForCalendar(result);
                break;
            case "getCalendarPermissions":
                getCalendarPermissions(result);
                break;
            case "requestCalendarPermissions":
                requestCalendarPermissions(result);
                break;
            case "findCalendars":
                findCalendars(result);
                break;
            case "saveEvent": {
                @SuppressWarnings("unchecked")
                Map<String, Object> arguments = (Map<String, Object>) call.arguments;
                String title = (String) arguments.get("title");
                Map<String, Object> details = (Map<String, Object>) arguments.get("details");
                Map<String, Object> options = (Map<String, Object>) arguments.get("options");
                saveEvent(title, details, options, result);
                break;
            }
            case "findAllEvents": {
                @SuppressWarnings("unchecked")
                Map<String, Object> arguments = (Map<String, Object>) call.arguments;
                Integer startDate = (Integer) arguments.get("startDate");
                Integer endDate = (Integer) arguments.get("endDate");
                ArrayList<String> calendars = (ArrayList<String>) arguments.get("calendars");
                findAllEvents(startDate, endDate, calendars, result);
                break;
            }
            case "findById": {
                @SuppressWarnings("unchecked")
                Map<String, String> arguments = (Map<String, String>) call.arguments;
                findById(arguments.get("eventId"), result);
                break;
            }
            case "removeEvent": {
                @SuppressWarnings("unchecked")
                Map<String, Object> arguments = (Map<String, Object>) call.arguments;
                Map<String, Object> options = (Map<String, Object>) arguments.get("options");
                removeEvent((String)arguments.get("eventId"), options, result);
                break;
            }
            case "openEventInCalendar": {
                @SuppressWarnings("unchecked")
                Map<String, String> arguments = (Map<String, String>) call.arguments;
                openEventInCalendar(arguments.get("eventId"));
                result.success(true);
                break;
            }
            default:
                result.notImplemented();
                break;
        }
    }

    public String getName() {
        return "CalendarEvents";
    }

    //region Calendar Permissions
    private void requestCalendarReadWritePermission(final Result promise)
    {

        PERMISSION_REQUEST_CODE++;
        permissionsResults.put(PERMISSION_REQUEST_CODE, promise);
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_CALENDAR,
                Manifest.permission.READ_CALENDAR
        }, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (permissionsResults.containsKey(requestCode)) {

            // If request is cancelled, the result arrays are empty.
            Result result = permissionsResults.get(requestCode);

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                result.success("authorized");
            } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                result.success("denied");
            } else if (permissionsResults.size() == 1) {
                result.error("permissions - unknown error", grantResults.length > 0 ? String.valueOf(grantResults[0]) : "Request was cancelled", null);
            }
            permissionsResults.remove(requestCode);
        }
    }

    private boolean haveCalendarReadWritePermissions() {
        int writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR);
        int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR);

        return writePermission == PackageManager.PERMISSION_GRANTED &&
                readPermission == PackageManager.PERMISSION_GRANTED;
    }
    //endregion

    private ArrayList<Map<String, Object>> findEventCalendars() {

        Cursor cursor;
        ContentResolver cr = getContentResolver();

        Uri uri = CalendarContract.Calendars.CONTENT_URI;

        cursor = cr.query(uri, new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.IS_PRIMARY,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                CalendarContract.Calendars.ALLOWED_AVAILABILITY,
                CalendarContract.Calendars.ACCOUNT_TYPE
        }, null, null, null);

        return serializeEventCalendars(cursor);
    }

    private Map<String, Object> findCalendarById(String calendarID) {

        Map<String, Object> result;
        Cursor cursor;
        ContentResolver cr = getContentResolver();
        Uri uri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, Integer.parseInt(calendarID));

        cursor = cr.query(uri, new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.IS_PRIMARY,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                CalendarContract.Calendars.ALLOWED_AVAILABILITY,
                CalendarContract.Calendars.ACCOUNT_TYPE
        }, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            result = serializeEventCalendar(cursor);
            cursor.close();
        } else {
            result = null;
        }

        return result;
    }

    //region Event Accessors
    private ArrayList<Map<String, Object>> findEvents(Integer startDate, Integer endDate, ArrayList<String> calendars) {
        Calendar eStartDate = Calendar.getInstance();
        Calendar eEndDate = Calendar.getInstance();

        eStartDate.setTimeInMillis((long)startDate);

        eEndDate.setTimeInMillis((long)endDate);

        Cursor cursor;
        ContentResolver cr = getContentResolver();

        Uri.Builder uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(uriBuilder, eStartDate.getTimeInMillis());
        ContentUris.appendId(uriBuilder, eEndDate.getTimeInMillis());

        Uri uri = uriBuilder.build();

        String selection = "((" + CalendarContract.Instances.BEGIN + " >= " + eStartDate.getTimeInMillis() + ") " +
                "AND (" + CalendarContract.Instances.END + " <= " + eEndDate.getTimeInMillis() + ") " +
                "AND (" + CalendarContract.Instances.VISIBLE + " = 1) " +
                "AND (" + CalendarContract.Instances.STATUS + " IS NOT " + CalendarContract.Events.STATUS_CANCELED + ") ";

        if (calendars.size() > 0) {
            String calendarQuery = "AND (";
            for (int i = 0; i < calendars.size(); i++) {
                calendarQuery += CalendarContract.Instances.CALENDAR_ID + " = " + calendars.get(i);
                if (i != calendars.size() - 1) {
                    calendarQuery += " OR ";
                }
            }
            calendarQuery += ")";
            selection += calendarQuery;
        }

        selection += ")";

        cursor = cr.query(uri, new String[]{
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.RRULE,
                CalendarContract.Instances.CALENDAR_ID,
                CalendarContract.Instances.AVAILABILITY,
                CalendarContract.Instances.HAS_ALARM,
                CalendarContract.Instances.ORIGINAL_ID,
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.DURATION,
                CalendarContract.Instances.ORIGINAL_SYNC_ID
        }, selection, null, null);

        return serializeEvents(cursor);
    }

    private Map<String, Object> findEventById(String eventID) {

        Map<String, Object> result;
        Cursor cursor = null;
        ContentResolver cr = getContentResolver();
        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, Integer.parseInt(eventID));

        String selection = "((" + CalendarContract.Events.DELETED + " != 1))";

        cursor = cr.query(uri, new String[]{
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.RRULE,
                CalendarContract.Events.CALENDAR_ID,
                CalendarContract.Events.AVAILABILITY,
                CalendarContract.Events.HAS_ALARM,
                CalendarContract.Instances.DURATION
        }, selection, null, null);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            result = serializeEvent(cursor);
        } else {
            result = null;
        }

        cursor.close();

        return result;
    }

    private Map<String, Object> findEventInstanceById(String eventID) {

        Map<String, Object> result;
        Cursor cursor;
        ContentResolver cr = getContentResolver();

        Uri.Builder uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(uriBuilder, Long.MIN_VALUE);
        ContentUris.appendId(uriBuilder, Long.MAX_VALUE);
        Uri uri = uriBuilder.build();

        String selection = "(Instances._ID = " + eventID + ")";

        cursor = cr.query(uri, new String[]{
                CalendarContract.Instances._ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.RRULE,
                CalendarContract.Instances.CALENDAR_ID,
                CalendarContract.Instances.AVAILABILITY,
                CalendarContract.Instances.HAS_ALARM,
                CalendarContract.Instances.ORIGINAL_ID,
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.DURATION
        }, selection, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            result = serializeEvent(cursor);
            cursor.close();
        } else {
            result = null;
        }

        return result;
    }

    private int addEvent(String title, Map<String, Object> details, Map<String, Object> options) throws ParseException {
        ContentResolver cr = getContentResolver();
        ContentValues eventValues = new ContentValues();

        if (title != null) {
            eventValues.put(CalendarContract.Events.TITLE, title);
        }

        if (details.containsKey("description")) {
            eventValues.put(CalendarContract.Events.DESCRIPTION, (String)details.get("description"));
        }

        if (details.containsKey("location")) {
            eventValues.put(CalendarContract.Events.EVENT_LOCATION, (String)details.get("location"));
        }

        if (details.containsKey("startDate")) {
           eventValues.put(CalendarContract.Events.DTSTART, (long)details.get("startDate"));
          }

        if (details.containsKey("endDate")) {
             eventValues.put(CalendarContract.Events.DTEND, (long)details.get("endDate"));
        }

        if (details.containsKey("recurrence")) {
            String rule = createRecurrenceRule((String)details.get("recurrence"), null, null, null);
            if (rule != null) {
                eventValues.put(CalendarContract.Events.RRULE, rule);
            }
        }

        if (details.containsKey("recurrenceRule")) {
            Map<String, Object> recurrenceRule = (Map<String, Object>)details.get("recurrenceRule");

            if (recurrenceRule.containsKey("frequency")) {
                String frequency = (String)recurrenceRule.get("frequency");
                String duration = "PT1H";
                Integer interval = null;
                Integer occurrence = null;
                String endDate = null;

                if (recurrenceRule.containsKey("interval")) {
                    interval = (Integer)recurrenceRule.get("interval");
                }

                if (recurrenceRule.containsKey("duration")) {
                    duration = (String)recurrenceRule.get("duration");
                }

                if (recurrenceRule.containsKey("occurrence")) {
                    occurrence = (Integer) recurrenceRule.get("occurrence");
                }

                if (recurrenceRule.containsKey("endDate")) {
                    Calendar calendar = Calendar.getInstance();
                    FORMAT_DATE.setTimeZone(TimeZone.getTimeZone("GMT"));

                    calendar.setTimeInMillis((long)recurrenceRule.get("endDate"));
                    endDate = FORMAT_DATE.format(calendar.getTime());
                 }

                String rule = createRecurrenceRule(frequency, interval, endDate, occurrence);
                if (duration != null) {
                    eventValues.put(CalendarContract.Events.DURATION, duration);
                }
                if (rule != null) {
                    eventValues.put(CalendarContract.Events.RRULE, rule);
                }
            }
        }

        if (details.containsKey("allDay")) {
            eventValues.put(CalendarContract.Events.ALL_DAY, (boolean)details.get("allDay") ? 1 : 0);
        }

        if (details.containsKey("timeZone")) {
            eventValues.put(CalendarContract.Events.EVENT_TIMEZONE, (String)details.get("timeZone"));
        } else {
            eventValues.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        }

        if (details.containsKey("endTimeZone")) {
            eventValues.put(CalendarContract.Events.EVENT_END_TIMEZONE, (String)details.get("endTimeZone"));
        } else {
            eventValues.put(CalendarContract.Events.EVENT_END_TIMEZONE, TimeZone.getDefault().getID());
        }

        if (details.containsKey("alarms")) {
            eventValues.put(CalendarContract.Events.HAS_ALARM, true);
        }

        if (details.containsKey("availability")) {
            eventValues.put(CalendarContract.Events.AVAILABILITY, availabilityConstantMatchingString((String)details.get("availability")));
        }


        if (details.containsKey("id")) {
            int eventID = Integer.parseInt((String)details.get("id"));
            Map<String, Object> eventInstance = findEventById((String)details.get("id"));

            if (eventInstance != null) {
                Map<String, String> eventCalendar = (Map<String, String>)eventInstance.get("calendar");

                if (!options.containsKey("exceptionDate")) {
                    Uri updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID);

                    if (options.containsKey("sync") && (Boolean)options.get("sync")) {
                        syncCalendar(cr, eventCalendar.get("id"));
                        updateUri = eventUriAsSyncAdapter(updateUri, eventCalendar.get("source"), eventCalendar.get("type"));
                    }
                    cr.update(updateUri, eventValues, null, null);

                } else {
                    Calendar exceptionStart = Calendar.getInstance();
                    eventValues.put(CalendarContract.Events.ORIGINAL_INSTANCE_TIME, (long) options.get("exceptionDate"));


                    Uri exceptionUri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_EXCEPTION_URI, Integer.toString(eventID));

                    if (options.containsKey("sync") && (boolean)options.get("sync")) {
                        syncCalendar(cr, eventCalendar.get("id"));
                        eventUriAsSyncAdapter(exceptionUri, eventCalendar.get("source"), eventCalendar.get("type"));
                    }

                    try {
                        Uri eventUri = cr.insert(exceptionUri, eventValues);
                        if (eventUri != null) {
                            eventID = Integer.parseInt(eventUri.getLastPathSegment());
                        }
                    } catch (Exception e) {
                        Log.d("Event exception error", e.toString());
                    }
                }
            }

            if (details.containsKey("alarms")) {
                createRemindersForEvent(cr, Integer.parseInt((String)details.get("id")), (ArrayList<Integer>)details.get("alarms"));
            }

            if (details.containsKey("attendees")) {
                createAttendeesForEvent(cr, Integer.parseInt((String)details.get("id")), (ArrayList<Map<String, String>>)details.get("attendees"));
            }

            return eventID;

        } else {
            Map<String, Object> calendar;
            int eventID = -1;

            if (details.containsKey("calendarId")) {
                calendar = findCalendarById((String)details.get("calendarId"));

                if (calendar != null) {
                    eventValues.put(CalendarContract.Events.CALENDAR_ID, Integer.parseInt((String)calendar.get("id")));
                } else {
                    eventValues.put(CalendarContract.Events.CALENDAR_ID, 1);
                }

            } else {
                calendar = findCalendarById("1");
                eventValues.put(CalendarContract.Events.CALENDAR_ID, 1);
            }

            Uri createEventUri = CalendarContract.Events.CONTENT_URI;

            if (options.containsKey("sync") && (boolean)options.get("sync")) {
                syncCalendar(cr, (String)calendar.get("id"));
                createEventUri = eventUriAsSyncAdapter(CalendarContract.Events.CONTENT_URI, (String)calendar.get("source"), (String)calendar.get("type"));
            }

            Uri eventUri = cr.insert(createEventUri, eventValues);

            if (eventUri != null) {
                eventID = Integer.parseInt(eventUri.getLastPathSegment());

                if (details.containsKey("alarms")) {
                    createRemindersForEvent(cr, eventID, (ArrayList<Integer>)details.get("alarms"));
                }

                if (details.containsKey("attendees")) {
                    createAttendeesForEvent(cr, eventID, (ArrayList<Map<String, String>>)details.get("attendees"));
                }

                return eventID;

            }
            return eventID;
        }

    }

    private boolean removeEvent(String eventID, Map<String, Object> options) {
        int rows = 0;

        try {
            ContentResolver cr = getContentResolver();
            Map<String, Object> eventInstance = findEventById(eventID);
            Map<String, String> eventCalendar = (Map<String, String>)eventInstance.get("calendar");

            if (!options.containsKey("exceptionDate")) {
                Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, (long) Integer.parseInt(eventID));

                if (options.containsKey("sync") && (boolean)options.get("sync")) {
                    syncCalendar(cr, eventCalendar.get("id"));
                    uri = eventUriAsSyncAdapter(uri, eventCalendar.get("source"), eventCalendar.get("type"));
                }
                rows = cr.delete(uri, null, null);

            } else {
                ContentValues eventValues = new ContentValues();

                Calendar exceptionStart = Calendar.getInstance();
                           eventValues.put(CalendarContract.Events.ORIGINAL_INSTANCE_TIME, (long) options.get("exceptionDate"));

                eventValues.put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CANCELED);

                Uri uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_EXCEPTION_URI, eventID);

                if (options.containsKey("sync") && (boolean)options.get("sync")) {
                    uri = eventUriAsSyncAdapter(uri, eventCalendar.get("source"), eventCalendar.get("type"));
                }

                Uri exceptionUri = cr.insert(uri, eventValues);
                if (exceptionUri != null) {
                    rows = 1;
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return rows > 0;
    }

    //sync adaptors
    private Uri eventUriAsSyncAdapter (Uri uri, String accountName, String accountType) {
        uri = uri.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
                .build();

        return uri;
    }

    public static void syncCalendar(ContentResolver cr, String calendarId) {
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
        values.put(CalendarContract.Calendars.VISIBLE, 1);

        cr.update(ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, Long.parseLong(calendarId)), values, null, null);
    }
    //endregion

    //region Attendees
    private void createAttendeesForEvent(ContentResolver resolver, int eventID, ArrayList<Map<String, String>> attendees) {
        Cursor cursor = CalendarContract.Attendees.query(resolver, eventID, new String[] {
                CalendarContract.Attendees._ID
        });

        while (cursor.moveToNext()) {
            long attendeeId = cursor.getLong(0);
            Uri attendeeUri = ContentUris.withAppendedId(CalendarContract.Attendees.CONTENT_URI, attendeeId);
            resolver.delete(attendeeUri, null, null);
        }
        cursor.close();

        for (int i = 0; i < attendees.size(); i++) {
            Map<String, String> attendee = attendees.get(i);
            ContentValues attendeeValues = new ContentValues();
            attendeeValues.put(CalendarContract.Attendees.EVENT_ID, eventID);
            attendeeValues.put(CalendarContract.Attendees.ATTENDEE_EMAIL, attendee.get("url"));
            attendeeValues.put(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP, CalendarContract.Attendees.RELATIONSHIP_ATTENDEE);

            attendeeValues.put(CalendarContract.Attendees.ATTENDEE_NAME, attendee.get("firstName"));
            resolver.insert(CalendarContract.Attendees.CONTENT_URI, attendeeValues);
     }
    }
    //endregion

    //region Reminders
    private void createRemindersForEvent(ContentResolver resolver, int eventID, ArrayList<Integer> reminders) {

        Cursor cursor = CalendarContract.Reminders.query(resolver, eventID, new String[] {
                CalendarContract.Reminders._ID
        });

        while (cursor.moveToNext()) {
            long reminderId = cursor.getLong(0);
            Uri reminderUri = ContentUris.withAppendedId(CalendarContract.Reminders.CONTENT_URI, reminderId);
            resolver.delete(reminderUri, null, null);
        }
        cursor.close();

        for (int i = 0; i < reminders.size(); i++) {
            int minutes = Math.abs((Integer)reminders.get(i));
            ContentValues reminderValues = new ContentValues();

            reminderValues.put(CalendarContract.Reminders.EVENT_ID, eventID);
            reminderValues.put(CalendarContract.Reminders.MINUTES, minutes);
            reminderValues.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);

            resolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues);
         }
    }

    private ArrayList<Long> findReminderByEventId(String eventID, long startDate) {

        ArrayList<Long> results = new ArrayList<Long>();
        ContentResolver cr = getContentResolver();
        String selection = "(" + CalendarContract.Reminders.EVENT_ID + " = ?)";

        Cursor cursor = cr.query(CalendarContract.Reminders.CONTENT_URI, new String[]{
                CalendarContract.Reminders.MINUTES
        }, selection, new String[] {eventID}, null);

        while (cursor != null && cursor.moveToNext()) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(startDate);
            cal.add(Calendar.MINUTE, Integer.parseInt(cursor.getString(0)));

            results.add(cal.getTime().getTime());
        }

        if (cursor != null) {
            cursor.close();
        }

        return results;
    }
    //endregion

    //region Availability
    private ArrayList<String> calendarAllowedAvailabilitiesFromDBString(String dbString) {
        ArrayList<String> availabilitiesStrings = new ArrayList<String>();
        for(String availabilityId: dbString.split(",")) {
            switch(Integer.parseInt(availabilityId)) {
                case CalendarContract.Events.AVAILABILITY_BUSY:
                    availabilitiesStrings.add("busy");
                    break;
                case CalendarContract.Events.AVAILABILITY_FREE:
                    availabilitiesStrings.add("free");
                    break;
                case CalendarContract.Events.AVAILABILITY_TENTATIVE:
                    availabilitiesStrings.add("tentative");
                    break;
            }
        }

        return availabilitiesStrings;
    }

    private String availabilityStringMatchingConstant(Integer constant)
    {
        switch(constant) {
            case CalendarContract.Events.AVAILABILITY_BUSY:
            default:
                return "busy";
            case CalendarContract.Events.AVAILABILITY_FREE:
                return "free";
            case CalendarContract.Events.AVAILABILITY_TENTATIVE:
                return "tentative";
        }
    }

    private Integer availabilityConstantMatchingString(String string) throws IllegalArgumentException {
        if (string.equals("free")){
            return CalendarContract.Events.AVAILABILITY_FREE;
        }

        if (string.equals("tentative")){
            return CalendarContract.Events.AVAILABILITY_TENTATIVE;
        }

        return CalendarContract.Events.AVAILABILITY_BUSY;
    }
    //endregion

    //region Recurrence Rule
    private String createRecurrenceRule(String recurrence, Integer interval, String endDate, Integer occurrence) {
        String rrule;

        if (recurrence.equals("daily")) {
            rrule=  "FREQ=DAILY";
        } else if (recurrence.equals("weekly")) {
            rrule = "FREQ=WEEKLY";
        }  else if (recurrence.equals("monthly")) {
            rrule = "FREQ=MONTHLY";
        } else if (recurrence.equals("yearly")) {
            rrule = "FREQ=YEARLY";
        } else {
            return null;
        }

        if (interval != null) {
            rrule += ";INTERVAL=" + interval;
        }

        if (endDate != null) {
            rrule += ";UNTIL=" + endDate;
        } else if (occurrence != null) {
            rrule += ";COUNT=" + occurrence;
        }

        return rrule;
    }
    //endregion

    // region Serialize Events
    private ArrayList<Map<String, Object>> serializeEvents(Cursor cursor) {
        ArrayList<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

        while (cursor.moveToNext()) {
            results.add(serializeEvent(cursor));
        }

        cursor.close();

        return results;
    }

    private Map<String, Object> serializeEvent(Cursor cursor) {

        Map<String, Object> event = new HashMap<String, Object>();

        FORMAT_DATE.setTimeZone(TimeZone.getTimeZone("GMT"));

        Calendar foundStartDate = Calendar.getInstance();
        Calendar foundEndDate = Calendar.getInstance();

        boolean allDay = false;
        Long startDateUTC = 0L;
        Long endDateUTC = 0L;

        if (cursor.getString(3) != null) {
            startDateUTC = Long.parseLong(cursor.getString(3));
        }

        if (cursor.getString(4) != null) {
            endDateUTC = Long.parseLong(cursor.getString(4));
        }

        if (cursor.getString(5) != null) {
            allDay = cursor.getInt(5) != 0;
        }

        if (cursor.getString(7) != null) {
            HashMap recurrenceRule = new HashMap();
            String[] recurrenceRules = cursor.getString(7).split(";");

            event.put("recurrence", recurrenceRules[0].split("=")[1].toLowerCase());
            recurrenceRule.put("frequency", recurrenceRules[0].split("=")[1].toLowerCase());

            if (cursor.getColumnIndex(CalendarContract.Events.DURATION) != -1 && cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DURATION)) != null) {
                recurrenceRule.put("duration", cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DURATION)));
            }

            if (recurrenceRules.length >= 2 && recurrenceRules[1].split("=")[0].equals("INTERVAL")) {
                recurrenceRule.put("interval", Integer.parseInt(recurrenceRules[1].split("=")[1]));
            }

            if (recurrenceRules.length >= 3) {
                if (recurrenceRules[2].split("=")[0].equals("UNTIL")) {
                    try {
                        recurrenceRule.put("endDate", FORMAT_DATE.parse(recurrenceRules[2].split("=")[1]).getTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else if (recurrenceRules[2].split("=")[0].equals("COUNT")) {
                    recurrenceRule.put("occurrence", Integer.parseInt(recurrenceRules[2].split("=")[1]));
                }

            }

            event.put("recurrenceRule", recurrenceRule);
        }

        event.put("id", cursor.getString(0));
        event.put("calendar", findCalendarById(cursor.getString(cursor.getColumnIndex("calendar_id"))));
        event.put("title", cursor.getString(cursor.getColumnIndex("title")));
        event.put("description", cursor.getString(2));
        event.put("startDate", startDateUTC);
        event.put("endDate", endDateUTC);
        event.put("allDay", allDay);
        event.put("location", cursor.getString(6));
        event.put("availability", availabilityStringMatchingConstant(cursor.getInt(9)));

        if (cursor.getInt(10) > 0) {
            event.put("alarms", findReminderByEventId(cursor.getString(0), Long.parseLong(cursor.getString(3))));
        } else {
            ArrayList<Long> emptyAlarms = new ArrayList<Long>();
            event.put("alarms", emptyAlarms);
        }

        if (cursor.getColumnIndex(CalendarContract.Events.ORIGINAL_ID) != -1 && cursor.getString(cursor.getColumnIndex(CalendarContract.Events.ORIGINAL_ID)) != null) {
            event.put("originalId", cursor.getString(cursor.getColumnIndex(CalendarContract.Events.ORIGINAL_ID)));
        }

        if (cursor.getColumnIndex(CalendarContract.Instances.ORIGINAL_SYNC_ID) != -1 && cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.ORIGINAL_SYNC_ID)) != null) {
            event.put("syncId", cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.ORIGINAL_SYNC_ID)));
        }

        return event;
    }

    private ArrayList<Map<String, Object>> serializeEventCalendars(Cursor cursor) {
        ArrayList<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

        while (cursor.moveToNext()) {
            results.add(serializeEventCalendar(cursor));
        }

        cursor.close();

        return results;
    }

    private Map<String, Object> serializeEventCalendar(Cursor cursor) {

        Map<String, Object> calendar = new HashMap<String, Object>();

        calendar.put("id", cursor.getString(0));
        calendar.put("title", cursor.getString(1));
        calendar.put("source", cursor.getString(2));
        calendar.put("allowedAvailabilities", calendarAllowedAvailabilitiesFromDBString(cursor.getString(5)));
        calendar.put("type", cursor.getString(6));

        if (cursor.getString(3) != null) {
            calendar.put("isPrimary", cursor.getString(3).equals("1"));
        }

        int accesslevel = cursor.getInt(4);

        if (accesslevel == CalendarContract.Calendars.CAL_ACCESS_ROOT ||
                accesslevel == CalendarContract.Calendars.CAL_ACCESS_OWNER ||
                accesslevel == CalendarContract.Calendars.CAL_ACCESS_EDITOR ||
                accesslevel == CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
            calendar.put("allowsModifications", true);
        } else {
            calendar.put("allowsModifications", false);
        }

        return calendar;
    }
    // endregion

    //region React Native Methods
    public void getCalendarPermissions(Result promise) {
        SharedPreferences sharedPreferences = getSharedPreferences(RNC_PREFS, MODE_PRIVATE);
        boolean permissionRequested = sharedPreferences.getBoolean("permissionRequested", false);

        if (this.haveCalendarReadWritePermissions()) {
            promise.success("authorized");
        } else if (!permissionRequested) {
            promise.success("undetermined");
        } else {
            promise.success("denied");
        }
    }

    public void requestCalendarPermissions(Result promise) {
        SharedPreferences sharedPreferences = getSharedPreferences(RNC_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("permissionRequested", true);
        editor.apply();

        if (this.haveCalendarReadWritePermissions()) {
            promise.success("authorized");
        } else {
            this.requestCalendarReadWritePermission(promise);
        }
    }

    public void findCalendars(final Result promise) {
        if (this.haveCalendarReadWritePermissions()) {
            try {
                Thread thread = new Thread(new Runnable(){
                    @Override
                    public void run() {
                         promise.success(findEventCalendars());
                    }
                });
                thread.start();
            } catch (Exception e) {
                promise.error("calendar request error", e.getMessage(), null);
            }
        } else {
            promise.error("add event error", "you don't have permissions to retrieve an event to the users calendar", null);
        }
    }

    public void saveEvent(final String title, final Map<String, Object> details, final Map<String, Object> options, final Result promise) {
        if (this.haveCalendarReadWritePermissions()) {
            try {
                Thread thread = new Thread(new Runnable(){
                    @Override
                    public void run() {
                        int eventId;
                        try {
                            eventId = addEvent(title, details, options);
                            if (eventId > -1) {
                                promise.success(Integer.toString(eventId));
                            } else {
                                promise.error("add event error", "Unable to save event", null);
                            }
                        } catch (ParseException e) {
                            promise.error("add event error", e.getMessage(), null);
                        }
                    }
                });
                thread.start();
            } catch (Exception e) {
                promise.error("add event error", e.getMessage(), null);
            }
        } else {
            promise.error("add event error", "you don't have permissions to add an event to the users calendar", null);
        }
    }

    public void findAllEvents(final int startDate, final int endDate, final ArrayList<String> calendars, final Result promise) {

        if (this.haveCalendarReadWritePermissions()) {
            try {
                Thread thread = new Thread(new Runnable(){
                    @Override
                    public void run() {
                        promise.success(findEvents(startDate, endDate, calendars));
                    }
                });
                thread.start();

            } catch (Exception e) {
                promise.error("find event error", e.getMessage(), null);
            }
        } else {
            promise.error("find event error", "you don't have permissions to read an event from the users calendar", null);
        }

    }

    public void findById(final String eventID, final Result promise) {
        if (this.haveCalendarReadWritePermissions()) {
            try {
                Thread thread = new Thread(new Runnable(){
                    @Override
                    public void run() {
                        promise.success(findEventById(eventID));
                    }
                });
                thread.start();

            } catch (Exception e) {
                promise.error("find event error", e.getMessage(), null);
            }
        } else {
            promise.error("find event error", "you don't have permissions to read an event from the users calendar", null);
        }

    }

    public void removeEvent(final String eventID, final Map<String, Object> options, final Result promise) {
        if (this.haveCalendarReadWritePermissions()) {
            try {
                Thread thread = new Thread(new Runnable(){
                    @Override
                    public void run() {
                        boolean successful = removeEvent(eventID, options);
                        promise.success(successful);
                    }
                });
                thread.start();

            } catch (Exception e) {
                promise.error("error removing event", e.getMessage(), null);
            }
        } else {
            promise.error("remove event error", "you don't have permissions to remove an event from the users calendar", null);
        }

    }

    public void openEventInCalendar(String eventID) {
        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, Long.parseLong(eventID));
        Intent sendIntent = new Intent(Intent.ACTION_VIEW).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setData(uri);

        if (sendIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(sendIntent);
        }
    }

     public void uriForCalendar(Result promise) {
        promise.success(CalendarContract.Events.CONTENT_URI.toString());
    }
    //endregion
}