package com.star.api.compiler;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

/**
 * Simplify the message print.
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/22 上午11:48
 */
public class Logger {
    private Messager msg;

    public Logger(Messager messager) {
        msg = messager;
    }

    /**
     * Print info log.
     */
    public void info(CharSequence info) {
        if (isNotEmpty(info)) {
            msg.printMessage(Diagnostic.Kind.NOTE, "::API " + info);
        }
    }

    public void error(CharSequence error) {
        if (isNotEmpty(error)) {
            msg.printMessage(Diagnostic.Kind.ERROR, "::API " + "An exception is encountered, [" + error + "]");
        }
    }

    public void error(Throwable error) {
        if (null != error) {
            msg.printMessage(Diagnostic.Kind.ERROR, "::API " + "An exception is encountered, [" + error.getMessage() + "]" + "\n" + formatStackTrace(error.getStackTrace()));
        }
    }

    public void warning(CharSequence warning) {
        if (isNotEmpty(warning)) {
            msg.printMessage(Diagnostic.Kind.WARNING, "::API " + warning);
        }
    }

    private String formatStackTrace(StackTraceElement[] stackTrace) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : stackTrace) {
            sb.append("    at ").append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    private boolean isNotEmpty(CharSequence value) {
        return value != null && value.length() > 0;
    }
}