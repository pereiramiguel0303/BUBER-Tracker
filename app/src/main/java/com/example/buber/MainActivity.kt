package com.example.buber

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.util.Log

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

import androidx.core.content.ContextCompat

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private var mostrarSplash by mutableStateOf(true)
    private var iniciarAoConceder = false
    private var segundosDesdeEnvio by mutableStateOf(0)

    // "inicial" ou "mapa"
    private var telaAtual by mutableStateOf("inicial")
    private var menuAberto by mutableStateOf(false)

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
                iniciarServico()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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


    private fun permissoesNecessarias(): Array<String> {
        val lista = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lista.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return lista.toTypedArray()
    }


    private fun aoClicarBotao() {
        if (LocationService.rodando.value) {
            pararServico()
        } else if (temPermissao()) {
            iniciarServico()
        } else {
            iniciarAoConceder = true
            solicitadorDePermissao.launch(permissoesNecessarias())
        }
    }


    private fun iniciarServico() {
        val intent = Intent(this, LocationService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }


    private fun pararServico() {
        val intent = Intent(this, LocationService::class.java)
        stopService(intent)
    }


    @SuppressLint("MissingPermission")
    private fun obterLocalizacaoUnica(aoObter: (Double, Double) -> Unit) {
        val cliente: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)

        cliente.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                aoObter(location.latitude, location.longitude)
            }
        }
    }


    @androidx.compose.runtime.Composable
    fun BuberApp() {

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = fundoEscuro
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                ConteudoApp()

                androidx.compose.animation.AnimatedVisibility(
                    visible = mostrarSplash,
                    exit = fadeOut(tween(500))
                ) {
                    SplashScreen(onFinished = { mostrarSplash = false })
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
                    animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
                )
            }
            launch {
                opacidade.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 900, easing = LinearOutSlowInEasing)
                )
            }
            delay(1500)
            onFinished()
        }

        Surface(modifier = Modifier.fillMaxSize(), color = fundoEscuro) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_buber),
                    contentDescription = "Logo BUBER",
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
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
    fun ConteudoApp() {

        val rastreando = LocationService.rodando.value

        val corBotao by animateColorAsState(
            targetValue = if (rastreando) verdeNeon else vermelhoNeon,
            animationSpec = tween(500),
            label = "corBotao"
        )

        Box(modifier = Modifier.fillMaxSize()) {

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

                    Column(
                        modifier = Modifier
                            .width(24.dp)
                            .clickable { menuAberto = !menuAberto }
                    ) {
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
                            modifier = Modifier.size(8.dp).clip(CircleShape).background(corBotao)
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

                Spacer(modifier = Modifier.height(20.dp))

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (telaAtual) {
                        "mapa" -> TelaMapa()
                        else -> ConteudoInicial()
                    }
                }
            }

            // ===== MENU SUSPENSO =====
            if (menuAberto) {
                Box(
                    modifier = Modifier
                        .padding(top = 56.dp, start = 24.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardEscuro)
                        .border(1.dp, Color(0xFF2A2F2D), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        ItemMenu("Tela inicial") {
                            telaAtual = "inicial"
                            menuAberto = false
                        }
                        ItemMenu("Mapa") {
                            telaAtual = "mapa"
                            menuAberto = false
                        }
                    }
                }
            }
        }
    }


    @androidx.compose.runtime.Composable
    fun ItemMenu(texto: String, aoClicar: () -> Unit) {
        Text(
            text = texto,
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier
                .clickable { aoClicar() }
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        )
    }


    @androidx.compose.runtime.Composable
    fun ConteudoInicial() {

        val rastreando = LocationService.rodando.value
        val precisaoMetros = LocationService.precisaoMetros.value

        LaunchedEffect(rastreando) {
            while (isActive) {
                val ultimoEnvio = LocationService.ultimoEnvioMillis.value
                if (rastreando && ultimoEnvio > 0) {
                    segundosDesdeEnvio =
                        ((System.currentTimeMillis() - ultimoEnvio) / 1000).toInt()
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
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.logo_buber),
                contentDescription = "Logo BUBER",
                modifier = Modifier.fillMaxWidth(0.75f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "T R A C K E R",
                color = cinzaClaro,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(cardEscuro)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "ID DO ÔNIBUS  ", color = cinzaClaro, fontSize = 11.sp)
                Text(
                    text = LocationService.ID_ONIBUS,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Box(contentAlignment = Alignment.Center) {

                Box(
                    modifier = Modifier
                        .size(tamanhoBotao + 70.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(corBotao.copy(alpha = 0.30f), Color.Transparent)
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
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(verdeNeon))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Compartilhando localização", color = Color.White, fontSize = 14.sp)
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

                        LinhaDado("Latitude", LocationService.latitude.value, verdeNeon)
                        LinhaDado("Longitude", LocationService.longitude.value, verdeNeon)
                        LinhaDado("Velocidade", LocationService.velocidade.value, verdeNeon)
                        LinhaDado("Precisão", LocationService.precisao.value, verdeNeon)
                        LinhaDado("Direção", LocationService.direcao.value, verdeNeon)
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
                            Text(text = "VEÍCULO", color = cinzaClaro, fontSize = 11.sp)
                            Text(
                                text = LocationService.ID_ONIBUS,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(verdeNeon))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Em operação", color = cinzaClaro, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }


    @androidx.compose.runtime.Composable
    fun TelaMapa() {

        var latInicial by remember { mutableStateOf(-29.7604) }
        var lonInicial by remember { mutableStateOf(-51.1469) }
        var pronto by remember { mutableStateOf(false) }
        var webViewRef by remember { mutableStateOf<WebView?>(null) }

        LaunchedEffect(Unit) {
            val latAtual = LocationService.latitude.value.replace(",", ".").toDoubleOrNull()
            val lonAtual = LocationService.longitude.value.replace(",", ".").toDoubleOrNull()

            if (LocationService.rodando.value && latAtual != null && lonAtual != null) {
                latInicial = latAtual
                lonInicial = lonAtual
                pronto = true
            } else if (temPermissao()) {
                obterLocalizacaoUnica { lat, lon ->
                    latInicial = lat
                    lonInicial = lon
                    pronto = true
                }
            } else {
                pronto = true
            }
        }

        LaunchedEffect(Unit) {
            while (isActive) {
                if (LocationService.rodando.value) {
                    val lat = LocationService.latitude.value.replace(",", ".").toDoubleOrNull()
                    val lon = LocationService.longitude.value.replace(",", ".").toDoubleOrNull()
                    if (lat != null && lon != null) {
                        webViewRef?.evaluateJavascript("moverMarcador($lat, $lon)", null)
                    }
                }
                delay(4000)
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (pronto) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true

                            webViewClient = object : WebViewClient() {
                                override fun onReceivedError(
                                    view: WebView?,
                                    errorCode: Int,
                                    description: String?,
                                    failingUrl: String?
                                ) {
                                    Log.e("BUBER_MAPA", "Erro ao carregar: $description ($failingUrl)")
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onConsoleMessage(msg: android.webkit.ConsoleMessage?): Boolean {
                                    Log.e("BUBER_MAPA", "Console: ${msg?.message()} (linha ${msg?.lineNumber()})")
                                    return true
                                }
                            }

                            webViewRef = this
                            loadDataWithBaseURL(
                                "https://unpkg.com/",
                                htmlMapa(latInicial, lonInicial),
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }


    private fun htmlMapa(lat: Double, lon: Double): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    html, body, #mapa { height: 100%; margin: 0; padding: 0; background: #0A0D0C; }
                </style>
            </head>
            <body>
                <div id="mapa"></div>
                <script>
                    console.log('PASSO 1: script iniciou');

                    try {
                        var latitudeInicial = $lat;
                        var longitudeInicial = $lon;

                        console.log('PASSO 2: coordenadas = ' + latitudeInicial + ', ' + longitudeInicial);

                        var mapa = L.map('mapa').setView([latitudeInicial, longitudeInicial], 16);

                        console.log('PASSO 3: mapa criado');

                        var camadaMapa = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            maxZoom: 19,
                            attribution: '&copy; OpenStreetMap'
                        }).addTo(mapa);

                        console.log('PASSO 4: camada de tiles adicionada');

                        camadaMapa.on('tileerror', function(erro) {
                            console.log('ERRO DE TILE detectado');
                        });

                        camadaMapa.on('load', function() {
                            console.log('PASSO 5: tiles carregados com sucesso');
                        });

                        var marcador = L.circleMarker([latitudeInicial, longitudeInicial], {
                            radius: 10,
                            fillColor: "#00E676",
                            color: "#00E676",
                            weight: 2,
                            opacity: 1,
                            fillOpacity: 0.85
                        }).addTo(mapa);

                        console.log('PASSO 6: marcador adicionado, tudo certo');

                    } catch (erro) {
                        console.log('ERRO CAPTURADO: ' + erro.message);
                    }

                    function moverMarcador(lat, lng) {
                        marcador.setLatLng([lat, lng]);
                        mapa.panTo([lat, lng]);
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }


    @androidx.compose.runtime.Composable
    fun LinhaDado(rotulo: String, valor: String, corAcento: Color) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(corAcento))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = rotulo, color = cinzaClaro, fontSize = 14.sp)
            }
            Text(text = valor, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }


    @androidx.compose.runtime.Composable
    fun IconeSinal(precisaoMetros: Float, modifier: Modifier = Modifier) {

        val corBom = Color(0xFF00E676)
        val corMedio = Color(0xFFFFC107)
        val corRuim = Color(0xFFFF3B30)
        val corInativo = Color(0xFF3A3F3D)

        val nivel = when {
            precisaoMetros <= 15f -> 3
            precisaoMetros <= 40f -> 2
            else -> 1
        }

        val cor = when (nivel) {
            3 -> corBom
            2 -> corMedio
            else -> corRuim
        }

        Row(
            modifier = modifier,
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val alturas = listOf(8.dp, 13.dp, 18.dp)
            for (i in 0..2) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(alturas[i])
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i < nivel) cor else corInativo)
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