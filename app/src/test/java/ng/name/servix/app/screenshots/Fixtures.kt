package ng.name.servix.app.screenshots

import ng.name.servix.app.data.model.*
import ng.name.servix.app.ui.ai.ChatTurn

/**
 * Fake data only. Shapes mirror the live JSON documented on each DTO.
 * Image URLs are deliberately null: screenshots must never touch the network.
 */
object Fixtures {
    const val USER_ID = "11111111-1111-4111-8111-111111111111"
    const val BOOKING_ID = "22222222-2222-4222-8222-222222222222"

    val user = UserDto(
        id = USER_ID,
        email = "adaeze@example.com",
        fullName = "Adaeze Okonkwo",
        role = "customer",
        emailVerified = true,
    )

    val categories = listOf(
        CategoryDto("graphic-design", "Graphic Design", "Logos, brand systems and print.", 124, "palette"),
        CategoryDto("web-development", "Web Development", "Websites and web apps.", 98, "code"),
        CategoryDto("writing", "Writing", "Copy, articles and scripts.", 76, "edit"),
        CategoryDto("photography", "Photography", "Product, event and portrait.", 64, "camera"),
        CategoryDto("digital-marketing", "Digital Marketing", "Ads, SEO and social.", 58, "trending_up"),
        CategoryDto("video-animation", "Video & Animation", "Editing and motion.", 41, "movie"),
        CategoryDto("music-audio", "Music & Audio", "Voice, mixing and jingles.", 30, "mic"),
        CategoryDto("business", "Business", "Plans, decks and strategy.", 27, "business_center"),
    )

    val professionals = listOf(
        ProfessionalSummaryDto(
            id = "tunde-bakare", name = "Tunde Bakare", title = "Brand & Graphic Designer",
            location = "Abuja, Nigeria", categoryId = "graphic-design", rating = 4.9, reviewCount = 156,
            startingPrice = 120000, verified = true, completedProjects = 189, responseTime = "Within 4 hours",
            memberSince = "2023", availability = "available", plan = "free", availableNow = true,
        ),
        ProfessionalSummaryDto(
            id = "chiamaka-eze", name = "Chiamaka Eze", title = "Product Designer",
            location = "Lagos, Nigeria", categoryId = "graphic-design", rating = 4.8, reviewCount = 92,
            startingPrice = 180000, verified = true, completedProjects = 104, responseTime = "Within 2 hours",
            memberSince = "2022", availability = "available", plan = "pro", availableNow = true,
        ),
        ProfessionalSummaryDto(
            id = "ibrahim-yusuf", name = "Ibrahim Yusuf", title = "Full-stack Developer",
            location = "Kano, Nigeria", categoryId = "web-development", rating = 4.7, reviewCount = 63,
            startingPrice = 350000, verified = true, completedProjects = 71, responseTime = "Within 1 day",
            memberSince = "2023", availability = "limited", plan = "free",
        ),
        ProfessionalSummaryDto(
            id = "grace-adeyemi", name = "Grace Adeyemi", title = "Copywriter",
            location = "Ibadan, Nigeria", categoryId = "writing", rating = 5.0, reviewCount = 38,
            startingPrice = 45000, verified = true, completedProjects = 44, responseTime = "Within 3 hours",
            memberSince = "2024", availability = "available", plan = "free", availableNow = true,
        ),
    )

    val professionalDetail = ProfessionalDetailDto(
        id = "tunde-bakare", name = "Tunde Bakare", title = "Brand & Graphic Designer",
        location = "Abuja, Nigeria", categoryId = "graphic-design", rating = 4.9, reviewCount = 156,
        startingPrice = 120000, verified = true, completedProjects = 189, responseTime = "Within 4 hours",
        memberSince = "2023", availability = "available", plan = "free",
        about = "I build brand identities for Nigerian businesses that need to look as good as the work they do.",
        skills = listOf("Brand identity", "Logo design", "Packaging", "Figma"),
    )

