package com.iriswallet.ui

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.navigation.fragment.findNavController
import com.iriswallet.R
import com.iriswallet.databinding.FragmentIssueRgb20AssetBinding
import com.iriswallet.utils.AppConstants

class IssueRgb20AssetFragment :
    MainBaseFragment<FragmentIssueRgb20AssetBinding>(FragmentIssueRgb20AssetBinding::inflate) {

    private lateinit var editableFields: Array<EditText>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tickerInputET.filters += InputFilter.AllCaps()
        editableFields = arrayOf(binding.tickerInputET, binding.nameInputET, binding.amountInputET)
        for (editText in editableFields) {
            editText.addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(
                        charSequence: CharSequence,
                        i: Int,
                        i1: Int,
                        i2: Int,
                    ) {}

                    override fun onTextChanged(
                        charSequence: CharSequence,
                        i: Int,
                        i1: Int,
                        i2: Int,
                    ) {}

                    override fun afterTextChanged(editable: Editable) {
                        if (editText.inputType == InputType.TYPE_CLASS_NUMBER)
                            fixETAmount(
                                editText,
                                editable.toString(),
                                maxULongAmount = AppConstants.ISSUE_MAX_AMOUNT,
                            )
                        binding.issueBtn.isEnabled =
                            allETsFilled(editableFields) && isETPositive(binding.amountInputET)
                    }
                }
            )
        }

        binding.amountInputET.setOnEditorActionListener(onKeyboardDoneListener)

        binding.issueBtn.setOnClickListener {
            disableUI()
            viewModel.issueRgb20Asset(
                binding.tickerInputET.text.toString(),
                binding.nameInputET.text.toString(),
                listOf(binding.amountInputET.text.toString()),
            )
        }

        viewModel.issuedRgb20Asset.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (response.data != null) {
                    viewModel.viewingAsset = response.data
                    findNavController()
                        .navigate(
                            IssueRgb20AssetFragmentDirections
                                .actionIssueRgb20AssetFragmentToAssetDetailFragment(
                                    viewModel.viewingAsset!!.name
                                )
                        )
                } else {
                    handleError(response.error!!) {
                        toastMsg(R.string.err_issuing_asset, response.error.message)
                        enableUI()
                    }
                }
            }
        }
    }

    override fun enableUI() {
        super.enableUI()
        binding.issueBtn.isEnabled = true
        binding.issuePB.visibility = View.INVISIBLE
    }

    private fun disableUI() {
        mActivity.backEnabled = false
        binding.issuePB.visibility = View.VISIBLE
        binding.issueBtn.isEnabled = false
    }
}
