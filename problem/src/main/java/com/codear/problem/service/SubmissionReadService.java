package com.codear.problem.service;

import com.codear.problem.dto.Submission;
import com.codear.problem.enums.SubmissionStatus;
import com.codear.problem.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SubmissionReadService {

    private final SubmissionRepository submissionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Submission getSubmissionById(String submissionId, int waited) {
        System.out.println("Fetching submission using Id: " + submissionId + " waited=" + waited);

        return submissionRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public SubmissionStatus getSubmissionStatusById(String submissionId, int waited) {
        System.out.println("Fetching status for Id: " + submissionId + " waited=" + waited);

        return submissionRepository.findBySubmissionId(submissionId)
                .map(Submission::getStatus) 
                .orElseThrow(() -> new RuntimeException("Submission not found"));
    }
}
