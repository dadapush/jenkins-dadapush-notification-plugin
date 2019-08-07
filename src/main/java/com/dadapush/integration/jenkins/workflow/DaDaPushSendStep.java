package com.dadapush.integration.jenkins.workflow;

import com.dadapush.integration.jenkins.DaDaPushNotifier;
import com.dadapush.integration.jenkins.DaDaPushService;
import com.dadapush.integration.jenkins.StandardDaDaPushService;
import hudson.AbortException;
import hudson.Extension;
import hudson.model.TaskListener;
import javax.inject.Inject;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Workflow step to send a DaDaPush channel notification.
 */
@SuppressWarnings("unused")
public class DaDaPushSendStep extends AbstractStepImpl {

  public String getTitle() {
    return title;
  }

  @DataBoundSetter
  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  @DataBoundSetter
  public void setContent(String content) {
    this.content = content;
  }

  private String title;
  private String content;
  private String basePath;
  private String channelToken;
  private boolean failOnError;

  @DataBoundConstructor
  public DaDaPushSendStep(String title, String content) {
    this.title = title;
    this.content = content;
  }

  public String getBasePath() {
    return basePath;
  }

  @DataBoundSetter
  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public String getChannelToken() {
    return channelToken;
  }

  @DataBoundSetter
  public void setChannelToken(String channelToken) {
    this.channelToken = channelToken;
  }


  public boolean isFailOnError() {
    return failOnError;
  }

  @DataBoundSetter
  public void setFailOnError(boolean failOnError) {
    this.failOnError = failOnError;
  }

  @SuppressWarnings("unused")
  @Extension
  public static class DescriptorImpl extends AbstractStepDescriptorImpl {

    public DescriptorImpl() {
      super(DaDaPushSendStepExecution.class);
    }

    @Override
    public String getFunctionName() {
      return "dadapushSend";
    }

    @Override
    public String getDisplayName() {
      return "Send DaDaPush message";
    }
  }

  public static class DaDaPushSendStepExecution extends
      AbstractSynchronousNonBlockingStepExecution<Void> {

    private static final long serialVersionUID = 1L;

    @Inject
    transient DaDaPushSendStep step;

    @StepContextParameter
    transient TaskListener listener;

    @Override
    protected Void run() throws Exception {

      //default to global config values if not set in step, but allow step to override all global settings
      Jenkins jenkins;
      //Jenkins.getInstance() may return null, no message sent in that case
      try {
        jenkins = Jenkins.getInstance();
      } catch (NullPointerException ne) {
        listener.error(String.format("DaDaPush notification failed with exception: %s", ne), ne);
        return null;
      }
      DaDaPushNotifier.DescriptorImpl descriptor = jenkins
          .getDescriptorByType(DaDaPushNotifier.DescriptorImpl.class);
      String basePath = step.basePath != null ? step.basePath : descriptor.getBasePath();
      String channelToken =
          step.channelToken != null ? step.channelToken : descriptor.getChannelToken();

      DaDaPushService service = getService(basePath, channelToken);
      try {
        boolean publishSuccess = service.publish(step.getTitle(), step.getContent());
        if (!publishSuccess) {
          listener.error("DaDaPush notification failed. See Jenkins logs for details.");
        }
      } catch (Exception e) {
        if (step.failOnError) {
          throw new AbortException("DaDaPush notification failed. See Jenkins logs for details.");
        }
      }
      return null;
    }

    //streamline unit testing
    DaDaPushService getService(String basePath, String channelToken) {
      return new StandardDaDaPushService(basePath, channelToken);
    }

  }

}
