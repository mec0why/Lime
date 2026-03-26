package mec0why.lime.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mec0why.lime.data.model.ChannelResponse

class ChannelHydrator(
    private val repository: KickRepository,
    context: Context
) {
    private val cache = mutableMapOf<String, ChannelResponse>()
    private val pendingRequests = mutableMapOf<String, Deferred<ChannelResponse?>>()
    private val mutex = Mutex()
    private val prefs: SharedPreferences = context.getSharedPreferences("lime_channel_meta", Context.MODE_PRIVATE)

    fun getImmediate(slug: String): Triple<String?, Boolean, Boolean> {
        cache[slug]?.let { return Triple(it.user?.username, it.verified, true) }

        val username = prefs.getString("username_$slug", null)
        val verified = prefs.getBoolean("verified_$slug", false)
        val hasData = prefs.contains("username_$slug")
        
        return Triple(username, verified, hasData)
    }

    suspend fun hydrate(slug: String): ChannelResponse? {
        var existing: Deferred<ChannelResponse?>? = null
        var isHydrator = false
        val deferred = CompletableDeferred<ChannelResponse?>()

        mutex.withLock {
            cache[slug]?.let { return it }
            
            existing = pendingRequests[slug]
            if (existing == null) {
                pendingRequests[slug] = deferred
                isHydrator = true
            }
        }

        if (!isHydrator) {
            return existing!!.await()
        }

        try {
            val result = repository.getChannel(slug).getOrNull()
            if (result != null) {
                mutex.withLock {
                    cache[slug] = result
                }
                prefs.edit {
                    putString("username_$slug", result.user?.username)
                    putBoolean("verified_$slug", result.verified)
                }
            }
            mutex.withLock {
                pendingRequests.remove(slug)
            }
            deferred.complete(result)
            return result
        } catch (e: Exception) {
            mutex.withLock {
                pendingRequests.remove(slug)
            }
            deferred.complete(null)
            return null
        }
    }
}
