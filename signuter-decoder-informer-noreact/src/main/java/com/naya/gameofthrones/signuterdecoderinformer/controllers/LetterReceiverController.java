package com.naya.gameofthrones.signuterdecoderinformer.controllers;

import com.naya.gameofthrones.signuterdecoderinformer.model.DecodedLetter;
import com.naya.gameofthrones.signuterdecoderinformer.model.Letter;
import com.naya.gameofthrones.signuterdecoderinformer.services.GuardService;
import com.naya.gameofthrones.signuterdecoderinformer.services.LetterDecoder;
import com.naya.speedadjuster.services.LetterRequesterService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

/**
 * @author Evgeny Borisov
 */
@Slf4j
@RestController
@RequestMapping("/analyse/letter")
public class LetterReceiverController {
    private final LetterDecoder          decoder;
    private final LetterRequesterService letterRequesterService;
    private       GuardService           guardService;
    private final Counter                counter;

    public LetterReceiverController(LetterDecoder decoder,
                                    LetterRequesterService letterRequesterService,
                                    GuardService guardService,
                                    MeterRegistry meterRegistry) {
        this.decoder = decoder;
        this.letterRequesterService = letterRequesterService;
        this.guardService = guardService;
        this.counter = meterRegistry.counter("letter.rps");
    }

    @PostConstruct
    public void init() {
        letterRequesterService.request(5);
    }

    @PostMapping
    @Async("letterProcessorExecutor")
    public void processLetter(@RequestBody Letter letter) throws InterruptedException {
        DecodedLetter decode = decoder.decode(letter);
        counter.increment();
        guardService.send(decode);
        letterRequesterService.request(1);
    }
}
