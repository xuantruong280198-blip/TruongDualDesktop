package com.truong.dualdesktop

import android.app.Activity
import android.app.ActivityManager
import android.app.Presentation
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.view.InputDevice
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), DisplayManager.DisplayListener, InputManager.InputDeviceListener {
    private lateinit var displayManager: DisplayManager
    private lateinit var inputManager: InputManager
    private lateinit var report: TextView
    private lateinit var status: TextView
    private lateinit var appPicker: LinearLayout
    private var desktop: DesktopPresentation? = null
    private var lastAction = "Chưa thử mở ứng dụng nào."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
        displayManager.registerDisplayListener(this, null)
        inputManager.registerInputDeviceListener(this, null)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 24, 22, 18)
            setBackgroundColor(0xFF101820.toInt())
        }
        status = TextView(this).apply {
            text = "TRUONG DUAL DESKTOP · 0.2 BETA"
            textSize = 20f
            setTextColor(0xFF65D6AD.toInt())
        }
        root.addView(status)
        root.addView(TextView(this).apply {
            text = "Desktop kết hợp ứng dụng Android"
            textSize = 15f
            setTextColor(0xFFE6EDF3.toInt())
            setPadding(0, 8, 0, 14)
        })
        root.addView(Button(this).apply {
            text = "Mở / làm mới Desktop trên HDMI"
            setOnClickListener { openDesktop() }
        })
        root.addView(Button(this).apply {
            text = "Làm mới báo cáo màn hình và chuột"
            setOnClickListener { refreshReport() }
        })
        root.addView(Button(this).apply {
            text = "Đóng Desktop HDMI"
            setOnClickListener { desktop?.dismiss(); desktop = null; refreshReport() }
        })
        root.addView(TextView(this).apply {
            text = "CHỌN ỨNG DỤNG TỪ MÀN HÌNH MÁY TÍNH BẢNG"
            textSize = 14f
            setTextColor(0xFF65D6AD.toInt())
            setPadding(0, 14, 0, 6)
        })
        appPicker = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(appPicker) }, LinearLayout.LayoutParams(-1, 220))
        report = TextView(this).apply {
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
            setTextColor(0xFFE6EDF3.toInt())
            setTextIsSelectable(true)
        }
        root.addView(ScrollView(this).apply { addView(report) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        populateAppPicker()
        refreshReport()
    }

    private fun populateAppPicker() {
        appPicker.removeAllViews()
        val apps = queryLaunchableApps()
        if (apps.isEmpty()) {
            appPicker.addView(TextView(this).apply { text = "Không tìm thấy ứng dụng."; setTextColor(0xFFE6EDF3.toInt()) })
            return
        }
        apps.forEach { app ->
            val label = app.loadLabel(packageManager).toString()
            appPicker.addView(Button(this).apply {
                text = "Mở $label trên HDMI"
                setOnClickListener {
                    val target = secondaryDisplay()
                    if (target == null) {
                        status.text = "Chưa kết nối màn hình HDMI."
                    } else {
                        launchOnDisplay(app, target.displayId)
                    }
                }
            }, LinearLayout.LayoutParams(-1, 48).apply { bottomMargin = 4 })
        }
    }

    private fun secondaryDisplay(): Display? = displayManager.displays.firstOrNull {
        it.displayId != Display.DEFAULT_DISPLAY && it.flags and Display.FLAG_PRESENTATION != 0
    }

    private fun openDesktop() {
        val target = secondaryDisplay()
        if (target == null) {
            status.text = "Chưa tìm thấy màn hình HDMI Presentation."
            return
        }
        try {
            desktop?.dismiss()
            desktop = DesktopPresentation(this, target, queryLaunchableApps()) { app ->
                launchOnDisplay(app, target.displayId)
            }.also { it.show() }
            status.text = "Desktop đã mở trên HDMI · Display ${target.displayId}"
            refreshReport()
        } catch (e: Exception) {
            status.text = "Không mở được Desktop: ${e.javaClass.simpleName}: ${e.message}"
        }
    }

    private fun queryLaunchableApps(): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }
    }

    private fun launchOnDisplay(app: ResolveInfo, displayId: Int) {
        val info = app.activityInfo
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(info.packageName, info.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            val supported = packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS
            )
            val allowed = (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .isActivityStartAllowedOnDisplay(this, displayId, intent)
            if (!supported) {
                lastAction = "KHÔNG HỖ TRỢ: thiết bị không công bố FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS."
                status.text = lastAction
                refreshReport()
                return
            }
            if (!allowed) {
                lastAction = "BỊ CHẶN: Android/HyperOS không cho app này mở trên Display $displayId. Không gửi lệnh mở."
                status.text = lastAction
                refreshReport()
                return
            }
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId)
            startActivity(intent, options.toBundle())
            lastAction = "ĐÃ GỬI YÊU CẦU: ${app.loadLabel(packageManager)} → Display $displayId. Android vẫn có thể điều hướng lại; cần kiểm tra thực tế trên HDMI."
            status.text = lastAction
            refreshReport()
        } catch (e: Exception) {
            lastAction = "LỖI KHI MỞ: ${e.javaClass.simpleName}: ${e.message}"
            status.text = lastAction
            refreshReport()
        }
    }

    private fun refreshReport() {
        val lines = mutableListOf<String>()
        lines += "DEVICE: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        lines += "ANDROID: ${android.os.Build.VERSION.RELEASE} / SDK ${android.os.Build.VERSION.SDK_INT}"
        lines += "BUILD: ${android.os.Build.DISPLAY}"
        val secondaryActivities = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS)
        lines += "FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS=$secondaryActivities"
        lines += "LAST_LAUNCH=$lastAction"
        lines += "TIME: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}"
        lines += ""
        val displays = displayManager.displays
        lines += "DISPLAYS (${displays.size})"
        displays.forEach { d ->
            val metrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION") d.getRealMetrics(metrics)
            lines += "id=${d.displayId} name=${d.name} state=${d.state}"
            lines += "flags=0x${d.flags.toString(16)} size=${metrics.widthPixels}x${metrics.heightPixels} presentation=${d.flags and Display.FLAG_PRESENTATION != 0}"
        }
        lines += ""
        lines += "INPUT DEVICES (${inputManager.inputDeviceIds.size})"
        inputManager.inputDeviceIds.forEach { id ->
            val dev = inputManager.getInputDevice(id) ?: return@forEach
            val sources = buildList {
                if (dev.supportsSource(InputDevice.SOURCE_MOUSE)) add("MOUSE")
                if (dev.supportsSource(InputDevice.SOURCE_KEYBOARD)) add("KEYBOARD")
                if (dev.supportsSource(InputDevice.SOURCE_TOUCHSCREEN)) add("TOUCHSCREEN")
                if (dev.supportsSource(InputDevice.SOURCE_STYLUS)) add("STYLUS")
            }
            lines += "id=$id name=${dev.name} sources=${sources.joinToString()} virtual=${dev.isVirtual}"
        }
        lines += ""
        lines += "Desktop mode: app-owned Presentation + explicit launch-on-display request."
        lines += "Cross-display system cursor routing is not claimed; it requires OS/OEM support."
        report.text = lines.joinToString("\n")
        status.text = if (lastAction == "Chưa thử mở ứng dụng nào.") "Sẵn sàng · ${displays.size} màn hình · ${inputManager.inputDeviceIds.size} thiết bị nhập" else lastAction
    }

    override fun onDisplayAdded(displayId: Int) { populateAppPicker(); refreshReport(); if (displayId != 0) openDesktop() }
    override fun onDisplayRemoved(displayId: Int) { desktop?.dismiss(); desktop = null; refreshReport() }
    override fun onDisplayChanged(displayId: Int) { refreshReport() }
    override fun onInputDeviceAdded(deviceId: Int) = refreshReport()
    override fun onInputDeviceRemoved(deviceId: Int) = refreshReport()
    override fun onInputDeviceChanged(deviceId: Int) = refreshReport()

    override fun onDestroy() {
        displayManager.unregisterDisplayListener(this)
        inputManager.unregisterInputDeviceListener(this)
        desktop?.dismiss()
        super.onDestroy()
    }
}

