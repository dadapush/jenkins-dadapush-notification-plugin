package com.dadapush.integration.jenkins;

import static hudson.Util.fixNull;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.listeners.ItemListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.verb.POST;


public class DaDaPushNotifier extends Notifier {

  private static final Logger logger = Logger.getLogger(DaDaPushNotifier.class.getName());
  private String basePath;
  private String channelToken;
  private boolean startNotification;
  private boolean notifySuccess;
  private boolean notifyAborted;
  private boolean notifyNotBuilt;
  private boolean notifyUnstable;
  private boolean notifyFailure;
  private boolean notifyBackToNormal;
  private boolean notifyRepeatedFailure;
  private boolean includeTestSummary;
  private boolean includeCustomMessage;
  private String customMessage;

  @DataBoundConstructor
  public DaDaPushNotifier(final String basePath, final String channelToken,
      final boolean startNotification, final boolean notifyAborted,
      final boolean notifyFailure,
      final boolean notifyNotBuilt, final boolean notifySuccess, final boolean notifyUnstable,
      final boolean notifyBackToNormal,
      final boolean notifyRepeatedFailure, final boolean includeTestSummary,
      final boolean includeCustomMessage, final String customMessage) {
    super();
    this.basePath = basePath;
    this.channelToken = channelToken;
    this.startNotification = startNotification;
    this.notifyAborted = notifyAborted;
    this.notifyFailure = notifyFailure;
    this.notifyNotBuilt = notifyNotBuilt;
    this.notifySuccess = notifySuccess;
    this.notifyUnstable = notifyUnstable;
    this.notifyBackToNormal = notifyBackToNormal;
    this.notifyRepeatedFailure = notifyRepeatedFailure;
    this.includeTestSummary = includeTestSummary;
    this.includeCustomMessage = includeCustomMessage;
    if (includeCustomMessage) {
      this.customMessage = customMessage;
    } else {
      this.customMessage = null;
    }
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

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  public boolean getStartNotification() {
    return startNotification;
  }

  @DataBoundSetter
  public void setStartNotification(boolean startNotification) {
    this.startNotification = startNotification;
  }

  public boolean getNotifySuccess() {
    return notifySuccess;
  }

  @DataBoundSetter
  public void setNotifySuccess(boolean notifySuccess) {
    this.notifySuccess = notifySuccess;
  }

  public boolean getNotifyAborted() {
    return notifyAborted;
  }

  @DataBoundSetter
  public void setNotifyAborted(boolean notifyAborted) {
    this.notifyAborted = notifyAborted;
  }

  public boolean getNotifyFailure() {
    return notifyFailure;
  }

  @DataBoundSetter
  public void setNotifyFailure(boolean notifyFailure) {
    this.notifyFailure = notifyFailure;
  }

  public boolean getNotifyNotBuilt() {
    return notifyNotBuilt;
  }

  @DataBoundSetter
  public void setNotifyNotBuilt(boolean notifyNotBuilt) {
    this.notifyNotBuilt = notifyNotBuilt;
  }

  public boolean getNotifyUnstable() {
    return notifyUnstable;
  }

  @DataBoundSetter
  public void setNotifyUnstable(boolean notifyUnstable) {
    this.notifyUnstable = notifyUnstable;
  }

  public boolean getNotifyBackToNormal() {
    return notifyBackToNormal;
  }

  @DataBoundSetter
  public void setNotifyBackToNormal(boolean notifyBackToNormal) {
    this.notifyBackToNormal = notifyBackToNormal;
  }

  public boolean getIncludeTestSummary() {
    return includeTestSummary;
  }

  @DataBoundSetter
  public void setIncludeTestSummary(boolean includeTestSummary) {
    this.includeTestSummary = includeTestSummary;
  }

  public boolean getNotifyRepeatedFailure() {
    return notifyRepeatedFailure;
  }

  @DataBoundSetter
  public void setNotifyRepeatedFailure(boolean notifyRepeatedFailure) {
    this.notifyRepeatedFailure = notifyRepeatedFailure;
  }

  public boolean getIncludeCustomMessage() {
    return includeCustomMessage;
  }

  @DataBoundSetter
  public void setIncludeCustomMessage(boolean includeCustomMessage) {
    this.includeCustomMessage = includeCustomMessage;
  }

  public String getCustomMessage() {
    return customMessage;
  }

  @DataBoundSetter
  public void setCustomMessage(@CheckForNull String customMessage) {
    this.customMessage = fixNull(customMessage);
  }


  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  public DaDaPushService newDaDaPushService(AbstractBuild r, BuildListener listener) {
    String basePath = this.basePath;
    if (StringUtils.isEmpty(basePath)) {
      basePath = getDescriptor().getBasePath();
    }
    String channelToken = this.channelToken;
    if (StringUtils.isEmpty(channelToken)) {
      channelToken = getDescriptor().getChannelToken();
    }

    EnvVars env = null;
    try {
      env = r.getEnvironment(listener);
    } catch (Exception e) {
      listener.getLogger().println("Error retrieving environment vars: " + e.getMessage());
      env = new EnvVars();
    }
    basePath = env.expand(basePath);
    channelToken = env.expand(channelToken);
    return new StandardDaDaPushService(basePath, channelToken);
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
      throws InterruptedException, IOException {
    return true;
  }

  @Override
  public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
    if (startNotification) {
      Map<Descriptor<Publisher>, Publisher> map = build.getProject().getPublishersList().toMap();
      for (Publisher publisher : map.values()) {
        if (publisher instanceof DaDaPushNotifier) {
          logger.info("Invoking Started...");
          new ActiveNotifier((DaDaPushNotifier) publisher, listener).started(build);
        }
      }
    }
    return super.prebuild(build, listener);
  }

  @Extension
  @Symbol("dadapushNotifier")
  public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    private String basePath;
    private String channelToken;

    public DescriptorImpl() {
      load();
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

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
      req.bindJSON(this, formData);
      save();
      return true;
    }

    DaDaPushService getDaDaPushService(final String basePath, final String channelToken) {
      return new StandardDaDaPushService(basePath, channelToken);
    }

    @Override
    public String getDisplayName() {
      return "DaDaPush Notifications";
    }

    @POST
    public FormValidation doTestConnection(@QueryParameter("basePath") final String basePath,
        @QueryParameter("channelToken") final String channelToken) throws FormException {
      if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
        return FormValidation.error("Insufficient permission.");
      }
      try {
        String targetBasePath = basePath;
        if (StringUtils.isEmpty(targetBasePath)) {
          targetBasePath = this.basePath;
        }
        String targetChannelToken = channelToken;
        if (StringUtils.isEmpty(targetChannelToken)) {
          targetChannelToken = this.channelToken;
        }

        DaDaPushService testDaDaPushService = getDaDaPushService(targetBasePath,
            targetChannelToken);
        String message = "DaDaPush/Jenkins plugin: you're all set!";
        boolean success = testDaDaPushService.publish("test DaDaPush/Jenkins plugin", message);
        return success ? FormValidation.ok("Success") : FormValidation.error("Failure");
      } catch (Exception e) {
        return FormValidation.error("Client error : " + e.getMessage());
      }
    }
  }


  @Deprecated
  public static class DaDaPushJobProperty extends
      hudson.model.JobProperty<AbstractProject<?, ?>> {

    private String basePath;
    private String channelToken;
    private boolean startNotification;
    private boolean notifySuccess;
    private boolean notifyAborted;
    private boolean notifyNotBuilt;
    private boolean notifyUnstable;
    private boolean notifyFailure;
    private boolean notifyBackToNormal;
    private boolean notifyRepeatedFailure;
    private boolean includeTestSummary;
    private boolean showCommitList;
    private String customMessage;
    private boolean includeCustomMessage;
    private String customMessageSuccess;
    private String customMessageAborted;
    private String customMessageNotBuilt;
    private String customMessageUnstable;
    private String customMessageFailure;
    private String customMessageBackToNormal;
    private String customMessageRepeatedFailure;

    @DataBoundConstructor
    public DaDaPushJobProperty(String basePath,
        String channelToken,
        boolean startNotification,
        boolean notifyAborted,
        boolean notifyFailure,
        boolean notifyNotBuilt,
        boolean notifySuccess,
        boolean notifyUnstable,
        boolean notifyBackToNormal,
        boolean notifyRepeatedFailure,
        boolean includeTestSummary,
        boolean showCommitList,
        boolean includeCustomMessage,
        String customMessage, String customMessageSuccess, String customMessageAborted,
        String customMessageNotBuilt, String customMessageUnstable,
        String customMessageFailure, String customMessageBackToNormal,
        String customMessageRepeatedFailure) {
      this.basePath = basePath;
      this.channelToken = channelToken;
      this.startNotification = startNotification;
      this.notifyAborted = notifyAborted;
      this.notifyFailure = notifyFailure;
      this.notifyNotBuilt = notifyNotBuilt;
      this.notifySuccess = notifySuccess;
      this.notifyUnstable = notifyUnstable;
      this.notifyBackToNormal = notifyBackToNormal;
      this.notifyRepeatedFailure = notifyRepeatedFailure;
      this.includeTestSummary = includeTestSummary;
      this.showCommitList = showCommitList;
      this.includeCustomMessage = includeCustomMessage;
      this.customMessage = customMessage;
      this.customMessageSuccess = customMessageSuccess;
      this.customMessageAborted = customMessageAborted;
      this.customMessageNotBuilt = customMessageNotBuilt;
      this.customMessageUnstable = customMessageUnstable;
      this.customMessageFailure = customMessageFailure;
      this.customMessageBackToNormal = customMessageBackToNormal;
      this.customMessageRepeatedFailure = customMessageRepeatedFailure;
    }

    @Exported
    public String getBasePath() {
      return basePath;
    }

    @Exported
    public String getChannelToken() {
      return channelToken;
    }

    @Exported
    public String getCustomMessageSuccess() {
      return customMessageSuccess;
    }

    @Exported
    public String getCustomMessageAborted() {
      return customMessageAborted;
    }

    @Exported
    public String getCustomMessageNotBuilt() {
      return customMessageNotBuilt;
    }

    @Exported
    public String getCustomMessageUnstable() {
      return customMessageUnstable;
    }

    @Exported
    public String getCustomMessageFailure() {
      return customMessageFailure;
    }

    @Exported
    public String getCustomMessageBackToNormal() {
      return customMessageBackToNormal;
    }

    @Exported
    public String getCustomMessageRepeatedFailure() {
      return customMessageRepeatedFailure;
    }

    @Exported
    public boolean getStartNotification() {
      return startNotification;
    }

    @Exported
    public boolean getNotifySuccess() {
      return notifySuccess;
    }

    @Exported
    public boolean getShowCommitList() {
      return showCommitList;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
      return super.prebuild(build, listener);
    }

    @Exported
    public boolean getNotifyAborted() {
      return notifyAborted;
    }

    @Exported
    public boolean getNotifyFailure() {
      return notifyFailure;
    }

    @Exported
    public boolean getNotifyNotBuilt() {
      return notifyNotBuilt;
    }

    @Exported
    public boolean getNotifyUnstable() {
      return notifyUnstable;
    }

    @Exported
    public boolean getNotifyBackToNormal() {
      return notifyBackToNormal;
    }

    @Exported
    public boolean includeTestSummary() {
      return includeTestSummary;
    }

    @Exported
    public boolean getNotifyRepeatedFailure() {
      return notifyRepeatedFailure;
    }


    @Exported
    public boolean includeCustomMessage() {
      return includeCustomMessage;
    }

    @Exported
    public String getCustomMessage() {
      return customMessage;
    }
  }


  @Extension
  public static final class Migrator extends ItemListener {

    @SuppressWarnings("deprecation")
    @Override
    public void onLoaded() {
      logger.info("Starting Settings Migration Process");
      for (AbstractProject<?, ?> p : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
        final DaDaPushJobProperty daDaPushJobProperty = p
            .getProperty(DaDaPushJobProperty.class);

        if (daDaPushJobProperty == null) {
          logger.fine(String
              .format("Configuration is already up to date for \"%s\", skipping migration",
                  p.getName()));
          continue;
        }

        DaDaPushNotifier mattermostNotifier = p.getPublishersList().get(DaDaPushNotifier.class);

        if (mattermostNotifier == null) {
          logger.fine(String
              .format("Configuration does not have a notifier for \"%s\", not migrating settings",
                  p.getName()));
        } else {
          logger.info(String.format("Starting migration for \"%s\"", p.getName()));
          //map settings
          if (StringUtils.isBlank(mattermostNotifier.basePath)) {
            mattermostNotifier.basePath = daDaPushJobProperty.getBasePath();
          }
          if (StringUtils.isBlank(mattermostNotifier.channelToken)) {
            mattermostNotifier.channelToken = daDaPushJobProperty.getChannelToken();
          }

          mattermostNotifier.startNotification = daDaPushJobProperty.getStartNotification();

          mattermostNotifier.notifyAborted = daDaPushJobProperty.getNotifyAborted();
          mattermostNotifier.notifyFailure = daDaPushJobProperty.getNotifyFailure();
          mattermostNotifier.notifyNotBuilt = daDaPushJobProperty.getNotifyNotBuilt();
          mattermostNotifier.notifySuccess = daDaPushJobProperty.getNotifySuccess();
          mattermostNotifier.notifyUnstable = daDaPushJobProperty.getNotifyUnstable();
          mattermostNotifier.notifyBackToNormal = daDaPushJobProperty.getNotifyBackToNormal();
          mattermostNotifier.notifyRepeatedFailure = daDaPushJobProperty
              .getNotifyRepeatedFailure();

          mattermostNotifier.includeTestSummary = daDaPushJobProperty.includeTestSummary();
          mattermostNotifier.includeCustomMessage = daDaPushJobProperty.includeCustomMessage();
          mattermostNotifier.customMessage = daDaPushJobProperty.getCustomMessage();
        }

        try {
          //property section is not used anymore - remove
          p.removeProperty(DaDaPushJobProperty.class);
          p.save();
          logger.info("Configuration updated successfully");
        } catch (IOException e) {
          logger.log(Level.SEVERE, e.getMessage(), e);
        }
      }
    }
  }
}
