package com.hs.railway_stats.view.util;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.textfield.TextField;

import java.util.function.Consumer;

public final class BrowserStorageUtils {

    private BrowserStorageUtils() {
    }

    // ── Crypto helpers ────────────────────────────────────────────────────────

    public static String buildCryptoJs(String secret, String salt) {
        return ("""
                const APP_SECRET = '%s';
                const SALT       = new TextEncoder().encode('%s');
                
                async function deriveKey() {
                    const keyMaterial = await crypto.subtle.importKey(
                        'raw', new TextEncoder().encode(APP_SECRET),
                        'PBKDF2', false, ['deriveKey']
                    );
                    return crypto.subtle.deriveKey(
                        { name: 'PBKDF2', salt: SALT, iterations: 100000, hash: 'SHA-256' },
                        keyMaterial,
                        { name: 'AES-GCM', length: 256 },
                        false, ['encrypt', 'decrypt']
                    );
                }
                
                function toHex(buf) {
                    return Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2,'0')).join('');
                }
                
                function fromHex(hex) {
                    const bytes = new Uint8Array(hex.length / 2);
                    for (let i = 0; i < bytes.length; i++) bytes[i] = parseInt(hex.substr(i*2,2),16);
                    return bytes;
                }
                """).formatted(secret, salt);
    }

    // ── Encrypted localStorage ────────────────────────────────────────────────

    public static void encryptedLocalStorageSave(String key, String value, String secret, String salt) {
        UI.getCurrent().getPage().executeJs(buildCryptoJs(secret, salt) + """
                (async () => {
                    const key = await deriveKey();
                    const iv  = crypto.getRandomValues(new Uint8Array(12));
                    const enc = await crypto.subtle.encrypt(
                        { name: 'AES-GCM', iv },
                        key,
                        new TextEncoder().encode($1)
                    );
                    localStorage.setItem($0, toHex(iv) + ':' + toHex(enc));
                })();
                """, key, value);
    }

    public static void encryptedLocalStorageLoad(String key, String secret, String salt,
                                                 Consumer<String> onSuccess) {
        UI.getCurrent().getPage().executeJs(buildCryptoJs(secret, salt) + """
                return (async () => {
                    const raw = localStorage.getItem($0);
                    if (!raw || !raw.includes(':')) return '';
                    try {
                        const [ivHex, dataHex] = raw.split(':');
                        const key = await deriveKey();
                        const dec = await crypto.subtle.decrypt(
                            { name: 'AES-GCM', iv: fromHex(ivHex) },
                            key,
                            fromHex(dataHex)
                        );
                        return new TextDecoder().decode(dec);
                    } catch(e) { return ''; }
                })();
                """, key).then(String.class, result -> {
            if (result != null && !result.isBlank()) {
                onSuccess.accept(result);
            }
        });
    }

    public static void localStorageRemove(String key) {
        UI.getCurrent().getPage().executeJs("localStorage.removeItem($0);", key);
    }

    // ── Cookie storage ────────────────────────────────────────────────────────

    public static void cookieSave(String key, String value) {
        UI.getCurrent().getPage().executeJs(
                "document.cookie = $0 + '=' + encodeURIComponent($1) + '; path=/; max-age=' + (365*24*60*60);",
                key, value);
    }

    public static void cookieLoad(String key, Consumer<String> onSuccess) {
        UI.getCurrent().getPage().executeJs(
                        "const match = document.cookie.split('; ').find(r => r.startsWith($0 + '='));" +
                                "return match ? decodeURIComponent(match.split('=')[1]) : '';", key)
                .then(String.class, value -> {
                    if (value != null && !value.isBlank()) {
                        onSuccess.accept(value);
                    }
                });
    }

    public static void cookieLoadIntoField(String key, TextField field) {
        cookieLoad(key, field::setValue);
    }
}
