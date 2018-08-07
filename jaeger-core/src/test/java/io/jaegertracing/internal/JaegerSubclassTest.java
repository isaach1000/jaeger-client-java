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

    public static CustomConfiguration fromEnv(String serviceName) {
      CustomConfiguration config = new CustomConfiguration(serviceName);
      config.initFromEnv();
      return config;
    }

    @Override
    public CustomTracer.CustomBuilder getTracerBuilder() {
      return (CustomTracer.CustomBuilder) super.getTracerBuilder();
    }

    @Override
    public synchronized CustomTracer getTracer() {
      return (CustomTracer) super.getTracer();
    }

    @Override
    protected TracingFactory tracingFactory() {
      return new CustomTracingFactory();
    }
  }

  private static class CustomTracer extends JaegerTracer {
    public static class CustomBuilder extends JaegerTracer.Builder {
      public CustomBuilder(String serviceName, TracingFactory tracingFactory) {
        super(serviceName, tracingFactory);
      }

      @Override
      public CustomTracer build() {
        return (CustomTracer) super.build();
      }
    }

    public class CustomSpanBuilder extends JaegerTracer.SpanBuilder {
      protected CustomSpanBuilder(String operationName) {
        super(operationName);
      }

      @Override
      public CustomSpan start() {
        return (CustomSpan) super.start();
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
    protected CustomTracingFactory tracingFactory() {
      return new CustomTracingFactory();
    }
  }

  private static class CustomSpan extends JaegerSpan {
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
    }

    @Override
    public CustomSpanContext context() {
      return (CustomSpanContext) super.context();
    }
  }

  private static class CustomSpanContext extends JaegerSpanContext {
    public CustomSpanContext(
        long traceId,
        long spanId,
        long parentId,
        byte flags,
        Map<String, String> baggage,
        String debugId) {
      super(traceId, spanId, parentId, flags, baggage, debugId);
    }

    @Override
    protected CustomTracingFactory tracingFactory() {
      return new CustomTracingFactory();
    }
  }

  private static class CustomTracingFactory extends TracingFactory {
    @Override
    public CustomTracer createTracer(
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
      return new CustomTracer(
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
    public CustomSpan createSpan(
        JaegerTracer tracer,
        String operationName,
        JaegerSpanContext context,
        long startTimeMicroseconds,
        long startTimeNanoTicks,
        boolean computeDurationViaNanoTicks,
        Map<String, Object> tags,
        List<Reference> references) {
      return new CustomSpan(
          tracer,
          operationName,
          context,
          startTimeMicroseconds,
          startTimeNanoTicks,
          computeDurationViaNanoTicks,
          tags,
          references);
    }

    @Override
    public CustomSpanContext createSpanContext(long traceId,
                                               long spanId,
                                               long parentId,
                                               byte flags,
                                               Map<String, String> baggage,
                                               String debugId) {
      return new CustomSpanContext(traceId, spanId, parentId, flags, baggage, debugId);
    }

    @Override
    public CustomTracer.CustomSpanBuilder createSpanBuilder(JaegerTracer tracer, String operationName) {
      return ((CustomTracer)tracer).new CustomSpanBuilder(operationName);
    }

    @Override
    public CustomTracer.CustomBuilder createTracerBuilder(String serviceName) {
      return new CustomTracer.CustomBuilder(serviceName, this);
    }
  }

  @Test
  public void testTracer() {
    final CustomConfiguration config = CustomConfiguration.fromEnv("test-service");
    final CustomTracer.CustomBuilder builder = config.getTracerBuilder();
    final CustomTracer tracer = builder.build();
    final Scope scope = tracer.buildSpan("test-operation").startActive(true);
    Assert.assertNotNull(tracer.scopeManager().active());
    Assert.assertTrue(tracer.scopeManager().active().span() instanceof CustomSpan);
    Assert.assertTrue(tracer.scopeManager().active().span().context() instanceof CustomSpanContext);
    scope.close();
    config.closeTracer();
  }
}
