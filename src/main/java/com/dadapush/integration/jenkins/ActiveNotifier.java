package com.dadapush.integration.jenkins;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import java.util.logging.Logger;


@SuppressWarnings("rawtypes")
public class ActiveNotifier implements FineGrainedNotifier {

  private static final Logger logger = Logger.getLogger(ActiveNotifier.class.getName());

  private DaDaPushNotifier notifier;
  private BuildListener listener;

  public ActiveNotifier(DaDaPushNotifier notifier, BuildListener listener) {
    super();
    this.notifier = notifier;
    this.listener = listener;
  }

  private DaDaPushService getDaDaPushService(AbstractBuild r) {
    return notifier.newDaDaPushService(r, listener);
  }

  public void deleted(AbstractBuild r) {
  }

  public void started(AbstractBuild build) {
    notifyStart(build);
  }

  private void notifyStart(AbstractBuild build) {
    MessageBuilder messageBuilder = new MessageBuilder(notifier, build, false);
    String title = messageBuilder.getTitle();
    String content = messageBuilder.getContent();
    getDaDaPushService(build).publish(title, content);
  }

  public void finalized(AbstractBuild r) {
  }

  public void completed(AbstractBuild r) {
    AbstractProject<?, ?> project = r.getProject();
    Result result = r.getResult();
    AbstractBuild<?, ?> previousBuild = project.getLastBuild();
    if (previousBuild != null) {
      do {
        previousBuild = previousBuild.getPreviousCompletedBuild();
      } while (previousBuild != null && previousBuild.getResult() == Result.ABORTED);
    }
    Result previousResult = (previousBuild != null) ? previousBuild.getResult() : Result.SUCCESS;
    if ((result == Result.ABORTED && notifier.getNotifyAborted())
        || (result == Result.FAILURE //notify only on single failed build
        && previousResult != Result.FAILURE
        && notifier.getNotifyFailure())
        || (result == Result.FAILURE //notify only on repeated failures
        && previousResult == Result.FAILURE
        && notifier.getNotifyRepeatedFailure())
        || (result == Result.NOT_BUILT && notifier.getNotifyNotBuilt())
        || (result == Result.SUCCESS
        && (previousResult == Result.FAILURE || previousResult == Result.UNSTABLE)
        && notifier.getNotifyBackToNormal())
        || (result == Result.SUCCESS && notifier.getNotifySuccess())
        || (result == Result.UNSTABLE && notifier.getNotifyUnstable())) {

      MessageBuilder messageBuilder = new MessageBuilder(notifier, r, false);
      String title = messageBuilder.getTitle();
      String content = messageBuilder.getContent();
      getDaDaPushService(r).publish(title, content);
    }
  }


}
