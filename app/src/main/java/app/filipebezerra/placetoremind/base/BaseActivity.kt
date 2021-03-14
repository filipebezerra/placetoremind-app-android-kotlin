package app.filipebezerra.placetoremind.base

import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import app.filipebezerra.placetoremind.R
import com.google.android.material.snackbar.Snackbar


abstract class BaseActivity : AppCompatActivity() {
    /**
     * Every fragment has to have an instance of a view model that extends from the BaseViewModel
     */
    abstract val viewModel: BaseViewModel

    abstract val rootLayout: ViewGroup

    val navController: NavController by lazy { findNavController(R.id.nav_host_fragment) }

    override fun onStart() {
        super.onStart()
        viewModel.showErrorMessage.observe(this) { it.showAsToast() }
        viewModel.showToast.observe(this) { it.showAsToast() }
        viewModel.showSnackBar.observe(this) { rootLayout.showSnackbar(it) }
        viewModel.showSnackBarInt.observe(this) { rootLayout.showSnackbar(getString(it)) }
        viewModel.navigationCommand.observe(this) { command ->
            when (command) {
                is NavigationCommand.To -> navController.navigate(command.directions)
                is NavigationCommand.Back -> navController.popBackStack()
                is NavigationCommand.BackTo -> navController.popBackStack(
                    command.destinationId,
                    false
                )
            }
        }
    }

    private fun String.showAsToast() = Toast.makeText(
        this@BaseActivity,
        this,
        Toast.LENGTH_LONG
    ).show()

    private fun View.showSnackbar(text: String) = Snackbar.make(
        this,
        text,
        Snackbar.LENGTH_LONG
    ).show()
}