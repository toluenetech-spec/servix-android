package ng.name.servix.app.location

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class NigerianCity(val name: String, val state: String, val latitude: Double, val longitude: Double)

// Approximate city centres, used only for local proximity matching, never sent to a server.
val nigerianCities = listOf(
    NigerianCity("Lagos", "Lagos", 6.4550, 3.3841),
    NigerianCity("Ikeja", "Lagos", 6.6018, 3.3515),
    NigerianCity("Lekki", "Lagos", 6.4698, 3.5852),
    NigerianCity("Abuja", "FCT", 9.0765, 7.3986),
    NigerianCity("Port Harcourt", "Rivers", 4.8156, 7.0498),
    NigerianCity("Ibadan", "Oyo", 7.3775, 3.9470),
    NigerianCity("Kano", "Kano", 12.0022, 8.5920),
    NigerianCity("Enugu", "Enugu", 6.5244, 7.5086),
    NigerianCity("Benin City", "Edo", 6.3350, 5.6037),
    NigerianCity("Kaduna", "Kaduna", 10.5105, 7.4165),
    NigerianCity("Jos", "Plateau", 9.8965, 8.8583),
    NigerianCity("Abeokuta", "Ogun", 7.1475, 3.3619),
    NigerianCity("Owerri", "Imo", 5.4836, 7.0333),
    NigerianCity("Calabar", "Cross River", 4.9517, 8.3220),
    NigerianCity("Uyo", "Akwa Ibom", 5.0377, 7.9128),
    NigerianCity("Warri", "Delta", 5.5167, 5.7500),
    NigerianCity("Ilorin", "Kwara", 8.4966, 4.5421),
    NigerianCity("Onitsha", "Anambra", 6.1498, 6.7857),
    NigerianCity("Asaba", "Delta", 6.1982, 6.7319),
    NigerianCity("Akure", "Ondo", 7.2571, 5.2058),
    NigerianCity("Osogbo", "Osun", 7.7710, 4.5569),
    NigerianCity("Ado-Ekiti", "Ekiti", 7.6211, 5.2214),
    NigerianCity("Minna", "Niger", 9.6139, 6.5569),
    NigerianCity("Lokoja", "Kogi", 7.8023, 6.7333),
    NigerianCity("Makurdi", "Benue", 7.7333, 8.5333),
    NigerianCity("Maiduguri", "Borno", 11.8311, 13.1510),
    NigerianCity("Sokoto", "Sokoto", 13.0059, 5.2476),
    NigerianCity("Zaria", "Kaduna", 11.1113, 7.7227),
    NigerianCity("Bauchi", "Bauchi", 10.3158, 9.8442),
    NigerianCity("Yola", "Adamawa", 9.2084, 12.4815),
    NigerianCity("Gombe", "Gombe", 10.2897, 11.1673),
    NigerianCity("Jalingo", "Taraba", 8.8937, 11.3596),
    NigerianCity("Umuahia", "Abia", 5.5320, 7.4860),
    NigerianCity("Awka", "Anambra", 6.2100, 7.0711),
    NigerianCity("Abakaliki", "Ebonyi", 6.3249, 8.1137),
    NigerianCity("Yenagoa", "Bayelsa", 4.9267, 6.2676),
    NigerianCity("Lafia", "Nasarawa", 8.4939, 8.5153),
    NigerianCity("Birnin Kebbi", "Kebbi", 12.4539, 4.1975),
    NigerianCity("Dutse", "Jigawa", 11.7590, 9.3385),
    NigerianCity("Katsina", "Katsina", 12.9908, 7.6018),
    NigerianCity("Gusau", "Zamfara", 12.1704, 6.6641),
    NigerianCity("Damaturu", "Yobe", 11.7470, 11.9608),
    NigerianCity("Aba", "Abia", 5.1066, 7.3667),
    NigerianCity("Ogbomoso", "Oyo", 8.1337, 4.2401),
    NigerianCity("Katsina-Ala", "Benue", 7.1697, 9.2870),
    NigerianCity("Ugep", "Cross River", 5.8086, 8.0819),
)

fun nearestCity(latitude: Double, longitude: Double): NigerianCity? {
    if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    fun radians(value: Double) = Math.toRadians(value)
    fun distance(city: NigerianCity): Double {
        val latDelta = radians(city.latitude - latitude)
        val lonDelta = radians(city.longitude - longitude)
        val a = sin(latDelta / 2).pow(2) + cos(radians(latitude)) * cos(radians(city.latitude)) * sin(lonDelta / 2).pow(2)
        return 6371.0 * 2 * asin(min(1.0, sqrt(a)))
    }
    val city = nigerianCities.minByOrNull(::distance) ?: return null
    return city.takeIf { distance(it) <= 100.0 }
}
