package com.skt.sensortest

class SensorData (val sensor:String = "",
    val timestamp:Long = Long.MAX_VALUE,
                  val value: Float = 0f,
                  var accuracy:Int = 0){


}