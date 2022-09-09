package org.iriswallet.ui

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import org.iriswallet.R
import org.iriswallet.databinding.ActivityMainBinding
import org.iriswallet.utils.Event

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    val binding
        get() = _binding!!

    private lateinit var navController: NavController

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
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.appTitle.visibility =
                if (destination.id == R.id.mainFragment) View.VISIBLE else View.GONE
        }
        setupActionBarWithNavController(navController)

        val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.cancelAll()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onStop() {
        super.onStop()
        if (mBound) unbindService(connection)
        mBound = false
    }

    override fun onBackPressed() {
        if (backEnabled) super.onBackPressed()
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
