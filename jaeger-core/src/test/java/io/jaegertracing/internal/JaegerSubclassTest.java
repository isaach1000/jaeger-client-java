package io.jaegertracing.internal;

import io.jaegertracing.internal.clock.Clock;
import io.jaegertracing.internal.metrics.Metrics;
import io.jaegertracing.spi.BaggageRestrictionManager;
import io.jaegertracing.spi.Reporter;
import io.jaegertracing.spi.Sampler;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JaegerSubclassTest {
  private static class Configuration extends io.jaegertracing.Configuration {
    Configuration(io.jaegertracing.Configuration config) {
      this(config.getServiceName());
      withCodec(config.getCodec());
      withMetricsFactory(config.getMetricsFactory());
      withReporter(config.getReporter());
      withSampler(config.getSampler());
      withTracerTags(config.getTracerTags());
    }

    public Configuration(String serviceName) {
      super(serviceName);
    }

    public static Configuration fromEnv() {
      return new Configuration(io.jaegertracing.Configuration.fromEnv());
    }

    @Override
    public synchronized Tracer getTracer() {
      return new Tracer(super.getTracer());
    }
  }

  private static class Tracer extends JaegerTracer {
    public class SpanBuilder extends JaegerTracer.SpanBuilder {
      protected SpanBuilder(String operationName) {
        super(operationName);
      }

      @Override
      public Span start() {
        return new Span(super.start());
      }

      @Override
      public Scope startActive(boolean finishSpanOnClose) {
        return scopeManager().activate(start(), finishSpanOnClose);
      }
    }

    Tracer(io.jaegertracing.internal.JaegerTracer tracer) {
      this(
          tracer.getServiceName(),
          tracer.getReporter(),
          tracer.getSampler(),
          tracer.getRegistry(),
          tracer.clock(),
          tracer.getMetrics(),
          new HashMap<>(tracer.tags()),
          tracer.isZipkinSharedRpcSpan(),
          tracer.scopeManager(),
          tracer.getBaggageRestrictionManager(),
          tracer.isExpandExceptionLogs());
    }

    public Tracer(
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
      super(
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
          expandExceptionLogs);
    }

    @Override
    public SpanBuilder buildSpan(String operationName) {
      return new SpanBuilder(operationName);
    }
  }

  private static class Span extends JaegerSpan {
    private final SpanContext ctx;

    Span(JaegerSpan span) {
      this(
          span.getTracer(),
          span.getOperationName(),
          new SpanContext(span.context()),
          span.getStart(),
          span.getStartTimeNanoTicks(),
          span.getComputeDurationViaNanoTicks(),
          span.getTags(),
          span.getReferences());
    }

    public Span(
        JaegerTracer tracer,
        String operationName,
        JaegerSpanContext context,
        long startTimeMicroseconds,
        long startTimeNanoTicks,
        boolean computeDurationViaNanoTicks,
        Map<String, Object> tags,
        List<Reference> references) {
      super(
          tracer,
          operationName,
          context,
          startTimeMicroseconds,
          startTimeNanoTicks,
          computeDurationViaNanoTicks,
          tags,
          references);
      ctx = new SpanContext(context);
    }

    @Override
    public SpanContext context() {
      return ctx;
    }
  }

  private static class SpanContext extends JaegerSpanContext {
    SpanContext(JaegerSpanContext ctx) {
      this(
          ctx.getTraceId(),
          ctx.getSpanId(),
          ctx.getParentId(),
          ctx.getFlags(),
          ctx.baggage(),
          ctx.getDebugId());
    }

    public SpanContext(
        long traceId,
        long spanId,
        long parentId,
        byte flags,
        Map<String, String> baggage,
        String debugId) {
      super(traceId, spanId, parentId, flags, baggage, debugId);
    }
  }

  @Test
  public void testTracer() {
    final Configuration config = new Configuration("test-service");
    final Tracer tracer = config.getTracer();
    final Scope scope = tracer.buildSpan("test-operation").startActive(true);
    Assert.assertNotNull(tracer.scopeManager().active());
    scope.close();
    config.closeTracer();
  }
}
