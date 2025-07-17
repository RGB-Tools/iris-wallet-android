package com.iriswallet.ui

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Rect
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import com.iriswallet.R
import com.iriswallet.data.RgbRepository
import com.iriswallet.data.SharedPreferencesManager
import com.iriswallet.databinding.ActivityMainBinding
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.BitcoinNetwork
import com.iriswallet.utils.Event
import com.iriswallet.utils.TAG

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    val binding
        get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    internal lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var inMainFragment = true

    var backEnabled = true

    var hideSplashScreen = false

    var loggedIn = false
    var loggingIn = false

    private val _services = MutableLiveData<Event<Map<String, Boolean>>>()
    val services: LiveData<Event<Map<String, Boolean>>>
        get() = _services

    var serviceMap: Map<String, Boolean>? = null

    internal lateinit var mService: ConnectivityService
    private var mBound: Boolean = false
    private val connection =
        object : ServiceConnection {

            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                val binder = service as ConnectivityService.ConnectivityBinder
                mService = binder.getService()
                mService.services.observe(this@MainActivity) {
                    serviceMap = it
                    _services.postValue(Event(it))
                }
                mBound = true
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                mBound = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        // see https://issuetracker.google.com/issues/36907463
        if (
            !isTaskRoot &&
                intent.hasCategory(Intent.CATEGORY_LAUNCHER) &&
                intent.action != null &&
                intent.action.equals(Intent.ACTION_MAIN)
        ) {
            finish()
            return
        }

        if (savedInstanceState != null) hideSplashScreen = true

        installSplashScreen()
        super.onCreate(savedInstanceState)
        setupSplashScreen()

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.mainToolbar)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(setOf(R.id.mainFragment), binding.drawerLayout)

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            binding.drawerLayout.close()
            menuItem.onNavDestinationSelected(navController)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.navView.visibility = View.VISIBLE
            inMainFragment = destination.id == R.id.mainFragment
        }

        setupActionBarWithNavController(navController, appBarConfiguration)

        val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.cancelAll()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Log.d(TAG, "handleOnBackPressed: enter")
                    if (!backEnabled) {
                        Toast.makeText(
                                baseContext,
                                getString(R.string.back_disabled),
                                Toast.LENGTH_SHORT,
                            )
                            .show()
                        return
                    }
                    isEnabled = false // disable this callback for the current event
                    onBackPressedDispatcher.onBackPressed() // re-trigger the dispatch
                    isEnabled = true // re-enable for future back presses
                }
            },
        )

        viewModel.refreshedFungibles.observe(this) {
            if (!inMainFragment) it.getContentIfNotHandled()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveState()
        super.onSaveInstanceState(outState)
    }

    override fun onSupportNavigateUp(): Boolean {
        return if (backEnabled)
            navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
        else {
            Toast.makeText(baseContext, getString(R.string.back_disabled), Toast.LENGTH_SHORT)
                .show()
            false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home && inMainFragment)
            return navController.navigateUp(appBarConfiguration) ||
                super.onOptionsItemSelected(item)
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        if (mBound) unbindService(connection)
        mBound = false
        RgbRepository.closeWallet()
        val needsBackup =
            !SharedPreferencesManager.backupGoogleAccount.isNullOrBlank() &&
                RgbRepository.isBackupRequired() &&
                !(SharedPreferencesManager.pinLoginConfigured && !loggedIn) &&
                !viewModel.avoidBackup
        if (needsBackup)
            applicationContext.startForegroundService(Intent(this, BackupService::class.java))
        super.onDestroy()
        _binding = null
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && ev.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                val clickedInsideET = outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())
                if (!clickedInsideET) {
                    v.clearFocus()
                    val imm: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    internal fun startConnectivityService() {
        Intent(this, ConnectivityService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun setupSplashScreen() {
        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    return if (hideSplashScreen) {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        false
                    }
                }
            }
        )
    }
}
