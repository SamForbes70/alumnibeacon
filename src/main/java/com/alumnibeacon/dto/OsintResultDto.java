package com.alumnibeacon.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Parsed representation of the OSINT adapter's JSON result.
 * Maps directly to the schema returned by osint-adapter/main.py
 * and the alumnibeacon-osint Agent Zero profile.
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
    String mode,

    /** Which engine produced this result: python | agent-zero | hybrid | python-fallback */
    @JsonProperty("engine")
    String engine,

    /** Number of tool calls used (Agent Zero investigations only). */
    @JsonProperty("tool_calls_used")
    Integer toolCallsUsed

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

    /** Returns true if this result was produced by Agent Zero. */
    public boolean isAgentZeroResult() {
        return "agent-zero".equalsIgnoreCase(engine);
    }

    /** Returns true if Agent Zero fell back to Python. */
    public boolean isFallbackResult() {
        return "python-fallback".equalsIgnoreCase(engine);
    }

    /** Display label for the engine badge. */
    public String engineLabel() {
        if (engine == null) return "Python";
        return switch (engine.toLowerCase()) {
            case "agent-zero"     -> "🤖 Deep Investigation";
            case "python-fallback"-> "⚡ Standard (fallback)";
            case "hybrid"        -> "🔀 Hybrid";
            default              -> "⚡ Standard";
        };
    }

    /** Tailwind CSS classes for the engine badge. */
    public String engineBadgeClass() {
        if (engine == null) return "bg-gray-100 text-gray-700";
        return switch (engine.toLowerCase()) {
            case "agent-zero"     -> "bg-purple-100 text-purple-800";
            case "python-fallback"-> "bg-yellow-100 text-yellow-800";
            case "hybrid"        -> "bg-blue-100 text-blue-800";
            default              -> "bg-gray-100 text-gray-700";
        };
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
