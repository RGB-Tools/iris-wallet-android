package com.iriswallet.ui

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
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
import com.iriswallet.databinding.ActivityMainBinding
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.BitcoinNetwork
import com.iriswallet.utils.Event

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    val binding
        get() = _binding!!

    private lateinit var navController: NavController
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

        binding.navView.menu.findItem(R.id.faucetFragment).isVisible =
            AppContainer.rgbFaucetURLS.isNotEmpty()
        binding.navView.menu.findItem(R.id.issueAssetFragment).isVisible =
            AppContainer.bitcoinNetwork != BitcoinNetwork.MAINNET
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
                    if (!backEnabled) return else navController.navigateUp(appBarConfiguration)
                }
            }
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return if (backEnabled)
            navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
        else false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home && inMainFragment)
            binding.drawerLayout.openDrawer(Gravity.LEFT)
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        if (mBound) unbindService(connection)
        mBound = false
        super.onDestroy()
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

    internal fun startConnectivityService() {
        Intent(this, ConnectivityService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }
}
