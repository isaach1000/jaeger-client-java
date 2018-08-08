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

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JaegerSubclassTest {
  private static class CustomConfiguration extends Configuration {
    public CustomConfiguration(String serviceName, JaegerObjectFactory objectFactory) {
      super(serviceName);
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
    protected CustomTracer.CustomBuilder createTracerBuilder(String serviceName) {
      return new CustomTracer.CustomBuilder(serviceName, new CustomObjectFactory());
    }
  }

  private static class CustomTracer extends JaegerTracer {
    public static class CustomBuilder extends JaegerTracer.Builder {
      public CustomBuilder(String serviceName, JaegerObjectFactory objectFactory) {
        super(serviceName, objectFactory);
      }

      @Override
      public CustomTracer build() {
        return (CustomTracer) super.build();
      }

      @Override
      protected JaegerTracer createTracer(String serviceName,
                                          Reporter reporter,
                                          Sampler sampler,
                                          PropagationRegistry registry,
                                          Clock clock,
                                          Metrics metrics,
                                          Map<String, Object> tags,
                                          boolean zipkinSharedRpcSpan,
                                          ScopeManager scopeManager,
                                          BaggageRestrictionManager baggageRestrictionManager,
                                          boolean expandExceptionLogs,
                                          JaegerObjectFactory objectFactory) {
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
            expandExceptionLogs,
            objectFactory);
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
        boolean expandExceptionLogs,
        JaegerObjectFactory objectFactory) {
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
          expandExceptionLogs,
          objectFactory);
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
        String debugId,
        JaegerObjectFactory objectFactory) {
      super(traceId, spanId, parentId, flags, baggage, debugId, objectFactory);
    }

    @Override
    public CustomSpanContext withFlags(byte flags) {
      return (CustomSpanContext) super.withFlags(flags);
    }

    @Override
    public CustomSpanContext withBaggage(Map<String, String> baggage) {
      return (CustomSpanContext) super.withBaggage(baggage);
    }

    @Override
    public CustomSpanContext withBaggageItem(String key, String value) {
      return (CustomSpanContext) super.withBaggageItem(key, value);
    }
  }

  private static class CustomObjectFactory extends JaegerObjectFactory {
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
      return new CustomSpanContext(traceId, spanId, parentId, flags, baggage, debugId, this);
    }

    @Override
    public CustomTracer.CustomSpanBuilder createSpanBuilder(JaegerTracer tracer, String operationName) {
      return ((CustomTracer)tracer).new CustomSpanBuilder(operationName);
    }
  }

  @Test
  public void testTracer() {
    final CustomObjectFactory tracingObjectFactory = new CustomObjectFactory();
    final CustomConfiguration config = new CustomConfiguration("test-service", tracingObjectFactory);
    final CustomTracer.CustomBuilder builder = config.getTracerBuilder();
    final CustomTracer tracer = builder.build();
    final Scope scope = tracer.buildSpan("test-operation").startActive(true);
    Assert.assertNotNull(tracer.scopeManager().active());
    Assert.assertTrue(tracer.scopeManager().active().span() instanceof CustomSpan);
    Assert.assertTrue(tracer.scopeManager().active().span().context() instanceof CustomSpanContext);
    final CustomSpanContext ctx = (CustomSpanContext) tracer.scopeManager().active().span().context();
    CustomSpanContext ctxCopy = ctx.withFlags((byte) 1);
    ctxCopy = ctxCopy.withBaggage(Collections.emptyMap());
    ctxCopy = ctxCopy.withBaggageItem("hello", "world");
    Assert.assertNotSame(ctx, ctxCopy);
    scope.close();
    config.closeTracer();
  }
}
