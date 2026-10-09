package com.nuvio.app.features.rzflix

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Offline contract tests: no request is made to an undocumented RzFlix service. */
class RzFlixNativeBackendTest {
    @Test
    fun catalogHandlesObjectEnvelopeAndTypeAliases() {
        val items = RzFlixPayloadMapper.catalog(
            """{"data":{"items":[{"id":"tt001","title":"First","type":"movie","poster":"https://cdn.example.org/p.jpg"},{"_id":"tv02","name":"Second","media_type":"tv"},{"id":"tt001","title":"First"}]}}"""
        )
        assertEquals(2, items.size)
        assertEquals("First", items[0].name)
        assertEquals("series", items[1].type)
    }

    @Test
    fun detailsMapsEpisodeMetadata() {
        val details = RzFlixPayloadMapper.details(
            """{"meta":{"id":"show1","title":"Series","type":"tv","episodes":[{"id":"ep1","title":"Pilot","season":1,"episode":1}]}}"""
        )
        assertEquals("series", details?.type)
        assertEquals("Pilot", details?.videos?.single()?.title)
        assertEquals(1, details?.videos?.single()?.episode)
        assertNull(RzFlixPayloadMapper.details("""{"error":"not_found"}"""))
    }

    @Test
    fun streamsRejectsInsecureLinksAndDoesNotExecuteAdPages() {
        val streams = RzFlixPayloadMapper.streams(
            """{"sources":[{"url":"https://media.example.org/stream.m3u8","quality":"1080p","subtitles":[{"url":"https://media.example.org/captions.vtt","lang":"en"}]},{"url":"http://insecure.example.org/ad"},{"url":"javascript:alert(1)"},{"externalUrl":"https://advertiser.example.org/popup"}]}"""
        )
        assertEquals(1, streams.size)
        assertEquals("1080p", streams.single().name)
        assertEquals(1, streams.single().externalSubtitles.size)
        assertEquals("rzflix:native", streams.single().addonId)
    }

    @Test
    fun validatesTransportUrls() {
        assertFailsWith<IllegalArgumentException> { requireHttpsUrl("http://foo.example.org/") }
        assertFailsWith<IllegalArgumentException> { requireHttpsUrl("https://user:secret@foo.example.org") }
        assertEquals("https://media.example.org/m.m3u8", requireHttpsUrl("https://media.example.org/m.m3u8"))
    }

    @Test
    fun discoveryRecognizesConfigurationButDoesNotInventCatalogRoutes() = runBlocking {
        val client = RzFlixNativeBackend { """{"data":{"base_url":"https://api.example.org/v1/"}}""" }
        val result = client.discover("https://config.example.org/config.txt")
        assertIs<RzFlixConfiguration.Endpoint>(result)
        assertEquals("https://api.example.org/v1/", result.url)
        val unknown = RzFlixNativeBackend { """{"banner":"Hello"}""" }
            .discover("https://config.example.org/config.txt")
        assertIs<RzFlixConfiguration.Unknown>(unknown)
        assertTrue(unknown.reason.isNotBlank())
    }
}
