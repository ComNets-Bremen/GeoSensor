/**
  Name:    GeoSensor.ino
  Created:  22.04.2017
  Author:   Eike Trumann
*/

/**
  These definitions are used to provide basic settings for the software.
  Change them according to your needs.
  Defines can be commented out using a // before the line.
*/
/** If GPS is defined, the software will include means to handle GPS data */
#define GPS_ENABLED
/** The baudrate used by the GPS module (typically 9600 baud) */
#define GPS_BAUDRATE 9600
/** Define this if the GPS is connected using a software serial
  Change the communication pins according to your wiring */
#ifdef GPS_ENABLED
#define GPS_SOFTWARE_SERIAL
#define GPS_SERIAL_RX 11
#define GPS_SERIAL_TX 10

/** If the GPS receiver uses a hardware serial connection, set this to the appropriate serial port */
// #define GPS_SERIAL Serial3
#endif GPS_ENABLED

/** Define the (hardware) serial connection used by the bluetooth module. Typically Serial, but on some boards Serial1.*/
#define BLUETOOTH_SERIAL Serial
/** The baudrate used for the bluetooth module. Typically 38400 (preferred if you can configure the model) or 9600 */
#define BLUETOOTH_BAUDRATE 38400

/** The pin to which the transmission status LED is connected */
#define STATUS_LED_PIN A2

/** The pin to which the transmission status LED is connected */
#define GPS_LED_PIN A1

/** The pin to which the button to trigger measurements is connected.
  This needs to be a pin allowing interrupts to be attached. On most Arduinos only pin 2 and 3 can be used.*/
#define BUTTON_PIN 2

/** If EEPROM is defined, the software uses the EEPROM in the microcontroller to save data for resending
  This can only be defined on devices including an EEPROM (like Arduino Uno, but for example not Arduino Due)
  Use the board documentation on arduino.cc to determine if your Arduino has EEPROM. For most use cases you will need at least 1 kB of EEPROM.*/
#define EEPROM_RESEND

/** If there is enough SRAM, it can be used to buffer outgoing messages for resending. Use this on Arduino Zero and Due
  The option is mutually exclusive with the EEPROM define, on some devices like Arduino Mega one might be chosen.
  This option is only recommended if the Arduino has more than 4 kiB of SRAM.*/
// #define SRAM_RESEND

/** This string is used in the transmitted data to give information about the version of this software.
  You might change it to whatever you want as long as it only contains letters, numbers, spaces and full stops / periods (but no commas) */
#define VERSION "GeoSensor Template " __DATE__ " " __TIME__

/** The size of the buffer needed depends on the contents to be included. The size is limited by the avaliable quantity of SRAM.
  Typically 1024 Bytes is a reasonable size, but if you want to use SRAM as a buffer for resending data you might need to reduce this.
  Most problems that might occur in this program are releated to buffer sizes.*/
#define JSON_BUFFER_SIZE 800

/**
  Imports used for basic functionality.
  Make sure the librarys are avaliable.
  Do not remove or change theese lines.
*/
/** The ArduinoJson library is used to provide JSON string encoding for bluetooth transmission
  Download in Arduino IDE or from https://github.com/bblanchon/ArduinoJson */
#include <ArduinoJson.h>

#ifdef GPS_ENABLED
/** The TinyGPS++ Library is used to parse the NMEA input from the GPS module
  Download from https://github.com/mikalhart/TinyGPSPlus */
#include <TinyGPS++.h>
#endif

#ifdef EEPROM_RESEND
/** The core EEPROM library */
#include <EEPROM.h>
/** External library used to write streams into EEPROM
  Download from https://github.com/lasselukkari/EepromStream */
#include <EepromStream.h>
#endif

/** This characters are defined by the ASCII table and used in the bluetooth protocol. Do not change.*/
#define SOT 0x03 // Start of Text - Reserved for future Use
#define EOT 0x03 // End of Text - Used to end a transmission message produced by this program
#define ACK 0x06 // Acknowledge - Returned from the receiver if the message was received without detected problems
#define DC1 0x11 // Device Control 1 - Used to trigger a measurement
#define DC2 0x11 // Device Control 2 - Reserved for future use
#define DC3 0x11 // Device Control 3 - Reserved for future use
#define DC4 0x11 // Device Control 4 - Reserved for future use
#define NAK 0x15 // Negative Acknowledge - Triggers a resend of the last message

