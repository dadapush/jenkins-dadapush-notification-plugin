package com.dadapush.integration.jenkins;

import com.dadapush.client.ApiClient;
import com.dadapush.client.ApiException;
import com.dadapush.client.Configuration;
import com.dadapush.client.api.DaDaPushMessageApi;
import com.dadapush.client.model.MessagePushRequest;
import com.dadapush.client.model.ResultOfMessagePushResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;

public class StandardDaDaPushService implements DaDaPushService {

  private static final Logger logger = Logger.getLogger(StandardDaDaPushService.class.getName());

  private String basePath;
  private String channelToken;
  private DaDaPushMessageApi apiInstance;

  public StandardDaDaPushService(String basePath, String channelToken) {
    this.basePath = basePath;
    this.channelToken = channelToken;
    init();
  }

  private void init() {
    ApiClient apiClient = Configuration.getDefaultApiClient();
    if(StringUtils.isNotEmpty(basePath)){
      apiClient.setBasePath(basePath);
    }
    apiInstance = new DaDaPushMessageApi(apiClient);
  }

  @Override
  public boolean publish(String title, String content) {
    MessagePushRequest body = new MessagePushRequest();
    body.setTitle(StringUtils.substring(title, 0, 50));
    body.setContent(StringUtils.substring(content, 0, 500));
    body.setNeedPush(true);
    logger.log(Level.INFO,body.toString());
    try {
      ResultOfMessagePushResponse result = apiInstance.createMessage(body, channelToken);
      return result.getCode() == 0;
    } catch (ApiException e) {
      logger.log(Level.WARNING,"send notification to DaDaPush channel fail",e);
      throw new RuntimeException(e);
    }
  }
}
