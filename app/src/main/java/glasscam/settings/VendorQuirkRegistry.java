package glasscam.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A small, generic dispatch table for per-vendor/per-framework implementation choices.
 *
 * Replaces the pattern of nested if/else chains scattered through camera parameter setup
 * (e.g. "if MTK use X, else if this parameter key matches Huawei's use Y, else use Z") with
 * an ordered list of rules registered once, resolved with a single call. Rules are checked
 * in registration order and the first match wins, so register more specific rules before
 * more general ones (mirrors how the original if/else chains were ordered).
 *
 * This does not replace the {@link Frameworks} enum or {@link SettingsManager}'s framework
 * detection — it sits on top of them. A rule's {@code matches} predicate is free to check
 * the current {@link Frameworks} value, a parameter key, a device model string, or any
 * combination, exactly as the code being replaced already did.
 *
 * @param <T> the common type produced for this feature (e.g. an AbstractParameter subclass).
 */
public class VendorQuirkRegistry<T> {

    /**
     * One vendor/framework-specific rule: a predicate over the current settings state,
     * and a factory that produces the implementation to use when that predicate matches.
     */
    public interface Rule<T> {
        boolean matches(SettingsManager settingsManager);
        T create();
    }

    private final List<Rule<T>> rules = new ArrayList<>();
    private final Supplier<T> fallback;

    /**
     * @param fallback supplies the default/generic implementation used when no registered
     *                 rule matches — this replaces the trailing "else" branch of the original
     *                 if/else chain, so behavior is unchanged for devices with no quirk.
     */
    public VendorQuirkRegistry(Supplier<T> fallback) {
        this.fallback = fallback;
    }

    /** Registers a rule. Returns this registry so calls can be chained while building it. */
    public VendorQuirkRegistry<T> register(Rule<T> rule) {
        rules.add(rule);
        return this;
    }

    /** Resolves to the first matching rule's implementation, or the fallback if none match. */
    public T resolve(SettingsManager settingsManager) {
        for (Rule<T> rule : rules) {
            if (rule.matches(settingsManager)) {
                return rule.create();
            }
        }
        return fallback.get();
    }
}
