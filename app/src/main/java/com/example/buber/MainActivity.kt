package com.example.buber

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.core.content.ContextCompat

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

import com.google.firebase.database.FirebaseDatabase

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive



class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val onibusRef =
        FirebaseDatabase.getInstance().getReference("onibus/TESTE-1")

    private var latitude by mutableStateOf("--")
    private var longitude by mutableStateOf("--")
    private var velocidade by mutableStateOf("--")
    private var precisao by mutableStateOf("--")
    private var direcao by mutableStateOf("--")

    private var rastreando by mutableStateOf(false)

    private var ultimoEnvioMillis by mutableStateOf(0L)

    private var segundosDesdeEnvio by mutableStateOf(0)

    private var precisaoMetros by mutableStateOf(999f)
    private var mostrarSplash by mutableStateOf(true)

    private var iniciarAoConceder = false

    // Cores do tema escuro
    private val fundoEscuro = Color(0xFF0A0D0C)
    private val cardEscuro = Color(0xFF12181A)
    private val cinzaClaro = Color(0xFF9AA0A6)
    private val verdeNeon = Color(0xFF00E676)
    private val vermelhoNeon = Color(0xFFFF3B30)


    private val solicitadorDePermissao =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissoes ->

            val precisa = permissoes[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val aproximada = permissoes[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if ((precisa || aproximada) && iniciarAoConceder) {
                iniciarGPS()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult) {

                for (location in locationResult.locations) {

                    val lat = location.latitude
                    val lon = location.longitude
                    val vel = location.speed * 3.6
                    val prec = location.accuracy
                    val dir = if (location.hasBearing()) location.bearing else -1f

                    latitude = "%.6f".format(lat)
                    longitude = "%.6f".format(lon)
                    velocidade = "%.1f km/h".format(vel)
                    precisao = "%.1f metros".format(prec)
                    direcao = if (location.hasBearing()) "%.0f°".format(dir) else "Indisponível"
                    precisaoMetros = prec
                    ultimoEnvioMillis = System.currentTimeMillis()

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

        setContent {
            BuberApp()
        }
    }


    private fun temPermissao(): Boolean {
        val precisa = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val aproximada = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return precisa || aproximada
    }


    private fun aoClicarBotao() {
        if (rastreando) {
            pararGPS()
        } else if (temPermissao()) {
            iniciarGPS()
        } else {
            iniciarAoConceder = true
            solicitadorDePermissao.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }


    @SuppressLint("MissingPermission")
    private fun iniciarGPS() {

        if (!temPermissao()) return

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

        rastreando = true
    }


    private fun pararGPS() {

        fusedLocationClient.removeLocationUpdates(locationCallback)

        onibusRef.child("status").setValue("offline")

        rastreando = false

        latitude = "--"
        longitude = "--"
        velocidade = "--"
        precisao = "--"
        direcao = "--"
    }


    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }


    @androidx.compose.runtime.Composable
    fun BuberApp() {

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = fundoEscuro
        ) {
            Crossfade(
                targetState = mostrarSplash,
                animationSpec = tween(durationMillis = 600)
            ) { splash ->
                if (splash) {
                    SplashScreen(onFinished = { mostrarSplash = false })
                } else {
                    TelaPrincipal()
                }
            }
        }
    }


    @androidx.compose.runtime.Composable
    fun SplashScreen(onFinished: () -> Unit) {

        val escala = remember { Animatable(1.7f) }
        val opacidade = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            launch {
                escala.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 1100,
                        easing = FastOutSlowInEasing
                    )
                )
            }
            launch {
                opacidade.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 900,
                        easing = LinearOutSlowInEasing
                    )
                )
            }

            delay(1500)
            onFinished()
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = fundoEscuro
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_buber),
                    contentDescription = "Logo BUBER",
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .graphicsLayer {
                            scaleX = escala.value
                            scaleY = escala.value
                            alpha = opacidade.value
                        }
                )
            }
        }
    }


    @androidx.compose.runtime.Composable
    fun TelaPrincipal() {
        LaunchedEffect(rastreando) {
            while (isActive) {
                if (rastreando && ultimoEnvioMillis > 0) {
                    segundosDesdeEnvio =
                        ((System.currentTimeMillis() - ultimoEnvioMillis) / 1000).toInt()
                }
                delay(1000)
            }
        }
        val corBotao by animateColorAsState(
            targetValue = if (rastreando) verdeNeon else vermelhoNeon,
            animationSpec = tween(500),
            label = "corBotao"
        )

        val tamanhoBotao by animateDpAsState(
            targetValue = if (rastreando) 170.dp else 250.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "tamanhoBotao"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {

            // ===== BARRA SUPERIOR =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(modifier = Modifier.width(24.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(cardEscuro)
                        .border(1.dp, corBotao.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(corBotao)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (rastreando) "ONLINE" else "OFFLINE",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== CONTEÚDO CENTRAL =====
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Image(
                    painter = painterResource(id = R.drawable.logo_buber),
                    contentDescription = "Logo BUBER",
                    modifier = Modifier
                        .fillMaxWidth(1f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "T R A C K E R",
                    color = cinzaClaro,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(48.dp))

                // ===== BOTÃO COM BRILHO =====
                Box(contentAlignment = Alignment.Center) {

                    Box(
                        modifier = Modifier
                            .size(tamanhoBotao + 70.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        corBotao.copy(alpha = 0.30f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(tamanhoBotao)
                            .clip(CircleShape)
                            .background(cardEscuro)
                            .border(width = 6.dp, color = corBotao, shape = CircleShape)
                            .clickable { aoClicarBotao() },
                        contentAlignment = Alignment.Center
                    ) {
                        IconePower(cor = corBotao, modifier = Modifier.size(tamanhoBotao * 0.35f))
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                AnimatedVisibility(
                    visible = rastreando,
                    enter = fadeIn(tween(500)) + expandVertically(tween(500)),
                    exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(cardEscuro)
                                .border(1.dp, verdeNeon.copy(alpha = 0.5f), RoundedCornerShape(50))
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(verdeNeon)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Compartilhando localização",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(cardEscuro)
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (segundosDesdeEnvio <= 1)
                                        "Último envio: agora"
                                    else
                                        "Último envio: há $segundosDesdeEnvio s",
                                    color = cinzaClaro,
                                    fontSize = 13.sp
                                )

                                IconeSinal(precisaoMetros = precisaoMetros)
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            LinhaDado("Latitude", latitude, verdeNeon)
                            LinhaDado("Longitude", longitude, verdeNeon)
                            LinhaDado("Velocidade", velocidade, verdeNeon)
                            LinhaDado("Precisão", precisao, verdeNeon)
                            LinhaDado("Direção", direcao, verdeNeon)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardEscuro)
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "VEÍCULO",
                                    color = cinzaClaro,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "TESTE-1",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(verdeNeon)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Em operação",
                                    color = cinzaClaro,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    @androidx.compose.runtime.Composable
    fun LinhaDado(rotulo: String, valor: String, corAcento: Color) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(corAcento)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = rotulo, color = cinzaClaro, fontSize = 14.sp)
            }
            Text(text = valor, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }


    @Composable
    fun IconeSinal(precisaoMetros: Float, modifier: Modifier = Modifier) {
        val corSinal = when {
            precisaoMetros <= 10f -> verdeNeon
            precisaoMetros <= 30f -> Color(0xFFFDD835)
            precisaoMetros <= 100f -> Color(0xFFFB8C00)
            else -> vermelhoNeon
        }

        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            repeat(4) { index ->
                val ativa = when (index) {
                    0 -> true
                    1 -> precisaoMetros <= 100f
                    2 -> precisaoMetros <= 30f
                    3 -> precisaoMetros <= 10f
                    else -> false
                }
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height((4 + (index * 4)).dp)
                        .background(
                            if (ativa) corSinal else cinzaClaro.copy(alpha = 0.3f),
                            RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp)
                        )
                )
            }
        }
    }


    @androidx.compose.runtime.Composable
    fun IconePower(cor: Color, modifier: Modifier = Modifier) {
        Canvas(modifier = modifier) {
            val largura = size.minDimension
            val espessura = largura * 0.11f

            drawLine(
                color = cor,
                start = Offset(largura / 2f, 0f),
                end = Offset(largura / 2f, largura * 0.5f),
                strokeWidth = espessura,
                cap = StrokeCap.Round
            )

            drawArc(
                color = cor,
                startAngle = -60f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(largura * 0.05f, largura * 0.05f),
                size = Size(largura * 0.9f, largura * 0.9f),
                style = Stroke(width = espessura, cap = StrokeCap.Round)
            )
        }
    }
}