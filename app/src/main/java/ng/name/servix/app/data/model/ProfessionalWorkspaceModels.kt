package ng.name.servix.app.data.model

import kotlinx.serialization.Serializable

/** Fields used by Home from GET /pro/analytics; verified against proWorkspace.ts. */
@Serializable
data class ProfessionalAnalyticsDto(val totals: ProfessionalAnalyticsTotalsDto? = null)

@Serializable
data class ProfessionalAnalyticsTotalsDto(
    val earnings: Double? = null,
    val completed: Int? = null,
    val responseRate: Int? = null,
    val ratingAvg: Double? = null,
    val reviewCount: Int? = null,
)

/** GET /pro/services returns a bare array of serializeOwnService results. */
@Serializable
data class OwnServiceDto(
    val id: String = "",
    val title: String = "",
    val status: String = "",
    val publishProblems: Map<String, String> = emptyMap(),
)
