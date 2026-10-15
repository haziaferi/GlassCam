package glasscam.utils;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * Shared background thread pool for camera modules.
 * Each module requests a named thread slot. Pool creates HandlerThread once per name,
 * reuses it across modules. Only destroys when all references released.
 */
public class SharedBackgroundHandlerPool {
    private static final String TAG = SharedBackgroundHandlerPool.class.getSimpleName();

    // Singleton instance
    private static SharedBackgroundHandlerPool instance;

    // Named thread slots with reference counting
    private final java.util.HashMap<String, Slot> slots = new java.util.HashMap<>();

    private SharedBackgroundHandlerPool() {}

    public static synchronized SharedBackgroundHandlerPool getInstance() {
        if (instance == null) {
            instance = new SharedBackgroundHandlerPool();
        }
        return instance;
    }

    /**
 * Request a named background handler. Creates thread if slot doesn't exist, increments ref count.
 */
    public synchronized Handler requestHandler(String name) {
        Slot slot = slots.get(name);
        if (slot == null) {
            slot = new Slot(name);
            slots.put(name, slot);
        }
        slot.refCount++;
        Log.d(TAG, "Request handler '" + name + "', refCount=" + slot.refCount);
        return slot.handler;
    }

    /**
 * Release a named background handler. Decrements ref count, destroys thread if count reaches 0.
 */
    public synchronized void releaseHandler(String name) {
        Slot slot = slots.get(name);
        if (slot == null) {
            Log.w(TAG, "Release handler '" + name + "', but slot not found");
            return;
        }
        slot.refCount--;
        Log.d(TAG, "Release handler '" + name + "', refCount=" + slot.refCount);
        if (slot.refCount <= 0) {
            slot.destroy();
            slots.remove(name);
        }
    }

    private static class Slot {
        final String name;
        HandlerThread thread;
        Handler handler;
        int refCount = 0;

        Slot(String name) {
            this.name = name;
            Log.d(TAG, "Creating HandlerThread '" + name + "'");
            thread = new HandlerThread(name);
            thread.start();
            handler = new Handler(thread.getLooper());
        }

        void destroy() {
            Log.d(TAG, "Destroying HandlerThread '" + name + "'");
            if (thread == null) return;
            thread.quitSafely();
            thread = null;
            handler = null;
        }
    }
}
