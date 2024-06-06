# PolarPVC2

This is an Android app to get ECG data from a [Polar H10 chest-strap
heart rate
sensor](https://www.polar.com/us-en/sensors/h10-heart-rate-sensor) and
identify [Premature Ventricular Complexes
(PVCs)](https://en.wikipedia.org/wiki/Premature_ventricular_contraction)
and to estimate the current approximate percent PVC.

It uses the [Polar SDK](https://github.com/polarofficial/polar-ble-sdk) and
[Android Plot](https://github.com/halfhp/androidplot).

I learned a great deal from the [KE.Net
ECG](https://github.com/KennethEvans/KE.Net-ECG) app.

The Device ID is hard-coded in [`MainActivity.kt`](https://github.com/kbroman/AndroidPolarPVC2/blob/main/app/src/main/java/com/kbroman/android/polarpvc/MainActivity.kt#L39).

---

Licensed under the [MIT license](LICENSE). (See <https://en.wikipedia.org/wiki/MIT_License>.)
