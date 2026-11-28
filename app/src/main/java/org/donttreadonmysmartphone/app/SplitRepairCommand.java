package org.donttreadonmysmartphone.app;

/** Fixed, non-interpolated shell transaction for the two global split flags. */
final class SplitRepairCommand {
    private SplitRepairCommand() {}

    static String verifiedFlags() {
        return """
                a=$(settings get global force_resizable_activities) || exit 1
                b=$(settings get global enable_non_resizable_multi_window) || exit 1
                case "$a" in null|0|1) ;; *) echo 'Valor anterior desconhecido; nenhuma alteracao'; exit 1;; esac
                case "$b" in null|0|1) ;; *) echo 'Valor anterior desconhecido; nenhuma alteracao'; exit 1;; esac
                restore() {
                  if [ "$2" = null ]; then settings delete global "$1"; else settings put global "$1" "$2"; fi
                  [ "$(settings get global "$1")" = "$2" ]
                }
                if settings put global force_resizable_activities 1 &&
                   settings put global enable_non_resizable_multi_window 1 &&
                   [ "$(settings get global force_resizable_activities)" = 1 ] &&
                   [ "$(settings get global enable_non_resizable_multi_window)" = 1 ]; then
                    echo 'FLAGS_VERIFICADAS: 1,1. Teste a divisao de tela; o resultado nao comprova recuperacao.'
                    exit 0
                fi
                echo 'Alteracao recusada ou nao confirmada; restaurando valores anteriores.'
                restore force_resizable_activities "$a"; ra=$?
                restore enable_non_resizable_multi_window "$b"; rb=$?
                if [ "$ra" = 0 ] && [ "$rb" = 0 ]; then
                    echo 'ROLLBACK_VERIFICADO'
                else
                    echo 'ROLLBACK_NAO_CONFIRMADO: verifique as duas flags manualmente'
                fi
                exit 1
                """;
    }

    static String windowManagerOverride() {
        return """
                wm get-multi-window-config || exit 1
                wm set-multi-window-config --supportsNonResizable 1 --respectsActivityMinWidthHeight -1 || exit 1
                wm get-multi-window-config
                """;
    }
}
