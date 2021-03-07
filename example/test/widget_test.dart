import 'package:flutter/material.dart';
import 'package:flutter_native_timezone_example/main.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('Verify Platform version', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(MyApp());

    // Verify that platform version is retrieved.
    expect(
      find.byWidgetPredicate(
            (Widget widget) =>
        widget is Text &&
            widget.data?.startsWith('Local timezone:') == true,
      ),
      findsOneWidget,
    );
  });
}
