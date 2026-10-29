package app.actioncam.data

import app.actioncam.model.CamInfo
import app.actioncam.model.Device
import app.actioncam.platform.localIpv4Addresses
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.readString

/**
 * Finds cameras on the current network two ways at once:
 * a UDP broadcast that camd answers, and a sweep of the local /24 on the API port
 * (for networks that drop broadcasts, like some phone hotspots).
 */
class Discovery(private val api: CamApi) {

    suspend fun scan(timeoutMs: Long = 2_500): List<Device> = coroutineScope {
        val found = mutableMapOf<String, Device>()
        val subnets = localIpv4Addresses().filter(::isPrivate).map { it.substringBeforeLast('.') }.distinct()
        val broadcast = async { broadcast(subnets, timeoutMs) }
        val sweep = async { sweep(subnets) }
        (broadcast.await() + sweep.await()).forEach { found.getOrPut(it.key) { it } }
        found.values.sortedBy { it.name.lowercase() }
    }

    private suspend fun broadcast(subnets: List<String>, timeoutMs: Long): List<Device> {
        val out = mutableListOf<Device>()
        val selector = SelectorManager(Dispatchers.Default)
        try {
            aSocket(selector).udp().bind(InetSocketAddress("0.0.0.0", 0)) { broadcast = true }.use { socket ->
                val targets = listOf("255.255.255.255") + subnets.map { "$it.255" }
                for (t in targets) runCatching {
                    socket.send(Datagram(buildPacket { writeText("ACTIONCAM?") }, InetSocketAddress(t, Device.DISCOVERY_PORT)))
                }
                withTimeoutOrNull(timeoutMs) {
                    while (true) {
                        val d = socket.receive()
                        val host = (d.address as? InetSocketAddress)?.resolveAddress()
                            ?.joinToString(".") { (it.toInt() and 0xff).toString() } ?: continue
                        val info = runCatching { json.decodeFromString<CamInfo>(d.packet.readString()) }.getOrNull() ?: continue
                        out += Device(host, info.port, info.name.ifBlank { host }, online = true, recording = info.recording)
                    }
                }
            }
        } catch (_: Exception) {
            // no network or broadcast not allowed: the sweep still runs
        } finally {
            selector.close()
        }
        return out
    }

    private suspend fun sweep(subnets: List<String>): List<Device> = coroutineScope {
        val limit = Semaphore(48)
        subnets.flatMap { net ->
            (1..254).map { i ->
                async {
                    limit.withPermit {
                        val host = "$net.$i"
                        api.probe(host)?.let { Device(host, Device.DEFAULT_PORT, it.name.ifBlank { host }, online = true, recording = it.recording) }
                    }
                }
            }
        }.awaitAll().filterNotNull()
    }

    private fun isPrivate(ip: String): Boolean {
        val p = ip.split('.').mapNotNull { it.toIntOrNull() }
        if (p.size != 4) return false
        return p[0] == 10 || (p[0] == 192 && p[1] == 168) || (p[0] == 172 && p[1] in 16..31)
    }
}

/** Checks a list of known devices in parallel and returns them with fresh online/recording flags. */
suspend fun CamApi.refresh(devices: List<Device>): List<Device> = coroutineScope {
    devices.map { d ->
        async {
            val info = probe(d.host, d.port)
            d.copy(online = info != null, recording = info?.recording ?: false, name = info?.name?.ifBlank { null } ?: d.name)
        }
    }.awaitAll()
}
