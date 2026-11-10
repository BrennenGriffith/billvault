package com.brennensubs.billvault.desktop;

import static org.junit.jupiter.api.Assertions.*;

import javafx.geometry.Rectangle2D;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DesktopWindowSizingTest {
  @ParameterizedTest
  @CsvSource({
    "0,0,1366,728", "0,0,1280,680", "0,0,1024,560", "0,0,800,560",
    "0,0,640,440", "0,0,320,240", "-1920,40,1920,1040", "2560,-900,1600,860"
  })
  void startupFrameFitsUsableDesktopIncludingScaledAndOffsetScreens(
      double x, double y, double width, double height) {
    var screen = new Rectangle2D(x, y, width, height);
    var frame = DesktopWindowSizing.initialBounds(screen);
    assertTrue(DesktopWindowSizing.contains(screen, frame));
    assertTrue(frame.getMinY() > screen.getMinY(), "Title bar needs a visible top margin");
    assertTrue(frame.getMaxY() < screen.getMaxY(), "Frame must stay above the taskbar");
    assertEquals(screen.getMinX() + width / 2, frame.getMinX() + frame.getWidth() / 2, 0.01);
    assertEquals(screen.getMinY() + height / 2, frame.getMinY() + frame.getHeight() / 2, 0.01);
  }
}
