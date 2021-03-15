package app.filipebezerra.placetoremind.addeditreminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.filipebezerra.placetoremind.base.BaseFragment
import app.filipebezerra.placetoremind.databinding.AddEditReminderFragmentBinding
import app.filipebezerra.placetoremind.databinding.AddEditReminderFragmentBinding.inflate
import app.filipebezerra.placetoremind.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject


class AddEditReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: AddEditReminderViewModel by inject()
    private lateinit var binding: AddEditReminderFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflate(
        inflater,
        container,
        false
    )
    .apply {
        binding = this
        viewModel = _viewModel
        setDisplayHomeAsUpEnabled(true)
    }
    .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
    }
}
