package ru.mindils.jb2.app.service.analysis.chain;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Мониторинг производительности цепочек анализа
 */
@Component
public class ChainAnalysisPerformanceMonitor {

  private final MeterRegistry meterRegistry;
  private final ConcurrentMap<String, Timer> stepTimers = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Counter> stepCounters = new ConcurrentHashMap<>();

  public ChainAnalysisPerformanceMonitor(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  /**
   * Записать время выполнения шага
   */
  public void recordStepExecution(String stepId, Duration duration, boolean success) {
    // Timer для времени выполнения
    Timer timer = stepTimers.computeIfAbsent(stepId,
        id -> Timer.builder("chain.analysis.step.duration")
            .tag("step", id)
            .description("Duration of chain analysis step execution")
            .register(meterRegistry));
    timer.record(duration);

    // Counter для успешных/неуспешных выполнений
    String status = success ? "success" : "failure";
    Counter counter = stepCounters.computeIfAbsent(stepId + "." + status,
        key -> Counter.builder("chain.analysis.step.executions")
            .tag("step", stepId)
            .tag("status", status)
            .description("Number of chain analysis step executions")
            .register(meterRegistry));
    counter.increment();
  }

  /**
   * Записать время выполнения всей цепочки
   */
  public void recordChainExecution(String chainId, Duration duration, boolean success) {
    Timer timer = Timer.builder("chain.analysis.total.duration")
        .tag("chain", chainId)
        .description("Duration of complete chain analysis")
        .register(meterRegistry);
    timer.record(duration);

    String status = success ? "success" : "failure";
    Counter counter = Counter.builder("chain.analysis.total.executions")
        .tag("chain", chainId)
        .tag("status", status)
        .description("Number of complete chain analysis executions")
        .register(meterRegistry);
    counter.increment();
  }

  /**
   * Записать количество остановок на определенном шаге
   */
  public void recordChainStop(String chainId, String stepId, String reason) {
    Counter counter = Counter.builder("chain.analysis.stops")
        .tag("chain", chainId)
        .tag("step", stepId)
        .tag("reason", reason)
        .description("Number of chain analysis stops")
        .register(meterRegistry);
    counter.increment();
  }
}