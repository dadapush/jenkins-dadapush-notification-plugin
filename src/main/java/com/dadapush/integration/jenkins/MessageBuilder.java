package com.dadapush.integration.jenkins;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;

import hudson.EnvVars;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.util.LogTaskListener;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;

public class MessageBuilder {

  private static final Logger LOGGER = Logger.getLogger(MessageBuilder.class.getName());

  public static final String STARTING_STATUS_MESSAGE = "Starting...",
      END_STATUS_MESSAGE = "Finished",
      BACK_TO_NORMAL_STATUS_MESSAGE = "Back to normal",
      STILL_FAILING_STATUS_MESSAGE = "Still Failing",
      SUCCESS_STATUS_MESSAGE = "Success",
      FAILURE_STATUS_MESSAGE = "Failure",
      ABORTED_STATUS_MESSAGE = "Aborted",
      NOT_BUILT_STATUS_MESSAGE = "Not built",
      UNSTABLE_STATUS_MESSAGE = "Unstable",
      UNKNOWN_STATUS_MESSAGE = "Unknown";

  private StringBuffer message;
  private DaDaPushNotifier notifier;
  private AbstractBuild build;
  private boolean finished;

  MessageBuilder(DaDaPushNotifier notifier, AbstractBuild build, boolean finished) {
    this.notifier = notifier;
    this.message = new StringBuffer();
    this.build = build;
    this.finished = finished;
    startMessage();
  }

  MessageBuilder appendStatusMessage() {
    message.append(this.getStatusMessage());
    return this;
  }

  String getTitle() {
    StringBuilder titleBuilder = new StringBuilder();
    titleBuilder.append("#").append(build.getNumber()).append(" ")
        .append(getStatusMessage());

    return titleBuilder.toString();
  }

  String getContent() {
    AbstractProject<?, ?> project = build.getProject();

    StringBuilder contentBuilder = new StringBuilder();

    contentBuilder.append("Job Name: ")
        .append(project.getFullDisplayName()).append("\n");
    if (build != null) {
      contentBuilder.append("Build Number: ").append(build.getNumber()).append("\n");
      String statusMessage = getStatusMessage();
      if (!statusMessage.equals(STARTING_STATUS_MESSAGE)) {
        String durationString;
        if (statusMessage.contains(BACK_TO_NORMAL_STATUS_MESSAGE)) {
          durationString = createBackToNormalDurationString();
        } else {
          durationString = build.getDurationString().replace("and counting", "");
        }
        contentBuilder.append("Build Duration: ").append(durationString).append("\n");
      }
      String url = DisplayURLProvider.get().getRunURL(build);

      if (notifier.getIncludeCustomMessage()) {
        String expandedCustomMessage = getExpandedCustomMessage(build);
        contentBuilder.append(expandedCustomMessage).append("\n");
      }
    }

    return contentBuilder.toString();
  }

  private static final Logger logger = Logger.getLogger(MessageBuilder.class.getName());

  String getExpandedCustomMessage(AbstractBuild build) {
    String result = "";
    if (notifier.getIncludeCustomMessage()) {
      String customMessage = notifier.getCustomMessage();
      EnvVars envVars = new EnvVars();
      try {
        envVars = build.getEnvironment(new LogTaskListener(logger, INFO));
      } catch (IOException | InterruptedException e) {
        logger.log(SEVERE, e.getMessage(), e);
      }
      result = envVars.expand(customMessage);
    }
    return result;
  }

