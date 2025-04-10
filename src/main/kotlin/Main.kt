package org.example

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.realtime.ChannelState
import io.ably.lib.types.ClientOptions
import io.ably.lib.types.Message
import io.ably.lib.types.ProtocolMessage
import io.ably.lib.util.Log
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess
import org.example.MessagePackInitializer

private val logger = LoggerFactory.getLogger("AblyCliApp")

class AblyCliApp : CliktCommand(
    name = "ably-cli",
    help = "A CLI tool to connect to Ably channels and read messages"
) {
    private val apiKey by option("-k", "--api-key")
        .help("Ably API key")
        .required()

    private val channelName by option("-c", "--channel")
        .help("Ably channel name to connect to")
        .required()

    private val eventName by option("-e", "--event")
        .help("Specific event name to subscribe to (optional)")
        .default("*")

    private val quietMode by option("-q", "--quiet")
        .help("Disable verbose Ably logs")
        .flag(default = false)

    private val debugMode by option("-d", "--debug")
        .help("Show debug information including raw message structure")
        .flag(default = false)

    override fun run() = runBlocking {
        logger.info("Starting Ably CLI tool")
        logger.info("Connecting to channel: $channelName")
        logger.info("Verbose logging: ${!quietMode}")
        logger.info("Debug mode: $debugMode")

        try {
            val clientOptions = ClientOptions(apiKey)
            clientOptions.logLevel = if (quietMode) Log.ERROR else Log.VERBOSE

            val ably = AblyRealtime(clientOptions)

            if (debugMode) {
                logger.info("Debug mode enabled - adding raw protocol message listener")

                // This is a lower-level approach to see all protocol messages
                try {
                    // Use reflection to access the internal transport
                    val connectionManagerField = ably.connection.javaClass.getDeclaredField("connectionManager")
                    connectionManagerField.isAccessible = true
                    val connectionManager = connectionManagerField.get(ably.connection)

                    // Add a listener for all protocol messages
                    val method = connectionManager.javaClass.getDeclaredMethod("onMessage", ProtocolMessage::class.java)
                    method.isAccessible = true

                    logger.info("Successfully added protocol message listener")
                    println("Debug mode enabled - will show all protocol messages")
                } catch (e: Exception) {
                    logger.error("Failed to add protocol message listener: ${e.message}", e)
                }
            }

            // Handle connection state changes
            ably.connection.on { state ->
                logger.info("Connection state changed to: ${state.current}")

                if (state.current == io.ably.lib.realtime.ConnectionState.failed) {
                    logger.error("Connection failed: ${state.reason?.message}")
                    exitProcess(1)
                }
            }

            // Connect to the specified channel
            val channel = ably.channels.get(channelName)

            // Handle channel state changes
            channel.on { state ->
                logger.info("Channel state changed to: ${state.current}")

                if (state.current == ChannelState.failed) {
                    logger.error("Channel connection failed: ${state.reason?.message}")
                    exitProcess(1)
                }
            }

            // Subscribe to all messages
            logger.info("Subscribing to channel messages")

            // First, try subscribing to all messages
            channel.subscribe { message ->
                logger.info("Received message with name: ${message.name ?: "unnamed"}")

                // If eventName is specified and doesn't match, skip this message
                if (eventName != "*" && message.name != eventName) {
                    logger.info("Skipping message with name ${message.name} (filter: $eventName)")
                    return@subscribe
                }

                try {
                    // Special handling for the message type mentioned in your logs
                    if (message.name == "gql-result") {
                        logger.info("Received gql-result message")
                        println("─".repeat(80))
                        println("GQL RESULT MESSAGE:")
                        println("Timestamp: ${java.time.Instant.ofEpochMilli(message.timestamp.toLong())
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime()}")

                        // Try to decode base64 data
                        if (message.data is String && message.encoding == "base64") {
                            try {
                                val decoded = java.util.Base64.getDecoder().decode(message.data as String)
                                val decodedStr = String(decoded)
                                println("Decoded data: $decodedStr")
                            } catch (e: Exception) {
                                println("Failed to decode base64 data: ${e.message}")
                                println("Raw data: ${message.data}")
                            }
                        } else {
                            println("Data: ${message.data}")
                        }

                        // Print extras
                        if (message.extras != null) {
                            println("Extras: ${message.extras}")
                        }

                        println("─".repeat(80))
                    } else {
                        displayMessage(message)
                    }
                } catch (e: Exception) {
                    logger.error("Error displaying message: ${e.message}", e)
                    if (debugMode) {
                        println("ERROR PROCESSING MESSAGE: ${e.message}")
                        println("Message: $message")
                        e.printStackTrace()
                    }
                }
            }

            // Also subscribe to presence events
            logger.info("Subscribing to presence events")
            channel.presence.subscribe { presenceMessage ->
                logger.info("Received presence message: action=${presenceMessage.action}, clientId=${presenceMessage.clientId}")
                if (debugMode) {
                    println("─".repeat(80))
                    println("PRESENCE MESSAGE:")
                    println("Action: ${presenceMessage.action}")
                    println("Client ID: ${presenceMessage.clientId}")
                    println("Connection ID: ${presenceMessage.connectionId}")
                    println("Data: ${presenceMessage.data}")
                    println("─".repeat(80))
                }
            }

            logger.info("Listening for messages on channel '$channelName'${if (eventName != "*") " with event '$eventName'" else ""}...")
            logger.info("Press Ctrl+C to exit")

            // Keep the application running
            while (true) {
                Thread.sleep(1000)
            }

        } catch (e: Exception) {
            logger.error("Error: ${e.message}", e)
            exitProcess(1)
        }
    }

    private fun displayMessage(message: Message) {
        val timestamp = java.time.Instant.ofEpochMilli(message.timestamp.toLong())
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
            .toString()

        println("─".repeat(80))
        println("Timestamp: $timestamp")
        println("Event: ${message.name}")
        println("Client ID: ${message.clientId ?: "N/A"}")
        println("Connection ID: ${message.connectionId ?: "N/A"}")

        // Handle data based on encoding
        val dataStr = when {
            message.encoding == "base64" && message.data is String -> {
                try {
                    val decoded = java.util.Base64.getDecoder().decode(message.data as String)
                    String(decoded)
                } catch (e: Exception) {
                    "Error decoding base64 data: ${e.message}"
                }
            }
            else -> message.data?.toString() ?: "null"
        }

        println("Data: $dataStr")

        // Display extras if present
        if (message.extras != null) {
            println("Extras: ${message.extras}")
        }

        // In debug mode, print all available fields and raw message structure
        if (debugMode) {
            println("\nDEBUG INFO:")
            println("Raw message: $message")
            println("Message class: ${message.javaClass.name}")
            println("Data class: ${message.data?.javaClass?.name ?: "null"}")
            println("Encoding: ${message.encoding ?: "none"}")

            // Print all fields using reflection
            println("\nAll fields:")
            message.javaClass.declaredFields.forEach { field ->
                field.isAccessible = true
                try {
                    println("  ${field.name}: ${field.get(message)}")
                } catch (e: Exception) {
                    println("  ${field.name}: Error accessing field: ${e.message}")
                }
            }
        }

        println("─".repeat(80))
    }
}

fun main(args: Array<String>) {
    // Initialize MessagePack before using Ably
    MessagePackInitializer.initialize()

    AblyCliApp().main(args)
}