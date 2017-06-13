package de.eiketrumann.geosensor;

import java.util.Hashtable;

/**
 * A measure data object represents a single measurement of a single measured variable.
 * The objects can be directly mapped to a database table.
 *
 * Created by Eike on 15.02.2017.
 */
class MeasureData{
    /** The type indicates a possibly translatable string of the measurement category (eg. length) */
    private final String type;
    /** This indicates the technical type designation of the sensor (eg. DS18B20) */
    private final String sensor;
    /** This is a name given by the programmer to designate a specific sensor in the transducer*/
    private final String name;
    /** The value is the actual numeric measured value */
    private final double value;
    /** The unit of the measurement (eg. Watt or Kelvin) */
    private final String unit;

    /**
     * Get a MeasureData object containing the information given to the constructor
     * @param type indicates a possibly translatable string of the measurement category (eg. length)
     * @param sensor indicates the technical type designation of the sensor (eg. DS18B20)
     * @param name name given by the programmer to designate a specific sensor in the transducer
     * @param value actual numeric measured value
     * @param unit unit of the measurement (eg. Watt or Kelvin)
     */
    MeasureData(String type, String sensor, String name, double value, String unit){
        this.type = type;
        this.sensor = sensor;
        this.name = name;
        this.value = value;
        this.unit = unit;
    }

    /**
     * @return indicates a possibly translatable string of the measurement category (eg. length)
     */
    String getType() {
        return type;
    }

    /**
     * @return technical type designation of the sensor (eg. DS18B20)
     */
    String getSensor() {
        return sensor;
    }

    /**
     * @return  name given by the programmer to designate a specific sensor in the transducer
     */
    public String getName() {
        return name;
    }

    /**
     * @return actual numeric measured value
     */
    double getValue() {
        return value;
    }

    /**
     * @return unit of the measurement (eg. Watt or Kelvin)
     */
    String getUnit() {
        return unit;
    }

    /**
     * Gives a localised human-readable short representation of the most important information
     * @return a String containg type, value and unit
     */
    @Override
    public String toString(){
        return getLocalType(type) + ": " + value + " "+ unit;
    }

    /**
     * This map maps incoming sensor type keywords to localised versions included in strings.xml
     */
    @SuppressWarnings("SpellCheckingInspection")
    private static final Hashtable<String,Integer> sensorTypeNames = new Hashtable<String,Integer>()
    {{  put("angular_frequency", R.string.angular_frequency);
        put("absorbed_dose", R.string.absorbed_dose);
        put("acceleration", R.string.acceleration);
        put("amount_of_substance", R.string.amount_of_substance);
        put("angle", R.string.angle);
        put("angular_acceleration", R.string.angular_acceleration);
        put("angular_velocity", R.string.angular_velocity);
        put("area", R.string.area);
        put("capacitance", R.string.capacitance);
        put("catalytic_activity", R.string.catalytic_activity);
        put("density", R.string.density);
        put("efficiency", R.string.efficiency);
        put("electric_charge", R.string.electric_charge);
        put("electric_current", R.string.electric_current);
        put("electric_resistance", R.string.electric_resistance);
        put("electrical_conductance", R.string.electrical_conductance);
        put("energy", R.string.energy);
        put("equivalent dose", R.string.equivalent_dose);
        put("force", R.string.force);
        put("frequency",  R.string.frequency);
        put("humidity", R.string.humidity);
        put("illuminance", R.string.illuminance);
        put("inductance", R.string.inductance);
        put("length", R.string.length);
        put("luminous_flux", R.string.luminous_flux);
        put("luminous_intensity", R.string.luminous_intensity);
        put("magnetic_flux", R.string.magnetic_flux);
        put("magnetic_flux_density", R.string.magnetic_flux_density);
        put("mass", R.string.mass);
        put("power", R.string.power);
        put("pressure", R.string.pressure);
        put("radioactivity", R.string.radioactivity);
        put("revolution speed", R.string.revolution_speed);
        put("solid_angle", R.string.solid_angle);
        put("speed", R.string.speed);
        put("temperature", R.string.temperature);
        put("time", R.string.time);
        put("torque", R.string.torque);
        put("voltage", R.string.voltage);
        put("volume", R.string.volume);
        put("wavelength", R.string.wavelength); }};


    /**
     * Used to map the generic (english) type name received from the transducer to a localised name
     * @param type the generic name
     * @return a localised name for data visualisation
     */
    static String getLocalType(String type){
        try {
            return App.getContext().getString(sensorTypeNames.get(type));
        } catch (NullPointerException e) {
            return type;
        }
    }
}


