package dev.ragnarok.fenrir.api.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import dev.ragnarok.fenrir.api.model.VKApiComment;
import dev.ragnarok.fenrir.api.model.VkApiAttachments;

public class CommentDtoAdapter extends AbsAdapter implements JsonDeserializer<VKApiComment> {
    private static final String TAG = CommentDtoAdapter.class.getSimpleName();

    @Override
    public VKApiComment deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!checkObject(json)) {
            throw new JsonParseException(TAG + " error parse object");
        }
        VKApiComment dto = new VKApiComment();
        JsonObject root = json.getAsJsonObject();

        dto.id = optInt(root, "id");
        dto.from_id = optInt(root, "from_id");

        if (dto.from_id == 0) {
            dto.from_id = optInt(root, "owner_id");
        }

        dto.date = optLong(root, "date");
        dto.text = optString(root, "text");
        dto.reply_to_user = optInt(root, "reply_to_user");
        dto.reply_to_comment = optInt(root, "reply_to_comment");

        if (hasArray(root, "attachments")) {
            dto.attachments = context.deserialize(root.get("attachments"), VkApiAttachments.class);
        }

        if (hasObject(root, "thread")) {
            dto.threads = optInt(root.get("thread").getAsJsonObject(), "count");
        }

        dto.pid = optInt(root, "pid");

        if (hasObject(root, "likes")) {
            JsonObject likesRoot = root.getAsJsonObject("likes");
            dto.likes = optInt(likesRoot, "count");
            dto.user_likes = optIntAsBoolean(likesRoot, "user_likes");
            dto.can_like = optIntAsBoolean(likesRoot, "can_like");
        }

        dto.can_edit = optIntAsBoolean(root, "can_edit");
        return dto;
    }
}
