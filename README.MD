# What it does
This is a front-end written in Java for a 24-bit data capture system.  Data is read via USB bulk transfers
 at 406k samples per second and can be output in realtime as:

* An oscillogram
* A voltage spectrum
* A waterfall plot
* Downconverted audio for listening to ultrasound in realtime

<img src="assets/Oscillogram display.png" width="200"/>
<img src="assets/Spectrum display.png" width="200"/>
<img src="assets/Waterfall display.png" width="200"/>
<img src="assets/Audio display.png" width="200"/>


# Some examples
The below results include the addition of a low noise preamp and either a Knowles 
FG-23629 hearing aid microphone or a Primo EM258 electret capsule.

Bat recording, slowed down to 44100 Hz sample rate:

[Bat search call - slowed down by a factor of 20](assets/BatSearchCall.wav)


Noisy power supply, slowed down to 22050 Hz sample rate:

[Noisy power supply](assets/ProcessedPSU.wav)


(c) 2024 Brian Knight