  @SuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  String getStatusMessage() {
    Result result = this.build.getResult();
    Result previousResult;
    Run previousBuild = this.build.getProject().getLastBuild().getPreviousBuild();
    Run previousSuccessfulBuild = this.build.getPreviousSuccessfulBuild();
    boolean buildHasSucceededBefore = previousSuccessfulBuild != null;

    /*
     * If the last build was aborted, go back to find the last non-aborted build.
     * This is so that aborted builds do not affect build transitions.
     * I.e. if build 1 was failure, build 2 was aborted and build 3 was a success the transition
     * should be failure -> success (and therefore back to normal) not aborted -> success.
     */
    Run lastNonAbortedBuild = previousBuild;
    while (lastNonAbortedBuild != null && lastNonAbortedBuild.getResult() == Result.ABORTED) {
      lastNonAbortedBuild = lastNonAbortedBuild.getPreviousBuild();
    }


    /* If all previous builds have been aborted, then use
     * SUCCESS as a default status so an aborted message is sent
     */
    if (lastNonAbortedBuild == null) {
      previousResult = Result.SUCCESS;
    } else {
      previousResult = lastNonAbortedBuild.getResult();
    }

    /* Back to normal should only be shown if the build has actually succeeded at some point.
     * Also, if a build was previously unstable and has now succeeded the status should be
     * "Back to normal"
     */
    if (result == Result.SUCCESS
        && (previousResult == Result.FAILURE || previousResult == Result.UNSTABLE)
        && buildHasSucceededBefore) {
      return BACK_TO_NORMAL_STATUS_MESSAGE;
    }
    if (result == Result.FAILURE && previousResult == Result.FAILURE) {
      return STILL_FAILING_STATUS_MESSAGE;
    }
    if (result == Result.SUCCESS) {
      return SUCCESS_STATUS_MESSAGE;
    }
    if (result == Result.FAILURE) {
      return FAILURE_STATUS_MESSAGE;
    }
    if (result == Result.ABORTED) {
      return ABORTED_STATUS_MESSAGE;
    }
    if (result == Result.NOT_BUILT) {
      return NOT_BUILT_STATUS_MESSAGE;
    }
    if (result == Result.UNSTABLE) {
      return UNSTABLE_STATUS_MESSAGE;
    }

    if (this.build.isBuilding()) {
      if (this.finished) {
        return END_STATUS_MESSAGE;
      } else {
        return STARTING_STATUS_MESSAGE;
      }
    }
    return UNKNOWN_STATUS_MESSAGE;
  }

  MessageBuilder append(String string) {
    message.append(this.escape(string));
    return this;
  }

  MessageBuilder append(Object string) {
    message.append(this.escape(string.toString()));
    return this;
  }

  private MessageBuilder startMessage() {
    message.append(this.escape(build.getProject().getFullDisplayName()));
    message.append(" - ");
    message.append(this.escape(build.getDisplayName()));
    message.append(" ");
    return this;
  }

  MessageBuilder appendDuration() {
    message.append(" after ");

    return this;
  }

  MessageBuilder appendTestSummary() {
    AbstractTestResultAction<?> action = this.build
        .getAction(AbstractTestResultAction.class);
    if (action != null) {
      int total = action.getTotalCount();
      int failed = action.getFailCount();
      int skipped = action.getSkipCount();
      message.append("\nTest Status:\n");
      message.append("\tPassed: " + (total - failed - skipped));
      message.append(", Failed: " + failed);
      message.append(", Skipped: " + skipped);
    } else {
      message.append("\nNo Tests found.");
    }
    return this;
  }

  MessageBuilder appendCustomMessage() {
    String customMessage = notifier.getCustomMessage();
    EnvVars envVars = new EnvVars();
    try {
      envVars = build.getEnvironment(new LogTaskListener(LOGGER, INFO));
    } catch (IOException e) {
      LOGGER.log(SEVERE, e.getMessage(), e);
    } catch (InterruptedException e) {
      LOGGER.log(SEVERE, e.getMessage(), e);
    }
    message.append("\n");
    message.append(envVars.expand(customMessage));
    return this;
  }

  MessageBuilder appendLog() {
    List<String> logs = null;
    try {
      logs = build.getLog(100);
    } catch (IOException e) {
      LOGGER.log(SEVERE, e.getMessage(), e);
    }
    if (logs != null) {
      for (String log : logs) {
        message.append("\n");
        message.append(log);
      }
    }
    return this;
  }

  @SuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private String createBackToNormalDurationString() {
    Run previousSuccessfulBuild = build.getPreviousSuccessfulBuild();
    long previousSuccessStartTime = previousSuccessfulBuild.getStartTimeInMillis();
    long previousSuccessDuration = previousSuccessfulBuild.getDuration();
    long previousSuccessEndTime = previousSuccessStartTime + previousSuccessDuration;
    long buildStartTime = build.getStartTimeInMillis();
    long buildDuration = build.getDuration();
    long buildEndTime = buildStartTime + buildDuration;
    long backToNormalDuration = buildEndTime - previousSuccessEndTime;
    return Util.getTimeSpanString(backToNormalDuration);
  }

  String escape(String string) {
    string = string.replace("&", "&amp;");
    string = string.replace("<", "&lt;");
    string = string.replace(">", "&gt;");

    return string;
  }

  public String toString() {
    return message.toString();
  }
}