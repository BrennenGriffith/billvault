package com.brennensubs.billvault.desktop;

import com.brennensubs.billvault.model.Expense;
import com.brennensubs.billvault.repository.ExpenseRepository;
import com.brennensubs.billvault.service.BillingCalculator;
import com.brennensubs.billvault.service.SubscriptionService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.util.Map;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Embedded services; network bridges start only when explicitly enabled. */
public final class DesktopStore implements AutoCloseable {
  @Configuration(proxyBeanMethods = false)
  @Profile("desktop")
  @EnableAutoConfiguration
  @EntityScan(basePackageClasses = Expense.class)
  @EnableJpaRepositories(basePackageClasses = ExpenseRepository.class)
  @Import({
    SubscriptionService.class,
    BillingCalculator.class,
    com.brennensubs.billvault.service.ExpenseImportService.class,
    com.brennensubs.billvault.service.MobileSyncService.class
  })
  static class LocalConfiguration {}

  private final ConfigurableApplicationContext context;
  public final SubscriptionService service;
  private final Validator validator;
  private DesktopImportBridge bridge;
  private DesktopMobileBridge mobile;
  private final Path dataDirectory;
  private final FileChannel lockChannel;
  private final FileLock lock;

  public DesktopStore(Path directory) throws IOException {
    Path data = directory.toAbsolutePath().normalize();
    dataDirectory = data;
    Files.createDirectories(data);
    if (data.toString().contains(";"))
      throw new IOException("Data directory cannot contain a semicolon");
    lockChannel =
        FileChannel.open(
            data.resolve("desktop.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    try {
      lock = lockChannel.tryLock();
      if (lock == null)
        throw new IOException(
            "This vault is already open. Close the other BillVault Desktop window first.");
    } catch (IOException | OverlappingFileLockException ex) {
      lockChannel.close();
      throw new IOException(
          "This vault is already open or cannot be locked. Close the other BillVault Desktop window"
              + " first.",
          ex);
    }
    var app = new SpringApplication(LocalConfiguration.class);
    app.setWebApplicationType(WebApplicationType.NONE);
    app.setRegisterShutdownHook(false);
    // Command-line properties override web/test environment configuration deliberately.
    try {
      context =
          app.run(
              "--spring.datasource.url=jdbc:h2:file:"
                  + data.resolve("billvault").toString().replace('\\', '/')
                  + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
              "--spring.datasource.username=sa",
              "--spring.datasource.password=",
              "--spring.main.web-application-type=none",
              "--spring.main.banner-mode=off",
              "--spring.jpa.hibernate.ddl-auto=validate",
              "--spring.flyway.enabled=true",
              "--spring.profiles.active=desktop",
              "--logging.level.root=WARN",
              "--spring.datasource.hikari.maximum-pool-size=3");
    } catch (RuntimeException | Error ex) {
      lock.release();
      lockChannel.close();
      throw ex;
    }
    service = context.getBean(SubscriptionService.class);
    validator = context.getBean(Validator.class);
  }

  public void enableImports(int port, String token, Runnable changed) throws IOException {
    if (bridge != null) throw new IllegalStateException("Integration already enabled");
    bridge =
        new DesktopImportBridge(
            port,
            token,
            this,
            context.getBean(com.brennensubs.billvault.service.ExpenseImportService.class),
            context.getBean(tools.jackson.databind.ObjectMapper.class),
            changed);
  }

  public synchronized DesktopMobileBridge.Pairing enableMobile(int port, Runnable changed)
      throws Exception {
    if (mobile == null)
      mobile =
          new DesktopMobileBridge(
              dataDirectory,
              port,
              context.getBean(com.brennensubs.billvault.service.MobileSyncService.class),
              context.getBean(org.springframework.jdbc.core.JdbcTemplate.class),
              context.getBean(tools.jackson.databind.ObjectMapper.class),
              changed);
    return mobile.newCode();
  }

  public synchronized void disableMobile() {
    if (mobile != null) {
      mobile.close();
      mobile = null;
    }
  }

  public synchronized void revokeMobile() {
    context
        .getBean(org.springframework.jdbc.core.JdbcTemplate.class)
        .update("UPDATE mobile_devices SET revoked=TRUE");
  }

  public <T> T validated(T request) {
    var violations = validator.validate(request);
    if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
    return request;
  }

  public boolean isWebServerRunning() {
    return context
        instanceof org.springframework.boot.web.server.context.WebServerApplicationContext;
  }

  public static Path defaultDirectory() {
    String os = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT);
    Map<String, String> env = System.getenv();
    if (os.contains("win"))
      return Path.of(
          env.getOrDefault("LOCALAPPDATA", System.getProperty("user.home")), "BillVaultDesktop");
    if (os.contains("mac"))
      return Path.of(
          System.getProperty("user.home"), "Library", "Application Support", "BillVaultDesktop");
    String xdg = env.get("XDG_DATA_HOME");
    Path base =
        xdg == null || xdg.isBlank()
            ? Path.of(System.getProperty("user.home"), ".local", "share")
            : Path.of(xdg);
    if (!base.isAbsolute()) base = Path.of(System.getProperty("user.home"), ".local", "share");
    return base.resolve("billvault-desktop");
  }

  @Override
  public void close() {
    try {
      if (bridge != null) bridge.close();
      disableMobile();
      context.close();
    } finally {
      try {
        lock.release();
        lockChannel.close();
      } catch (IOException ex) {
        throw new java.io.UncheckedIOException(ex);
      }
    }
  }
}
