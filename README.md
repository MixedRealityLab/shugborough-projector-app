# shugborough-projector-app
Test app for Android powered projectors in project with Ben Wigley and Holger Schnadelbach.

The app displays videos based on sensor data send from a connected Arduino.

The app attempts to start when the device boots. When opened the app will ask the user to 
show 3 colours to the Arduino's colour sensor. These are associated to 3 videos: it loads 
videos from the downloads directory named "test1.mp4", "test2.mp4" and "test3.mp4". 

After this setup a video is played when colour data is received. The video and is played 
depends on how close the presented colour is to the 3 colours presented in setup. If it's 
closest to the first colour video "test1.mp4" will be played, closest to second 
"test2.mp4", and closest to third "test3.mp4".

How "close" the colours are is the absolute difference between the values returned by the 
sensor.

## Dependancies

- *UsbSerial* (MIT License) https://github.com/felHR85/UsbSerial
