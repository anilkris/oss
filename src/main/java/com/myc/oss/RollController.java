package com.myc.oss;


import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.Random;

import io.opentelemetry.api.metrics.Meter;

@RestController
public class RollController {
  private static final Logger logger = LoggerFactory.getLogger(RollController.class);

    private final AttributeKey<String> ATTR_METHOD = AttributeKey.stringKey("method");

  private final Random random = new Random();
  private final Tracer tracer;
   private final LongHistogram doWorkHistogram;


    @Autowired
  RollController(OpenTelemetry openTelemetry) {
    tracer = openTelemetry.getTracer(RollController.class.getName(), "0.1.0");
      Meter meter = openTelemetry.getMeter(RollController.class.getName());
    doWorkHistogram = meter.histogramBuilder("do-work").ofLongs().build();
  }
 @GetMapping("/rolldice")
  public List<Integer> index(@RequestParam("player") Optional<String> player,
      @RequestParam("rolls") Optional<Integer> rolls) {
    Span span = tracer.spanBuilder("rollTheDice").startSpan();

    // Make the span the current span
    try (Scope scope = span.makeCurrent()) {

      if (!rolls.isPresent()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing rolls parameter", null);
      }

      List<Integer> result = new Dice(1, 6).rollTheDice(rolls.get());

      if (player.isPresent()) {
        logger.info("{} is rolling the dice: {}", player.get(), result);
      } else {
        logger.info("Anonymous player is rolling the dice: {}", result);
      }
      return result;
    } catch(Throwable t) {
      span.recordException(t);
      throw t;
    } finally {
      span.end();
    }
  }


  @GetMapping("/ping")
  public String ping() throws InterruptedException {
    int sleepTime = random.nextInt(200);
    doWork(sleepTime);
    doWorkHistogram.record(sleepTime, Attributes.of(ATTR_METHOD, "ping"));
    return "pong";
  }

  private void doWork(int sleepTime) throws InterruptedException {
    Span span = tracer.spanBuilder("doWork").startSpan();
    try (Scope ignored = span.makeCurrent()) {
      Thread.sleep(sleepTime);
      logger.info("A sample log message!");
    } finally {
      span.end();
    }
  }
}