/**
   The actual code starts here.
   You typically only need to modify the code above the delimiter line in order to support the sensors you need.
*/

/**
   First of all, include the libraries needed for your sensors.
*/
// The DHT 11 humodity and temperature sensor
#include <DHT_U.h>
// The DS18B20 digital temperature sensor
#include <OneWire.h>
#include <DallasTemperature.h>

/**
   Set the constants needed for sensor operation
*/
// For the DHT humidity and temperature sensor
#define DHTPIN 12
#define DHTTYPE DHT11
// For the D18B20 temperature sensor
#define ONE_WIRE_PIN 9

/**
   Initialise the objects used to represent the sensors
*/
// This object is used to read Data from DHT
DHT dht(DHTPIN, DHTTYPE);
// This is the one-wire connection for the DS18B20
OneWire oneWireBus(ONE_WIRE_PIN);
DallasTemperature ds18b20(&oneWireBus);

/**
   Do anything that might be neccessary to get the sensors running.
   This method is called once on device startup.
*/
void initializeSensors() {
  pinMode(DHTPIN, INPUT);
  ds18b20.begin();
}

/**
   This is where the actuas sensor data is put into the message sent to the Android device.
   The structure needs to follow the example exactly, as only messages carrying theese exact keys
   (i.e. type, sensor, name, value and unit) will be interpreted correctly in the android app.
   The values are however up to you.
   Value is a floating point number (of arbitrary precision when build with double_with_n_digits()).
   All other data fields are strings which must only consist of letters, numbers, spaces and full stops / periods.
   If the strings are constant (you write them here in the code) consider using the F()-Macro like in the example as this will save SRAM.
   Note that including too many sensors or very long strings might lead to a memory shortage.
   You might notice strange behaviour on resends if the total message can not fit into EEPROM.

   Usage example:
   // You need to create a nested object for each value
   JsonObject& potentiometer = sensors.createNestedObject(F("potentiometer"));

   // You need to always include all five data fields for each object
   temperature_air[F("type")] = F("voltage");
   temperature_air[F("sensor")] = F("analogRead on Arduino pin");
   temperature_air[F("name")] = F("potentiometer test device");
   temperature_air[F("value")] = double_with_n_digits(analogRead(A1)*(1024.0 / 3.3));
   temperature_air[F("unit")] = F("V");
*/
void buildSensorJson(JsonObject& sensors) {
  JsonObject& temperature_air = sensors.createNestedObject(F("temperature-air"));

  temperature_air[F("type")] = F("temperature");
  temperature_air[F("sensor")] = F("DHT11");
  temperature_air[F("name")] = F("air-temperature");
  double temperature = dht.readTemperature();
  // This avoids sending a NaN and replaces it with some illegitimate value
  temperature_air[F("value")] = double_with_n_digits(temperature == temperature ? temperature : -1000, 4);
  temperature_air[F("unit")] = F("deg C");

  JsonObject& humidity = sensors.createNestedObject(F("humidity-air"));

  humidity[F("type")] = F("humidity");
  humidity[F("sensor")] = F("DHT11");
  humidity[F("name")] = F("air-humidity");
  double humidityValue = dht.readHumidity();
  humidity[F("value")] = double_with_n_digits(humidityValue == humidityValue ? humidityValue : -1, 4);
  humidity[F("unit")] = "%";

  JsonObject& temperature_soil = sensors.createNestedObject("temperature-soil");

  temperature_soil[F("type")] = F("temperature");
  temperature_soil[F("sensor")] = F("DS18B20");
  temperature_soil[F("name")] = F("soil-temperature");
  // this might take some time
  ds18b20.requestTemperatures();
  temperature_soil[F("value")] = ds18b20.getTempCByIndex(0);
  temperature_soil[F("unit")] = F("deg C");
}

String getComment() {
  return F("test comment");
}

/*------------------------------------------------------------------------------------------------------------------*/
/**
   The code under this line provides the basic functionality of the software and usually does not need to be modified
*/
/*------------------------------------------------------------------------------------------------------------------*/

/** The processor time when the last message has been sent */
long lastSentMillis = 0;

