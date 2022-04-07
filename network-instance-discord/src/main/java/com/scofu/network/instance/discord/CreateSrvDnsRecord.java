package com.scofu.network.instance.discord;

import java.util.Map;
import net.renfei.cloudflare.entity.CreateDnsRecord;

/**
 * Create SRV DNS record.
 */
final class CreateSrvDnsRecord extends CreateDnsRecord {

  private Map<String, Object> data;

  public Map<String, Object> getData() {
    return data;
  }

  public void setData(Map<String, Object> data) {
    this.data = data;
  }
}
