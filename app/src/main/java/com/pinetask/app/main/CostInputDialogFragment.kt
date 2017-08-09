package com.pinetask.app.main

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.pinetask.app.R
import com.pinetask.app.common.PineTaskApplication
import com.pinetask.app.common.PineTaskDialogFragment
import com.pinetask.app.list_items.ListItemsPresenter
import com.pinetask.app.list_items.PineTaskItemExt
import kotlinx.android.synthetic.main.cost_input_dialog.*
import kotlinx.android.synthetic.main.dialog_button_bar.*
import javax.inject.Inject

/** Dialog which prompts the user to input the price of a specified item.  When the user clicks OK, makes a request for the price to be saved with the item. **/
class CostInputDialogFragment : PineTaskDialogFragment() {

    companion object Factory {
        const val ITEM_KEY: String = "Item"
        fun newInstance(item: PineTaskItemExt): CostInputDialogFragment {
            val dialog: CostInputDialogFragment = CostInputDialogFragment()
            dialog.arguments = Bundle()
            dialog.arguments.putSerializable(ITEM_KEY, item)
            dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
            return dialog
        }
    }

    @Inject lateinit var mListItemsPresenter: ListItemsPresenter

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View = inflater!!.inflate(R.layout.cost_input_dialog, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PineTaskApplication.getInstance().userComponent.inject(this)
        showSoftKeyboard(costEditText)
        okButton.setOnClickListener(this::okButtonOnClick)
        cancelButton.setOnClickListener { hideSoftKeyboard(); dismiss() }
    }

    @Suppress("UNUSED_PARAMETER")
    fun okButtonOnClick(view: View) {
        val strCost = costEditText.text.toString()
        if (strCost.isNotEmpty()) {
            try {
                val cost = strCost.toFloat()
                val item = arguments.getSerializable(ITEM_KEY) as PineTaskItemExt
                logMsg("Storing cost %.2f for item %s", cost, item.id)
                item.cost = cost
                mListItemsPresenter.updateItem(item)
            }
            catch (ex: NumberFormatException) {
                logMsg("Invalid cost '%s'", strCost)
            }
        }
        hideSoftKeyboard()
        dismiss()
    }
}