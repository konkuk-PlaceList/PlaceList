package org.konkuk.placelist.weather

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.renderscript.RenderScript
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.konkuk.placelist.R
import org.konkuk.placelist.main.MainActivity
import org.konkuk.placelist.weather.models.airquality.MeasuredValue
import org.konkuk.placelist.weather.models.weatherforecast.WeatherForecast
import org.konkuk.placelist.weather.models.weatherforecast.WeatherForecasts
import org.konkuk.placelist.weather.`object`.GpsConverter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class WeatherAlarmReceiver : BroadcastReceiver() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var cancellationTokenSource: CancellationTokenSource? = null
    private val scope = MainScope()

    var measuredValue: MeasuredValue? = null
    private var weatherForecasts: List<WeatherForecast?>? = null
    lateinit var stationName: String

    lateinit var cal: Calendar
    lateinit var baseDate: String
    lateinit var baseTime: String

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.getIntExtra("code", 0) == 1000) {
            Toast.makeText(context, "alarm ring", Toast.LENGTH_SHORT).show()
            fetchWeatherData(context)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchWeatherData(context: Context) {
        cancellationTokenSource = CancellationTokenSource()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationProviderClient.getCurrentLocation(
            LocationRequest.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource!!.token
        )
            .addOnSuccessListener { location ->
                scope.launch {
                    val monitoringStation =
                        Repository.getNearbyMonitoringStation(location.latitude, location.longitude)
                    measuredValue =
                        Repository.getLatestAirQualityData(monitoringStation!!.stationName!!)
                    stationName = monitoringStation.stationName.toString()

                    Log.d("weather", measuredValue?.o3Grade.toString())
                    Log.d("weather", measuredValue?.pm10Grade.toString())
                    Log.d("weather", measuredValue?.pm25Grade.toString())

                    setBaseTime()

                    val curPoint = GpsConverter.dfsXyConv(location.latitude, location.longitude)
                    weatherForecasts = Repository.getWeatherForecasts(
                        baseDate,
                        baseTime,
                        "JSON",
                        curPoint.x,
                        curPoint.y
                    )
                    withContext(Dispatchers.Main) {
                        makeNotification(context)
                    }
                }
            }
    }

    private fun makeNotification(context: Context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "날씨 알림"
            val channelName = "PlaceList"
            val notificationChannel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
                    .apply{
                        enableLights(true)
                        lightColor = Color.BLUE
                        lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                    }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 1000, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setContentTitle("날씨 알림")
                .setContentText("$stationName 예보 확인하기")
                .setSmallIcon(R.drawable.baseline_alarm_24)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                    .bigText(getWeatherForecastMsg()))
            val notification = notificationBuilder.build()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.apply {
                createNotificationChannel(notificationChannel)
                notify(0, notification)
            }
        }
    }

    private fun getWeatherForecastMsg(): CharSequence? {
        var msg = ""
        weatherForecasts?.forEach {
            //PTY: 강수정보
            if (it?.category == "PTY") {
                when (it?.fcstValue) {
                    "1" -> {
                        msg += getPtyMsg(it?.fcstDate, it?.fcstTime, "비")
                        return@forEach
                    }
                    "2" -> {
                        msg += getPtyMsg(it?.fcstDate, it?.fcstTime, "비/눈")
                        return@forEach
                    }
                    "3" -> {
                        msg += getPtyMsg(it?.fcstDate, it?.fcstTime, "눈")
                        return@forEach
                    }
                    "4" -> {
                        msg += getPtyMsg(it?.fcstDate, it?.fcstTime, "소나기")
                        return@forEach
                    }
                }
            }
        }
        msg += "오존 등급: ${measuredValue?.o3Grade.toString()}\n" +
                "미세먼지 등급: ${measuredValue?.pm10Grade.toString()}\n" +
                "초미세먼지 등급: ${measuredValue?.pm10Grade.toString()}\n"
        return msg
    }

    private fun getPtyMsg(fcstDate: String?, fcstTime: String?, pty: String): String{
        val today = if(fcstDate == getDate()) { "오늘" } else { "내일" }
        return "$today ${fcstTime}시에 $pty 예보가 있어요."
    }

    private fun getDate(): String {
        val date = Date(System.currentTimeMillis())
        val dataFormat = SimpleDateFormat("yyyyMMdd")
        return dataFormat.format(date)
    }

    private fun setBaseTime() {
        cal = Calendar.getInstance()

        when (SimpleDateFormat("HH", Locale.getDefault()).format(cal.time)) {
            "00", "01" -> {
                cal.add(Calendar.DATE, -1).toString()
                baseTime = "2300"
            }
            "02", "03", "04" -> {
                cal = Calendar.getInstance()
                baseTime = "0200"
            }
            "05", "06", "07" -> {
                cal = Calendar.getInstance()
                baseTime = "0500"
            }
            "08", "09", "10" -> {
                cal = Calendar.getInstance()
                baseTime = "0800"
            }
            "11", "12", "13" -> {
                cal = Calendar.getInstance()
                baseTime = "1100"
            }
            "14", "15", "16" -> {
                cal = Calendar.getInstance()
                baseTime = "1400"
            }
            "17", "18", "19" -> {
                cal = Calendar.getInstance()
                baseTime = "1700"
            }
            "20", "21", "22" -> {
                cal = Calendar.getInstance()
                baseTime = "2000"
            }
            "23" -> {
                cal = Calendar.getInstance()
                baseTime = "2300"
            }
        }
        baseDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.time)
    }
}