/** The EEPROM uses the first three bytes for different information and therefore the information starts at position 0x03 */
#define EEPROM_MESSAGE_START 0x03

/** This object is used to parse NMEA data and store it's contents */
#ifdef GPS_ENABLED
TinyGPSPlus gps;
#endif

/**
   this tag is set if measurement data should be acquired and sent via bluetooth
   it is needed in order to avoid running all the code inside of an interrupt service routine (ISR)
*/
boolean acquireTag = false;

/**
   If the GPS module is connected using a software serial interface, the interface is prepared here
*/
#ifdef GPS_SOFTWARE_SERIAL
#include <SoftwareSerial.h>
SoftwareSerial gpsSerial(GPS_SERIAL_RX, GPS_SERIAL_TX);
#define GPS_SERIAL gpsSerial
#endif

/** if we want to save the message to SRAM we need this variables */
#ifdef SRAM_RESEND
bool receiveConfirmed = true;
String lastOutput = "";
#endif

/**
   The serial connections are set to the required baudrate and the button pin is configured as an input
*/
void setup() {
  // this is the red state LED
  pinMode(STATUS_LED_PIN, OUTPUT);
  digitalWrite(STATUS_LED_PIN, LOW);
  pinMode(GPS_LED_PIN, OUTPUT);
  digitalWrite(GPS_LED_PIN, LOW);

  // this is the Bluetooth connection (and PC debug)
  BLUETOOTH_SERIAL.begin(BLUETOOTH_BAUDRATE);
  BLUETOOTH_SERIAL.write(EOT); // empty the input pipeline in the Android device

  // this is the connection to the GPS module
#ifdef GPS_ENABLED
  GPS_SERIAL.begin(9600);
#endif

  // pin 2 is connected to a button pulling it to gnd when pressed
  pinMode(2, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(2), setAcquireTag, FALLING);

  initializeSensors();
}

// the loop function runs over and over again until power down or reset
void loop() {
#ifdef EEPROM_RESEND
  if (millis() - lastSentMillis > 5000 && EEPROM.read(0x00) == true) {
    resend();
  }
#elif defined SRAM_RESEND
  if (millis() - lastSentMillis > 5000 && !receiveConfirmed) {
    resend();
  }
#endif



  if (acquireTag) {
    // this signalises to the user we are handling the request
    digitalWrite(STATUS_LED_PIN, HIGH);
    // the data is acquired and sent
    buildDataPacket();
    // we set the send time to determine the right time for a retry in case no acknowledgement was received
    lastSentMillis = millis();
    // the tag is reset and thus another measurement might be triggered
    acquireTag = false;
  }

  // the data arriving from the GPS module is given to the parser (which also saves the state)
#ifdef GPS_ENABLED
  while (GPS_SERIAL.available() > 0) {
    gps.encode(GPS_SERIAL.read());
  }

  // show LED when GPS data is ready
  if (gps.location.isValid() && gps.location.age() <= 3000) {
    digitalWrite(GPS_LED_PIN, HIGH);
  } else {
    // Let the LED blink to show the device is operating
    if (millis() % 1000 < 500) {
      digitalWrite(GPS_LED_PIN, HIGH);
    } else {
      digitalWrite(GPS_LED_PIN, LOW);
    }
  }
#endif

#ifndef GPS_ENABLED
  // Let the LED blink to show the device is operating
  if (millis() % 1000 < 500) {
    digitalWrite(GPS_LED_PIN, HIGH);
  } else {
    digitalWrite(GPS_LED_PIN, LOW);
  }
#endif


  // Incoming commands are read from the serial interface
  while (BLUETOOTH_SERIAL.available()) {
    // As the protocol only uses one-byte-messages, only one char is evaluated at a time
    char incoming = BLUETOOTH_SERIAL.read();

    switch (incoming) {
      case ACK: // ACK Acknowledgement
      case 'a': // for debug purposes
#ifdef EEPROM_RESEND
        // the first EEPROM cell contains a boolean persisting in case the message has never been received
        EEPROM.write(0x00, false);
#endif
#ifdef SRAM_RESEND
        receiveConfirmed = true;
#endif
        // the status LED is turned off
        digitalWrite(STATUS_LED_PIN, LOW);
        break;
      case NAK: // NAK Negative Acknowledgement
      case 'b':
        resend();
        break;
      // DC1 is a request equivalent to pushing the trigger button.
      case DC1: // DC1 Device Control 1
      case 'c':
        setAcquireTag();
        break;
    }
  }
}

