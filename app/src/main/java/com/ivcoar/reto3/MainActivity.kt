package com.ivcoar.reto3

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.util.*
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences

    private val positions = mutableListOf<Location>()
    private lateinit var positionListAdapter: PositionListAdapter
    private var distance: Float by Delegates.observable(0.0f) { _, _, d ->
        distanceText.text = getString(R.string.label_distance, distanceToUnit(d))
    }
    private lateinit var distanceText: TextView

    private lateinit var locationManager: LocationManager
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            positions.add(location)
            positionListAdapter.notifyDataSetChanged()

            distance = if (positions.isEmpty())
                0.0f
            else
                positions.drop(1).fold(0.0f to positions[0]) { acc, p ->
                    acc.first + p.distanceTo(acc.second) to p
                }.first

        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String?) {}

        override fun onProviderDisabled(provider: String?) {}
    }
    private lateinit var sensorManager: SensorManager
    private var tempSensor: Sensor? = null
    private val sensorListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

        override fun onSensorChanged(event: SensorEvent) {
            addTemp(event.values[0])
        }

    }
    private val tempValues = mutableListOf<Float>()
    private var currentTemp: Float by Delegates.observable(0.0f) { _, _, t ->
        currentTempText.text = getString(R.string.label_current_temp, "${t}ºC")
        averageTempText.text = getString(R.string.label_current_temp, "${getAgerageTemp()}ºC")
    }
    private lateinit var currentTempText: TextView
    private lateinit var averageTempText: TextView

    private lateinit var positionSwitch: Switch
    private lateinit var tempSwitch: Switch

    private val lowBat = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.intent.action.BATTERY_LOW") {
                disableLocation()
                disableTemp()
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == "distanceUnits")
                distanceText.text = getString(R.string.label_distance, distanceToUnit(distance))
        }

        helloText.text = getString(R.string.msg_hello, intent.getStringExtra(EXTRA_NAME))

        positionListAdapter = PositionListAdapter(this, positions)
        positionList.adapter = positionListAdapter
        positionList.transcriptMode = ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL
        distanceText = findViewById(R.id.distanceText)
        distanceText.text = getString(R.string.label_distance, "...")

        currentTempText = findViewById(R.id.currentTempText)
        currentTempText.text = getString(R.string.label_current_temp, "...")
        averageTempText = findViewById(R.id.averageTempText)
        averageTempText.text = getString(R.string.label_average_temp, "...")

        positionSwitch = findViewById(R.id.positionSwitch)
        positionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                enableLocation()
            else
                disableLocation()
        }
        tempSwitch = findViewById(R.id.temperatureSwitch)
        tempSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableTemp()
            }
            else {
                disableTemp()
                currentTempText.text = getString(R.string.label_current_temp, "...")
            }
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        // Initialization
        if (savedInstanceState != null) {
            tempValues.addAll(savedInstanceState.getFloatArray(TEMP_LIST).toList())
            currentTemp = savedInstanceState.getFloat(CURRENT_TEMP)
            positions.clear()
            positions.addAll(savedInstanceState.getParcelableArray(POSITIONS) as Array<Location>)
            distance = savedInstanceState.getFloat(DISTANCE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putFloatArray(TEMP_LIST, tempValues.toFloatArray())
        outState.putFloat(CURRENT_TEMP, currentTemp)
        outState.putParcelableArray(POSITIONS, positions.toTypedArray())
        outState.putFloat(DISTANCE, distance)
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("android.intent.action.BATTERY_LOW")
        filter.addAction("android.intent.action.BATTERY_OKAY")
        registerReceiver(lowBat, filter)
    }

    override fun onPause() {
        super.onPause()
        disableLocation()
        disableTemp()
    }

    override fun onResume() {
        super.onResume()

        if (positionSwitch.isChecked)
            enableLocation()

        if (tempSwitch.isChecked)
            enableTemp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) =
        when (requestCode) {
            0 ->
                if (grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_DENIED })
                    Toast.makeText(this, resources.getString(R.string.err_no_location), Toast.LENGTH_LONG).show()
                else {}
            else -> {}
        }

    private fun enableLocation() {
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
            (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5, 1.0f, locationListener)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 0)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5, 1.0f, locationListener)
        }
    }
    private fun disableLocation() {
        locationManager.removeUpdates(locationListener)
        positionSwitch.isChecked = false
    }

    private fun enableTemp() {
        if (tempSensor != null)
            sensorManager.registerListener(sensorListener, tempSensor, SensorManager.SENSOR_DELAY_NORMAL)
        else
            Toast.makeText(this, resources.getString(R.string.err_no_temp), Toast.LENGTH_LONG).show()
    }
    private fun disableTemp() {
        if (tempSensor != null)
            sensorManager.unregisterListener(sensorListener)
        tempSwitch.isChecked = false
    }

    private fun addTemp(temp: Float) {
        tempValues.add(temp)
        currentTemp = temp
    }

    private fun getAgerageTemp(): Float {
        return if (tempValues.isEmpty())
            0.0f
        else
            tempValues.reduce { total, x -> total + x } / tempValues.size
    }

    private fun distanceToUnit(d: Float): String {
        val unit = prefs.getString("distanceUnits", "m")
        println(unit)
        return if (unit == "m")
            "${String.format("%.2f", d)}m"
        else
            "${String.format("%.2f", d * 3.28)}ft"
    }
}
