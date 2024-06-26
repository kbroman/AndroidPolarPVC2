# PolarPVC2 <img src="app_icon.png" align="right" alt="PolarPVC2 app logo with red background and an ECG trace in white of three heart beats with middle one being a PVC"/>

[![zenodo DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.11626183.svg)](https://doi.org/10.5281/zenodo.11626183)

This is an Android app to get ECG data from a [Polar H10 chest-strap
heart rate
sensor](https://www.polar.com/us-en/sensors/h10-heart-rate-sensor) and
identify [Premature Ventricular Complexes
(PVCs)](https://en.wikipedia.org/wiki/Premature_ventricular_contraction)
and to estimate the current approximate percent PVC.

It uses the [Polar SDK](https://github.com/polarofficial/polar-ble-sdk) and
[Android Plot](https://github.com/halfhp/androidplot).

I learned a great deal, and adapted much of the code, from the [KE.Net
ECG](https://github.com/KennethEvans/KE.Net-ECG) app.

The Device ID is hard-coded in [`MainActivity.kt`](https://github.com/kbroman/AndroidPolarPVC2/blob/main/app/src/main/java/org/kbroman/android/polarpvc2/MainActivity.kt#L35).

---

Licensed under the [MIT license](LICENSE). (See <https://en.wikipedia.org/wiki/MIT_License>.)
