package com.offsec.nethunter.bridge;

import androidx.annotation.NonNull;

import java.util.Objects;

public class SessionId {
  /**
   * 创建一个新的会话. 
   */
  public static final SessionId NEW_SESSION = SessionId.of("new");

  /**
   * 表示 NeoTerm 中的当前会话. 
   */
  public static final SessionId CURRENT_SESSION = SessionId.of("current");

  private final String sessionId;

  SessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getSessionId() {
    return sessionId;
  }

  @NonNull
  @Override
  public String toString() {
    return "终端会话 { id = " + sessionId + " }";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SessionId sessionId1 = (SessionId) o;
    return Objects.equals(sessionId, sessionId1.sessionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sessionId);
  }

  public static SessionId of(String sessionId) {
    return new SessionId(sessionId);
  }
}
