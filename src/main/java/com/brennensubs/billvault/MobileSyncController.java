package com.brennensubs.billvault;

import com.brennensubs.billvault.dto.MobileApi.*;
import com.brennensubs.billvault.service.MobileSyncService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mobile")
public class MobileSyncController {
  private final MobileSyncService mobile;

  public MobileSyncController(MobileSyncService mobile) {
    this.mobile = mobile;
  }

  @GetMapping("/snapshot")
  public Snapshot snapshot() {
    return mobile.snapshot();
  }

  @PostMapping("/commands")
  public Result command(@Valid @RequestBody Command value) {
    return mobile.command(value);
  }
}
