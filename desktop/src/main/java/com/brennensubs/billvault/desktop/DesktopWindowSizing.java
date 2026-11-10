package com.brennensubs.billvault.desktop;

import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;

/** Window dimensions use JavaFX logical coordinates, including the native frame. */
final class DesktopWindowSizing {
  private DesktopWindowSizing() {}

  static Rectangle2D initialBounds(Rectangle2D visualBounds) {
    double margin = Math.min(16, Math.min(visualBounds.getWidth(), visualBounds.getHeight()) / 20);
    double width = Math.min(1160, visualBounds.getWidth() - 2 * margin);
    double height = Math.min(780, visualBounds.getHeight() - 2 * margin);
    return new Rectangle2D(
        visualBounds.getMinX() + (visualBounds.getWidth() - width) / 2,
        visualBounds.getMinY() + (visualBounds.getHeight() - height) / 2,
        width,
        height);
  }

  static void fit(Stage stage, Rectangle2D visualBounds) {
    Rectangle2D bounds = initialBounds(visualBounds);
    // Minimum sizes must not force a window beyond a small or DPI-scaled desktop.
    stage.setMinWidth(Math.min(820, bounds.getWidth()));
    stage.setMinHeight(Math.min(580, bounds.getHeight()));
    stage.setWidth(bounds.getWidth());
    stage.setHeight(bounds.getHeight());
    stage.setX(bounds.getMinX());
    stage.setY(bounds.getMinY());
  }

  static boolean contains(Rectangle2D visual, Rectangle2D window) {
    double tolerance = 1;
    return window.getMinX() >= visual.getMinX() - tolerance
        && window.getMinY() >= visual.getMinY() - tolerance
        && window.getMaxX() <= visual.getMaxX() + tolerance
        && window.getMaxY() <= visual.getMaxY() + tolerance;
  }
}
