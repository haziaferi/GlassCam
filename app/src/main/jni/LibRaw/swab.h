#include <sys/types.h>
#include <unistd.h>

/* Android's bionic libc has declared swab() (with C linkage) inside
 * unistd.h since API 28. minSdk 30 means every supported device already
 * has it, so on Android we simply use the system one instead of
 * redeclaring it with C++ linkage here -- redeclaring it caused
 * "declaration of 'swab' has a different language linkage" build errors.
 * The custom implementation below is only compiled in for non-Android
 * targets or hypothetical pre-28 API levels. */
#if !defined(__ANDROID__) || (defined(__ANDROID_API__) && __ANDROID_API__ < 28)
void swab(const void *from, void *to, ssize_t n);
#endif
