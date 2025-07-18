package com.iriswallet.ui

import android.util.Log
import androidx.lifecycle.*
import com.iriswallet.data.AppRepository
import com.iriswallet.data.BackupRepository
import com.iriswallet.data.RgbRepository
import com.iriswallet.data.SharedPreferencesManager
import com.iriswallet.data.db.RgbCertifiedAsset
import com.iriswallet.data.retrofit.Distribution
import com.iriswallet.data.retrofit.DistributionMode
import com.iriswallet.data.retrofit.RetrofitModule.assetCertificationService
import com.iriswallet.data.retrofit.RgbAsset
import com.iriswallet.data.retrofit.RgbAssetGroup
import com.iriswallet.utils.*
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class MainViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _refreshedAssets = MutableLiveData<Event<AppResponse<List<AppAsset>>>>()
    val refreshedAssets: LiveData<Event<AppResponse<List<AppAsset>>>>
        get() = _refreshedAssets

    private val _refreshedFungibles = MutableLiveData<Event<AppResponse<Void>>>()
    val refreshedFungibles: LiveData<Event<AppResponse<Void>>>
        get() = _refreshedFungibles

    private val _refreshedCollectibles = MutableLiveData<Event<AppResponse<Void>>>()
    val refreshedCollectibles: LiveData<Event<AppResponse<Void>>>
        get() = _refreshedCollectibles

    private val _offlineAssets = MutableLiveData<Event<AppResponse<List<AppAsset>>>>()
    val offlineAssets: LiveData<Event<AppResponse<List<AppAsset>>>>
        get() = _offlineAssets

    private val _asset = MutableLiveData<Event<AppResponse<AppAsset>>>()
    val asset: LiveData<Event<AppResponse<AppAsset>>>
        get() = _asset

    private val _metadata = MutableLiveData<Event<AppResponse<org.rgbtools.Metadata>>>()
    val metadata: LiveData<Event<AppResponse<org.rgbtools.Metadata>>>
        get() = _metadata

    private val _recipient = MutableLiveData<Event<AppResponse<Receiver>>>()
    val recipient: LiveData<Event<AppResponse<Receiver>>>
        get() = _recipient

    private val _wentOnlineAgain = MutableLiveData<Event<AppResponse<Boolean>>>()
    val wentOnlineAgain: LiveData<Event<AppResponse<Boolean>>>
        get() = _wentOnlineAgain

    private val _sent = MutableLiveData<Event<AppResponse<String>>>()
    val sent: LiveData<Event<AppResponse<String>>>
        get() = _sent

    private val _issuedRgb20Asset = MutableLiveData<Event<AppResponse<AppAsset>>>()
    val issuedRgb20Asset: LiveData<Event<AppResponse<AppAsset>>>
        get() = _issuedRgb20Asset

    private val _issuedRgb25Asset = MutableLiveData<Event<AppResponse<AppAsset>>>()
    val issuedRgb25Asset: LiveData<Event<AppResponse<AppAsset>>>
        get() = _issuedRgb25Asset

    private val _unspents = MutableLiveData<Event<AppResponse<List<UTXO>>>>()
    val unspents: LiveData<Event<AppResponse<List<UTXO>>>>
        get() = _unspents

    private val _rgbFaucets = MutableLiveData<Event<AppResponse<List<RgbFaucet>>>>()
    val rgbFaucets: LiveData<Event<AppResponse<List<RgbFaucet>>>>
        get() = _rgbFaucets

    private val _rgbFaucetResponse =
        MutableLiveData<Event<AppResponse<Pair<RgbAsset, Distribution>>>>()
    val rgbFaucetResponse: LiveData<Event<AppResponse<Pair<RgbAsset, Distribution>>>>
        get() = _rgbFaucetResponse

    private val _hidden = MutableLiveData<Event<AppResponse<Boolean>>>()
    val hidden: LiveData<Event<AppResponse<Boolean>>>
        get() = _hidden

    private val _certified = MutableLiveData<Pair<String, Boolean>>()
    val certified: LiveData<Pair<String, Boolean>>
        get() = _certified

    private val _backup = MutableLiveData<Event<AppResponse<Boolean>>>()
    val backup: LiveData<Event<AppResponse<Boolean>>>
        get() = _backup

    private val _restore = MutableLiveData<Event<AppResponse<Boolean>>>()
    val restore: LiveData<Event<AppResponse<Boolean>>>
        get() = _restore

    var cachedFungibles: List<AppAsset> = listOf()

    var cachedCollectibles: List<AppAsset> = listOf()

    var viewingAsset: AppAsset? = null

    var viewingTransfer: AppTransfer? = null

    var refreshingAsset: Boolean = false
    var refreshingAssets: Boolean = false

    var avoidBackup: Boolean = false

    internal fun saveState() {
        Log.d(TAG, "Saving state...")
        savedStateHandle[AppConstants.BUNDLE_APP_ASSETS] = AppRepository.appAssets
        if (viewingAsset != null) savedStateHandle[AppConstants.BUNDLE_ASSET_ID] = viewingAsset!!.id
        if (viewingTransfer != null)
            savedStateHandle[AppConstants.BUNDLE_TRANSFER_ID] =
                viewingAsset!!.transfers.indexOf(viewingTransfer)
        Log.d(TAG, "State saved")
    }

    internal fun restoreState() {
        Log.d(TAG, "Restoring state...")
        if (savedStateHandle.contains(AppConstants.BUNDLE_APP_ASSETS)) {
            Log.d(TAG, "Recovering assets from saved state...")
            AppRepository.appAssets.clear()
            AppRepository.appAssets.addAll(
                savedStateHandle.get<MutableList<AppAsset>>(AppConstants.BUNDLE_APP_ASSETS)!!
            )
            savedStateHandle.remove<MutableList<AppAsset>>(AppConstants.BUNDLE_APP_ASSETS)
            cacheAssets()
        }
        if (savedStateHandle.contains(AppConstants.BUNDLE_ASSET_ID)) {
            Log.d(TAG, "Recovering asset from saved state...")
            val cachedAssetId = savedStateHandle.get<String>(AppConstants.BUNDLE_ASSET_ID)!!
            viewingAsset = AppRepository.getCachedAsset(cachedAssetId)
            savedStateHandle.remove<String>(AppConstants.BUNDLE_ASSET_ID)
        }
        if (savedStateHandle.contains(AppConstants.BUNDLE_TRANSFER_ID)) {
            Log.d(TAG, "Recovering transfer from state...")
            val cachedTransferId = savedStateHandle.get<Int>(AppConstants.BUNDLE_TRANSFER_ID)!!
            if (viewingAsset != null) {
                viewingTransfer = viewingAsset!!.transfers[cachedTransferId]
            }
            savedStateHandle.remove<Int>(AppConstants.BUNDLE_TRANSFER_ID)
        }
        Log.d(TAG, "Restore completed")
    }

    private inline fun callWithTimeout(
        timeout: Long,
        noinline timeoutCallback: () -> Unit,
        crossinline callback: suspend () -> Unit,
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
        requestID: String? = null,
        noinline successCallback: ((data: T) -> Unit)? = null,
        noinline failureCallback: (() -> Unit)? = null,
        crossinline callback: suspend () -> T,
    ) {
        callWithTimeout(
            timeout,
            timeoutCallback = {
                liveData.postValue(
                    Event(
                        AppResponse(
                            requestID = requestID,
                            error = AppError(type = AppErrorType.TIMEOUT_EXCEPTION),
                        )
                    )
                )
            },
        ) {
            try {
                val data = callback()
                liveData.postValue(Event(AppResponse(requestID = requestID, data = data)))
                successCallback?.let { it(data) }
            } catch (e: Exception) {
                liveData.postValue(Event(AppResponse(requestID = requestID, error = AppError(e))))
                failureCallback?.let { it() }
            }
        }
    }

    fun initNewApp() {
        viewModelScope.launch(Dispatchers.IO) {
            if (AppContainer.btcFaucetURL != null)
                runCatching { AppRepository.receiveFromBitcoinFaucet() }
            getOfflineAssets()
        }
    }

    private fun cacheAssets() {
        cachedFungibles = AppRepository.getCachedFungibles()
        cachedCollectibles = AppRepository.getCachedCollectibles()
    }

    fun getOfflineAssets() {
        tryCallWithTimeout(AppConstants.LONG_TIMEOUT, _offlineAssets) {
            val assets = AppRepository.getAssets()
            cacheAssets()
            assets
        }
    }

    fun refreshAssets(firstAppRefresh: Boolean = false) {
        refreshingAssets = true
        tryCallWithTimeout(
            AppConstants.VERY_LONG_TIMEOUT,
            _refreshedAssets,
            successCallback = { refreshingAssets = false },
            failureCallback = { refreshingAssets = false },
        ) {
            val refreshedAssets = AppRepository.getRefreshedAssets(firstAppRefresh)
            cacheAssets()
            _refreshedFungibles.postValue(Event(AppResponse()))
            _refreshedCollectibles.postValue(Event(AppResponse()))
            refreshedAssets
        }
    }

    fun refreshAssetDetail(asset: AppAsset) {
        refreshingAsset = true
        tryCallWithTimeout(
            AppConstants.VERY_LONG_TIMEOUT,
            _asset,
            requestID = asset.id,
            successCallback = { refreshingAsset = false },
            failureCallback = { refreshingAsset = false },
        ) {
            AppRepository.refreshAssetDetail(asset)
        }
    }

    fun getAssetMetadata(asset: AppAsset) {
        tryCallWithTimeout(AppConstants.SHORT_TIMEOUT, _metadata) {
            AppRepository.getAssetMetadata(asset)
        }
    }

    fun getBitcoinUnspents() {
        tryCallWithTimeout(AppConstants.SHORT_TIMEOUT, _unspents) {
            AppRepository.getBitcoinUnspents()
        }
    }

    fun genReceiveData(asset: AppAsset?) {
        tryCallWithTimeout(AppConstants.SHORT_TIMEOUT, _recipient) {
            val data = AppRepository.genReceiveData(asset)
            cacheAssets()
            data
        }
    }

    fun goOnlineAgain(electrumURL: String) {
        tryCallWithTimeout(AppConstants.LONG_TIMEOUT, _wentOnlineAgain) {
            RgbRepository.goOnlineAgain(electrumURL)
            true
        }
    }

    fun sendAsset(
        asset: AppAsset,
        recipient: String,
        amount: String,
        transportEndpoints: List<String>,
        feeRate: ULong,
    ) {
        tryCallWithTimeout(AppConstants.LONG_TIMEOUT, _sent) {
            val txid =
                AppRepository.sendAsset(
                    asset,
                    recipient,
                    amount.toULong(),
                    transportEndpoints,
                    feeRate,
                )
            cacheAssets()
            txid
        }
    }

    fun issueRgb20Asset(ticker: String, name: String, amounts: List<String>) {
        tryCallWithTimeout(AppConstants.LONG_TIMEOUT, _issuedRgb20Asset) {
            val asset = AppRepository.issueRgb20Asset(ticker, name, amounts.map { it.toULong() })
            cacheAssets()
            asset
        }
    }

    fun issueRgb25Asset(
        name: String,
        amounts: List<String>,
        description: String?,
        fileStream: InputStream?,
    ) {
        tryCallWithTimeout(AppConstants.LONG_TIMEOUT, _issuedRgb25Asset) {
            val asset =
                AppRepository.issueRgb25Asset(
                    name,
                    amounts.map { it.toULong() },
                    description,
                    fileStream,
                )
            cacheAssets()
            asset
        }
    }

    fun deleteTransfer(asset: AppAsset, transfer: AppTransfer) {
        tryCallWithTimeout(AppConstants.SHORT_TIMEOUT, _asset, requestID = asset.id) {
            val updatedAsset = AppRepository.deleteRGBTransfer(asset, transfer)
            cacheAssets()
            updatedAsset
        }
    }

    fun handleAssetVisibility(assetID: String) {
        tryCallWithTimeout(AppConstants.SHORT_TIMEOUT, _hidden) {
            val hidden = AppRepository.handleAssetVisibility(assetID)
            cacheAssets()
            hidden
        }
    }

    fun checkCache() {
        viewModelScope.launch(Dispatchers.IO) { if (AppRepository.isCacheDirty) refreshAssets() }
    }

    fun checkAssetCertified(assetID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val certified = assetCertificationService.isAssetCertified(assetID).code() == 200
                _certified.postValue(Pair(assetID, certified))
                val isSavedAsCertified =
                    AppContainer.db.rgbCertifiedAssetDao().getRgbCertifiedAsset(assetID) != null
                if (certified && !isSavedAsCertified) {
                    AppContainer.db
                        .rgbCertifiedAssetDao()
                        .insertRgbCertifiedAsset(RgbCertifiedAsset(assetID))
                } else if (!certified && isSavedAsCertified) {
                    AppContainer.db.rgbCertifiedAssetDao().deleteRgbCertifiedAsset(assetID)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error trying to check if asset is certified: $e")
            }
        }
    }

    fun getFaucetAssetGroups() {
        tryCallWithTimeout(AppConstants.LONG_TIMEOUT, _rgbFaucets) {
            AppRepository.getRgbFaucetAssetGroups()
        }
    }

    fun receiveFromRgbFaucet(url: String, group: Map.Entry<String, RgbAssetGroup>) {
        tryCallWithTimeout(AppConstants.VERY_LONG_TIMEOUT, _rgbFaucetResponse) {
            val (asset, distribution) = AppRepository.receiveFromRgbFaucet(url, group)
            if (distribution.modeEnum() == DistributionMode.STANDARD) cacheAssets()
            Pair(asset, distribution)
        }
    }

    fun startBackup(driveAccessToken: String, backupGoogleAccount: String) {
        tryCallWithTimeout(AppConstants.BACKUP_RESTORE_TIMEOUT, _backup) {
            val driveClient = GoogleDriveAuthHelper.initializeDriveClient(driveAccessToken)
            BackupRepository.doBackup(driveClient)
            SharedPreferencesManager.backupGoogleAccount = backupGoogleAccount
            true
        }
    }

    fun restoreBackup(
        driveAccessToken: String,
        backupGoogleAccount: String,
        restoredMnemonic: String,
    ) {
        tryCallWithTimeout(AppConstants.BACKUP_RESTORE_TIMEOUT, _restore) {
            val driveClient = GoogleDriveAuthHelper.initializeDriveClient(driveAccessToken)
            BackupRepository.restoreBackup(driveClient, restoredMnemonic)
            SharedPreferencesManager.backupGoogleAccount = backupGoogleAccount
            true
        }
    }
}
