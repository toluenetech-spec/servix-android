package ng.name.servix.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import ng.name.servix.app.data.model.AvailabilitySummaryDto
import ng.name.servix.app.data.model.BookingDto
import ng.name.servix.app.data.model.CategoryDto
import ng.name.servix.app.data.model.CountViewRequest
import ng.name.servix.app.data.model.ProfessionalDetailDto
import ng.name.servix.app.data.model.ProfessionalsPageResponse
import ng.name.servix.app.data.model.ReviewDto
import ng.name.servix.app.data.model.SaveProfessionalRequest
import ng.name.servix.app.data.model.SavedProfessionalItemDto
import ng.name.servix.app.data.model.ServiceAvailabilityResponse
import ng.name.servix.app.data.model.ServiceDetailDto
import ng.name.servix.app.data.model.ServiceSummaryDto
import ng.name.servix.app.data.model.ServicesPageResponse
import ng.name.servix.app.data.model.StatsDto
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.data.network.ServixApiService

class CatalogueRepository(
    private val apiProvider: () -> ServixApiService,
) {
    private val api: ServixApiService
        get() = apiProvider()

    private val _cachedCategories = MutableStateFlow<List<CategoryDto>>(emptyList())
    val cachedCategories: StateFlow<List<CategoryDto>> = _cachedCategories.asStateFlow()

    suspend fun getCategories(forceRefresh: Boolean = false): Result<List<CategoryDto>> =
        withContext(Dispatchers.IO) {
            if (!forceRefresh && _cachedCategories.value.isNotEmpty()) {
                return@withContext Result.success(_cachedCategories.value)
            }
            try {
                val list = api.getCategories()
                _cachedCategories.value = list
                Result.success(list)
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }

    fun categoryNameFor(slug: String?): String {
        if (slug.isNullOrBlank()) return ""
        return _cachedCategories.value.firstOrNull { it.id == slug }?.name
            ?: slug.replace('-', ' ').replaceFirstChar { it.uppercase() }
    }

    suspend fun getStats(): Result<StatsDto> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.getStats())
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun getProfessionals(
        q: String? = null,
        category: String? = null,
        location: String? = null,
        minRating: Double? = null,
        maxPrice: Int? = null,
        availability: String? = null,
        available: String? = null,
        sort: String? = "recommended",
        page: Int = 1,
        pageSize: Int = 10,
    ): Result<ProfessionalsPageResponse> = withContext(Dispatchers.IO) {
        try {
            val res = api.getProfessionals(
                q = q?.trim()?.takeIf { it.isNotEmpty() },
                category = category?.trim()?.takeIf { it.isNotEmpty() },
                location = location?.trim()?.takeIf { it.isNotEmpty() },
                minRating = minRating,
                maxPrice = maxPrice,
                availability = availability?.trim()?.takeIf { it.isNotEmpty() },
                available = available?.trim()?.takeIf { it.isNotEmpty() },
                sort = sort?.trim()?.takeIf { it.isNotEmpty() } ?: "recommended",
                page = page,
                pageSize = pageSize,
            )
            Result.success(res)
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun getProfessional(slug: String): Result<ProfessionalDetailDto> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getProfessional(slug))
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }

    suspend fun getProfessionalServices(slug: String): Result<List<ServiceSummaryDto>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getProfessionalServices(slug))
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }

    suspend fun getProfessionalReviews(slug: String): Result<List<ReviewDto>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getProfessionalReviews(slug))
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }

    suspend fun getProfessionalAvailabilitySummary(slug: String): Result<AvailabilitySummaryDto> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getProfessionalAvailabilitySummary(slug))
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }

    suspend fun getServices(
        q: String? = null,
        category: String? = null,
        minPrice: Int? = null,
        maxPrice: Int? = null,
        minRating: Double? = null,
        location: String? = null,
        availability: String? = null,
        available: String? = null,
        sort: String? = "recommended",
        page: Int = 1,
        pageSize: Int = 10,
    ): Result<ServicesPageResponse> = withContext(Dispatchers.IO) {
        try {
            val res = api.getServices(
                q = q?.trim()?.takeIf { it.isNotEmpty() },
                category = category?.trim()?.takeIf { it.isNotEmpty() },
                minPrice = minPrice,
                maxPrice = maxPrice,
                minRating = minRating,
                location = location?.trim()?.takeIf { it.isNotEmpty() },
                availability = availability?.trim()?.takeIf { it.isNotEmpty() },
                available = available?.trim()?.takeIf { it.isNotEmpty() },
                sort = sort?.trim()?.takeIf { it.isNotEmpty() } ?: "recommended",
                page = page,
                pageSize = pageSize,
            )
            Result.success(res)
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun getService(slug: String): Result<ServiceDetailDto> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getService(slug))
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }

    suspend fun getServiceReviews(slug: String): Result<List<ReviewDto>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getServiceReviews(slug))
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }

    suspend fun getServiceAvailability(slug: String, days: Int = 14): Result<ServiceAvailabilityResponse> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getServiceAvailability(slug, days))
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }

    suspend fun countView(type: String, slug: String) = withContext(Dispatchers.IO) {
        try {
            api.countView(CountViewRequest(type = type, slug = slug))
        } catch (_: Throwable) {
            // Analytics must never break the screen (marketplaceApi.js:50)
        }
    }

    suspend fun getSavedProfessionals(): Result<List<SavedProfessionalItemDto>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getSavedProfessionals())
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }

    suspend fun saveProfessional(profileSlug: String): Result<SavedProfessionalItemDto> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.saveProfessional(SaveProfessionalRequest(profileSlug = profileSlug)))
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }

    suspend fun unsaveProfessional(slug: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                api.unsaveProfessional(slug)
                Result.success(Unit)
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }

    suspend fun getMyBookings(): Result<List<BookingDto>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getMyBookings())
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }
}
