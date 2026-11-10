package com.brennensubs.billvault.desktop;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/** Vector SVG icons for JavaFX controls without external icon-font dependencies or emoji. */
final class DesktopIcons {
  static final String DASHBOARD =
      "M3 3h7v9H3z M14 3h7v5h-7z M14 12h7v9h-7z M3 16h7v5H3z";
  static final String SUBSCRIPTIONS =
      "M17 2l4 4-4 4 M3 11v-1a4 4 0 0 1 4-4h14 M7 22l-4-4 4-4 M21 13v1a4 4 0 0 1-4 4H3";
  static final String BILLS =
      "M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1z M8 7h8 M8 11h8 M8 15h5";
  static final String CALENDAR =
      "M19 4H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2z M16 2v4 M8 2v4 M3 10h18";
  static final String HISTORY =
      "M12 8v4l3 3 M3.05 11a9 9 0 1 1 .5 4m-.5 5v-5h5";
  static final String REPORTS =
      "M18 20V10 M12 20V4 M6 20v-6 M3 20h18";
  static final String BELL =
      "M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9 M13.73 21a2 2 0 0 1-3.46 0";
  static final String SETTINGS =
      "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9c.26.604.852.997 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z";
  static final String WALLET =
      "M21 12V7H5a2 2 0 0 1 0-4h14v4 M3 5v14a2 2 0 0 0 2 2h16v-5 M18 12a2 2 0 0 0 0 4h4v-4z";
  static final String SHIELD =
      "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z M9 12l2 2 4-4";
  static final String PLUS =
      "M12 5v14 M5 12h14";
  static final String SEARCH =
      "M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z M21 21l-4.35-4.35";
  static final String CHECK =
      "M20 6L9 17l-5-5";
  static final String EDIT =
      "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7 M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z";
  static final String TRASH =
      "M3 6h18 M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2";
  static final String DOWNLOAD =
      "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4 M7 10l5 5 5-5 M12 15V3";
  static final String CHEVRON_LEFT =
      "M15 18l-6-6 6-6";
  static final String CHEVRON_RIGHT =
      "M9 18l6-6-6-6";
  static final String ARROW_UP_RIGHT =
      "M7 17L17 7 M7 7h10v10";
  static final String ARROW_DOWN_LEFT =
      "M17 7L7 17 M17 17H7V7";
  static final String SUN =
      "M12 17a5 5 0 1 0 0-10 5 5 0 0 0 0 10z M12 1v2 M12 21v2 M4.22 4.22l1.42 1.42 M18.36 18.36l1.42 1.42 M1 12h2 M21 12h2 M4.22 19.78l1.42-1.42 M18.36 5.64l1.42-1.42";
  static final String MOON =
      "M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z";
  static final String FOLDER =
      "M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z";
  static final String REFRESH =
      "M23 4v6h-6 M1 20v-6h6 M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15";

  private DesktopIcons() {}

  static Node icon(String pathData, double size) {
    SVGPath svg = new SVGPath();
    svg.setContent(pathData);
    svg.getStyleClass().add("icon-stroke");
    svg.setStrokeLineCap(StrokeLineCap.ROUND);
    svg.setStrokeLineJoin(StrokeLineJoin.ROUND);
    double scale = size / 24.0;
    svg.setScaleX(scale);
    svg.setScaleY(scale);
    StackPane box = new StackPane(svg);
    box.getStyleClass().add("icon-box");
    box.setAlignment(Pos.CENTER);
    box.setMinSize(size, size);
    box.setPrefSize(size, size);
    box.setMaxSize(size, size);
    return box;
  }
}
