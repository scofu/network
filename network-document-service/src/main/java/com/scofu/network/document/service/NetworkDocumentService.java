package com.scofu.network.document.service;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.scofu.app.Service;
import com.scofu.app.bootstrap.BootstrapModule;

/** Network document service. */
public class NetworkDocumentService extends Service {

  public static void main(String[] args) {
    load(Stage.PRODUCTION, new NetworkDocumentService());
  }

  @Override
  protected void configure() {
    install(new BootstrapModule(getClass().getClassLoader()));
    bindFeature(BsonAdapter.class).in(Scopes.SINGLETON);
    bindFeature(DocumentController.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  MongoDatabase mongoDatabase() {
    return MongoClients.create(System.getenv("MONGO_URI"))
        .getDatabase(System.getenv("MONGO_DATABASE"));
  }
}
