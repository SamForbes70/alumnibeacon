package com.alumnibeacon.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Parsed representation of the OSINT adapter's JSON result.
 * Maps directly to the schema returned by osint-adapter/main.py.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OsintResultDto(

    @JsonProperty("confidence_score")
    Integer confidenceScore,

    @JsonProperty("found_email")
    String foundEmail,

    @JsonProperty("found_phone")
    String foundPhone,

    @JsonProperty("found_address")
    String foundAddress,

    @JsonProperty("found_employer")
    String foundEmployer,

    @JsonProperty("found_linkedin")
    String foundLinkedin,

    @JsonProperty("found_facebook")
    String foundFacebook,

    @JsonProperty("sources")
    List<String> sources,

    @JsonProperty("summary")
    String summary,

    @JsonProperty("confidence_breakdown")
    Map<String, Integer> confidenceBreakdown,

    @JsonProperty("recommended_actions")
    List<String> recommendedActions,

    @JsonProperty("privacy_flags")
    List<String> privacyFlags,

    @JsonProperty("mode")
    String mode

) {
    /** Returns true if any contact information was found. */
    public boolean hasContactInfo() {
        return isPresent(foundEmail) || isPresent(foundPhone)
            || isPresent(foundAddress) || isPresent(foundEmployer);
    }

    /** Returns true if any social media profiles were found. */
    public boolean hasSocialMedia() {
        return isPresent(foundLinkedin) || isPresent(foundFacebook);
    }

    /** Returns true if privacy flags exist. */
    public boolean hasPrivacyFlags() {
        return privacyFlags != null && !privacyFlags.isEmpty();
    }

    /** Returns true if recommended actions exist. */
    public boolean hasRecommendedActions() {
        return recommendedActions != null && !recommendedActions.isEmpty();
    }

    /** Returns true if sources exist. */
    public boolean hasSources() {
        return sources != null && !sources.isEmpty();
    }

    /** Returns true if this is a mock/AI-only result. */
    public boolean isMockResult() {
        return "mock".equalsIgnoreCase(mode)
            || (sources != null && sources.stream()
                .anyMatch(s -> s != null && s.toLowerCase().contains("mock")));
    }

    /** Confidence level label for display. */
    public String confidenceLabel() {
        if (confidenceScore == null) return "Unknown";
        if (confidenceScore >= 80) return "High";
        if (confidenceScore >= 50) return "Medium";
        if (confidenceScore >= 25) return "Low";
        return "Very Low";
    }

    /** Confidence colour class for Tailwind. */
    public String confidenceColour() {
        if (confidenceScore == null) return "text-gray-500";
        if (confidenceScore >= 80) return "text-green-600";
        if (confidenceScore >= 50) return "text-yellow-600";
        return "text-red-600";
    }

    private static boolean isPresent(String s) {
        return s != null && !s.isBlank() && !s.equalsIgnoreCase("null");
    }
}
