package ng.name.servix.app.data.model

import kotlinx.serialization.Serializable

/**
 * Bound to GET /categories (reference/api-routes/categories.ts:6-16, serializeCategory):
 * Observed live JSON:
 * {
 *   "id": "web-development",
 *   "name": "Web Development",
 *   "description": "Websites, web apps and e-commerce built to production standards.",
 *   "serviceCount": 124,
 *   "icon": "code"
 * }
 */
@Serializable
data class CategoryDto(
    val id: String,
    val name: String = "",
    val description: String = "",
    val serviceCount: Int = 0,
    val icon: String = "",
) {
    val slug: String get() = id
}

/**
 * Bound to GET /stats (reference/web/src/lib/httpApi.js:86-88, HomePage.jsx:28-33):
 * Observed live JSON:
 * {
 *   "verifiedProfessionals": 7,
 *   "activeServices": 13,
 *   "serviceCategories": 8,
 *   "averageRating": 4.8
 * }
 */
@Serializable
data class StatsDto(
    val verifiedProfessionals: Int? = null,
    val activeServices: Int? = null,
    val serviceCategories: Int? = null,
    val averageRating: Double? = null,
)

/**
 * Bound to GET /professionals (reference/api-routes/professionals.ts:51-83):
 * Response shape: { items, total, page, pageSize }
 */
@Serializable
data class ProfessionalsPageResponse(
    val items: List<ProfessionalSummaryDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
)

/**
 * Bound to serializeProfessionalSummary in reference/api-routes/professionals.ts:64,77:
 * Observed live JSON:
 * {
 *   "id": "tunde-bakare",
 *   "name": "Tunde Bakare",
 *   "title": "Brand & Graphic Designer",
 *   "location": "Abuja, Nigeria",
 *   "categoryId": "graphic-design",
 *   "rating": 4.9,
 *   "reviewCount": 156,
 *   "startingPrice": 120000,
 *   "verified": true,
 *   "completedProjects": 189,
 *   "responseTime": "Within 4 hours",
 *   "memberSince": "2023",
 *   "availability": "available",
 *   "image": "/images/professionals/tunde-bakare.jpg",
 *   "plan": "free",
 *   "availableNow": true
 * }
 */
@Serializable
data class ProfessionalSummaryDto(
    val id: String,
    val name: String = "",
    val title: String = "",
    val location: String = "",
    val categoryId: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val startingPrice: Long = 0L,
    val verified: Boolean = false,
    val completedProjects: Int = 0,
    val responseTime: String = "",
    val memberSince: String = "",
    val availability: String = "available",
    val image: String? = null,
    val plan: String = "free",
    val availableNow: Boolean = false,
) {
    val slug: String get() = id
}

/**
 * Bound to GET /professionals/:slug (reference/api-routes/professionals.ts:85-109):
 * Observed live JSON keys:
 * ['id', 'name', 'title', 'location', 'categoryId', 'rating', 'reviewCount',
 *  'startingPrice', 'verified', 'completedProjects', 'responseTime', 'memberSince',
 *  'availability', 'image', 'plan', 'about', 'skills', 'serviceIds', 'portfolio',
 *  'details', 'verifiedProjects', 'trust']
 */
@Serializable
data class ProfessionalDetailDto(
    val id: String,
    val name: String = "",
    val title: String = "",
    val location: String = "",
    val categoryId: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val startingPrice: Long = 0L,
    val verified: Boolean = false,
    val completedProjects: Int = 0,
    val responseTime: String = "",
    val memberSince: String = "",
    val availability: String = "available",
    val image: String? = null,
    val plan: String = "free",
    val about: String = "",
    val skills: List<String> = emptyList(),
    val serviceIds: List<String> = emptyList(),
    val portfolio: List<PortfolioItemDto> = emptyList(),
    val details: ProfessionalDetailsDto? = null,
    val verifiedProjects: Int = 0,
    val trust: TrustSummaryDto? = null,
) {
    val slug: String get() = id
}

@Serializable
data class PortfolioItemDto(
    val id: String,
    val title: String = "",
    val category: String = "",
    val description: String = "",
    val image: String? = null,
    val verified: Boolean = false,
    val completedAt: String? = null,
    val customerRating: Int? = null,
    val deliveryDays: Int? = null,
)

@Serializable
data class ProfessionalDetailsDto(
    val occupation: String = "",
    val website: String = "",
    val languages: List<LanguageItemDto> = emptyList(),
    val education: List<EducationItemDto> = emptyList(),
    val certifications: List<CertificationItemDto> = emptyList(),
    val experience: List<ExperienceItemDto> = emptyList(),
)

