package io.jaegertracing.internal;

import io.jaegertracing.internal.clock.Clock;
import io.jaegertracing.internal.metrics.Metrics;
import io.jaegertracing.spi.BaggageRestrictionManager;
import io.jaegertracing.spi.Reporter;
import io.jaegertracing.spi.Sampler;
import io.opentracing.ScopeManager;

import java.util.List;
import java.util.Map;

public class JaegerObjectFactory {
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
}
