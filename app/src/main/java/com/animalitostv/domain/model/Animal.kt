package com.animalitostv.domain.model

data class Animal(
    val numero: Int,
    val nombre: String
)

/**
 * Catálogo de animales estándar (38 figuras: 00 y 0-36 + 37).
 * Usado por: Lotto Activo, La Granjita, Lotto Rey, Selva Plus.
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
 * Catálogo Guacharito Millonario (101 figuras: 00-100).
 * Incluye los 76 anteriores + 25 adicionales.
 */
val ANIMALES_GUACHARITO: List<Animal> = ANIMALES_EXTENDIDOS + listOf(
    Animal(76, "Delfín 2"),
    Animal(77, "Carnero 2"),
    Animal(78, "Toro 2"),
    Animal(79, "Ciempiés 2"),
    Animal(80, "Alacrán 2"),
    Animal(81, "León 2"),
    Animal(82, "Rana 2"),
    Animal(83, "Perico 2"),
    Animal(84, "Ratón 2"),
    Animal(85, "Águila 2"),
    Animal(86, "Tigre 2"),
    Animal(87, "Gato 2"),
    Animal(88, "Caballo 2"),
    Animal(89, "Mono 2"),
    Animal(90, "Paloma 2"),
    Animal(91, "Zorro 2"),
    Animal(92, "Oso 2"),
    Animal(93, "Pavo 2"),
    Animal(94, "Burro 2"),
    Animal(95, "Chivo 2"),
    Animal(96, "Cochino 2"),
    Animal(97, "Gallo 2"),
    Animal(98, "Camello 2"),
    Animal(99, "Cebra 2"),
    Animal(100, "Iguana 2")
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
        com.animalitostv.domain.model.Loteria.GUACHARO_ACTIVO -> ANIMALES_EXTENDIDOS
        else -> ANIMALES_ESTANDAR
    }
    return catalogo.firstOrNull { it.numero == numero }?.nombre ?: "Animal $numero"
}
