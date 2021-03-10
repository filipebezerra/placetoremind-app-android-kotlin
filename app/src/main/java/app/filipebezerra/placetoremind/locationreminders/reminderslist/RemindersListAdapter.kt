package app.filipebezerra.placetoremind.locationreminders.reminderslist

import app.filipebezerra.placetoremind.R
import app.filipebezerra.placetoremind.base.BaseRecyclerViewAdapter


//Use data binding to show the reminder on the item
class RemindersListAdapter(callBack: (selectedReminder: ReminderDataItem) -> Unit) :
    BaseRecyclerViewAdapter<ReminderDataItem>(callBack) {
    override fun getLayoutRes(viewType: Int) = R.layout.it_reminder
}