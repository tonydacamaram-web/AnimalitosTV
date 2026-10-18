package com.animalitostv.domain.model

data class Animal(
    val numero: Int,
    val nombre: String
)

/**
 * Catálogo de animales estándar (38 figuras: 00 y 0-36 + 37).
 * Usado por: Lotto Activo, La Granjita, Lotto Rey.
 */
val ANIMALES_ESTANDAR: List<Animal> = listOf(
    Animal(-1, "Ballena"),  // "00" en los sitios web; -1 como clave interna para no chocar con 0 (Delfín)
    Animal(0, "Delfín"),
    Animal(1, "Carnero"),
    Animal(2, "Toro"),
    Animal(3, "Ciempiés"),
    Animal(4, "Alacrán"),
    Animal(5, "León"),
    Animal(6, "Rana"),
    Animal(7, "Perico"),
    Animal(8, "Ratón"),
    Animal(9, "Águila"),
    Animal(10, "Tigre"),
    Animal(11, "Gato"),
    Animal(12, "Caballo"),
    Animal(13, "Mono"),
    Animal(14, "Paloma"),
    Animal(15, "Zorro"),
    Animal(16, "Oso"),
    Animal(17, "Pavo"),
    Animal(18, "Burro"),
    Animal(19, "Chivo"),
    Animal(20, "Cochino"),
    Animal(21, "Gallo"),
    Animal(22, "Camello"),
    Animal(23, "Cebra"),
    Animal(24, "Iguana"),
    Animal(25, "Gallina"),
    Animal(26, "Vaca"),
    Animal(27, "Perro"),
    Animal(28, "Zamuro"),
    Animal(29, "Elefante"),
    Animal(30, "Caimán"),
    Animal(31, "Lapa"),
    Animal(32, "Ardilla"),
    Animal(33, "Pescado"),
    Animal(34, "Venado"),
    Animal(35, "Jirafa"),
    Animal(36, "Culebra"),
    Animal(37, "Tortuga")
)

/**
 * Catálogo extendido (77 figuras: 00 y 0-75).
 * Usado por: Guacharo Activo, Guacharito Millonario.
 */
val ANIMALES_EXTENDIDOS: List<Animal> = ANIMALES_ESTANDAR + listOf(
    Animal(38, "Búfalo"),
    Animal(39, "Lechuza"),
    Animal(40, "Avispa"),
    Animal(41, "Canguro"),
    Animal(42, "Tucán"),
    Animal(43, "Mariposa"),
    Animal(44, "Chigüire"),
    Animal(45, "Garza"),
    Animal(46, "Puma"),
    Animal(47, "Pavo Real"),
    Animal(48, "Puercoespín"),
    Animal(49, "Pereza"),
    Animal(50, "Canario"),
    Animal(51, "Pelícano"),
    Animal(52, "Pulpo"),
    Animal(53, "Caracol"),
    Animal(54, "Grillo"),
    Animal(55, "Oso Hormiguero"),
    Animal(56, "Tiburón"),
    Animal(57, "Pato"),
    Animal(58, "Hormiga"),
    Animal(59, "Pantera"),
    Animal(60, "Camaleón"),
    Animal(61, "Panda"),
    Animal(62, "Cachicamo"),
    Animal(63, "Cangrejo"),
    Animal(64, "Gavilán"),
    Animal(65, "Araña"),
    Animal(66, "Lobo"),
    Animal(67, "Avestruz"),
    Animal(68, "Jaguar"),
    Animal(69, "Conejo"),
    Animal(70, "Bisonte"),
    Animal(71, "Guacamaya"),
    Animal(72, "Gorila"),
    Animal(73, "Hipopótamo"),
    Animal(74, "Turpial"),
    Animal(75, "Guácharo")
)

/**
 * Catálogo Guacharito Millonario / Selva Plus (101 figuras: 00 y 0-99).
 * Incluye las 77 anteriores (00 + 0-75) + 24 adicionales (76-99) = 101 figuras.
 * Nota: el número 100 NO existe — "0" es Delfín y "00" es Ballena.
 */
val ANIMALES_GUACHARITO: List<Animal> = ANIMALES_EXTENDIDOS + listOf(
    Animal(76, "Rinoceronte"),
    Animal(77, "Pingüino"),
    Animal(78, "Antílope"),
    Animal(79, "Calamar"),
    Animal(80, "Murciélago"),
    Animal(81, "Cuervo"),
    Animal(82, "Cucaracha"),
    Animal(83, "Búho"),
    Animal(84, "Camarón"),
    Animal(85, "Hámster"),
    Animal(86, "Buey"),
    Animal(87, "Cabra"),
    Animal(88, "Erizo de Mar"),
    Animal(89, "Anguila"),
    Animal(90, "Hurón"),
    Animal(91, "Morrocoy"),
    Animal(92, "Cisne"),
    Animal(93, "Gaviota"),
    Animal(94, "Pajuil"),
    Animal(95, "Escarabajo"),
    Animal(96, "Caballito de Mar"),
    Animal(97, "Loro"),
    Animal(98, "Cocodrilo"),
    Animal(99, "Guacharito")
)

/** Devuelve el nombre del animal por número para la lotería indicada. */
fun nombreAnimal(numero: Int, esExtendida: Boolean): String {
    val catalogo = if (esExtendida) ANIMALES_EXTENDIDOS else ANIMALES_ESTANDAR
    return catalogo.firstOrNull { it.numero == numero }?.nombre ?: "Animal $numero"
}

/** Devuelve el nombre del animal usando el catálogo específico de cada lotería. */
fun nombreAnimalPorLoteria(numero: Int, loteria: com.animalitostv.domain.model.Loteria): String {
    val catalogo = when (loteria) {
        com.animalitostv.domain.model.Loteria.GUACHARITO_MILLONARIO -> ANIMALES_GUACHARITO
        com.animalitostv.domain.model.Loteria.SELVA_PLUS -> ANIMALES_GUACHARITO
        com.animalitostv.domain.model.Loteria.GUACHARO_ACTIVO -> ANIMALES_EXTENDIDOS
        else -> ANIMALES_ESTANDAR
    }
    return catalogo.firstOrNull { it.numero == numero }?.nombre ?: "Animal $numero"
}
