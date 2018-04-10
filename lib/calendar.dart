import 'dart:async';
import 'package:flutter/services.dart';

class SystemCalendarData {
  String id;
  String title;
  String source;
  String type;
  bool isPrimary;
  bool allowsModifications;
  List<String> allowedAvailabilities;

  static const String _ID = "id";
  static const String _TITLE = "title";
  static const String _SOURCE = "source";
  static const String _ALLOWEDAVAILABILITIES = "allowedAvailabilities";
  static const String _TYPE = "type";
  static const String _ALLOWSMODIFICATIONS = "allowsModifications";
  static const String _ISPRIMARY = "isPrimary";

  void fromSystem(Map<String, dynamic> data) {
    id = data[_ID];
    title = data[_TITLE];
    source = data[_SOURCE];
    allowedAvailabilities= data[_ALLOWEDAVAILABILITIES];
    type = data[_TYPE];
    isPrimary = data[_ISPRIMARY];
    allowsModifications = data[_ALLOWSMODIFICATIONS];
  }

  Map<String, dynamic> toSystem() {
    Map<String, dynamic> data = new Map<String, dynamic>();
    data[_ID] = id;
    data[_TITLE] = title;
    data[_SOURCE] = source;
    data[_ALLOWEDAVAILABILITIES] = allowedAvailabilities;
    data[_TYPE] = type;
    data[_ISPRIMARY] = isPrimary;
    data[_ALLOWSMODIFICATIONS] = allowsModifications;
  }

}

class ReoccuranceRule {
   String frequency;
   String duration;
   int interval;
   DateTime endDate;
   int occurance;

   static const String _FREQUENCY = "frequency";
   static const String _DURATION = "duration";
   static const String _INTERVAL = "interval";
   static const String _ENDDATE = "endDate";
   static const String _OCCURANCE = "occurance";

   void fromSystem(Map<String, dynamic> data) {
     frequency = data[_FREQUENCY];
     duration = data[_DURATION];
     interval = data[_INTERVAL];
     if (data.containsKey(_ENDDATE)) {
       endDate = new DateTime.fromMillisecondsSinceEpoch(data[_ENDDATE]);
     }
     if (data.containsKey(_OCCURANCE)) {
       occurance = data[_OCCURANCE];
     }
   }

   Map<String, dynamic> toSystem() {
     Map<String, dynamic> data = new Map<String, dynamic>();
     data[_FREQUENCY] = frequency;
     data[_DURATION] = duration;
     data[_INTERVAL] = interval;
     if (endDate != null) {
       data[_ENDDATE] = endDate.millisecondsSinceEpoch;
     }
     if (occurance != null) {
       data[_OCCURANCE] = occurance;
     }
   }
}

class CalendarEvent {
  String id;
  String originalId;
  String syncId;
  String title;
  String description;
  DateTime startDate;
  DateTime endDate;
  bool allDay;
  String location;
  String availability;
  String reoccurance;
  List<ReoccuranceRule> reoccuranceRules;
  List<DateTime> alarms;
  SystemCalendarData calendar;

  static const String _ID = "id";
  static const String _CALENDAR = "calendar";
  static const String _TITLE = "title";
  static const String _DESCRIPTION = "description";
  static const String _STARTDATE = "startDate";
  static const String _ENDATE = "endDate";
  static const String _ALLDAY = "allDay";
  static const String _LOCATION = "location";
  static const String _AVAILABILITY = "availability";
  static const String _ALARMS = "alarms";
  static const String _ORIGINALID = "originalId";
  static const String _SYNCID = "syncId";


  void fromSystem(Map<String, dynamic> data) {
    id = data[_ID];
    calendar = new Calendar();
    calendar.fromSystem(data[_CALENDAR]);
    title = data[_TITLE];
    description = data[_DESCRIPTION];
    startDate = new DateTime.fromMillisecondsSinceEpoch(data[_STARTDATE]);
    endDate = new DateTime.fromMillisecondsSinceEpoch(data[_ENDATE]);
    allDay = data[_ALLDAY];
    location = data[_LOCATION];
    availability = data[_AVAILABILITY];
    originalId = data[_ORIGINALID];
    syncId = data[_SYNCID];
    alarms = data[_ALARMS];
  }

  Map<String, dynamic> toSystem() {
    Map<String, dynamic> data = new Map<String, dynamic>();

    data[_ID] = id;
    data[_CALENDAR] = calendar.toSystem();
    data[_TITLE] = title;
    data[_DESCRIPTION] = description;
    data[_STARTDATE] = startDate.millisecondsSinceEpoch;
    data[_ENDATE] = endDate.millisecondsSinceEpoch;
    data[_ALLDAY] = allDay;
    data[_LOCATION] = location;
    data[_AVAILABILITY] = availability;
    data[_ORIGINALID] = originalId;
    data[_SYNCID] = syncId;
    data[_ALARMS] = alarms;
  }
}

class SystemCalendar {
  static SystemCalendar instance = new SystemCalendar();
  static MethodChannel _channel = const MethodChannel("com.whelksoft.calendar");


  Future<bool> requestPermissions() {
    return _channel.invokeMethod("requestCalendarPermissions");
  }

  Future<String> getCalendarPermissions() {
    return _channel.invokeMethod("getCalendarPermissions");
  }

  Future<List<SystemCalendarData>> findCalendars() async {
    List<Map<String, dynamic>> data = await _channel.invokeMethod("findCalendars");
    List<SystemCalendarData> ret = [];
    data.forEach((Map<String, dynamic> data) {
      SystemCalendarData cal = new SystemCalendarData();
      cal.fromSystem(data);
      ret.add(cal);
    });
    return ret;
  }
}


