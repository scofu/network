package com.scofu.network.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.scofu.app.Service;
import com.scofu.app.bootstrap.BootstrapModule;
import com.scofu.common.json.Json;
import com.scofu.network.document.lazy.LazyDocument;
import com.scofu.network.document.lazy.LazyDocumentFactory;
import com.scofu.network.message.Dispatcher;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * Tests lazy documents.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class LazyDocumentTest extends Service {

  @Inject
  private LazyDocumentFactory lazyDocumentFactory;
  @Inject
  private Json json;

  @Override
  protected void configure() {
    install(new BootstrapModule(getClass().getClassLoader()));
    bind(Dispatcher.class).to(LocalDispatcher.class).in(Scopes.SINGLETON);
  }

  @BeforeAll
  public void setup() {
    load(Stage.PRODUCTION, this);
  }

  @Test
  public void testBindings() {
    interface User extends LazyDocument {

      Optional<String> name();

      void setName(String name);

      String password();

      void setPassword(String password);

      int coins();

      void setCoins(int coins);
    }

    final var user = lazyDocumentFactory.create(User.class, "test");
    assertEquals("test", user.id());
    assertTrue(user.name().isEmpty());
    assertNull(user.password());
    assertEquals(0, user.coins());
    assertEquals("{\"_id\":\"test\"}", user.toString());
    assertEquals(user, json.fromString(User.class, user.toString()));

    user.setName("Name");
    user.setPassword("Password");
    user.setCoins(100);

    assertEquals("Name", user.name().orElseThrow());
    assertEquals("Password", user.password());
    assertEquals(100, user.coins());
    assertEquals("{\"_id\":\"test\",\"name\":\"Name\",\"password\":\"Password\",\"coins\":100}",
        user.toString());
    assertEquals(user, json.fromString(User.class, user.toString()));

    user.setName(null);
    assertTrue(user.name().isEmpty());
    assertEquals("{\"_id\":\"test\",\"password\":\"Password\",\"coins\":100}", user.toString());
  }

  @Test
  public void testDefaults() {
    final var user = lazyDocumentFactory.create(PublicUser.class, "test");
    user.setName("nAmE");
    assertEquals("Name", user.fancyName());
  }

  /**
   * Has to be public to support default methods.
   */
  public interface PublicUser extends LazyDocument {

    String name();

    void setName(String name);

    default String fancyName() {
      final var substring = name().substring(1);
      return Character.toUpperCase(name().charAt(0)) + substring.toLowerCase(Locale.ROOT);
    }
  }
}
