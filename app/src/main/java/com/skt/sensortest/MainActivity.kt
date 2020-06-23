package com.skt.sensortest

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.lang.Exception
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity(), SensorEventListener {

    lateinit var sensorManager: SensorManager
    var sensor: Sensor? = null

    // initializing Sensor related variables
    private lateinit var accelerometer: Sensor
    private lateinit var gyroscope: Sensor
    private lateinit var magnetometer: Sensor
    private lateinit var barometer: Sensor

    private val testAccLog2 = mutableListOf<Long>()
    private val testGyroLog2 = mutableListOf<Long>()
    private val testMagLog2 = mutableListOf<Long>()
    private val testBarLog2 = mutableListOf<Long>()

    private val loggingAcc = mutableListOf<SensorData>()
    private val loggingGyro = mutableListOf<SensorData>()
    private val loggingMag = mutableListOf<SensorData>()
    private val loggingBar = mutableListOf<SensorData>()

    var testBarCount: Int = 0
    var testMaxCount: Int = 1
    var testMaxCountPre: Int = 1
    var testTime: Double = 0.0

    var accCal: Boolean = false
    var gyroCal: Boolean = false
    var magCal: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val sensorList: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)

        logging_button.isEnabled = false
        logging_button.isClickable = false

        for (thisSensor in sensorList) {
            when (thisSensor.type) {
                //Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> {
                Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> {
                    accCal = true
                    accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED)
                }
                Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> {
                    gyroCal = true
                    gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED)
                }
                Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> {
                    magCal = true
                    magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED)
                }
                Sensor.TYPE_PRESSURE -> {
                    barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
                }
                else -> {
                }
            }
        }
        if (!(accCal && gyroCal && magCal)) {
            for (thisSensor in sensorList) {
                when (thisSensor.getType()) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        if (!accCal) {
                            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                        }
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        if (!gyroCal) {
                            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        if (!magCal) {
                            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                        }
                    }
                    else -> {
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        sensor_button.setOnClickListener {

            sensorManager.unregisterListener(this)

            val sensorDelayView = findViewById(R.id.sensordelay_edittext) as EditText

            val sensorDelay = sensorDelayView.text.toString()
            var delay = 10000

            if(sensorDelay != "") {
                delay = Integer.parseInt(sensorDelay)
            } else {
                Toast.makeText(this, "put in Sensor Delay (Default = 10000)", Toast.LENGTH_SHORT).show()
            }
            sensorManager.registerListener(this, sensor, delay)
            sensorManager.registerListener(this, accelerometer, delay)
            sensorManager.registerListener(this, gyroscope, delay)
            sensorManager.registerListener(this, magnetometer, delay)
            sensorManager.registerListener(this, barometer, delay)

            logging_button.isEnabled = true
            logging_button.isClickable = true


        }
        logging_button.setOnClickListener {

            var delayCount: Long = 1000
            var count: Int = 30
            val resultDataList = mutableListOf<SensorData>()

            if (count_edittext.text.toString() == "") {
                Toast.makeText(this, "put in count (Default = 30)", Toast.LENGTH_SHORT).show()
            } else {
                count = count_edittext.text.toString().toInt()
            }

            if (interval_edittext.text.toString() == "") {
                Toast.makeText(this, "put in interval (Default = 1000ms)", Toast.LENGTH_SHORT).show()
            } else {
                delayCount = interval_edittext.text.toString().toLong()
            }

            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "logging Start", Toast.LENGTH_SHORT).show()

                val job = GlobalScope.launch {

                    logging_textview.text = "Logging status = running"

                    for (item in 0..count) {
                        resultDataList.add(loggingAcc.last())
                        resultDataList.add(loggingGyro.last())
                        resultDataList.add(loggingMag.last())
                        resultDataList.add(loggingBar.last())
                        Log.d("log", resultDataList.count().toString())
                        delay(delayCount)
                    }

                    if(writeLogFile(resultDataList)) {
                        logging_textview.text = "Logging status = Clear"
                    } else {
                        logging_textview.text = "Logging status = False"
                    }

                }
            } else {
                Toast.makeText(this, "Need Storage Permission", Toast.LENGTH_SHORT).show()
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
            }
        }

    }

    override fun onStop() {
        super.onStop()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> {
                testAccLog2.add(event.timestamp)
                if (testMaxCount >= 1) {
                    accerlerometer_textview2.text = event.values[0].toString()
                    loggingAcc.add(SensorData(event.sensor.name ,event.timestamp, event.values[0], event.accuracy))
                }
            }
            Sensor.TYPE_GYROSCOPE, Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> {
                testGyroLog2.add(event.timestamp)
                if (testMaxCount >= 2) {
                    gyroscope_textview2.text = event.values[0].toString()
                    loggingGyro.add(SensorData(event.sensor.name ,event.timestamp, event.values[0], event.accuracy))
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> {
                testMagLog2.add(event.timestamp)

                if (testMaxCount >= 3) {
                    magetic_textview2.text = event.values[0].toString()
                    loggingMag.add(SensorData(event.sensor.name ,event.timestamp, event.values[0], event.accuracy))
                }
            }
            Sensor.TYPE_PRESSURE -> {

                barometer_textview2.text = event.values[0].toString()
                loggingBar.add(SensorData(event.sensor.name ,event.timestamp, event.values[0], event.accuracy))

                testBarCount++
                testBarLog2.add(event.timestamp)
                if (testBarCount < 4) {
                    testTime += 0.1
                } else {
                    testTime += 0.1

                    when(testMaxCount){
                        1-> {
                            if(testGyroLog2.size>5 && isRange(testGyroLog2.takeLast(5), testAccLog2[testAccLog2.size-2])){
                                testMaxCount = 2
                            }
                        }
                        2-> {
                            if(testMagLog2.size>2 && isRange(testAccLog2.takeLast(2 * 10000 / 10000), testMagLog2[testMagLog2.size-1])){
                                testMaxCount = 3
                            }
                        }
                        3-> {
                            if(testBarLog2.size>2 && isRange(testAccLog2.takeLast(2 * 10000 / 40000), testBarLog2[testBarLog2.size-1])){
                                testMaxCount = 4
                            }
                        }
                        else ->{}
                    }
                    if (testMaxCount > testMaxCountPre) {

                        when(testMaxCount){
                            1->{
                                sync_textview.text = "Accelerometer sync clear"
                            }
                            2->{
                                sync_textview.text = "Accelerometer & Gyroscope sync clear"
                            }
                            3->{
                                sync_textview.text = "Accelerometer & Gyroscope & Magnetic sync clear"
                            }
                        }
                    }
                    testMaxCountPre = testMaxCount
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun isRange(list: List<Long>, target: Long): Boolean {

        var lessThen: Boolean = true
        var moreThen: Boolean = false

        list.forEach{
            if (lessThen) {
                if (it >= target) {
                    moreThen = true
                }
            }
        }
        return lessThen && moreThen
    }

    private fun writeLogFile(sensorList: List<SensorData>): Boolean {

        var dirName: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        dirName += "/SensorTest"
        val myFile = File(dirName)
        if (!myFile.exists()) {
            myFile.mkdirs()
        }

        try {
            val fileName: String =
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd_HHmmss"))

            val resultFile = File("$dirName/$fileName.csv")

            val fileWriter = FileWriter(resultFile)
            val CSV_HEADER = "sensor,timestamp,value,accuracy"

            fileWriter.append(CSV_HEADER)
            fileWriter.append('\n')

            for (sensor in sensorList) {

                fileWriter.append(sensor.sensor)
                fileWriter.append(',')
                fileWriter.append(sensor.timestamp.toString())
                fileWriter.append(',')
                fileWriter.append(sensor.value.toString())
                fileWriter.append(',')
                fileWriter.append(sensor.accuracy.toString())
                fileWriter.append('\n')
            }
            fileWriter.close()
            return true;
        } catch (e: Exception) {
            return false;
        }

    }



}

