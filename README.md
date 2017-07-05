# GeoSensor
The GeoSensor geotagged data acquisition system was build as the bachelor thesis of Eike Trumann.

It is made of two major parts: a hardware measurement transducer and an Android application.

The hardware can be build by anyone familiar with the Arduino platform. Apart from the Arduino itself the only required part needed to build the transducer is a Bluetooth serial pass-through module.
The system was tested using the commonly available and rather cheap HC-05 bluetooth module.

When the transducer device has been completed, the only neccessary step is installing the Andorid app. An this moment, the compiled android App can be found in this repository or compiled from source. 
The app does not need to be adjusted as long as your transducer respects the protocol specification.

Troubleshooting:
If the system does not work as expected, the most common problem is the Arduino running low on SRAM. As the protocol used between the transducer device and the Android app is well-defined (you can read about it the the bacelorthesis.pdf) your best bet is to control if the data your transducer sends respects the protocol specification and contains the information you want by redirecting it to the serial monitor on the pc.

Another common problem is the electrical connection between the Arduino and the bluetooth module. If you connect the computer for programming and the Bluetooth module at the same time, probably neither of them will work.
