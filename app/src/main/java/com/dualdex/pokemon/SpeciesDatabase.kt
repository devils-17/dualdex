package com.dualdex.pokemon

data class SpeciesInfo(
    val id: Int,
    val name: String,
    val type1: PokemonType,
    val type2: PokemonType? = null,
    val baseHP: Int = 70,
    val baseAtk: Int = 70,
    val baseDef: Int = 70,
    val baseSpA: Int = 70,
    val baseSpD: Int = 70,
    val baseSpe: Int = 70
)

object SpeciesDatabase {

    private val speciesMap = mutableMapOf<Int, SpeciesInfo>()
    private val nameMap = mutableMapOf<String, SpeciesInfo>()

    private fun register(id: Int, name: String, t1: PokemonType, t2: PokemonType? = null,
                         hp: Int = 70, atk: Int = 70, def: Int = 70, spa: Int = 70, spd: Int = 70, spe: Int = 70) {
        val info = SpeciesInfo(id, name, t1, t2, hp, atk, def, spa, spd, spe)
        speciesMap[id] = info
        nameMap[name.lowercase()] = info
    }

    init {
        // Gen 1 Starters & Key Pokemon
        register(1, "Bulbasaur", PokemonType.GRASS, PokemonType.POISON, 45, 49, 49, 65, 65, 45)
        register(2, "Ivysaur", PokemonType.GRASS, PokemonType.POISON, 60, 62, 63, 80, 80, 60)
        register(3, "Venusaur", PokemonType.GRASS, PokemonType.POISON, 80, 82, 83, 100, 100, 80)
        register(4, "Charmander", PokemonType.FIRE, null, 39, 52, 43, 60, 50, 65)
        register(5, "Charmeleon", PokemonType.FIRE, null, 58, 64, 58, 80, 65, 80)
        register(6, "Charizard", PokemonType.FIRE, PokemonType.FLYING, 78, 84, 78, 109, 85, 100)
        register(7, "Squirtle", PokemonType.WATER, null, 44, 48, 65, 50, 64, 43)
        register(8, "Wartortle", PokemonType.WATER, null, 59, 63, 80, 65, 80, 58)
        register(9, "Blastoise", PokemonType.WATER, null, 79, 83, 100, 85, 105, 78)
        register(25, "Pikachu", PokemonType.ELECTRIC, null, 35, 55, 40, 50, 50, 90)
        register(26, "Raichu", PokemonType.ELECTRIC, null, 60, 90, 55, 90, 80, 110)
        register(94, "Gengar", PokemonType.GHOST, PokemonType.POISON, 60, 65, 60, 130, 75, 110)
        register(130, "Gyarados", PokemonType.WATER, PokemonType.FLYING, 95, 125, 79, 60, 100, 81)
        register(131, "Lapras", PokemonType.WATER, PokemonType.ICE, 130, 85, 80, 85, 95, 60)
        register(143, "Snorlax", PokemonType.NORMAL, null, 160, 110, 65, 65, 110, 30)
        register(149, "Dragonite", PokemonType.DRAGON, PokemonType.FLYING, 91, 134, 95, 100, 100, 80)
        register(150, "Mewtwo", PokemonType.PSYCHIC, null, 106, 110, 90, 154, 90, 130)

        // Gen 3 Starters & Key Pokemon
        register(252, "Treecko", PokemonType.GRASS, null, 40, 45, 35, 65, 55, 70)
        register(253, "Grovyle", PokemonType.GRASS, null, 50, 65, 45, 85, 65, 95)
        register(254, "Sceptile", PokemonType.GRASS, null, 70, 85, 65, 105, 85, 120)
        register(255, "Torchic", PokemonType.FIRE, null, 45, 60, 40, 70, 50, 45)
        register(256, "Combusken", PokemonType.FIRE, PokemonType.FIGHTING, 60, 85, 60, 85, 60, 55)
        register(257, "Blaziken", PokemonType.FIRE, PokemonType.FIGHTING, 80, 120, 70, 110, 70, 80)
        register(258, "Mudkip", PokemonType.WATER, null, 50, 70, 50, 50, 50, 40)
        register(259, "Marshtomp", PokemonType.WATER, PokemonType.GROUND, 70, 85, 70, 60, 70, 50)
        register(260, "Swampert", PokemonType.WATER, PokemonType.GROUND, 100, 110, 90, 85, 90, 60)
        register(282, "Gardevoir", PokemonType.PSYCHIC, PokemonType.FAIRY, 68, 65, 65, 125, 115, 80)
        register(302, "Sableye", PokemonType.DARK, PokemonType.GHOST, 50, 75, 75, 65, 65, 50)
        register(306, "Aggron", PokemonType.STEEL, PokemonType.ROCK, 70, 110, 180, 60, 60, 50)
        register(330, "Flygon", PokemonType.GROUND, PokemonType.DRAGON, 80, 100, 80, 80, 80, 100)
        register(373, "Salamence", PokemonType.DRAGON, PokemonType.FLYING, 95, 135, 80, 110, 80, 100)
        register(376, "Metagross", PokemonType.STEEL, PokemonType.PSYCHIC, 80, 135, 130, 95, 90, 70)
        register(384, "Rayquaza", PokemonType.DRAGON, PokemonType.FLYING, 105, 150, 90, 150, 90, 95)

        // Ghost Grey Custom / Regional Variants
        register(500, "Lichtoise", PokemonType.WATER, PokemonType.GHOST, 79, 63, 100, 85, 105, 78)
        register(501, "Spectrasaur", PokemonType.GRASS, PokemonType.GHOST, 80, 82, 83, 100, 100, 80)
        register(502, "Phantomander", PokemonType.FIRE, PokemonType.GHOST, 78, 84, 78, 109, 85, 100)
    }

    fun get(speciesId: Int): SpeciesInfo {
        return speciesMap[speciesId] ?: SpeciesInfo(
            id = speciesId,
            name = "Pokemon #$speciesId",
            type1 = PokemonType.NORMAL,
            type2 = null
        )
    }

    fun getByName(name: String): SpeciesInfo? {
        return nameMap[name.lowercase()]
    }
}
