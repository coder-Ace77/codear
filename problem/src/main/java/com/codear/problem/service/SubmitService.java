package com.codear.problem.service;
import org.springframework.stereotype.Service;
import com.codear.problem.dto.Code;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SubmitService {

    private final KafkaSender kafkaSender;

    public void processSubmittedCode(Code code) {
        kafkaSender.sendMessage(code);
    }
}
