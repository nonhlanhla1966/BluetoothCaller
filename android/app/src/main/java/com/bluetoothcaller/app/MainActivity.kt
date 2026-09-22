package com.bluetoothcaller.app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bluetoothcaller.app.bluetooth.BluetoothCallService

class MainActivity : AppCompatActivity(), BluetoothCallService.Listener {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var callService: BluetoothCallService
    private lateinit var statusText: TextView
    private lateinit var deviceAdapter: ArrayAdapter<String>

    private val devices = mutableListOf<BluetoothDevice>()
    private val deviceNames = mutableListOf<String>()

    private var incomingDevice: BluetoothDevice? = null

    companion object {
        private const val REQUEST_PERMISSIONS = 1001

        private const val CALL_REQUEST = "CALL_REQUEST"
        private const val CALL_ACCEPT = "CALL_ACCEPT"
        private const val CALL_REJECT = "CALL_REJECT"
        private const val CALL_END = "CALL_END"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bluetoothManager =
            getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

        bluetoothAdapter = bluetoothManager.adapter

        callService = BluetoothCallService(
            bluetoothAdapter,
            this
        )

        buildInterface()
        requestPermissionsIfNeeded()

        if (hasBluetoothConnectPermission()) {
            callService.startServer()
            loadPairedDevices()
        }
    }

    private fun buildInterface() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val title = TextView(this).apply {
            text = "Bluetooth Caller"
            textSize = 26f
            setPadding(0, 0, 0, 12)
        }

        statusText = TextView(this).apply {
            text = if (bluetoothAdapter.isEnabled) {
                "Ready — Bluetooth is ON"
            } else {
                "Bluetooth is OFF"
            }
            textSize = 16f
            setPadding(0, 0, 0, 16)
        }

        val scanButton = Button(this).apply {
            text = "Refresh Paired Phones"
        }

        val disconnectButton = Button(this).apply {
            text = "End Call / Disconnect"
        }

        val acceptButton = Button(this).apply {
            text = "Accept Call"
            visibility = View.GONE
        }

        val rejectButton = Button(this).apply {
            text = "Reject Call"
            visibility = View.GONE
        }

        val listView = ListView(this)

        deviceAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            deviceNames
        )

        listView.adapter = deviceAdapter

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(scanButton)
        layout.addView(acceptButton)
        layout.addView(rejectButton)
        layout.addView(disconnectButton)
        layout.addView(listView)

        setContentView(layout)

        scanButton.setOnClickListener {
            loadPairedDevices()
        }

        disconnectButton.setOnClickListener {
            callService.sendMessage(CALL_END)
            callService.disconnect()

            incomingDevice = null

            acceptButton.visibility = View.GONE
            rejectButton.visibility = View.GONE

            statusText.text = "Disconnected"
        }

        acceptButton.setOnClickListener {
            callService.sendMessage(CALL_ACCEPT)

            incomingDevice = null

            acceptButton.visibility = View.GONE
            rejectButton.visibility = View.GONE

            statusText.text = "Call connected"
        }

        rejectButton.setOnClickListener {
            callService.sendMessage(CALL_REJECT)
            callService.disconnect()

            incomingDevice = null

            acceptButton.visibility = View.GONE
            rejectButton.visibility = View.GONE

            statusText.text = "Call rejected"
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            if (position >= devices.size) return@setOnItemClickListener

            val device = devices[position]

            statusText.text = "Connecting..."

            callService.connect(device)
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                REQUEST_PERMISSIONS
            )
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun loadPairedDevices() {
        if (!hasBluetoothConnectPermission()) {
            Toast.makeText(
                this,
                "Bluetooth permission is required",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        devices.clear()
        deviceNames.clear()

        try {
            for (device in bluetoothAdapter.bondedDevices) {
                devices.add(device)

                val name = try {
                    device.name ?: "Unnamed phone"
                } catch (_: SecurityException) {
                    "Bluetooth device"
                }

                deviceNames.add(name)
            }

            deviceAdapter.notifyDataSetChanged()

            statusText.text =
                "${devices.size} paired phone(s) found"

        } catch (_: SecurityException) {
            statusText.text = "Bluetooth permission required"
        }
    }

    override fun onConnected(device: BluetoothDevice) {
        runOnUiThread {
            val name = try {
                device.name ?: "Phone"
            } catch (_: SecurityException) {
                "Phone"
            }

            statusText.text = "Connected to $name"

            callService.sendMessage(CALL_REQUEST)
        }
    }

    override fun onDisconnected(device: BluetoothDevice) {
        runOnUiThread {
            statusText.text = "Disconnected"
        }
    }

    override fun onMessage(message: String) {
        runOnUiThread {
            when (message) {

                CALL_REQUEST -> {
                    incomingDevice =
                        callService.getConnectedDevice()

                    statusText.text = "Incoming call"

                    Toast.makeText(
                        this,
                        "Incoming Bluetooth call",
                        Toast.LENGTH_LONG
                    ).show()

                    val acceptButton =
                        findButton("Accept Call")

                    val rejectButton =
                        findButton("Reject Call")

                    acceptButton?.visibility = View.VISIBLE
                    rejectButton?.visibility = View.VISIBLE
                }

                CALL_ACCEPT -> {
                    statusText.text = "Call connected"
                }

                CALL_REJECT -> {
                    statusText.text = "Call rejected"
                    callService.disconnect()
                }

                CALL_END -> {
                    statusText.text = "Call ended"
                    callService.disconnect()
                }

                else -> {
                    statusText.text = message
                }
            }
        }
    }

    private fun findButton(text: String): Button? {
        return findButtonRecursive(
            window.decorView,
            text
        )
    }

    private fun findButtonRecursive(
        view: View,
        text: String
    ): Button? {

        if (view is Button && view.text == text) {
            return view
        }

        if (view is android.view.ViewGroup) {
            for (index in 0 until view.childCount) {
                val result =
                    findButtonRecursive(
                        view.getChildAt(index),
                        text
                    )

                if (result != null) {
                    return result
                }
            }
        }

        return null
    }

    override fun onError(message: String) {
        runOnUiThread {
            statusText.text = message

            Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        callService.stopServer()
        super.onDestroy()
    }
}
