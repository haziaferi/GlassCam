package camera2_hidden_keys;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.Field;

/**
 * org.lsposed.hiddenapibypass.HiddenApiBypass has no getDeclaredField(Class, String) of its
 * own - only getInstanceFields(Class)/getStaticFields(Class), which return every field on the
 * class so the caller filters by name. This restores the one-line lookup shape that
 * org.chickenhook.restrictionbypass.RestrictionBypass.getDeclaredField() used to provide, since
 * this package used it in two places (VendorKeyParser, VendorKeyTestLog).
 */
final class HiddenApiBypassCompat {

    private HiddenApiBypassCompat() {
    }

    static Field getDeclaredField(Class<?> clazz, String fieldName) throws NoSuchMethodException {
        for (Field field : HiddenApiBypass.getInstanceFields(clazz)) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        for (Field field : HiddenApiBypass.getStaticFields(clazz)) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        throw new NoSuchMethodException("No field named " + fieldName + " on " + clazz);
    }
}
