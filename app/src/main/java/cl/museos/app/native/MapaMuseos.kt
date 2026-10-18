package cl.museos.app.native

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.core.content.ContextCompat
import cl.museos.app.R
import java.io.File
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cl.museos.app.model.Museo
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable fun MapaMuseos(museos: List<Museo>, abrir: (Long) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val callback by rememberUpdatedState(abrir)
    val mapa = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().osmdroidBasePath = File(context.cacheDir, "mapa").apply { mkdirs() }
        Configuration.getInstance().osmdroidTileCache = File(context.cacheDir, "mapa/teselas").apply { mkdirs() }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true)
            controller.setZoom(2.8); controller.setCenter(GeoPoint(-15.0, -60.0))
            contentDescription = "Mapa de museos. También puedes seleccionar una ficha en la lista inferior."
        }
    }
    DisposableEffect(mapa, lifecycle) {
        val observador = LifecycleEventObserver { _, event ->
            when (event) { Lifecycle.Event.ON_RESUME -> mapa.onResume(); Lifecycle.Event.ON_PAUSE -> mapa.onPause(); else -> Unit }
        }
        lifecycle.addObserver(observador)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapa.onResume()
        onDispose { lifecycle.removeObserver(observador); mapa.onPause(); mapa.onDetach() }
    }
    LaunchedEffect(museos) {
        mapa.overlays.clear()
        museos.forEach { museo -> mapa.overlays.add(Marker(mapa).apply {
            position = GeoPoint(museo.latitud, museo.longitud); title = museo.nombre
            icon = ContextCompat.getDrawable(context, R.drawable.ic_marcador)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setOnMarkerClickListener { _, _ -> callback(museo.id); true }
        }) }
        if (museos.size == 1) { mapa.controller.setCenter(GeoPoint(museos[0].latitud, museos[0].longitud)); mapa.controller.setZoom(5.0) }
        else { mapa.controller.setZoom(2.8); mapa.controller.setCenter(GeoPoint(-15.0, -60.0)) }
        mapa.invalidate()
    }
    AndroidView(factory = { mapa }, modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(16.dp)))
}
