package com.iriswallet.ui

import android.content.res.AssetFileDescriptor
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.iriswallet.R
import com.iriswallet.databinding.FragmentIssueRgb25AssetBinding
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.TAG
import java.io.BufferedInputStream
import java.io.InputStream

class IssueRgb25AssetFragment :
    MainBaseFragment<FragmentIssueRgb25AssetBinding>(FragmentIssueRgb25AssetBinding::inflate) {

    private lateinit var editableFields: Array<EditText>

    private var mediaFileStream: InputStream? = null

    private var getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                val fileDescriptor =
                    requireContext().contentResolver.openAssetFileDescriptor(it, "r")
                if (
                    fileDescriptor == null ||
                        fileDescriptor.length == AssetFileDescriptor.UNKNOWN_LENGTH
                ) {
                    Log.d(TAG, "Cannot retrieve file length. File descriptor: $fileDescriptor")
                    toastMsg(R.string.err_retrieving_file_length)
                    return@registerForActivityResult
                }
                if (fileDescriptor.length >= AppConstants.MAX_MEDIA_BYTES) {
                    Log.d(TAG, "Media file too big: ${fileDescriptor.length}")
                    toastMsg(R.string.file_too_big)
                    return@registerForActivityResult
                }
                mediaFileStream =
                    BufferedInputStream(requireContext().contentResolver.openInputStream(it))
                assert(mediaFileStream != null)
                assert(mediaFileStream!!.markSupported())
                mediaFileStream!!.mark(
                    AppConstants.MAX_MEDIA_BYTES
                ) // mark current position (begin) of stream

                binding.issueUploadFileBtn.text = getString(R.string.change_file_button)
                binding.issueBtn.isEnabled = enableIssueBtn()

                val collectibleImg = Drawable.createFromStream(mediaFileStream, it.toString())
                mediaFileStream!!.reset() // reset stream to beginning so it can be read again

                if (collectibleImg != null) {
                    binding.issueCollectibleImg.setImageDrawable(collectibleImg)
                    binding.issueNoPreviewCardView.visibility = View.GONE
                    binding.issueCollectibleImg.visibility = View.VISIBLE
                } else {
                    binding.issueCollectibleImg.visibility = View.GONE
                    binding.issueNoPreviewCardView.visibility = View.VISIBLE
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editableFields = arrayOf(binding.nameInputET, binding.amountInputET)
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
                        binding.issueBtn.isEnabled = enableIssueBtn()
                    }
                }
            )
        }

        binding.amountInputET.setOnEditorActionListener(onKeyboardDoneListener)

        binding.issueBtn.setOnClickListener {
            disableUI()
            viewModel.issueRgb25Asset(
                binding.nameInputET.text.toString(),
                listOf(binding.amountInputET.text.toString()),
                binding.descriptionInputET.text.toString(),
                mediaFileStream,
            )
        }

        binding.issueUploadFileBtn.setOnClickListener { getContent.launch("*/*") }

        viewModel.issuedRgb25Asset.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (response.data != null) {
                    viewModel.viewingAsset = response.data
                    enableUI()
                    findNavController()
                        .navigate(
                            IssueRgb25AssetFragmentDirections
                                .actionIssueRgb25AssetFragmentToAssetDetailFragment(
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
        binding.issueUploadFileBtn.isEnabled = false
    }

    private fun enableIssueBtn(): Boolean {
        return allETsFilled(editableFields) &&
            isETPositive(binding.amountInputET) &&
            mediaFileStream != null
    }
}
