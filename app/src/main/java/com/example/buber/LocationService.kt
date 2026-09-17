package com.example.buber

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper

import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

import com.google.firebase.database.FirebaseDatabase


class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val onibusRef =
        FirebaseDatabase.getInstance().getReference("onibus/$ID_ONIBUS")
    companion object {
        const val CANAL_ID = "buber_tracker_canal"
        const val NOTIFICACAO_ID = 1001
        const val ID_ONIBUS = "TESTE-1"

        val latitude = mutableStateOf("--")
        val longitude = mutableStateOf("--")
        val velocidade = mutableStateOf("--")
        val precisao = mutableStateOf("--")
        val direcao = mutableStateOf("--")
        val precisaoMetros = mutableStateOf(999f)
        val ultimoEnvioMillis = mutableStateOf(0L)
        val rodando = mutableStateOf(false)
    }

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult) {

                for (location in locationResult.locations) {

                    val lat = location.latitude
                    val lon = location.longitude
                    val vel = location.speed * 3.6
                    val prec = location.accuracy
                    val dir = if (location.hasBearing()) location.bearing else -1f

                    latitude.value = "%.6f".format(lat)
                    longitude.value = "%.6f".format(lon)
                    velocidade.value = "%.1f km/h".format(vel)
                    precisao.value = "%.1f metros".format(prec)
                    direcao.value =
                        if (location.hasBearing()) "%.0f°".format(dir) else "Indisponível"

                    precisaoMetros.value = prec
                    ultimoEnvioMillis.value = System.currentTimeMillis()

                    val dados = mapOf(
                        "latitude" to lat,
                        "longitude" to lon,
                        "velocidade" to vel,
                        "direcao" to dir,
                        "precisao" to prec,
                        "timestamp" to System.currentTimeMillis(),
                        "status" to "online"
                    )

                    onibusRef.setValue(dados)
                }
            }
        }
    }


    @Suppress("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        criarCanalNotificacao()

        val notificacao = NotificationCompat.Builder(this, CANAL_ID)
            .setContentTitle("BUBER Tracker")
            .setContentText("Compartilhando localização em segundo plano")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICACAO_ID, notificacao)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        )
            .setMinUpdateIntervalMillis(3000)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        rodando.value = true

        return START_STICKY
    }


    private fun criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                "Rastreamento BUBER",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(canal)
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        fusedLocationClient.removeLocationUpdates(locationCallback)
        onibusRef.child("status").setValue("offline")

        rodando.value = false
        latitude.value = "--"
        longitude.value = "--"
        velocidade.value = "--"
        precisao.value = "--"
        direcao.value = "--"
    }


    override fun onBind(intent: Intent?): IBinder? = null
}