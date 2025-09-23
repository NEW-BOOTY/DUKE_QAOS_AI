/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 *
 * TaskScheduler: Self-optimizing scheduler adapting to hardware/user/emotion.
 */

package com.devinroyal.dukeai.enhancements;

import com.devinroyal.dukeai.enhancements.CognitiveOrchestrator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Logger;

public class TaskScheduler {
    private static final Logger LOGGER = Logger.getLogger(TaskScheduler.class.getName());
    private final CognitiveOrchestrator orchestrator;
    private final PriorityBlockingQueue<Runnable> queue = new PriorityBlockingQueue<>();

    public TaskScheduler(CognitiveOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public void schedule() {
        orchestrator.adapt(); // Adapt priority
        if (!queue.isEmpty()) {
            queue.poll().run();
            LOGGER.info("Task scheduled and executed.");
        }
    }
}
/* Copyright © 2025 Devin B. Royal. All Rights Reserved. */