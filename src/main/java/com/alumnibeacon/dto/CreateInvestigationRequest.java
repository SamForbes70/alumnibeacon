package com.alumnibeacon.dto;
import jakarta.validation.constraints.NotBlank;
public record CreateInvestigationRequest(
    @NotBlank String subjectName,
    String subjectDob,
    String subjectLastKnownAddress,
    String subjectLastKnownEmail,
    String subjectLastKnownPhone,
    Integer subjectGraduationYear,
    String subjectLastKnownEmployer,
    String subjectNotes
) {}
