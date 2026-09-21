package io.github.yaml.pinterm;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import org.jetbrains.annotations.Nullable;

final class PinTermKeys {
    static final Key<Boolean> OWNED = Key.create("io.github.yaml.pinterm.owned");

    private PinTermKeys() {
    }

    static void markOwned(@Nullable UserDataHolder holder) {
        if (holder != null) {
            holder.putUserData(OWNED, Boolean.TRUE);
        }
    }

    static boolean isOwned(@Nullable UserDataHolder holder) {
        return holder != null && Boolean.TRUE.equals(holder.getUserData(OWNED));
    }
}