private class DesktopPresentation(
    context: Context,
    display: Display,
    private val apps: List<ResolveInfo>,
    private val onLaunch: (ResolveInfo) -> Unit
) : Presentation(context, display) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 22, 28, 18)
            setBackgroundColor(0xFF101820.toInt())
        }
        root.addView(TextView(context).apply {
            text = "TRUONG DESKTOP"
            textSize = 26f
            setTextColor(0xFF65D6AD.toInt())
        })
        root.addView(TextView(context).apply {
            text = "ỨNG DỤNG ANDROID · ${apps.size} ứng dụng"
            textSize = 14f
            setTextColor(0xFFB8C7D9.toInt())
            setPadding(0, 4, 0, 16)
        })
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        apps.forEach { app ->
            val label = app.loadLabel(context.packageManager).toString()
            list.addView(Button(context).apply {
                text = label
                textSize = 16f
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setOnClickListener { onLaunch(app) }
            }, LinearLayout.LayoutParams(-1, 54).apply { bottomMargin = 5 })
        }
        root.addView(ScrollView(context).apply { addView(list) }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextView(context).apply {
            text = "Chọn ứng dụng để yêu cầu Android mở trên màn hình HDMI."
            textSize = 12f
            setTextColor(0xFF8FA3B8.toInt())
            setPadding(0, 12, 0, 0)
        })
        setContentView(root)
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    }
}
