package com.scofu.network.instance;

import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.scofu.common.json.Json;
import com.scofu.network.document.AbstractDocumentRepository;
import com.scofu.network.document.RepositoryConfiguration;
import com.scofu.network.message.MessageFlow;
import com.scofu.network.message.MessageQueue;
import java.util.concurrent.TimeUnit;

/**
 * Group repository.
 */
public class GroupRepository extends AbstractDocumentRepository<Group> {

  @Inject
  GroupRepository(MessageQueue messageQueue, MessageFlow messageFlow, Json json) {
    super(messageQueue, messageFlow, Group.class, json, RepositoryConfiguration.builder()
        .withCollection("scofu.groups")
        .withCacheBuilder(CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS))
        .build());
  }
}
