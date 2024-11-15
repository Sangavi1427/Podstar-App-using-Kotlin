package ac.mdiq.podcini.ui.activity

import ac.mdiq.podcini.R
import ac.mdiq.podcini.databinding.SubscriptionSelectionActivityBinding
import ac.mdiq.podcini.preferences.ThemeSwitcher
import ac.mdiq.podcini.storage.database.Feeds.getFeedList
import ac.mdiq.podcini.storage.model.Feed
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.lifecycleScope
import coil.imageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO: need to enable
class SelectSubscriptionActivity : AppCompatActivity() {
    private var _binding: SubscriptionSelectionActivityBinding? = null
    private val binding get() = _binding!!

    @Volatile
    private var listItems: List<Feed> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeSwitcher.getTranslucentTheme(this))
        super.onCreate(savedInstanceState)

        _binding = SubscriptionSelectionActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setTitle(R.string.shortcut_select_subscription)

        binding.transparentBackground.setOnClickListener { finish() }
        binding.card.setOnClickListener(null)

        loadSubscriptions()

        val checkedPosition = arrayOfNulls<Int>(1)
        binding.list.choiceMode = ListView.CHOICE_MODE_SINGLE
        binding.list.onItemClickListener =
            AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                checkedPosition[0] = position
            }
        binding.shortcutBtn.setOnClickListener {
            if (checkedPosition[0] != null && Intent.ACTION_CREATE_SHORTCUT == intent.action) getBitmapFromUrl(listItems[checkedPosition[0]!!])
        }
    }

    fun getFeedItems(items: List<Feed?>, result: MutableList<Feed>): List<Feed> {
        for (item in items) {
            if (item == null) continue
            val feed: Feed = item
            if (!result.contains(feed)) result.add(feed)
        }
        return result
    }

     private fun addShortcut(feed: Feed, bitmap: Bitmap?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.setAction(Intent.ACTION_MAIN)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtra(MainActivity.Extras.fragment_feed_id.name, feed.id.toString())
        val id = "subscription-" + feed.id

        val icon: IconCompat = if (bitmap != null) IconCompat.createWithAdaptiveBitmap(bitmap)
        else IconCompat.createWithResource(this, R.drawable.ic_subscriptions_shortcut)

        val shortcut: ShortcutInfoCompat = ShortcutInfoCompat.Builder(this, id)
            .setShortLabel(feed.title?:"")
            .setLongLabel(feed.eigenTitle?:"")
            .setIntent(intent)
            .setIcon(icon)
            .build()

        setResult(Activity.RESULT_OK, ShortcutManagerCompat.createShortcutResultIntent(this, shortcut))
        finish()
    }

    private fun getBitmapFromUrl(feed: Feed) {
        val iconSize = (128 * resources.displayMetrics.density).toInt()
        val request = ImageRequest.Builder(this)
            .data(feed.imageUrl)
            .setHeader("User-Agent", "Mozilla/5.0")
            .placeholder(R.color.light_gray)
            .listener(object : ImageRequest.Listener {
                 override fun onError(request: ImageRequest, throwable: ErrorResult) {
                    addShortcut(feed, null)
                }
                 override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                    addShortcut(feed, (result.drawable as BitmapDrawable).bitmap)
                }
            })
            .size(iconSize, iconSize)
            .build()
        imageLoader.enqueue(request)
    }

    private fun loadSubscriptions() {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    getFeedList()
//                    val data: NavDrawerData = DBReader.getNavDrawerData()
//                    getFeedItems(data.items, ArrayList())
                }
                withContext(Dispatchers.Main) {
                    listItems = result
                    val titles = ArrayList<String>()
                    for (feed in result) {
                        if (feed.title != null) titles.add(feed.title!!)
                    }
                    val adapter: ArrayAdapter<String> = ArrayAdapter<String>(this@SelectSubscriptionActivity, R.layout.simple_list_item_multiple_choice_on_start, titles)
                    binding.list.adapter = adapter
                }
            } catch (e: Throwable) {
                Log.e(TAG, Log.getStackTraceString(e))
            }
        }

    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    companion object {
        private val TAG: String = SelectSubscriptionActivity::class.simpleName ?: "Anonymous"
    }
}