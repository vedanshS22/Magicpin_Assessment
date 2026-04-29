package com.magicpin.mde.domain.reply;

import org.springframework.stereotype.Component;

@Component
public class IntentClassifier {

  public IntentResult classify(String text) {
    String s = text == null ? "" : text.trim().toLowerCase();

    if (s.isBlank()) {
      return new IntentResult(IntentType.UNKNOWN, null, "");
    }

    // STOP / unsubscribe intent
    if (matchesAny(
            s,
            "stop",
            "unsubscribe",
            "don't message",
            "do not message",
            "remove me",
            "stop messaging"
    )) {
      return new IntentResult(
              IntentType.STOP,
              ObjectionType.NOT_INTERESTED,
              s
      );
    }

    // POSITIVE / approval intent
    if (matchesAny(
            s,
            "yes",
            "y",
            "ok",
            "okay",
            "launch",
            "go ahead",
            "sure",
            "approve",
            "start",
            "start this campaign",
            "yes start",
            "yes, start",
            "proceed"
    )) {
      return new IntentResult(
              IntentType.AFFIRM,
              null,
              s
      );
    }

    // NEGATIVE / decline intent
    if (matchesAny(
            s,
            "no",
            "n",
            "not now",
            "don't",
            "do not",
            "skip",
            "not interested"
    )) {

      ObjectionType obj = null;

      if (
              s.contains("later")
                      || s.contains("tomorrow")
                      || s.contains("next week")
      ) {
        obj = ObjectionType.TIMING;
      }

      if (
              s.contains("cost")
                      || s.contains("expensive")
                      || s.contains("discount")
                      || s.contains("price")
      ) {
        obj = ObjectionType.PRICE;
      }

      if (
              s.contains("already")
                      || s.contains("too many")
                      || s.contains("spam")
      ) {
        obj = ObjectionType.SATURATION;
      }

      return new IntentResult(
              IntentType.DECLINE,
              obj,
              s
      );
    }

    // LATER intent
    if (matchesAny(
            s,
            "later",
            "tomorrow",
            "next week",
            "next month",
            "after",
            "remind me"
    )) {
      return new IntentResult(
              IntentType.LATER,
              ObjectionType.TIMING,
              s
      );
    }

    // MORE INFO intent
    if (matchesAny(
            s,
            "tell me more",
            "details",
            "how",
            "what",
            "explain",
            "why"
    )) {
      return new IntentResult(
              IntentType.MORE_INFO,
              ObjectionType.NEED_DETAILS,
              s
      );
    }

    // soft objection detection
    ObjectionType obj = null;

    if (
            s.contains("price")
                    || s.contains("cost")
                    || s.contains("budget")
    ) {
      obj = ObjectionType.PRICE;
    }

    if (
            s.contains("trust")
                    || s.contains("genuine")
                    || s.contains("quality")
    ) {
      obj = ObjectionType.TRUST;
    }

    if (
            s.contains("later")
                    || s.contains("timing")
    ) {
      obj = ObjectionType.TIMING;
    }

    return new IntentResult(
            IntentType.UNKNOWN,
            obj,
            s
    );
  }

  private static boolean matchesAny(String s, String... tokens) {
    for (String t : tokens) {
      if (
              s.equals(t)
                      || s.startsWith(t + " ")
                      || s.contains(" " + t + " ")
                      || s.contains(t)
      ) {
        return true;
      }
    }
    return false;
  }
}