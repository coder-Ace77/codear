package com.codear.problem;

import com.codear.problem.dto.Submission;
import com.codear.problem.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@Service
public class SubmissionService {
    @Autowired
    private SubmissionRepository submissionRepository;

    public Submission getSubmissionById(String submissionId) {
        return submissionRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
    }

    public List<Submission> getSubmissionsByProblemId(Long problemId) {
        return submissionRepository.findByProblemId(problemId);
    }

}
