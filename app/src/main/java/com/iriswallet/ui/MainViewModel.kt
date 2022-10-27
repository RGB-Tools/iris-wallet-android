package com.iriswallet.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iriswallet.data.*
import com.iriswallet.data.retrofit.RgbAsset
import com.iriswallet.utils.*
import com.iriswallet.utils.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class MainViewModel : ViewModel() {

    val cachedFungibles
        get() = AppRepository.getCachedFungibles()
    val cachedCollectibles
        get() = AppRepository.getCachedCollectibles()

    var viewingAsset: AppAsset? = null
    var viewingTransfer: AppTransfer? = null

    var refreshingAsset: Boolean = false
    var refreshingAssets: Boolean = true

    private val _refreshedAssets = MutableLiveData<Event<AppResponse<List<AppAsset>>>>()
    val refreshedAssets: LiveData<Event<AppResponse<List<AppAsset>>>>
        get() = _refreshedAssets

    private val _refreshedFungibles = MutableLiveData<Event<AppResponse<List<AppAsset>>>>()
    val refreshedFungibles: LiveData<Event<AppResponse<List<AppAsset>>>>
        get() = _refreshedFungibles

    private val _refreshedCollectibles = MutableLiveData<Event<AppResponse<List<AppAsset>>>>()
    val refreshedCollectibles: LiveData<Event<AppResponse<List<AppAsset>>>>
        get() = _refreshedCollectibles

    private val _offlineAssets = MutableLiveData<Event<AppResponse<List<AppAsset>>>>()
    val offlineAssets: LiveData<Event<AppResponse<List<AppAsset>>>>
        get() = _offlineAssets

    private val _asset = MutableLiveData<Event<AppResponse<AppAsset>>>()
    val asset: LiveData<Event<AppResponse<AppAsset>>>
        get() = _asset

    private val _recipient = MutableLiveData<Event<AppResponse<Receiver>>>()
    val recipient: LiveData<Event<AppResponse<Receiver>>>
        get() = _recipient

    private val _sent = MutableLiveData<Event<AppResponse<String>>>()
    val sent: LiveData<Event<AppResponse<String>>>
        get() = _sent

    private val _issuedAsset = MutableLiveData<Event<AppResponse<AppAsset>>>()
    val issuedAsset: LiveData<Event<AppResponse<AppAsset>>>
        get() = _issuedAsset

    private val _unspents = MutableLiveData<Event<AppResponse<List<UTXO>>>>()
    val unspents: LiveData<Event<AppResponse<List<UTXO>>>>
        get() = _unspents

    private val _rgbFaucets = MutableLiveData<Event<AppResponse<List<RgbFaucet>>>>()
    val rgbFaucets: LiveData<Event<AppResponse<List<RgbFaucet>>>>
        get() = _rgbFaucets

    private val _rgbAsset = MutableLiveData<Event<AppResponse<RgbAsset>>>()
    val rgbAsset: LiveData<Event<AppResponse<RgbAsset>>>
        get() = _rgbAsset

    private inline fun callWithTimeout(
        timeout: Long,
        noinline timeoutCallback: () -> Unit,
        crossinline callback: suspend () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val job = viewModelScope.launch(Dispatchers.IO) { callback() }
            try {
                withTimeout(timeout) { job.join() }
            } catch (ex: TimeoutCancellationException) {
                timeoutCallback()
            }
        }
    }

    private inline fun <T> tryCallWithTimeout(
        timeout: Long,
        liveData: MutableLiveData<Event<AppResponse<T>>>,
        noinline successCallback: ((data: T) -> Unit)? = null,
        noinline failureCallback: (() -> Unit)? = null,
        crossinline callback: suspend () -> T
    ) {
        callWithTimeout(
            timeout,
            timeoutCallback = {
                liveData.postValue(
                    Event(AppResponse(error = AppError(type = AppErrorType.TIMEOUT_EXCEPTION)))
                )
            }
        ) {
            try {
                val data = callback()
                liveData.postValue(Event(AppResponse(data = data)))
                successCallback?.let { it(data) }
            } catch (e: Exception) {
                liveData.postValue(Event(AppResponse(error = AppError(e))))
                failureCallback?.let { it() }
            }
        }
    }

    fun initNewApp() {
        viewModelScope.launch(Dispatchers.IO) {
            if (AppContainer.btcFaucetURLS != null)
                runCatching { AppRepository.receiveFromBitcoinFaucet() }
            getOfflineAssets()
        }
    }

    private fun postAssets() {
        _refreshedFungibles.postValue(Event(AppResponse(data = cachedFungibles)))
        _refreshedCollectibles.postValue(Event(AppResponse(data = cachedCollectibles)))
    }

    fun getOfflineAssets() {
        tryCallWithTimeout(
            AppConstants.longTimeout,
            _offlineAssets,
            successCallback = { postAssets() },
        ) {
            AppRepository.getAssets()
        }
    }

    fun refreshAssets(allowFailures: Boolean = false) {
        refreshingAssets = true
        tryCallWithTimeout(
            AppConstants.veryLongTimeout,
            _refreshedAssets,
            successCallback = {
                refreshingAssets = false
                postAssets()
            },
            failureCallback = { refreshingAssets = false },
        ) {
            AppRepository.getRefreshedAssets(allowFailures)
        }
    }

    fun refreshAssetDetail(asset: AppAsset, allowFailures: Boolean = false) {
        refreshingAsset = true
        tryCallWithTimeout(
            AppConstants.veryLongTimeout,
            _asset,
            successCallback = { refreshingAsset = false },
            failureCallback = { refreshingAsset = false },
        ) {
            AppRepository.refreshAssetDetail(asset, allowFailures)
        }
    }

    fun getBitcoinUnspents() {
        tryCallWithTimeout(AppConstants.shortTimeout, _unspents) {
            AppRepository.getBitcoinUnspents()
        }
    }

    fun genReceiveData(asset: AppAsset?) {
        tryCallWithTimeout(AppConstants.shortTimeout, _recipient) {
            AppRepository.genReceiveData(asset)
        }
    }

    fun sendAsset(asset: AppAsset, recipient: String, amount: String) {
        tryCallWithTimeout(AppConstants.longTimeout, _sent) {
            AppRepository.sendAsset(asset, recipient, amount.toULong())
        }
    }

    fun issueAsset(ticker: String, name: String, amounts: List<String>) {
        tryCallWithTimeout(AppConstants.longTimeout, _issuedAsset) {
            AppRepository.issueRGBAsset(ticker, name, amounts.map { it.toULong() })
        }
    }

    fun deleteTransfer(asset: AppAsset, recipient: String) {
        viewModelScope.launch(Dispatchers.IO) {
            AppRepository.deleteRGBTransfer(recipient)
            refreshAssetDetail(asset, allowFailures = true)
            AppRepository.allowedFailure = null
        }
    }

    fun checkCache() {
        viewModelScope.launch(Dispatchers.IO) { if (AppRepository.isCacheDirty) refreshAssets() }
    }

    fun getFaucetAssetGroups() {
        tryCallWithTimeout(AppConstants.longTimeout, _rgbFaucets) {
            AppRepository.getRgbFaucetAssetGroups()
        }
    }

    fun receiveFromRgbFaucet(url: String, group: String) {
        tryCallWithTimeout(AppConstants.veryLongTimeout, _rgbAsset) {
            AppRepository.receiveFromRgbFaucet(url, group)
        }
    }
}
