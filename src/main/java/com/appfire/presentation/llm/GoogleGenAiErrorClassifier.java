package com.appfire.presentation.llm;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GoogleGenAiErrorClassifier {

    private static final Pattern HTTP_STATUS = Pattern.compile("\\b(4\\d{2}|5\\d{2})\\b");

    enum FailureKind {
        RETRY_EXP_BACKOFF,
        RETRY_JITTER,
        FAIL_NO_RETRY,
        FAIL_AUTH
    }

    record Classification(FailureKind kind, Integer httpStatus) {}

    private GoogleGenAiErrorClassifier() {}

    static Classification classify(Throwable error) {
        Integer status = extractHttpStatus(error);
        if (status != null) {
            if (status == 429) {
                return new Classification(FailureKind.RETRY_EXP_BACKOFF, status);
            }
            if (status == 400) {
                return new Classification(FailureKind.FAIL_NO_RETRY, status);
            }
            if (status == 401 || status == 403) {
                return new Classification(FailureKind.FAIL_AUTH, status);
            }
            if (status == 408 || status == 503 || status >= 500) {
                return new Classification(FailureKind.RETRY_JITTER, status);
            }
        }
        String message = messageLower(error);
        if (message.contains("401") || message.contains("403") || message.contains("unauthenticated")
                || message.contains("permission denied")) {
            return new Classification(FailureKind.FAIL_AUTH, status);
        }
        if (message.contains("400") || message.contains("invalid argument")) {
            return new Classification(FailureKind.FAIL_NO_RETRY, status);
        }
        if (message.contains("429") || message.contains("rate limit")) {
            return new Classification(FailureKind.RETRY_EXP_BACKOFF, status);
        }
        if (message.contains("timeout") || message.contains("timed out") || message.contains("503")
                || message.contains("unavailable") || message.contains("408")) {
            return new Classification(FailureKind.RETRY_JITTER, status);
        }
        return new Classification(FailureKind.RETRY_JITTER, status);
    }

    static Classification classifyEmptyResponse() {
        return new Classification(FailureKind.RETRY_JITTER, null);
    }

    static Classification classifyMalformedJson() {
        return new Classification(FailureKind.RETRY_JITTER, null);
    }

    static Integer extractHttpStatus(Throwable error) {
        Throwable current = error;
        while (current != null) {
            Integer fromType = statusFromExceptionType(current);
            if (fromType != null) {
                return fromType;
            }
            Integer fromMessage = statusFromMessage(current.getMessage());
            if (fromMessage != null) {
                return fromMessage;
            }
            current = current.getCause();
        }
        return null;
    }

    private static Integer statusFromExceptionType(Throwable error) {
        String name = error.getClass().getName();
        if (name.endsWith("ClientException") || name.endsWith("ApiException") || name.endsWith("HttpException")) {
            try {
                var method = error.getClass().getMethod("code");
                Object code = method.invoke(error);
                if (code instanceof Number number) {
                    return number.intValue();
                }
            } catch (ReflectiveOperationException ignored) {
                // fall through
            }
            try {
                var method = error.getClass().getMethod("statusCode");
                Object code = method.invoke(error);
                if (code instanceof Number number) {
                    return number.intValue();
                }
            } catch (ReflectiveOperationException ignored) {
                // fall through
            }
        }
        return null;
    }

    private static Integer statusFromMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        Matcher matcher = HTTP_STATUS.matcher(message);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private static String messageLower(Throwable error) {
        String message = error.getMessage();
        return message == null ? "" : message.toLowerCase(Locale.ROOT);
    }
}