/**
   This is the interrupt service routine for the button press.
   It only sets a flag in order to not block other interrupts.
*/
void setAcquireTag() {
  acquireTag = true;
}

/**
   The resend method is different for the different types of data buffers.
   Therefore there is a version for EEPROM, one for SRAM and one mockup that does nothing.
   The method will repeat writing out the whole 0last message to the serial transceiver.
*/
#ifdef EEPROM_RESEND
void resend() {
  // This will save SRAM as we do not need a copy in memory
  for (int i = 5; true; i++) {
    byte c = EEPROM.read(i);
    // If the char is EOT we have reached the end of the message
    if (c == EOT) {
      break;
    }
    // Every char is written out on the Bluetooth serial connection
    BLUETOOTH_SERIAL.write(c);
  }

  // Writing the message is the same as writing it the first time.
  BLUETOOTH_SERIAL.println("");
  BLUETOOTH_SERIAL.write(EOT);
  BLUETOOTH_SERIAL.flush();

  // As we wait some time to repeat a message it has not been received correctly the counter is reset
  lastSentMillis = millis();
}
#endif

#ifdef SRAM_RESEND
void resend() {
  BLUETOOTH_SERIAL.print(lastOutput);

  BLUETOOTH_SERIAL.println("");
  BLUETOOTH_SERIAL.write(EOT);
  BLUETOOTH_SERIAL.flush();

  lastSentMillis = millis();
}
#endif

#if !defined(EEPROM_RESEND) && !defined(SRAM_RESEND)
void resend() {
  // We can not resend if we do not have a buffer
}
#endif

/**
   This method assembles a data packet.
   It calls the appropriate functions to fill the position and sensor objects.
   The data then is formatted as a string and written to the serial transceiver.
   If a method to retain the data for resending is active, the data is also stored there.
*/
void buildDataPacket() {
  StaticJsonBuffer<JSON_BUFFER_SIZE> jsonBuffer;
  JsonObject& message = jsonBuffer.createObject();

  // The basic informations about the software are given
  message[F("arduino_software")] = F(VERSION);
  message[F("arduino_time")] = millis();
  message[F("comment")] = getComment();

  //The objects must be called exactly like this
#ifdef GPS_ENABLED
  JsonObject& position = message.createNestedObject(F("position"));
  buildPositionJson(position);
#endif

  // The buildSensorJson method here is the one that is adapted to the sensors in use
  JsonObject& sensors = message.createNestedObject(F("sensors"));
  buildSensorJson(sensors);

  // The message is written out to the serial transmitter (pretty format is for easier debugging, might be changed to prontTo()
  message.prettyPrintTo(BLUETOOTH_SERIAL);

  // The message ends in a newline and an EOT character as specified in the protocol
  BLUETOOTH_SERIAL.println("");
  BLUETOOTH_SERIAL.write(EOT);
  BLUETOOTH_SERIAL.flush();

  // If we use EEPROM for resending, the message is written into EEPROM
#ifdef EEPROM_RESEND
  EEPROM.put(0x00, true);
  EepromStream eepromStream = EepromStream(EEPROM_MESSAGE_START, EEPROM.length() - 5);
  eepromStream.init();
  message.prettyPrintTo(eepromStream);
  eepromStream.write(0x03);
  eepromStream.flush();
#endif

  // If we use SRAM it is written into a global variable
#ifdef SRAM_RESEND
  receiveConfirmed = false;
  lastOutput = "";
  message.prettyPrintTo(lastOutput);
#endif
}

#ifdef GPS_ENABLED
void buildPositionJson(JsonObject& position) {
  if (gps.location.isValid()) {
    position[F("valid")] = gps.location.isValid();
    position[F("age")] = gps.location.age();
    position[F("latitude")] = double_with_n_digits(gps.location.lat(), 8);
    position[F("longitude")] = double_with_n_digits(gps.location.lng(), 8);
    position[F("altitude")] = gps.altitude.meters();
    position[F("date")] = gps.date.value();
    position[F("time")] = gps.time.value();
  }
  else {
    position[F("valid")] = gps.location.isValid();
  }
}
#endif



