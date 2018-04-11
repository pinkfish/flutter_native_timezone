import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_native_timezone/timezone.dart';
import 'dart:async';

void main() => runApp(new MyExampleApp());

class MyExampleApp extends StatefulWidget {
  @override
  _MyExampleState createState() => new _MyExampleState();
}

class _MyExampleState extends State<MyExampleApp> {
  String _timezone = 'Unknown';

  @override
  initState() {
    super.initState();
    initTimezone();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<bool> initTimezone() async {
    String timezone;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      timezone = await NativeTimezone.getLocalTimezone();
    } on PlatformException {
      timezone = 'Failed to get local timezone.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted)
      return false;

    setState(() {
      _timezone = timezone;
    });
    return true;
  }

  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: new Scaffold(
        appBar: new AppBar(
          title: new Text('Local timezone'),
        ),
        body: new Center(
          child: new Text('Local timezone: $_timezone\n'),
        ),
      ),
    );
  }
}
