package app.filipebezerra.placetoremind.reminderslist

import android.os.Bundle
import android.view.*
import app.filipebezerra.placetoremind.R
import app.filipebezerra.placetoremind.base.BaseFragment
import app.filipebezerra.placetoremind.databinding.FragmentRemindersBinding
import app.filipebezerra.placetoremind.databinding.FragmentRemindersBinding.inflate
import app.filipebezerra.placetoremind.utils.setDisplayHomeAsUpEnabled
import app.filipebezerra.placetoremind.utils.setTitle
import app.filipebezerra.placetoremind.utils.setup
import org.koin.androidx.viewmodel.ext.android.viewModel


class ReminderListFragment : BaseFragment() {
    override val _viewModel: RemindersListViewModel by viewModel()

    private lateinit var binding: FragmentRemindersBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflate(
        inflater,
        container,
        false
    ).apply {
        viewModel = _viewModel

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(false)
        setTitle(getString(R.string.app_name))

        refreshLayout.setOnRefreshListener { _viewModel.loadReminders() }
        binding = this
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        _viewModel.loadReminders()
    }

    private fun setupRecyclerView() {
        RemindersListAdapter {

        }.also { binding.reminderssRecyclerView.setup(it) }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) = inflater.inflate(
        R.menu.main_menu,
        menu
    )

    override fun onPrepareOptionsMenu(menu: Menu) = _viewModel.onPrepareOptionsMenu(menu)

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.logout -> _viewModel.onLogoutRequested().let { true }
        else -> super.onOptionsItemSelected(item)
    }
}
