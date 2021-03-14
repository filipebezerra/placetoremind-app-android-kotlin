package app.filipebezerra.placetoremind

import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import app.filipebezerra.placetoremind.base.BaseActivity
import app.filipebezerra.placetoremind.databinding.PlaceToRemindActivityBinding
import org.koin.androidx.viewmodel.ext.android.viewModel


/**
 * The RemindersActivity that holds the reminders fragments
 */
class PlaceToRemindActivity : BaseActivity() {

    private lateinit var binding: PlaceToRemindActivityBinding

    override val viewModel: PlaceToRemindViewModel by viewModel()

    override val rootLayout: ViewGroup by lazy { binding.placeToRemindRootLayout }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<PlaceToRemindActivityBinding>(
            this,
            R.layout.place_to_remind_activity
        ).apply {
            binding = this
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                navController.popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
