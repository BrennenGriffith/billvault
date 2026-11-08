package com.brennensubs.billvault.desktop;

/** Separate launcher avoids the JDK's special JavaFX Application main-class handling. */
public final class DesktopLauncher {
  public static void main(String[] args) {
    javafx.application.Application.launch(DesktopApplication.class, args);
  }
}
