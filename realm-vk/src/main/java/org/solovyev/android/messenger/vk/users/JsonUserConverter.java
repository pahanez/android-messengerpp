package org.solovyev.android.messenger.vk.users;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.solovyev.android.messenger.http.IllegalJsonException;
import org.solovyev.android.messenger.http.IllegalJsonRuntimeException;
import org.solovyev.android.messenger.realms.Realm;
import org.solovyev.android.messenger.users.User;
import org.solovyev.common.Converter;
import org.solovyev.common.collections.Collections;

import java.util.ArrayList;
import java.util.List;

/**
 * User: serso
 * Date: 5/30/12
 * Time: 10:15 PM
 */
public class JsonUserConverter implements Converter<String, List<User>> {

    @NotNull
    private final Realm realm;

    private JsonUserConverter(@NotNull Realm realm) {
        this.realm = realm;
    }

    @NotNull
    @Override
    public List<User> convert(@NotNull String json) {
        final Gson gson = new Gson();

        final JsonUsers jsonUsersResult = gson.fromJson(json, JsonUsers.class);
        final List<JsonUser> jsonUsers = jsonUsersResult.getResponse();

        final List<User> result = new ArrayList<User>(jsonUsers == null ? 0 : jsonUsers.size());

        try {
            if (!Collections.isEmpty(jsonUsers)) {
                for (JsonUser jsonUser : jsonUsers) {
                    result.add(jsonUser.toUser(realm));
                }
            }
        } catch (IllegalJsonException e) {
            throw new IllegalJsonRuntimeException(e);
        }

        return result;
    }

    @NotNull
    public static Converter<String, List<User>> newInstance(@NotNull Realm realm) {
        return new JsonUserConverter(realm);
    }
}
