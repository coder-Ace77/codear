package com.codear.problem;

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

    /**
     * This method now runs in its OWN NEW transaction every time it's called.
     * This allows it to see data committed by other services.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Submission getSubmissionById(String submissionId, int waited) {
        System.out.println("Fetching submission using Id: " + submissionId + " waited=" + waited);

        return submissionRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
    }

    // / Change return type to the enum
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public SubmissionStatus getSubmissionStatusById(String submissionId, int waited) {
        System.out.println("Fetching status for Id: " + submissionId + " waited=" + waited);

        // Use a simple query to fetch only the status.
        // This is more efficient and avoids loading the entity.
        return submissionRepository.findBySubmissionId(submissionId)
                .map(Submission::getStatus) // Map to the status
                .orElseThrow(() -> new RuntimeException("Submission not found"));
    }
}
