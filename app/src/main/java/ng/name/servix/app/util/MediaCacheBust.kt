package ng.name.servix.app.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cache-bust stamps for uploaded media.
 *
 * An avatar is replaced at the same URL (the API's `PATCH /account/profile` stores a URL, not a new
 * object per upload), so Coil's memory and disk caches would happily keep serving the old portrait.
 * When an upload succeeds we record a stamp for that URL; every avatar composable reads
 * [versions] and keys its request with `?v=<stamp>` so the next load misses both caches.
 *
 * The value is only a *cache key* — the request itself still goes to the real URL, so a stamp never
 * changes what the server sees.
 */
object MediaCacheBust {
    private val _versions = MutableStateFlow<Map<String, Long>>(emptyMap())
    val versions: StateFlow<Map<String, Long>> = _versions.asStateFlow()

    /** Record that [url] now points at new bytes. Safe to call with null/blank. */
    fun bump(url: String?, at: Long = System.currentTimeMillis()) {
        val key = url?.trim().orEmpty()
        if (key.isEmpty()) return
        _versions.value = _versions.value.toMutableMap().apply { put(key, at) }
    }

    fun versionOf(url: String?): Long? = _versions.value[url?.trim().orEmpty()]

    /** The key Coil should use for [resolvedUrl], with `?v=` appended when a stamp exists. */
    fun cacheKey(resolvedUrl: String, version: Long?): String =
        if (version == null) resolvedUrl else "$resolvedUrl?v=$version"

    /** Test hook: forget every stamp. */
    fun clear() {
        _versions.value = emptyMap()
    }
}
