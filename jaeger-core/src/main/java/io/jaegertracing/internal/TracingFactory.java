package io.jaegertracing.internal;

import io.jaegertracing.internal.clock.Clock;
import io.jaegertracing.internal.metrics.Metrics;
import io.jaegertracing.spi.BaggageRestrictionManager;
import io.jaegertracing.spi.Reporter;
import io.jaegertracing.spi.Sampler;
import io.opentracing.ScopeManager;

import java.util.List;
import java.util.Map;

public class TracingFactory {
  public JaegerTracer createTracer(
      String serviceName,
      Reporter reporter,
      Sampler sampler,
      PropagationRegistry registry,
      Clock clock,
      Metrics metrics,
      Map<String, Object> tags,
      boolean zipkinSharedRpcSpan,
      ScopeManager scopeManager,
      BaggageRestrictionManager baggageRestrictionManager,
      boolean expandExceptionLogs) {
    return new JaegerTracer(
        serviceName,
        reporter,
        sampler,
        registry,
        clock,
        metrics,
        tags,
        zipkinSharedRpcSpan,
        scopeManager,
        baggageRestrictionManager,
        expandExceptionLogs,
        this);
  }

  public JaegerSpan createSpan(
      JaegerTracer tracer,
      String operationName,
      JaegerSpanContext context,
      long startTimeMicroseconds,
      long startTimeNanoTicks,
      boolean computeDurationViaNanoTicks,
      Map<String, Object> tags,
      List<Reference> references) {
    return new JaegerSpan(
        tracer,
        operationName,
        context,
        startTimeMicroseconds,
        startTimeNanoTicks,
        computeDurationViaNanoTicks,
        tags,
        references);
  }

  public JaegerSpanContext createSpanContext(long traceId,
                                             long spanId,
                                             long parentId,
                                             byte flags,
                                             Map<String, String> baggage,
                                             String debugId) {
    return new JaegerSpanContext(traceId, spanId, parentId, flags, baggage, debugId, this);
  }

  public JaegerTracer.SpanBuilder createSpanBuilder(JaegerTracer tracer, String operationName) {
    return tracer.new SpanBuilder(operationName);
  }

  public JaegerTracer.Builder createTracerBuilder(String serviceName) {
    return new JaegerTracer.Builder(serviceName, this);
  }
}
