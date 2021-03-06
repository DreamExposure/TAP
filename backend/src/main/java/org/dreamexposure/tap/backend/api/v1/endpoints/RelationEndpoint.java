package org.dreamexposure.tap.backend.api.v1.endpoints;

import org.dreamexposure.tap.backend.network.auth.Authentication;
import org.dreamexposure.tap.backend.network.database.AccountDataHandler;
import org.dreamexposure.tap.backend.network.database.BlogDataHandler;
import org.dreamexposure.tap.backend.network.database.FollowerDataHandler;
import org.dreamexposure.tap.backend.objects.auth.AuthenticationState;
import org.dreamexposure.tap.backend.utils.ResponseUtils;
import org.dreamexposure.tap.core.objects.account.Account;
import org.dreamexposure.tap.core.objects.blog.GroupBlog;
import org.dreamexposure.tap.core.objects.blog.IBlog;
import org.dreamexposure.tap.core.objects.blog.PersonalBlog;
import org.dreamexposure.tap.core.utils.Logger;
import org.dreamexposure.tap.core.utils.MathsUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * @author NovaFox161
 * Date Created: 12/20/2018
 * For Project: TAP-Backend
 * Author Website: https://www.novamaday.com
 * Company Website: https://www.dreamexposure.org
 * Contact: nova@dreamexposure.org
 */
@SuppressWarnings("Duplicates")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/v1/relation")
public class RelationEndpoint {

    @PostMapping(value = "/follow", produces = "application/json")
    public static String follow(HttpServletRequest request, HttpServletResponse response, @RequestBody String requestBody) {
        //Authenticate...
        AuthenticationState authState = Authentication.authenticate(request);
        if (!authState.isSuccess()) {
            response.setStatus(authState.getStatus());
            response.setContentType("application/json");
            return authState.toJson();
        }

        try {
            JSONObject body = new JSONObject(requestBody);
            Account account = AccountDataHandler.get().getAccountFromId(authState.getId());

            UUID blogToFollow = UUID.fromString(body.getString("blog_id"));

            //Check and see if the user is a minor and the blog disallows minors...
            IBlog blog = BlogDataHandler.get().getBlog(blogToFollow);
            if (!blog.isAllowUnder18() && MathsUtils.determineAge(account.getBirthday()) < 18) {
                response.setContentType("application/json");
                response.setStatus(403);

                return ResponseUtils.getJsonResponseMessage("You cannot follow this blog as it is an adults only blog. Please come back when you are at least 18 years old");
            }

            FollowerDataHandler.get().follow(account.getAccountId(), blogToFollow);

            response.setContentType("application/json");
            response.setStatus(200);
            return ResponseUtils.getJsonResponseMessage("Success");
        } catch (JSONException | IllegalArgumentException e) {
            response.setContentType("application/json");
            response.setStatus(400);
            return ResponseUtils.getJsonResponseMessage("Bad Request");
        } catch (Exception e) {
            Logger.getLogger().exception("Failed to handle account data update.", e, true, BlogEndpoint.class);

            response.setContentType("application/json");
            response.setStatus(500);
            return ResponseUtils.getJsonResponseMessage("Internal Server Error");
        }
    }

    @PostMapping(value = "/unfollow", produces = "application/json")
    public static String unfollow(HttpServletRequest request, HttpServletResponse response, @RequestBody String requestBody) {
        //Authenticate...
        AuthenticationState authState = Authentication.authenticate(request);
        if (!authState.isSuccess()) {
            response.setStatus(authState.getStatus());
            response.setContentType("application/json");
            return authState.toJson();
        }

        try {
            JSONObject body = new JSONObject(requestBody);
            Account account = AccountDataHandler.get().getAccountFromId(authState.getId());

            UUID blogToFollow = UUID.fromString(body.getString("blog_id"));

            FollowerDataHandler.get().unfollow(account.getAccountId(), blogToFollow);

            response.setContentType("application/json");
            response.setStatus(200);
            return ResponseUtils.getJsonResponseMessage("Success");
        } catch (JSONException | IllegalArgumentException e) {
            response.setContentType("application/json");
            response.setStatus(400);
            return ResponseUtils.getJsonResponseMessage("Bad Request");
        } catch (Exception e) {
            Logger.getLogger().exception("Failed to handle account data update.", e, true, BlogEndpoint.class);

            response.setContentType("application/json");
            response.setStatus(500);
            return ResponseUtils.getJsonResponseMessage("Internal Server Error");
        }
    }

    @SuppressWarnings("RedundantCast")
    @PostMapping(value = "/get/following", produces = "application/json")
    public static String getFollowing(HttpServletRequest request, HttpServletResponse response, @RequestBody String requestBody) {
        //Authenticate...
        AuthenticationState authState = Authentication.authenticate(request);
        if (!authState.isSuccess()) {
            response.setStatus(authState.getStatus());
            response.setContentType("application/json");
            return authState.toJson();
        }

        try {
            JSONObject body = new JSONObject(requestBody);
            Account account = AccountDataHandler.get().getAccountFromId(authState.getId());

            JSONArray jBlogs = new JSONArray();
            for (IBlog b : FollowerDataHandler.get().getFollowingBlogList(account.getAccountId())) {
                if (b instanceof PersonalBlog)
                    jBlogs.put(((PersonalBlog) b).toJson());
                else
                    jBlogs.put(((GroupBlog) b).toJson());
            }

            body.put("message", "Success");
            body.put("count", jBlogs.length());
            body.put("blogs", jBlogs);

            response.setContentType("application/json");
            response.setStatus(200);
            return body.toString();
        } catch (JSONException | IllegalArgumentException e) {
            response.setContentType("application/json");
            response.setStatus(400);
            return ResponseUtils.getJsonResponseMessage("Bad Request");
        } catch (Exception e) {
            Logger.getLogger().exception("Failed to handle account data update.", e, true, BlogEndpoint.class);

            response.setContentType("application/json");
            response.setStatus(500);
            return ResponseUtils.getJsonResponseMessage("Internal Server Error");
        }
    }

    @PostMapping(value = "/get/followers", produces = "application/json")
    public static String getFollowers(HttpServletRequest request, HttpServletResponse response, @RequestBody String requestBody) {
        //Authenticate...
        AuthenticationState authState = Authentication.authenticate(request);
        if (!authState.isSuccess()) {
            response.setStatus(authState.getStatus());
            response.setContentType("application/json");
            return authState.toJson();
        }

        try {
            JSONObject body = new JSONObject(requestBody);

            UUID blogId = UUID.fromString(body.getString("blog_id"));

            JSONArray jAccounts = new JSONArray();
            for (Account a : FollowerDataHandler.get().getFollowersAccountList(blogId)) {
                jAccounts.put(a.toJsonNoPersonal());
            }

            body.put("message", "Success");
            body.put("count", jAccounts.length());
            body.put("accounts", jAccounts);

            response.setContentType("application/json");
            response.setStatus(200);
            return body.toString();
        } catch (JSONException | IllegalArgumentException e) {
            response.setContentType("application/json");
            response.setStatus(400);
            return ResponseUtils.getJsonResponseMessage("Bad Request");
        } catch (Exception e) {
            Logger.getLogger().exception("Failed to handle account data update.", e, true, BlogEndpoint.class);

            response.setContentType("application/json");
            response.setStatus(500);
            return ResponseUtils.getJsonResponseMessage("Internal Server Error");
        }
    }
}
