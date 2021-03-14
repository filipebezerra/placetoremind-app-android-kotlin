package app.filipebezerra.placetoremind.authentication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import app.filipebezerra.placetoremind.base.BaseFragment
import app.filipebezerra.placetoremind.databinding.AuthenticationFragmentBinding
import org.koin.androidx.viewmodel.ext.android.viewModel


/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationFragment : BaseFragment() {

    override val _viewModel: AuthenticationViewModel by viewModel()

    private lateinit var binding: AuthenticationFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = AuthenticationFragmentBinding.inflate(
        inflater,
        container,
        false
    ).apply {
        binding = this

//          TODO: If the user was authenticated, send him to RemindersActivity
//          TODO: a bonus is to customize the sign in flow to look nice using :
        //https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#custom-layout
        loginButton.setOnClickListener { _viewModel.onLoginButtonClicked() }

        _viewModel.userAuthenticated.observe(viewLifecycleOwner) { authenticated ->
            authenticated.takeIf { it }?.let { findNavController().popBackStack() }
        }
    }.root

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        _viewModel.onActivityResult(requestCode, resultCode, data)
    }
}
