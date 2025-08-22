package com.offsec.nethunter.service;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/** 占位辅助功能服务, 目前未实现具体功能 */
public class DummyAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        // 占位方法, 当前未实现任何逻辑
        // 可用于未来扩展辅助功能相关的操作
    }

    @Override
    public void onInterrupt() {
        // 占位方法, 当前未实现任何逻辑
        // 用于处理辅助功能服务中断时的操作
    }
}
