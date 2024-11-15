package ac.mdiq.podcini.playback.cast

import ac.mdiq.podcini.R
import ac.mdiq.podcini.util.Logd
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Activity that allows for showing the MediaRouter button whenever there's a cast device in the
 * network.
 */
abstract class CastEnabledActivity : AppCompatActivity() {
    private var canCast by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        canCast = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS
        if (canCast) {
            try { CastContext.getSharedInstance(this)
            } catch (e: Exception) {
                e.printStackTrace()
                canCast = false
            }
        }
    }

    fun requestCastButton(menu: Menu?) {
        if (!canCast) return
        menuInflater.inflate(R.menu.cast_button, menu)
        CastButtonFactory.setUpMediaRouteButton(applicationContext, menu!!, R.id.media_route_menu_item)
    }

    @Composable
    fun CastIconButton() {
        if (canCast) {
            AndroidView( modifier = Modifier.size(24.dp),
                factory = { ctx ->
                    MediaRouteButton(ctx).apply {
                        CastButtonFactory.setUpMediaRouteButton(ctx, this)
                    }
                },
            )
        }
    }
}
