package org.dreamexposure.tap.backend.network.database;

import org.dreamexposure.novautils.database.DatabaseInfo;
import org.dreamexposure.tap.core.enums.blog.BlogType;
import org.dreamexposure.tap.core.enums.post.PostType;
import org.dreamexposure.tap.core.utils.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * @author NovaFox161
 * Date Created: 12/20/2018
 * For Project: TAP-Backend
 * Author Website: https://www.novamaday.com
 * Company Website: https://www.dreamexposure.org
 * Contact: nova@dreamexposure.org
 */
@SuppressWarnings({"UnusedReturnValue", "SqlNoDataSourceInspection", "Duplicates", "FieldCanBeLocal"})
public class DataCountHandling {
    private static DataCountHandling instance;

    private DatabaseInfo masterInfo;
    private DatabaseInfo slaveInfo;

    private DataCountHandling() {
    }

    public static DataCountHandling get() {
        if (instance == null) instance = new DataCountHandling();

        return instance;
    }

    void init(DatabaseInfo _master, DatabaseInfo _slave) {
        masterInfo = _master;
        slaveInfo = _slave;
    }

    public int getAccountCount() {
        int amount = -1;
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String tableName = String.format("%saccounts", slaveInfo.getSettings().getPrefix());

            String query = "SELECT COUNT(*) FROM " + tableName + ";";
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet res = statement.executeQuery();

            if (res.next())
                amount = res.getInt(1);
            else
                amount = 0;


            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception("Failed to get account count", e, true, this.getClass());
        }
        return amount;
    }

    public int getAuthCount(UUID accountId) {
        int amount = -1;
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String tableName = String.format("%sauth", slaveInfo.getSettings().getPrefix());

            String query = "SELECT COUNT(*) FROM " + tableName + " WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, accountId.toString());

            ResultSet res = statement.executeQuery();

            if (res.next())
                amount = res.getInt(1);
            else
                amount = 0;


            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception("Failed to get auth count for account.", e, true, this.getClass());
        }
        return amount;
    }

    public int getBlogCount() {
        int amount = -1;
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String tableName = String.format("%sblog", slaveInfo.getSettings().getPrefix());

            String query = "SELECT COUNT(*) FROM " + tableName + ";";
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet res = statement.executeQuery();

            if (res.next())
                amount = res.getInt(1);
            else
                amount = 0;


            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception("Failed to get blog count", e, true, this.getClass());
        }
        return amount;
    }

    public int getBlogCount(BlogType type) {
        int amount = -1;
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String tableName = String.format("%sblog", slaveInfo.getSettings().getPrefix());

            String query = "SELECT COUNT(*) FROM " + tableName + " WHERE blog_type = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, type.name());

            ResultSet res = statement.executeQuery();

            if (res.next())
                amount = res.getInt(1);
            else
                amount = 0;


            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception("Failed to get blog count", e, true, this.getClass());
        }
        return amount;
    }

    public int getBlogCount(UUID accountId) {
        int amount = -1;
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String tableName = String.format("%sblog", slaveInfo.getSettings().getPrefix());

            //Include personal AND group blogs
            String query = "SELECT COUNT(*) FROM " + tableName + " WHERE owner = ? OR owners LIKE ?;";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, accountId.toString());
            statement.setString(2, "%" + accountId.toString() + "%");

            ResultSet res = statement.executeQuery();

            if (res.next())
                amount = res.getInt(1);
            else
                amount = 0;


            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception("Failed to get blog count for account", e, true, this.getClass());
        }
        return amount;
    }

    public int getPostCount() {
        int amount = -1;
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String tableName = String.format("%spost", slaveInfo.getSettings().getPrefix());

            String query = "SELECT COUNT(*) FROM " + tableName + ";";
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet res = statement.executeQuery();

            if (res.next())
                amount = res.getInt(1);
            else
                amount = 0;


            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception("Failed to get post count", e, true, this.getClass());
        }
        return amount;
    }

    public int getPostCount(PostType type) {
        int amount = -1;
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String tableName = String.format("%spost", slaveInfo.getSettings().getPrefix());

            String query = "SELECT COUNT(*) FROM " + tableName + " WHERE post_type = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, type.name());

            ResultSet res = statement.executeQuery();

            if (res.next())
                amount = res.getInt(1);
            else
                amount = 0;


            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception("Failed to get post count by type", e, true, this.getClass());
        }
        return amount;
    }

    public int getPostCountForBlog(UUID blogId) {
        int amount = -1;
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String tableName = String.format("%spost", slaveInfo.getSettings().getPrefix());

            String query = "SELECT COUNT(*) FROM " + tableName + " WHERE origin_blog_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, blogId.toString());

            ResultSet res = statement.executeQuery();

            if (res.next())
                amount = res.getInt(1);
            else
                amount = 0;


            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception("Failed to get post count for blog", e, true, this.getClass());
        }
        return amount;
    }

    public int getPostCountForBlog(UUID blogId, PostType type) {
        int amount = -1;
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String tableName = String.format("%spost", slaveInfo.getSettings().getPrefix());

            String query = "SELECT COUNT(*) FROM " + tableName + " WHERE origin_blog_id = ? AND post_type = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, blogId.toString());
            statement.setString(2, type.name());

            ResultSet res = statement.executeQuery();

            if (res.next())
                amount = res.getInt(1);
            else
                amount = 0;


            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception("Failed to get post count for blog", e, true, this.getClass());
        }
        return amount;
    }

    public int getPostCountForAccount(UUID accountId) {
        int amount = -1;
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String tableName = String.format("%spost", slaveInfo.getSettings().getPrefix());

            String query = "SELECT COUNT(*) FROM " + tableName + " WHERE creator_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, accountId.toString());

            ResultSet res = statement.executeQuery();

            if (res.next())
                amount = res.getInt(1);
            else
                amount = 0;


            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception("Failed to get post count for account", e, true, this.getClass());
        }
        return amount;
    }

    public int getPostCountForAccount(UUID accountId, PostType type) {
        int amount = -1;
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String tableName = String.format("%spost", slaveInfo.getSettings().getPrefix());

            String query = "SELECT COUNT(*) FROM " + tableName + " WHERE creator_id = ? AND post_type = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, accountId.toString());
            statement.setString(2, type.name());

            ResultSet res = statement.executeQuery();

            if (res.next())
                amount = res.getInt(1);
            else
                amount = 0;


            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception("Failed to get post count for account", e, true, this.getClass());
        }
        return amount;
    }

    public int getFollowingCount(UUID user) {
        int amount = -1;
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String tableName = String.format("%sfollow", slaveInfo.getSettings().getPrefix());

            String query = "SELECT COUNT(*) FROM " + tableName + " WHERE user_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, user.toString());

            ResultSet res = statement.executeQuery();

            if (res.next())
                amount = res.getInt(1);
            else
                amount = 0;


            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception("Failed to get following count for user", e, true, this.getClass());
        }
        return amount;
    }

    public int getFollowerCount(UUID following) {
        int amount = -1;
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String tableName = String.format("%sfollow", slaveInfo.getSettings().getPrefix());

            String query = "SELECT COUNT(*) FROM " + tableName + " WHERE following_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, following.toString());

            ResultSet res = statement.executeQuery();

            if (res.next())
                amount = res.getInt(1);
            else
                amount = 0;


            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception("Failed to get follower count for user", e, true, this.getClass());
        }
        return amount;
    }

    public int getReblogCount(UUID postId) {
        int amount = -1;

        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String tableName = String.format("%spost", slaveInfo.getSettings().getPrefix());

            String query = "SELECT COUNT(*) FROM " + tableName + " WHERE parent = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, postId.toString());

            ResultSet res = statement.executeQuery();

            if (res.next())
                amount = res.getInt(1);
            else
                amount = 0;

            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception("Failed to get reblog count for post", e, true, this.getClass());
        }
        return amount;
    }

    public int getBookmarkCount(UUID postId) {
        int amount = -1;

        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String tableName = String.format("%sbookmark", slaveInfo.getSettings().getPrefix());

            String query = "SELECT COUNT(*) FROM " + tableName + " WHERE post_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, postId.toString());

            ResultSet res = statement.executeQuery();

            if (res.next())
                amount = res.getInt(1);
            else
                amount = 0;

            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception("Failed to get bookmark count for post", e, true, this.getClass());
        }
        return amount;
    }
}