@Serializable
data class LanguageItemDto(
    val name: String = "",
    val level: String = "",
)

@Serializable
data class EducationItemDto(
    val school: String = "",
    val degree: String = "",
    val year: String = "",
)

@Serializable
data class CertificationItemDto(
    val name: String = "",
    val issuer: String = "",
    val year: String = "",
)

@Serializable
data class ExperienceItemDto(
    val title: String = "",
    val company: String = "",
    val start: String = "",
    val end: String = "",
    val description: String = "",
)

@Serializable
data class TrustSummaryDto(
    val servixVerified: Boolean = false,
    val identityVerified: Boolean = false,
    val completedJobs: Int = 0,
    val verifiedProjects: Int = 0,
    val accountAgeDays: Int? = null,
    val reliability: TrustMetricDto? = null,
    val responseRate: TrustMetricDto? = null,
    val repeatCustomers: TrustMetricDto? = null,
    val rating: TrustRatingMetricDto? = null,
    val achievements: List<AchievementDto> = emptyList(),
)

@Serializable
data class TrustMetricDto(
    val enough: Boolean = false,
    val percent: Int? = null,
    val measurable: Int? = null,
    val minimum: Int? = null,
    val medianHours: Double? = null,
    val customers: Int? = null,
)

@Serializable
data class TrustRatingMetricDto(
    val enough: Boolean = false,
    val average: Double? = null,
    val count: Int = 0,
    val minimum: Int? = null,
)

@Serializable
data class AchievementDto(
    val slug: String = "",
    val name: String = "",
    val description: String = "",
    val icon: String = "star",
)

/**
 * Bound to GET /professionals/:slug/availability/summary (reference/web/src/lib/marketplaceApi.js:44):
 * Observed live JSON:
 * {
 *   "nextAvailableAt": "2026-11-05T11:00:00.000Z",
 *   "availableToday": true,
 *   "availableTomorrow": true,
 *   "availableThisWeek": true
 * }
 */
@Serializable
data class AvailabilitySummaryDto(
    val nextAvailableAt: String? = null,
    val availableToday: Boolean = false,
    val availableTomorrow: Boolean = false,
    val availableThisWeek: Boolean = false,
)

/**
 * Bound to GET /services (reference/api-routes/services.ts:51-76):
 * Response shape: { items, total, page, pageSize }
 */
@Serializable
data class ServicesPageResponse(
    val items: List<ServiceSummaryDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
)

@Serializable
data class EmbeddedProfessionalDto(
    val id: String,
    val name: String = "",
    val image: String? = null,
    val verified: Boolean = false,
) {
    val slug: String get() = id
}

/**
 * Bound to serializeServiceSummary in reference/api-routes/services.ts:62,74 and professionals.ts:139:
 * Observed live JSON:
 * {
 *   "id": "brand-identity-package",
 *   "title": "Complete Brand Identity Package",
 *   "categoryId": "graphic-design",
 *   "professionalId": "tunde-bakare",
 *   "rating": 4.9,
 *   "reviewCount": 71,
 *   "price": 350000,
 *   "priceUnit": "per project",
 *   "duration": "2–3 weeks",
 *   "location": "Remote",
 *   "availability": "available",
 *   "image": "/images/services/brand-identity.jpg",
 *   "shortDescription": "Logo, color system, typography and brand guidelines — a complete identity built to last.",
 *   "professional": {
 *     "id": "tunde-bakare",
 *     "name": "Tunde Bakare",
 *     "image": "/images/professionals/tunde-bakare.jpg",
 *     "verified": true
 *   }
 * }
 */
@Serializable
data class ServiceSummaryDto(
    val id: String,
    val title: String = "",
    val categoryId: String = "",
    val professionalId: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val price: Long = 0L,
    val priceUnit: String = "per project",
    val duration: String = "",
    val location: String = "",
    val availability: String = "available",
    val image: String? = null,
    val shortDescription: String = "",
    val professional: EmbeddedProfessionalDto? = null,
    val availableNow: Boolean = false,
) {
    val slug: String get() = id
}

/**
 * Bound to GET /services/:slug (reference/api-routes/services.ts:78-90, serializeServiceDetail):
 * Observed live JSON keys:
 * ['id', 'title', 'categoryId', 'professionalId', 'rating', 'reviewCount', 'price', 'priceUnit',
 *  'duration', 'location', 'availability', 'image', 'shortDescription', 'professional', 'gallery',
 *  'media', 'video', 'documents', 'description', 'serviceType', 'searchTags', 'deliveryDays',
 *  'revisions', 'included', 'requirements', 'faqs']
 */
