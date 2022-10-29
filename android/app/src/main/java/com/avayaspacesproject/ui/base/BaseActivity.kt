package com.avayaspacesproject.ui.base

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Build.ID
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.avaya.spacescsdk.utils.UcLog
import com.avaya.spacescsdk.utils.setupSoftInputMode
import com.avayaspacesproject.utils.CommonUtils
import dagger.android.AndroidInjection
import javax.inject.Inject

abstract class BaseActivity<V : ViewBinding> : AppCompatActivity() {

    lateinit var binding: V
    private var progressDialog: ProgressDialog? = null

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.setDefaultUncaughtExceptionHandler(handleAppCrash)
        super.onCreate(savedInstanceState)
        binding = getViewBinding()
        setContentView(binding.root)
        injectDependency()
        initializeViewModel()
        initializeView()
    }

    private fun performDependencyInjection() {
        AndroidInjection.inject(this)
    }

    abstract fun getViewBinding(): V

    abstract fun initializeView()

    abstract fun initializeViewModel()

    abstract fun injectDependency()


    fun navigateToScreen(resId: Int) {

    }

    fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    fun showProgress(context: Context) {
        progressDialog?.hide()
        progressDialog = CommonUtils.showProgressDialog(context)
    }

    fun hideProgress() {
        progressDialog?.let { if (it.isShowing) it.cancel() }
    }

    fun showLogs(screenName: String, logMessage: String) {
        Log.d(screenName, logMessage)
    }


    @Deprecated("")
    protected open fun onResumeImpl() {
    }

    override fun onResume() {
        UcLog.d(ID, "onResume")
        super.onResume()

        onResumeImpl()
        this.setupSoftInputMode(null)
    }

    override fun onPause() {
        UcLog.d(ID, "onPause")
        super.onPause()
    }


    private val handleAppCrash =
        Thread.UncaughtExceptionHandler { thread, exception ->
            Log.e("error", exception.toString())
            exception.printStackTrace()

            val intent = Intent()
            intent.action = "com.avayaspacesproject.SEND_LOG"

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            startActivity(intent)

            System.exit(1)

        }
}