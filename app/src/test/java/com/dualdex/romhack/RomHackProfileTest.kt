package com.dualdex.romhack

import org.junit.Assert.*
import org.junit.Test

class RomHackProfileTest {

    private val ghostGreyJson = """
        {
          "id": "ghost_grey",
          "name": "Pokemon Ghost Grey",
          "baseGame": "FireRed",
          "gameId": 6,
          "developer": "JoeyZeed",
          "engine": "HexManiacAdvance",
          "hasEvs": false,
          "hasIvs": false,
          "hasPhysSpecSplit": true,
          "steelResistsGhostDark": true,
          "cfruOffsets": false,
          "playerPartyOffset": 33702532,
          "enemyPartyOffset": 33701932,
          "docsUrl": null,
          "headerTitles": ["BPRE", "GHOSTGREY"],
          "sha256Hashes": ["abcdef1234567890"],
          "customSpecies": {
            "500": { "name": "Lichtoise", "type1": "Water", "type2": "Ghost", "hp": 79, "atk": 63, "def": 100, "spa": 85, "spd": 105, "spe": 78 }
          }
        }
    """.trimIndent()

    private val radicalRedJson = """
        {
          "id": "radical_red",
          "name": "Pokemon Radical Red",
          "baseGame": "FireRed",
          "gameId": 5,
          "engine": "CFRU",
          "hasEvs": true,
          "hasIvs": true,
          "hasPhysSpecSplit": true,
          "steelResistsGhostDark": false,
          "cfruOffsets": true,
          "docsUrl": "https://dex.radicalred.net",
          "headerTitles": ["RADICALRED"]
        }
    """.trimIndent()

    @Test
    fun testParseGhostGreyProfile() {
        val profile = ProfileLoader.parseProfile(ghostGreyJson)
        assertEquals("ghost_grey", profile.id)
        assertEquals("Pokemon Ghost Grey", profile.name)
        assertEquals("FireRed", profile.baseGame)
        assertEquals(6, profile.gameId)
        assertFalse(profile.hasEvs)
        assertFalse(profile.hasIvs)
        assertTrue(profile.hasPhysSpecSplit)
        assertTrue(profile.steelResistsGhostDark)
        assertFalse(profile.cfruOffsets)

        assertTrue(profile.customSpecies.containsKey(500))
        val lichtoise = profile.customSpecies[500]!!
        assertEquals("Lichtoise", lichtoise.name)
        assertEquals("Water", lichtoise.type1)
        assertEquals("Ghost", lichtoise.type2)
        assertEquals(79, lichtoise.hp)
    }

    @Test
    fun testParseRadicalRedProfile() {
        val profile = ProfileLoader.parseProfile(radicalRedJson)
        assertEquals("radical_red", profile.id)
        assertEquals("CFRU", profile.engine)
        assertTrue(profile.hasEvs)
        assertTrue(profile.hasIvs)
        assertTrue(profile.cfruOffsets)
        assertEquals("https://dex.radicalred.net", profile.docsUrl)
    }

    private val heartAndSoulJson = """
        {
          "id": "heart_and_soul",
          "name": "Pokemon Heart & Soul",
          "baseGame": "Emerald",
          "gameId": 1,
          "developer": "Lil Dill / PokemonHnS-Development",
          "engine": "pokeemerald-expansion",
          "hasEvs": true,
          "hasIvs": true,
          "hasPhysSpecSplit": true,
          "steelResistsGhostDark": false,
          "cfruOffsets": false,
          "playerPartyOffset": 33703148,
          "enemyPartyOffset": 33703748,
          "docsUrl": "https://pokemonhns-development.github.io/pokehns-expansion-documentation/",
          "headerTitles": ["HEARTSOUL", "HNS", "POKEHNS"]
        }
    """.trimIndent()

    @Test
    fun testParseHeartAndSoulProfile() {
        val profile = ProfileLoader.parseProfile(heartAndSoulJson)
        assertEquals("heart_and_soul", profile.id)
        assertEquals("Pokemon Heart & Soul", profile.name)
        assertEquals("Emerald", profile.baseGame)
        assertEquals(1, profile.gameId)
        assertTrue(profile.hasEvs)
        assertTrue(profile.hasIvs)
        assertTrue(profile.hasPhysSpecSplit)
        assertFalse(profile.steelResistsGhostDark)
        assertEquals("pokeemerald-expansion", profile.engine)
        assertEquals("https://pokemonhns-development.github.io/pokehns-expansion-documentation/", profile.docsUrl)
    }

    @Test
    fun testRomHackDetectorByHeaderTitle() {
        val profiles = listOf(
            ProfileLoader.parseProfile(ghostGreyJson),
            ProfileLoader.parseProfile(radicalRedJson),
            RomHackProfile.DEFAULT_FIRERED
        )

        // Mock Ghost Grey header (offset 160: "GHOSTGREY\u0000\u0000\u0000")
        val ghostGreyHeader = ByteArray(192)
        val ggBytes = "GHOSTGREY".toByteArray(Charsets.US_ASCII)
        System.arraycopy(ggBytes, 0, ghostGreyHeader, 160, ggBytes.size)

        val detectedGG = RomHackDetector.detectProfileFromBytes(ghostGreyHeader, profiles = profiles)
        assertEquals("ghost_grey", detectedGG.id)

        // Mock Radical Red header (offset 160: "RADICALRED\u0000\u0000")
        val radRedHeader = ByteArray(192)
        val rrBytes = "RADICALRED".toByteArray(Charsets.US_ASCII)
        System.arraycopy(rrBytes, 0, radRedHeader, 160, rrBytes.size)

        val detectedRR = RomHackDetector.detectProfileFromBytes(radRedHeader, profiles = profiles)
        assertEquals("radical_red", detectedRR.id)

        // Mock Vanilla FireRed header (offset 160: "POKEMON FIRE")
        val fireRedHeader = ByteArray(192)
        val frBytes = "POKEMON FIRE".toByteArray(Charsets.US_ASCII)
        System.arraycopy(frBytes, 0, fireRedHeader, 160, frBytes.size)

        val detectedFR = RomHackDetector.detectProfileFromBytes(fireRedHeader, profiles = profiles)
        assertEquals("vanilla_firered", detectedFR.id)
    }

    @Test
    fun testRomHackDetectorBySha256() {
        val profiles = listOf(
            ProfileLoader.parseProfile(ghostGreyJson),
            RomHackProfile.DEFAULT_FIRERED
        )

        val dummyHeader = ByteArray(192)
        val detected = RomHackDetector.detectProfileFromBytes(dummyHeader, sha256 = "abcdef1234567890", profiles = profiles)
        assertEquals("ghost_grey", detected.id)
    }
}
