package org.donttreadonmysmartphone.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityEvent;

import java.lang.ref.WeakReference;

/** Porta mínima da ação global do MEGA3 Split Rescue.
 *  Sem leitura de janelas, sem telemetria e sem execução automática.
 */
public final class SplitAccessibilityService extends AccessibilityService {
    private static WeakReference<SplitAccessibilityService> current = new WeakReference<>(null);

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.eventTypes = 0;
            setServiceInfo(info);
        }
        current = new WeakReference<>(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Não observa eventos nem conteúdo da tela.
    }

    @Override
    public void onInterrupt() {
        // Sem processo de observação contínua.
    }

    @Override
    public void onDestroy() {
        current.clear();
        super.onDestroy();
    }

    /** true informa aceitação da ação global, NÃO sucesso funcional do split-screen. */
    public static boolean toggleSplitScreen() {
        SplitAccessibilityService instance = current.get();
        return instance != null &&
                instance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
    }
}
