package org.perazimchurch.app.data.remote;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.perazimchurch.app.data.local.session.SessionManager;

/**
 * Remote source for all Phase B Community API interactions.
 */
public final class CommunityRemoteSource {

    private final Context context;

    public interface CommunityCallback<T> {
        void onSuccess(T result);
        void onError(int statusCode, String message);
    }

    public CommunityRemoteSource(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    private String getAuthToken() {
        return SessionManager.getInstance(context).getAuthToken();
    }

    public void fetchPosts(@Nullable String type, @Nullable String groupId, @Nullable Long cursor,
                           @NonNull CommunityCallback<JSONObject> callback) {
        StringBuilder ep = new StringBuilder("api/community/posts?limit=30");
        if (type != null && !type.trim().isEmpty() && !"ALL".equalsIgnoreCase(type)) {
            ep.append("&type=").append(type.trim().toUpperCase());
        }
        if (groupId != null && !groupId.trim().isEmpty()) {
            ep.append("&groupId=").append(groupId.trim());
        }
        if (cursor != null && cursor > 0) {
            ep.append("&cursor=").append(cursor);
        }

        ApiClient.get(context, ep.toString(), getAuthToken(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(int statusCode, @NonNull JSONObject json) {
                callback.onSuccess(json);
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                callback.onError(statusCode, errorMessage);
            }
        });
    }

    public void createPost(@NonNull String type, @NonNull String title, @NonNull String content,
                           @Nullable String mediaUrl, @Nullable String groupId,
                           @Nullable Long eventDate, @Nullable String eventLocation,
                           @NonNull CommunityCallback<JSONObject> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("type", type);
            body.put("title", title);
            body.put("content", content);
            if (mediaUrl != null && !mediaUrl.isEmpty()) body.put("mediaUrl", mediaUrl);
            if (groupId != null && !groupId.isEmpty()) body.put("groupId", groupId);
            if (eventDate != null) body.put("eventDate", eventDate);
            if (eventLocation != null && !eventLocation.isEmpty()) body.put("eventLocation", eventLocation);

            ApiClient.post(context, "api/community/posts", body, getAuthToken(), new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, @NonNull JSONObject json) {
                    callback.onSuccess(json);
                }

                @Override
                public void onError(int statusCode, @NonNull String errorMessage) {
                    callback.onError(statusCode, errorMessage);
                }
            });
        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }

    public void amenPost(@NonNull String postId, @NonNull CommunityCallback<JSONObject> callback) {
        ApiClient.post(context, "api/community/posts/" + postId + "/amen", new JSONObject(), getAuthToken(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(int statusCode, @NonNull JSONObject json) {
                callback.onSuccess(json);
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                callback.onError(statusCode, errorMessage);
            }
        });
    }

    public void fetchComments(@NonNull String postId, @NonNull CommunityCallback<JSONArray> callback) {
        ApiClient.get(context, "api/community/posts/" + postId + "/comments", getAuthToken(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(int statusCode, @NonNull JSONObject json) {
                callback.onSuccess(json.optJSONArray("comments"));
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                callback.onError(statusCode, errorMessage);
            }
        });
    }

    public void addComment(@NonNull String postId, @NonNull String content, @NonNull CommunityCallback<JSONObject> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("content", content);
            ApiClient.post(context, "api/community/posts/" + postId + "/comments", body, getAuthToken(), new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, @NonNull JSONObject json) {
                    callback.onSuccess(json);
                }

                @Override
                public void onError(int statusCode, @NonNull String errorMessage) {
                    callback.onError(statusCode, errorMessage);
                }
            });
        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }

    public void fetchGroups(@NonNull CommunityCallback<JSONArray> callback) {
        ApiClient.get(context, "api/community/groups", getAuthToken(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(int statusCode, @NonNull JSONObject json) {
                callback.onSuccess(json.optJSONArray("groups"));
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                callback.onError(statusCode, errorMessage);
            }
        });
    }

    public void fetchGroupMembers(@NonNull String groupId, @NonNull CommunityCallback<JSONArray> callback) {
        ApiClient.get(context, "api/community/groups/" + groupId + "/members", getAuthToken(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(int statusCode, @NonNull JSONObject json) {
                callback.onSuccess(json.optJSONArray("members"));
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                callback.onError(statusCode, errorMessage);
            }
        });
    }

    public void fetchConversations(@NonNull CommunityCallback<JSONArray> callback) {
        ApiClient.get(context, "api/community/conversations", getAuthToken(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(int statusCode, @NonNull JSONObject json) {
                callback.onSuccess(json.optJSONArray("conversations"));
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                callback.onError(statusCode, errorMessage);
            }
        });
    }

    public void fetchMessages(@NonNull String partnerId, @NonNull CommunityCallback<JSONArray> callback) {
        ApiClient.get(context, "api/community/messages/" + partnerId, getAuthToken(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(int statusCode, @NonNull JSONObject json) {
                callback.onSuccess(json.optJSONArray("messages"));
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                callback.onError(statusCode, errorMessage);
            }
        });
    }

    public void sendMessage(@NonNull String receiverId, @NonNull String content, @Nullable String mediaUrl,
                            @NonNull CommunityCallback<JSONObject> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("receiverId", receiverId);
            body.put("content", content);
            if (mediaUrl != null && !mediaUrl.isEmpty()) body.put("mediaUrl", mediaUrl);

            ApiClient.post(context, "api/community/messages", body, getAuthToken(), new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, @NonNull JSONObject json) {
                    callback.onSuccess(json);
                }

                @Override
                public void onError(int statusCode, @NonNull String errorMessage) {
                    callback.onError(statusCode, errorMessage);
                }
            });
        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }

    public void uploadPhoto(@NonNull String base64Data, @NonNull String filename,
                            @NonNull CommunityCallback<String> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("base64Data", base64Data);
            body.put("filename", filename);

            ApiClient.post(context, "api/community/media/upload", body, getAuthToken(), new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, @NonNull JSONObject json) {
                    callback.onSuccess(json.optString("mediaUrl"));
                }

                @Override
                public void onError(int statusCode, @NonNull String errorMessage) {
                    callback.onError(statusCode, errorMessage);
                }
            });
        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }

    public void rsvpEvent(@NonNull String postId, @NonNull String status,
                          @NonNull CommunityCallback<JSONObject> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("status", status);

            ApiClient.post(context, "api/community/posts/" + postId + "/rsvp", body, getAuthToken(), new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, @NonNull JSONObject json) {
                    callback.onSuccess(json);
                }

                @Override
                public void onError(int statusCode, @NonNull String errorMessage) {
                    callback.onError(statusCode, errorMessage);
                }
            });
        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }

    public void fetchTodayChallenge(@NonNull CommunityCallback<JSONObject> callback) {
        ApiClient.get(context, "api/community/challenge/today", getAuthToken(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(int statusCode, @NonNull JSONObject json) {
                callback.onSuccess(json);
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                callback.onError(statusCode, errorMessage);
            }
        });
    }

    public void submitChallenge(@NonNull String answer, @NonNull CommunityCallback<JSONObject> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("answer", answer);

            ApiClient.post(context, "api/community/challenge/submit", body, getAuthToken(), new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, @NonNull JSONObject json) {
                    callback.onSuccess(json);
                }

                @Override
                public void onError(int statusCode, @NonNull String errorMessage) {
                    callback.onError(statusCode, errorMessage);
                }
            });
        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }

    public void fetchNotifications(@NonNull CommunityCallback<JSONObject> callback) {
        ApiClient.get(context, "api/community/notifications", getAuthToken(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(int statusCode, @NonNull JSONObject json) {
                callback.onSuccess(json);
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                callback.onError(statusCode, errorMessage);
            }
        });
    }

    public void markNotificationsRead(@Nullable JSONArray notificationIds, @NonNull CommunityCallback<Boolean> callback) {
        try {
            JSONObject body = new JSONObject();
            if (notificationIds != null) body.put("notificationIds", notificationIds);

            ApiClient.post(context, "api/community/notifications/read", body, getAuthToken(), new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, @NonNull JSONObject json) {
                    callback.onSuccess(true);
                }

                @Override
                public void onError(int statusCode, @NonNull String errorMessage) {
                    callback.onError(statusCode, errorMessage);
                }
            });
        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }

    public void updateProfile(@NonNull String name, @NonNull String bio, @NonNull String avatarUrl,
                              @NonNull CommunityCallback<JSONObject> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("name", name);
            body.put("bio", bio);
            body.put("avatarUrl", avatarUrl);

            ApiClient.put(context, "api/community/profile", body, getAuthToken(), new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(int statusCode, @NonNull JSONObject json) {
                    callback.onSuccess(json);
                }

                @Override
                public void onError(int statusCode, @NonNull String errorMessage) {
                    callback.onError(statusCode, errorMessage);
                }
            });
        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }

    public void updateAvatar(@NonNull String avatarUrl, @NonNull CommunityCallback<JSONObject> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("avatarUrl", avatarUrl);
            ApiClient.put(context, "api/community/profile", body, getAuthToken(), new ApiClient.ApiCallback() {
                @Override public void onSuccess(int statusCode, @NonNull JSONObject json) {
                    callback.onSuccess(json);
                }
                @Override public void onError(int statusCode, @NonNull String errorMessage) {
                    callback.onError(statusCode, errorMessage);
                }
            });
        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }
}
