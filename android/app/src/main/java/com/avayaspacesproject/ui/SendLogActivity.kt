package com.avayaspacesproject.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.FileProvider
import com.avayaspacesproject.R
import com.avayaspacesproject.ReferenceApp
import com.avayaspacesproject.ReferenceApp.Companion.context
import com.avayaspacesproject.di.component.ActivityComponent
import com.avayaspacesproject.di.module.ActivityModule
import com.avayaspacesproject.di.module.DefaultSharedPreferences
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject


private const val SEND_REPORT_EMAIL = "send_report_email"

class SendLogActivity : Activity() {

    private lateinit var activityComponent: ActivityComponent

    private lateinit var btnSendLog: AppCompatButton

    private lateinit var emailEditText: AppCompatEditText

    @field:DefaultSharedPreferences
    @Inject
    protected lateinit var sharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_send_log)
        injectDependency()
        btnSendLog = findViewById(R.id.btnSendLogs)
        emailEditText = findViewById(R.id.edtTextEmail)
        if (!getSendReportEmailPreference().isNullOrEmpty()) {
            emailEditText.setText(getSendReportEmailPreference())
        }
        btnSendLog.setOnClickListener(View.OnClickListener {
            var email = emailEditText.text.toString()
            saveSendReportEmailPreference(email)
            if (!email.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                sendLogFileByMail()
            } else {
                Toast.makeText(
                    this,
                    "Please enter email address to send log report",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

    }

    private fun saveSendReportEmailPreference(email: String) {
        sharedPreferences.edit().putString(SEND_REPORT_EMAIL, email).apply()
    }

    private fun getSendReportEmailPreference(): String? =
        sharedPreferences.getString(SEND_REPORT_EMAIL, "")

    fun injectDependency() {
        activityComponent = ReferenceApp.appComponent.buildActivityComponent(ActivityModule(this))
        activityComponent.inject(this)
    }

    private fun sendLogFileByMail() {

        val fileName = getLogsToFile() ?: return
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "plain/text"
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(getSendReportEmailPreference()))
        intent.putExtra(Intent.EXTRA_SUBJECT, "ReferenceClient App log file")
        intent.putExtra(
            Intent.EXTRA_STREAM,
            FileProvider.getUriForFile(
                this,
                context.getApplicationContext().getPackageName() + ".provider",
                File(fileName)
            )
        )
        intent.putExtra(
            Intent.EXTRA_TEXT,
            "Log file attached."
        )

        startActivity(Intent.createChooser(intent, "Sending email..."))


    }

    private fun getLogsToFile(): String? {

        val packageManager: PackageManager = this.getPackageManager()
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = packageManager.getPackageInfo(this.getPackageName(), 0)
        } catch (e: PackageManager.NameNotFoundException) {
        }
        var buildModel: String = Build.MODEL
        if (!buildModel.startsWith(Build.MANUFACTURER))
            buildModel = Build.MANUFACTURER + " " + buildModel


        var filePath: String =
            Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).path + "/"

        var fileName: String = filePath + "RefAppLog.txt"

        var file = File(fileName)

        val isNewFileCreated: Boolean = file.createNewFile()

        if (isNewFileCreated) {
            println("$fileName is created successfully.")
        } else {
            println("$fileName already exists.")
            file.delete()
            file.createNewFile()
        }

        var inputReader: InputStreamReader? = null
        var fileWriter: FileWriter? = null
        try {

            var command: String =
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
                    "logcat -d -v time MyApp:v dalvikvm:v System.err:v *:s" else
                    "logcat -d -v time";


            var process: Process = Runtime.getRuntime().exec(command);
            inputReader = InputStreamReader(process.getInputStream());


            fileWriter = FileWriter(file)
            fileWriter.write("Android version: " + Build.VERSION.SDK_INT + "\n")
            fileWriter.write("Device: $buildModel\n")
            fileWriter.write("App version: " + (packageInfo?.versionCode ?: "(null)") + "\n")

            var bufferArray = CharArray(1000)
            do {
                var number: Int = inputReader.read(bufferArray, 0, bufferArray.size)
                if (number == -1)
                    break;
                fileWriter.write(bufferArray, 0, number)
            } while (true);

            inputReader.close()
            fileWriter.close()
        } catch (e: IOException) {
            if (fileWriter != null)
                try {
                    fileWriter.close();
                } catch (e: IOException) {
                }
            if (inputReader != null)
                try {
                    inputReader.close()
                } catch (e: IOException) {
                }

            return null
        }

        return fileName

    }


}