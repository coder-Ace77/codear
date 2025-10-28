package com.codear.problem;

import com.codear.problem.dto.Submission;
import com.codear.problem.enums.SubmissionStatus;
import com.codear.problem.repository.SubmissionRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.AccessOptions.SetOptions.Propagation;

import java.util.List;

@Service
@AllArgsConstructor
public class SubmissionService {
    private final SubmissionReadService submissionReadService;

    // @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    // public Submission getSubmissionById(String submissionId, int waited) {
    //     System.out.println("Fetching submission using Id: " + submissionId + " waited=" + waited);

    //     return submissionRepository.findBySubmissionId(submissionId)
    //             .orElseThrow(() -> new RuntimeException("Submission not found"));
    // }


    public Submission longPollingService(String submissionId) throws InterruptedException {
        int maxWaitTime = 10000;  // 10s
        int baseInterval = 2000;
        int waited = 0;

        System.out.println("Entered long polling service for " + submissionId);

        SubmissionStatus submission = submissionReadService.getSubmissionStatusById(submissionId, waited);

        long startTime = System.currentTimeMillis();
        System.out.println("⏳ Entered polling loop for " + submissionId);

        try {
            while (submission == SubmissionStatus.IN_PROGRESS && waited < maxWaitTime) {
                // int currentInterval = Math.min(baseInterval * (1 << (waited / baseInterval)), 2000);
                // int currentInterval = 100;
                Thread.sleep(baseInterval);
                waited += baseInterval;

                submission = submissionReadService.getSubmissionStatusById(submissionId , waited);
                System.out.println("Looping... waited=" + waited + " status=" + submission);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Exception in polling loop: " + e.getMessage());
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("✅ Exited polling loop for " + submissionId);
        System.out.println("⏱️ Total time spent: " + duration + " ms");
        System.out.println("Final submission status: " + submission);

        Submission submission2 = submissionReadService.getSubmissionById(submissionId, waited);
        submission2.setStatus(submission);
        return submission2;
    }



    // public List<Submission> getSubmissionsByProblemId(Long problemId) {
    //     return submissionRepository.findByProblemId(problemId);
    // }

}
