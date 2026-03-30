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
    LocalDateTime completedAt
) {
    public static InvestigationDto from(Investigation i) {
        return new InvestigationDto(
            i.getId(), i.getSubjectName(), i.getSubjectDob(),
            i.getSubjectLastKnownAddress(), i.getSubjectLastKnownEmail(),
            i.getSubjectGraduationYear() != null ? i.getSubjectGraduationYear().toString() : null,
            i.getStatus().name(), i.getConfidenceScore(),
            i.getResultJson(), i.getErrorMessage(),
            i.getCreatedAt(), i.getCompletedAt()
        );
    }
}
