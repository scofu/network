package com.scofu.network.instance.discord;

import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.scofu.app.Service;
import com.scofu.app.bootstrap.BootstrapModule;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import javax.security.auth.login.LoginException;

/**
 * Network instance discord.
 */
public class NetworkInstanceDiscord extends Service {

  private final GatewayDiscordClient client;

  NetworkInstanceDiscord(GatewayDiscordClient client) {
    this.client = client;
  }

  public static void main(String[] args) throws LoginException, InterruptedException {
    load(Stage.PRODUCTION, new NetworkInstanceDiscord(
        DiscordClient.builder(System.getenv("DISCORD_TOKEN")).build().gateway().login().block()));
  }

  @Override
  protected void configure() {
    install(new BootstrapModule(getClass().getClassLoader()));
    bind(GatewayDiscordClient.class).toInstance(client);
    bindFeature(Test.class).in(Scopes.SINGLETON);
    System.out.println("here2");
  }
}
