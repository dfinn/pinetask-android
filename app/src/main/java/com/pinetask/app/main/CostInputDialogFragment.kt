package com.pinetask.app.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.pinetask.app.R
import com.pinetask.app.common.PineTaskDialogFragment
import com.pinetask.app.list_items.PineTaskItemExt

/** Dialog which prompts the user to input the price of a specified item.  When the user clicks OK, makes a request for the price to be saved with the item. **/
class CostInputDialogFragment : PineTaskDialogFragment() {

    companion object Factory {
        const val ITEM_KEY: String = "Item"
        fun newInstance(item: PineTaskItemExt): CostInputDialogFragment {
            val dialog: CostInputDialogFragment = CostInputDialogFragment()
            dialog.arguments = Bundle()
            dialog.arguments.putSerializable(ITEM_KEY, item)
            return dialog
        }
    }

    @BindView(R.id.costEditText) var mCostEditText: EditText? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.cost_input_dialog, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    @OnClick(R.id.okButton)
    fun okButtonOnClick(view: View) {
        val strCost = mCostEditText!!.text.toString()
        val cost = strCost.toFloat()
        val item = arguments.getSerializable(ITEM_KEY) as PineTaskItemExt
        logMsg("Storing cost %.2f for item %s", cost, item.id)
    }
}