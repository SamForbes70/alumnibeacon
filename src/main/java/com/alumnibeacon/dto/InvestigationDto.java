package com.alumnibeacon.dto;
import com.alumnibeacon.model.Investigation;
import java.time.LocalDateTime;

public record InvestigationDto(
    String id,
    String subjectName,
    String subjectDob,
    String subjectLastKnownAddress,
    String subjectLastKnownEmail,
    String subjectGraduationYear,
    String status,
    Integer confidenceScore,
    String resultJson,
    String errorMessage,
    LocalDateTime createdAt,
    LocalDateTime completedAt,
    /** 'python' | 'agent-zero' | null */
    String preferredEngine
) {
    public static InvestigationDto from(Investigation i) {
        return new InvestigationDto(
            i.getId(), i.getSubjectName(), i.getSubjectDob(),
            i.getSubjectLastKnownAddress(), i.getSubjectLastKnownEmail(),
            i.getSubjectGraduationYear() != null ? i.getSubjectGraduationYear().toString() : null,
            i.getStatus().name(), i.getConfidenceScore(),
            i.getResultJson(), i.getErrorMessage(),
            i.getCreatedAt(), i.getCompletedAt(),
            i.getPreferredEngine()
        );
    }

    /** Human-readable engine label for UI display */
    public String engineLabel() {
        if ("agent-zero".equals(preferredEngine)) return "🤖 Deep Investigation";
        if ("python".equals(preferredEngine))     return "⚡ Standard";
        return "⚡ Standard"; // default
    }

    /** Tailwind badge classes for engine display */
    public String engineBadgeClass() {
        if ("agent-zero".equals(preferredEngine))
            return "inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-purple-100 text-purple-800";
        return "inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-600";
    }

    /** Estimated duration string for progress display */
    public String estimatedDuration() {
        if ("agent-zero".equals(preferredEngine)) return "~10–15 minutes";
        return "~1–2 minutes";
    }

    public boolean isDeepInvestigation() {
        return "agent-zero".equals(preferredEngine);
    }
}
