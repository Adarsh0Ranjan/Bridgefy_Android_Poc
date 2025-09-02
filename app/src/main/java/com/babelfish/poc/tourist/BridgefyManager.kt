// In BridgefyManager.kt
package com.babelfish.poc.tourist

import android.content.Context
import android.util.Log
import me.bridgefy.Bridgefy
import me.bridgefy.commons.TransmissionMode
import me.bridgefy.commons.exception.BridgefyException
import me.bridgefy.commons.listener.BridgefyDelegate
import me.bridgefy.commons.peer.BridgefyPeer
import me.bridgefy.logger.enums.LogType
import org.json.JSONObject
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BridgefyState(val description: String) {
    STOPPED("Stopped"),
    STARTING("Starting..."),
    STARTED("Ready"),
    FAILED("Failed")
}

class BridgefyManager(private val context: Context) : BridgefyDelegate {

    private val _lastReceivedMessage = MutableStateFlow("Waiting for a message...")
    val lastReceivedMessage = _lastReceivedMessage.asStateFlow()

    private val _state = MutableStateFlow(BridgefyState.STOPPED)
    val state = _state.asStateFlow()

    private val tourID = "POC_TOUR_01"
    private val apiKey = "e947ba04-9751-4b59-aab1-576f4f4776be"

    private var bridgefy: Bridgefy? = null
    private var currentUserID: UUID? = null

    init {
        bridgefy = Bridgefy(context)
        try {
            bridgefy?.init(UUID.fromString(apiKey), this, LogType.ConsoleLogger(Log.DEBUG))
        } catch (e: Exception) {
            println("Error initializing Bridgefy: ${e.message}")
            _state.value = BridgefyState.FAILED
        }
    }

    fun start() {
        _state.value = BridgefyState.STARTING
        bridgefy?.start()
    }

    fun send(messageText: String) {
        val senderId = this.currentUserID ?: run {
            println("Cannot send message: Bridgefy not started or user ID not available.")
            return
        }

        try {
            val payload = JSONObject()
            payload.put("tourID", tourID)
            payload.put("text", messageText)
            val data = payload.toString().toByteArray(Charsets.UTF_8)

            val transmissionMode = TransmissionMode.Broadcast(sender = senderId)

            bridgefy?.send(data, transmissionMode)
            println("Successfully sent: $messageText")
        } catch (e: Exception) {
            println("Error creating or sending message: ${e.message}")
        }
    }


    override fun onStarted(userId: UUID) {
        println("Bridgefy started successfully with User ID: $userId")
        this.currentUserID = userId
        _state.value = BridgefyState.STARTED
    }

    override fun onConnected(peerID: UUID) {
        TODO("Not yet implemented")
    }

    override fun onConnectedPeers(connectedPeers: List<UUID>) {
        TODO("Not yet implemented")
    }

    override fun onReceiveData(data: ByteArray, messageID: UUID, transmissionMode: TransmissionMode) {
        when (transmissionMode) {
            is TransmissionMode.Broadcast -> {
                try {
                    val jsonString = String(data, Charsets.UTF_8)
                    val json = JSONObject(jsonString)
                    val incomingTourID = json.getString("tourID")
                    val text = json.getString("text")

                    if (incomingTourID == tourID) {
                        println("Received valid broadcast message: $text")
                        _lastReceivedMessage.value = text
                    }
                } catch (e: Exception) {
                    println("Error decoding received data: ${e.message}")
                }
            }
            else -> { /* Ignore other modes */ }
        }
    }

    override fun onStopped() {
        println("Bridgefy stopped.")
    }

    override fun onFailToStop(error: BridgefyException) {
        println("Bridgefy failed to stop: ${error.message}")
    }

    override fun onDestroySession() {
        println("Bridgefy session destroyed.")
    }

    override fun onDisconnected(peerID: UUID) {
        TODO("Not yet implemented")
    }

    override fun onFailToDestroySession(error: BridgefyException) {
        println("Bridgefy session failed to destroy: ${error.message}")
    }



    override fun onSend(messageID: UUID) {
        println("Successfully sent message with ID: $messageID")
    }

    override fun onFailToSend(messageID: UUID, error: BridgefyException) {
        println("Failed to send message with ID: $messageID, error: ${error.message}")
    }

    override fun onFailToStart(error: BridgefyException) {
        TODO("Not yet implemented")
    }

    override fun onProgressOfSend(messageID: UUID, position: Int, size: Int) {
        // This is for large file transfers, we can ignore it for this PoC
    }

    override fun onEstablishSecureConnection(userId: UUID) {
        println("Secure connection established with: $userId")
    }

    override fun onFailToEstablishSecureConnection(userId: UUID, error: BridgefyException) {
        println("Failed to establish secure connection with $userId: ${error.message}")
    }
}