package com.scofu.network.instance;

import com.google.inject.Inject;
import com.scofu.common.inject.Feature;
import com.scofu.network.document.DocumentStateListener;
import com.scofu.text.ThemeRegistry;

final class SystemThemeHandler implements Feature {

  private final ThemeRegistry themeRegistry;
  private final SystemRepository systemRepository;

  @Inject
  SystemThemeHandler(ThemeRegistry themeRegistry, SystemRepository systemRepository) {
    this.themeRegistry = themeRegistry;
    this.systemRepository = systemRepository;
    systemRepository.addStateListener(
        new DocumentStateListener<>() {
          @Override
          public void onUpdate(System system, boolean cached) {
            themeRegistry.setDefaultTheme(
                themeRegistry
                    .byName(system.theme())
                    .orElseGet(() -> themeRegistry.byName("Vanilla").orElseThrow()));
          }
        });
  }

  @Override
  public void enable() {
    systemRepository
        .get()
        .flatMap(
            system -> {
              themeRegistry.setDefaultTheme(
                  themeRegistry
                      .byName(system.theme())
                      .orElseGet(() -> themeRegistry.byName("Vanilla").orElseThrow()));
              return systemRepository.update(system);
            });
  }
}
