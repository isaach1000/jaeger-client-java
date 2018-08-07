package io.jaegertracing.internal;

import io.jaegertracing.Configuration;
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
  private static class CustomConfiguration extends Configuration {
    public CustomConfiguration(String serviceName) {
      super(serviceName);
    }

    public static Configuration fromEnv() {
      CustomConfiguration config = new CustomConfiguration(getProperty(JAEGER_SERVICE_NAME));
      config.initFromEnv();
      return config;
    }

    @Override
    public CustomTracer.Builder getTracerBuilder() {
      return (CustomTracer.Builder) super.getTracerBuilder();
    }

    @Override
    public CustomTracer getTracer() {
      return (CustomTracer) super.getTracer();
    }

    @Override
    protected CustomTracer.Builder createTracerBuilder(String serviceName) {
      return new CustomTracer.Builder(serviceName);
    }
  }

  private static class CustomTracer extends JaegerTracer {
    public static class Builder extends JaegerTracer.Builder {
      public Builder(String serviceName) {
        super(serviceName);
      }

      @Override
      protected CustomTracer createTracer(
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
          return new CustomTracer(serviceName, reporter, sampler, registry, clock, metrics, tags,
          zipkinSharedRpcSpan, scopeManager, baggageRestrictionManager, expandExceptionLogs);
        }
    }

    public class SpanBuilder extends JaegerTracer.SpanBuilder {
      protected SpanBuilder(String operationName) {
        super(operationName);
      }

      @Override
      public CustomSpan start() {
        return new CustomSpan(super.start());
      }

      @Override
      public Scope startActive(boolean finishSpanOnClose) {
        return scopeManager().activate(start(), finishSpanOnClose);
      }
    }

    public CustomTracer(
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

  private static class CustomSpan extends JaegerSpan {
    private final CustomSpanContext ctx;

    CustomSpan(JaegerSpan span) {
      this(
          span.getTracer(),
          span.getOperationName(),
          new CustomSpanContext(span.context()),
          span.getStart(),
          span.getStartTimeNanoTicks(),
          span.getComputeDurationViaNanoTicks(),
          span.getTags(),
          span.getReferences());
    }

    public CustomSpan(
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
      ctx = new CustomSpanContext(context);
    }

    @Override
    public CustomSpanContext context() {
      return ctx;
    }
  }

  private static class CustomSpanContext extends JaegerSpanContext {
    CustomSpanContext(JaegerSpanContext ctx) {
      this(
          ctx.getTraceId(),
          ctx.getSpanId(),
          ctx.getParentId(),
          ctx.getFlags(),
          ctx.baggage(),
          ctx.getDebugId());
    }

    public CustomSpanContext(
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
    final CustomConfiguration config = new CustomConfiguration("test-service");
    final CustomTracer tracer = config.getTracer();
    final Scope scope = tracer.buildSpan("test-operation").startActive(true);
    Assert.assertNotNull(tracer.scopeManager().active());
    scope.close();
    config.closeTracer();
  }
}
