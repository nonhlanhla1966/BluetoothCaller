package com.bluetoothcaller.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors

class BluetoothCallService(
    private val adapter: BluetoothAdapter,
    private val listener: Listener
) {

    companion object {
        private const val SERVICE_NAME = "BluetoothCaller"
        private val SERVICE_UUID =
            UUID.fromString("7f4a1000-9d3e-4b7c-a001-1b2c3d4e5f60")
    }

    interface Listener {
        fun onConnected(device: BluetoothDevice)
        fun onDisconnected(device: BluetoothDevice)
        fun onMessage(message: String)
        fun onError(message: String)
    }

    private val executor = Executors.newCachedThreadPool()

    private var socket: BluetoothSocket? = null
    private var connectedDevice: BluetoothDevice? = null
    private var serverSocket: BluetoothServerSocket? = null

    @SuppressLint("MissingPermission")
    fun startServer() {
        executor.execute {
            try {
                serverSocket =
                    adapter.listenUsingRfcommWithServiceRecord(
                        SERVICE_NAME,
                        SERVICE_UUID
                    )

                while (true) {
                    val incoming = serverSocket?.accept() ?: break

                    closeCurrentConnection()

                    socket = incoming
                    connectedDevice = incoming.remoteDevice

                    connectedDevice?.let {
                        listener.onConnected(it)
                    }

                    listen(incoming)
                }

            } catch (e: IOException) {
                listener.onError(
                    "Bluetooth server stopped: ${e.message ?: "connection closed"}"
                )
            } catch (e: SecurityException) {
                listener.onError("Bluetooth permission is required")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        executor.execute {
            try {
                adapter.cancelDiscovery()

                closeCurrentConnection()

                val newSocket =
                    device.createRfcommSocketToServiceRecord(SERVICE_UUID)

                newSocket.connect()

                socket = newSocket
                connectedDevice = device

                listener.onConnected(device)

                listen(newSocket)

            } catch (e: IOException) {
                closeCurrentConnection()

                listener.onError(
                    "Connection failed: ${e.message ?: "unknown error"}"
                )
            } catch (e: SecurityException) {
                listener.onError("Bluetooth permission is required")
            }
        }
    }

    private fun listen(bluetoothSocket: BluetoothSocket) {
        val buffer = ByteArray(4096)

        try {
            while (true) {
                val count = bluetoothSocket.inputStream.read(buffer)

                if (count <= 0) break

                val message =
                    String(buffer, 0, count, Charsets.UTF_8)

                listener.onMessage(message)
            }
        } catch (_: IOException) {
        } finally {
            val device = connectedDevice

            closeCurrentConnection()

            device?.let {
                listener.onDisconnected(it)
            }
        }
    }

    fun sendMessage(message: String) {
        executor.execute {
            try {
                socket?.outputStream?.write(
                    message.toByteArray(Charsets.UTF_8)
                )

                socket?.outputStream?.flush()

            } catch (_: IOException) {
                listener.onError("Unable to send message")
            }
        }
    }

    fun disconnect() {
        executor.execute {
            closeCurrentConnection()
        }
    }

    fun stopServer() {
        executor.execute {
            try {
                serverSocket?.close()
            } catch (_: IOException) {
            }

            serverSocket = null
            closeCurrentConnection()
        }
    }

    private fun closeCurrentConnection() {
        try {
            socket?.close()
        } catch (_: IOException) {
        }

        socket = null
        connectedDevice = null
    }
}