@Serializable
data class ServiceDetailDto(
    val id: String,
    val title: String = "",
    val categoryId: String = "",
    val professionalId: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val price: Long = 0L,
    val priceUnit: String = "per project",
    val duration: String = "",
    val location: String = "",
    val availability: String = "available",
    val image: String? = null,
    val shortDescription: String = "",
    val professional: EmbeddedProfessionalDto? = null,
    val gallery: List<String> = emptyList(),
    val media: List<ServiceMediaDto> = emptyList(),
    val video: ServiceMediaDto? = null,
    val documents: List<ServiceMediaDto> = emptyList(),
    val description: String = "",
    val serviceType: String = "",
    val searchTags: List<String> = emptyList(),
    val deliveryDays: Int? = null,
    val revisions: Int? = null,
    val included: List<String> = emptyList(),
    val requirements: List<ServiceRequirementDto> = emptyList(),
    val faqs: List<ServiceFaqDto> = emptyList(),
) {
    val slug: String get() = id
}

@Serializable
data class ServiceMediaDto(
    val url: String = "",
    val kind: String = "image",
    val fileName: String = "",
)

@Serializable
data class ServiceRequirementDto(
    val question: String = "",
    val type: String = "text",
    val options: List<String> = emptyList(),
    val required: Boolean = false,
)

@Serializable
data class ServiceFaqDto(
    val q: String = "",
    val a: String = "",
)

/**
 * Bound to serializeReview in reference/api-routes/professionals.ts:123 and services.ts:104:
 * Observed live JSON:
 * {
 *   "id": "58b92edc-b778-41f7-9d61-b0956a7a8972",
 *   "serviceId": "brand-identity-package",
 *   "professionalId": "tunde-bakare",
 *   "author": "F. Danjuma",
 *   "rating": 5,
 *   "date": "2026-08-01",
 *   "text": "The discovery workshop alone was worth it..."
 * }
 */
@Serializable
data class ReviewDto(
    val id: String,
    val serviceId: String? = null,
    val professionalId: String? = null,
    val author: String = "",
    val rating: Int = 5,
    val date: String = "",
    val text: String = "",
)

/**
 * Bound to GET /services/:slug/availability (reference/api-routes/bookings.ts:118-129):
 * Observed live JSON: { "slots": ["2026-11-05T11:00:00.000Z", ...] }
 */
@Serializable
data class ServiceAvailabilityResponse(
    val slots: List<String> = emptyList(),
)

/**
 * Bound to POST /views (reference/web/src/lib/marketplaceApi.js:45-51):
 * { type: "professional" | "service", slug }
 */
@Serializable
data class CountViewRequest(
    val type: String,
    val slug: String,
)

/**
 * Bound to GET /account/saved and POST /account/saved (reference/api-routes/account.ts:76-110):
 */
@Serializable
data class SavedProfessionalItemDto(
    val id: String = "",
    val savedAt: String = "",
    val preferred: Boolean = false,
    val note: String = "",
    val professional: SavedProfessionalBriefDto,
)

@Serializable
data class SavedProfessionalBriefDto(
    val slug: String,
    val name: String = "",
    val title: String = "",
    val imageUrl: String? = null,
    val locationCity: String? = null,
    val ratingAvg: Double = 0.0,
    val reviewCount: Int = 0,
    val verification: String = "",
    val availability: String = "available",
    val startingPrice: Long? = null,
    val currency: String = "NGN",
)

@Serializable
data class SaveProfessionalRequest(
    val profileSlug: String,
)

@Serializable
data class RemoveSavedResponse(
    val removed: Int = 0,
)

/**
 * Bound to serializeBooking in reference/api-routes/bookings.ts:46-92:
 */
@Serializable
data class BookingDto(
    val id: String,
    val reference: String = "",
    val status: String = "",
    val scheduledAt: String = "",
    val amount: Long = 0L,
    val currency: String = "NGN",
    val serviceTitle: String = "",
    val priceUnit: String = "per project",
    val serviceId: String? = null,
    val professionalId: String? = null,
    val professionalName: String? = null,
    val customerName: String? = null,
    val notes: String = "",
    val cancelledBy: String? = null,
    val cancelReason: String? = null,
    val disputeReason: String? = null,
    val deliveredAt: String? = null,
    val completedAt: String? = null,
    val autoConfirmed: Boolean = false,
    val hasReview: Boolean = false,
    val createdAt: String = "",
)