    val services = listOf(
        ServiceSummaryDto(
            id = "brand-identity-package", title = "Complete Brand Identity Package", categoryId = "graphic-design",
            professionalId = "tunde-bakare", rating = 4.9, reviewCount = 71, price = 350000,
            priceUnit = "per project", duration = "2–3 weeks", location = "Remote", image = null,
            shortDescription = "Logo, color system, typography and brand guidelines.",
            professional = EmbeddedProfessionalDto("tunde-bakare", "Tunde Bakare", null, true),
        ),
        ServiceSummaryDto(
            id = "landing-page-build", title = "Conversion Landing Page", categoryId = "web-development",
            professionalId = "ibrahim-yusuf", rating = 4.7, reviewCount = 28, price = 480000,
            priceUnit = "per project", duration = "7 days", location = "Remote",
            shortDescription = "Fast, responsive landing page with analytics.",
            professional = EmbeddedProfessionalDto("ibrahim-yusuf", "Ibrahim Yusuf", null, true),
        ),
    )

    val serviceDetail = ServiceDetailDto(
        id = "brand-identity-package", title = "Complete Brand Identity Package", categoryId = "graphic-design",
        professionalId = "tunde-bakare", rating = 4.9, reviewCount = 71, price = 350000,
        priceUnit = "per project", duration = "2–3 weeks", location = "Remote",
        shortDescription = "Logo, color system, typography and brand guidelines — a complete identity built to last.",
        description = "You get a full identity system you can hand to any printer or developer.",
        professional = EmbeddedProfessionalDto("tunde-bakare", "Tunde Bakare", null, true),
        included = listOf("Logo suite (primary, secondary, mark)", "Colour and type system", "Brand guidelines PDF", "Source files"),
        faqs = listOf(
            ServiceFaqDto("How many revisions?", "Two rounds of revisions are included."),
            ServiceFaqDto("Do I own the files?", "Yes — full ownership on delivery."),
        ),
        deliveryDays = 14, revisions = 2,
    )

    val reviews = listOf(
        ReviewDto("r1", author = "Chinedu A.", rating = 5, date = "2026-10-18", text = "Delivered ahead of schedule and the files were spotless."),
        ReviewDto("r2", author = "Bisi O.", rating = 5, date = "2026-09-30", text = "Understood the brief in one call. Would book again."),
    )

    fun booking(status: String, id: String = BOOKING_ID, service: String = "Complete Brand Identity Package") = BookingDto(
        id = id, reference = "SVX-4712", status = status, scheduledAt = "2026-11-12T10:00:00.000Z",
        amount = 350000, serviceTitle = service, priceUnit = "per project",
        serviceId = "brand-identity-package", professionalId = "tunde-bakare", professionalName = "Tunde Bakare",
        customerName = "Adaeze Okonkwo", notes = "I need the identity before a trade fair in December.",
        createdAt = "2026-10-28T09:15:00.000Z",
    )

    val bookings = listOf(
        booking("pending_payment"),
        booking("delivered", "33333333-3333-4333-8333-333333333333", "Social media starter kit"),
        booking("completed", "44444444-4444-4444-8444-444444444444", "Pitch deck design"),
    )

    val availabilitySlots = listOf(
        "2026-11-10T09:00:00.000Z", "2026-11-10T13:00:00.000Z",
        "2026-11-11T11:00:00.000Z", "2026-11-12T10:00:00.000Z",
    )

    val usage = AiUsage(
        planLabel = "Free", subject = "user", used = 2300, allowed = 20000, percent = 11.5,
        level = "ok", resetAt = "2026-12-01T00:00:00.000Z", enabled = true,
        member = AiMeter(used = 2300, allowed = 20000, percent = 11.5, level = "ok"),
    )

    val aiTurns = listOf(
        ChatTurn(1L, "user", "Who can design a logo for a small bakery in Lagos?"),
        ChatTurn(
            2L, "assistant",
            "**Tunde Bakare** is a strong fit — a verified brand designer in Abuja with a 4.9 rating from 156 reviews.\n\n" +
                "His Complete Brand Identity Package starts at **₦120,000** and includes a logo suite, colour and type system, and brand guidelines.",
        ),
    )
}
