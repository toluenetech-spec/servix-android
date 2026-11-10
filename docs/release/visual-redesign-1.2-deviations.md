# Visual redesign 1.2 — data-backed deviations

## S10 professional profile

The corrected HTML illustrates a recent-work gallery containing images, PDFs and video. The professional profile endpoint serializes portfolio items as `id`, `title`, `category`, `description` and `image`, with no media type. Per owner decision, S10 renders these as images only and does not claim that portfolio PDFs or video are present. Service media counts are read separately from each service-detail endpoint's typed `media` list; unavailable detail reads do not display a fabricated zero count. At most four service-detail reads run concurrently.

The HTML's service-area, years-of-experience and warranty examples are not manufactured when absent from the professional or service response.
