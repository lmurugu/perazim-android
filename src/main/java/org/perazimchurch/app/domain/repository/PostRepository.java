package org.perazimchurch.app.domain.repository;

import androidx.annotation.NonNull;
import org.perazimchurch.app.domain.model.Post;
import java.util.List;

/**
 * Repository interface for community posts.
 */
public interface PostRepository {
    /**
     * Retrieve all public posts.
     */
    @NonNull List<Post> getAllPublicPosts();

    /**
     * Retrieve posts by type.
     */
    @NonNull List<Post> getPostsByType(@NonNull String type);

    /**
     * Retrieve posts for a specific group.
     */
    @NonNull List<Post> getPostsByGroup(@NonNull String groupId);

    /**
     * Retrieve a single post by its id.
     */
    Post getPostById(@NonNull String postId);

    /**
     * Create or update a post.
     */
    void upsertPost(@NonNull Post post);

    /**
     * Delete a post.
     */
    void deletePost(@NonNull String postId);
}
