# Visual redesign 1.2 — data-backed deviations

## S10 professional profile

The corrected HTML illustrates a recent-work gallery containing images, PDFs and video. The professional profile endpoint serializes portfolio items as `id`, `title`, `category`, `description` and `image`, with no media type. Per owner decision, S10 renders these as images only and does not claim that portfolio PDFs or video are present. Service media counts are read separately from each service-detail endpoint's typed `media` list; unavailable detail reads do not display a fabricated zero count. At most four service-detail reads run concurrently.

The HTML's service-area, years-of-experience and warranty examples are not manufactured when absent from the professional or service response.

## S11 service detail

The service gallery uses `media.kind`, with the API's image gallery/video/document fields as a compatibility fallback. Documents and video open their real URLs externally rather than pretending an in-app player exists. The fixed-price example, inclusions and trust labels are shown only when the returned service supports them; the professional's verification does not imply separate service verification. The booking sheet still uses actual available slots, but the requested scheduling preview and FAQ blocks are absent from the detail page. Description, customer requirements and real reviews remain below the illustrated protected-payment section so existing catalogue information is not lost.

The S10 and S11 mockups also show share controls and S11 shows a service bookmark. These controls are not yet present in this implementation; the existing saved endpoint is for professionals, not services. The screens retain the real professional save and service navigation instead of presenting a nonfunctional service-save action.
