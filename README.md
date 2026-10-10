# Jetour Cluster Grid

Android app that draws a 24-column grid on the instrument-cluster display (default display id 2) and
hosts widgets in it: **Weather**, **Media**, **Seat belts + moving-with-unfastened-belt alarm**.

Layout (edit `ClusterLayout.kt`):  weather cols 1-6 | media cols 7-16 | belts cols 17-24.
Add a widget = subclass `ClusterWidget`, add one `grid.addWidget(...)` line.

## Build
Open the folder in Android Studio (it creates the Gradle wrapper), then Build > Build APK.

## Install & one-time setup (ADB)
    adb install -r app-debug.apk
    adb shell appops set com.example.jetourcluster SYSTEM_ALERT_WINDOW allow
    adb shell cmd notification allow_listener com.example.jetourcluster/com.example.jetourcluster.MediaListenerService
    adb shell pm grant com.example.jetourcluster android.permission.ACCESS_COARSE_LOCATION   # optional, weather by location
    adb shell dumpsys display | grep "Display Id="      # confirm which id is the cluster
    adb shell am start -n com.example.jetourcluster/.MainActivity

Open the app, check the display id, tick **Simulator** first to test the grid, alarm and widgets, then untick it to use real car data.

## Vehicle data (the part that depends on your firmware)
`CarApiSource` reads `PERF_VEHICLE_SPEED` and `SEAT_BELT_BUCKLED` (+ `SEAT_OCCUPANCY`) via the Android
Automotive Car API. Those permissions (`CAR_SPEED`, `CONTROL_CAR_SEATS`) are privileged on most builds, so a
normal APK may get no data (widget shows NO DATA, alarm stays silent). Options: install as a privileged/system
app, check `adb shell dumpsys car_service` / `logcat -s CarApiSource` for what your unit exposes, or write
another `VehicleSource` for the Chery/Autolink vendor API. Driver/passenger seat area ids are in `ClusterPrefs`.

## Build with GitHub Actions
Every push to `main` builds the APK: Actions tab > latest run > Artifacts > `JetourCluster-debug`.
To publish a release: `git tag v0.1 && git push origin v0.1` and the APK is attached to the Release.
