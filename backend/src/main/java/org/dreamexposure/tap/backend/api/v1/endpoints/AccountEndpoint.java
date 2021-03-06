package org.dreamexposure.tap.backend.api.v1.endpoints;

import de.triology.recaptchav2java.ReCaptcha;
import org.dreamexposure.novautils.crypto.KeyGenerator;
import org.dreamexposure.tap.backend.network.auth.Authentication;
import org.dreamexposure.tap.backend.network.database.AccountDataHandler;
import org.dreamexposure.tap.backend.network.database.AuthorizationDataHandler;
import org.dreamexposure.tap.backend.network.email.EmailHandler;
import org.dreamexposure.tap.backend.objects.auth.AuthenticationState;
import org.dreamexposure.tap.backend.utils.Generator;
import org.dreamexposure.tap.backend.utils.ResponseUtils;
import org.dreamexposure.tap.backend.utils.Sanitizer;
import org.dreamexposure.tap.backend.utils.Validator;
import org.dreamexposure.tap.core.conf.GlobalVars;
import org.dreamexposure.tap.core.conf.SiteSettings;
import org.dreamexposure.tap.core.objects.account.Account;
import org.dreamexposure.tap.core.objects.auth.AccountAuthentication;
import org.dreamexposure.tap.core.utils.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * @author NovaFox161
 * Date Created: 12/5/18
 * For Project: TAP-Backend
 * Author Website: https://www.novamaday.com
 * Company Website: https://www.dreamexposure.org
 * Contact: nova@dreamexposure.org
 */
@SuppressWarnings("Duplicates")
@RestController
@RequestMapping("/v1/account")
public class AccountEndpoint {
    @PostMapping(value = "/register", produces = "application/json")
    public static String register(HttpServletRequest request, HttpServletResponse response, @RequestBody String requestBody) {
        //Skip authentication, user is registering account and does not have auth credentials yet...
        
        
        JSONObject body = new JSONObject(requestBody);
        if (body.has("username") && body.has("email") && body.has("password") && body.has("gcap") && body.has("birthday")) {
            if (new ReCaptcha(SiteSettings.RECAP_KEY_SITE.get()).isValid(body.getString("gcap")) || new ReCaptcha(SiteSettings.RECAP_KEY_ANDROID.get()).isValid(body.getString("gcap")) || new ReCaptcha(SiteSettings.RECAP_KEY_IOS.get()).isValid(body.getString("gcap"))) {
                String username = Sanitizer.sanitizeUserInput(body.getString("username"));
                String email = body.getString("email");
                String birthday = body.getString("birthday");
                if (!AccountDataHandler.get().usernameOrEmailTaken(username, email)) {
                    if (username.length() < 4 || username.length() > 64) {
                        response.setContentType("application/json");
                        response.setStatus(400);

                        JSONObject responseBody = new JSONObject();
                        responseBody.put("message", "Username is too short or too long");

                        return responseBody.toString();
                    }
                    if (!Validator.validEmail(email)) {
                        response.setContentType("application/json");
                        response.setStatus(400);

                        JSONObject responseBody = new JSONObject();
                        responseBody.put("message", "Invalid email supplied");

                        return responseBody.toString();
                    }
                    if (!Validator.validBirthdate(birthday)) {
                        response.setContentType("application/json");
                        response.setStatus(400);

                        JSONObject responseBody = new JSONObject();
                        responseBody.put("message", "Birthday is invalid.");

                        return responseBody.toString();
                    }
                    //Generate hash and create account in the database.
                    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                    String hash = encoder.encode(body.getString("password"));

                    AccountDataHandler.get().createAccount(username, email, hash, birthday);
                    Account account = AccountDataHandler.get().getAccountFromEmail(email);
                    
                    //Send confirmation email!!!
                    EmailHandler.getHandler().sendEmailConfirm(email, Generator.generateEmailConfirmationLink(account));
                    
                    //Generate tokens...
                    AccountAuthentication auth = new AccountAuthentication();
                    auth.setAccountId(account.getAccountId());
                    auth.setAccessToken(KeyGenerator.csRandomAlphaNumericString(32));
                    auth.setRefreshToken(KeyGenerator.csRandomAlphaNumericString(32));
                    auth.setExpire(System.currentTimeMillis() + GlobalVars.oneDayMs); //Auth token good for 24 hours, unless manually revoked.
                    AuthorizationDataHandler.get().saveAuth(auth);
                    
                    Logger.getLogger().api("User registered account: " + account.getUsername(), request.getRemoteAddr());
                    
                    response.setContentType("application/json");
                    response.setStatus(200);
                    
                    JSONObject responseBody = new JSONObject();
                    responseBody.put("message", "Success");
                    responseBody.put("credentials", auth.toJson());
                    
                    return responseBody.toString();
                } else {
                    response.setContentType("application/json");
                    response.setStatus(400);
                    
                    JSONObject responseBody = new JSONObject();
                    responseBody.put("message", "Email/Username Invalid");
                    
                    return responseBody.toString();
                }
            } else {
                response.setContentType("application/json");
                response.setStatus(400);
                
                JSONObject responseBody = new JSONObject();
                responseBody.put("message", "Failed to verify ReCAPTCHA");
                
                return responseBody.toString();
            }
        } else {
            response.setContentType("application/json");
            response.setStatus(400);
            
            JSONObject responseBody = new JSONObject();
            responseBody.put("message", "Bad request");
            
            return responseBody.toString();
        }
    }
    
    @PostMapping(value = "/login", produces = "application/json")
    public static String login(HttpServletRequest request, HttpServletResponse response, @RequestBody String requestBody) {
        //Skip authentication, user is logging in and does not yet have auth.
        
        JSONObject body = new JSONObject(requestBody);
        if (body.has("email") && body.has("password") && body.has("gcap")) {
            if (new ReCaptcha(SiteSettings.RECAP_KEY_SITE.get()).isValid(body.getString("gcap")) || new ReCaptcha(SiteSettings.RECAP_KEY_ANDROID.get()).isValid(body.getString("gcap")) || new ReCaptcha(SiteSettings.RECAP_KEY_IOS.get()).isValid(body.getString("gcap"))) {
                String email = body.getString("email");
                if (AccountDataHandler.get().validLogin(email, body.getString("password"))) {

                    Account account = AccountDataHandler.get().getAccountFromEmail(email);

                    //Generate tokens...
                    AccountAuthentication auth = new AccountAuthentication();
                    auth.setAccountId(account.getAccountId());
                    auth.setAccessToken(KeyGenerator.csRandomAlphaNumericString(32));
                    auth.setRefreshToken(KeyGenerator.csRandomAlphaNumericString(32));
                    auth.setExpire(System.currentTimeMillis() + GlobalVars.oneDayMs); //Auth token good for 24 hours, unless manually revoked.
                    AuthorizationDataHandler.get().saveAuth(auth);
                    
                    Logger.getLogger().api("User logged into account: " + account.getUsername(), request.getRemoteAddr());
                    
                    response.setContentType("application/json");
                    response.setStatus(200);
                    
                    JSONObject responseBody = new JSONObject();
                    responseBody.put("message", "Success");
                    responseBody.put("credentials", auth.toJson());
                    
                    return responseBody.toString();
                } else {
                    response.setContentType("application/json");
                    response.setStatus(400);
                    
                    JSONObject responseBody = new JSONObject();
                    responseBody.put("message", "Email/Password Invalid");
                    
                    return responseBody.toString();
                }
            } else {
                response.setContentType("application/json");
                response.setStatus(400);
                
                JSONObject responseBody = new JSONObject();
                responseBody.put("message", "Failed to verify ReCAPTCHA");
                
                return responseBody.toString();
            }
        } else {
            response.setContentType("application/json");
            response.setStatus(400);
            
            JSONObject responseBody = new JSONObject();
            responseBody.put("message", "Bad request");
            
            return responseBody.toString();
        }
    }
    
    @PostMapping(value = "/logout", produces = "application/json")
    public static String logout(HttpServletRequest request, HttpServletResponse response) {
        if (request.getHeader("Authorization_Access") != null && request.getHeader("Authorization_Refresh") != null) {
            //User is currently logged in, we can now revoke access and confirm the logout.
            String accessToken = request.getHeader("Authorization_Access");
            String refreshToken = request.getHeader("Authorization_Refresh");

            AccountAuthentication auth = AuthorizationDataHandler.get().getAuthFromRefreshToken(refreshToken);
            if (auth == null)
                auth = AuthorizationDataHandler.get().getAuthFromAccessToken(accessToken);
            
            if (auth != null) {
                //Revoke credentials
                AuthorizationDataHandler.get().removeAuthByRefreshToken(auth.getRefreshToken());
                
                response.setContentType("application/json");
                response.setStatus(200);
                
                return ResponseUtils.getJsonResponseMessage("Success");
            } else {
                //Credentials were already revoked, technically this is a success.
                response.setContentType("application/json");
                response.setStatus(200);
                
                return ResponseUtils.getJsonResponseMessage("Success");
            }
        } else {
            //Credentials not provided, cannot revoke credentials if we don't know them.
            response.setContentType("application/json");
            response.setStatus(400);
            
            JSONObject responseBody = new JSONObject();
            responseBody.put("message", "Bad request");
            
            return responseBody.toString();
        }
    }
    
    @PostMapping(value = "/get", produces = "application/json")
    public static String get(HttpServletRequest request, HttpServletResponse response) {
        //Authenticate...
        AuthenticationState authState = Authentication.authenticate(request);
        if (!authState.isSuccess()) {
            response.setStatus(authState.getStatus());
            response.setContentType("application/json");
            return authState.toJson();
        }

        try {
            Account account = AccountDataHandler.get().getAccountFromId(authState.getId());

            response.setContentType("application/json");
            response.setStatus(200);

            JSONObject responseBody = new JSONObject();
            responseBody.put("message", "Success");
            responseBody.put("account", account.toJson());

            return responseBody.toString();
        } catch (Exception e) {
            Logger.getLogger().exception("Failed to handle account data get", e, true, BlogEndpoint.class);

            response.setContentType("application/json");
            response.setStatus(500);
            return ResponseUtils.getJsonResponseMessage("Internal Server Error");
        }
    }

    @PostMapping(value = "/get/other", produces = "application/json")
    public static String get(HttpServletRequest request, HttpServletResponse response, @RequestBody String requestBody) {
        //Authenticate...
        AuthenticationState authState = Authentication.authenticate(request);
        if (!authState.isSuccess()) {
            response.setStatus(authState.getStatus());
            response.setContentType("application/json");
            return authState.toJson();
        }

        try {
            JSONObject body = new JSONObject(requestBody);

            UUID id = UUID.fromString(body.getString("id"));

            Account account = AccountDataHandler.get().getAccountFromId(id);

            if (account != null) {
                response.setContentType("application/json");
                response.setStatus(200);

                JSONObject responseBody = new JSONObject();
                responseBody.put("message", "Success");
                responseBody.put("account", account.toJsonNoPersonal());

                return responseBody.toString();
            } else {
                response.setContentType("application/json");
                response.setStatus(404);

                JSONObject responseBody = new JSONObject();
                responseBody.put("message", "Account not found");

                return responseBody.toString();
            }
        } catch (JSONException | IllegalArgumentException e) {
            response.setContentType("application/json");
            response.setStatus(400);
            return ResponseUtils.getJsonResponseMessage("Bad Request");
        } catch (Exception e) {
            Logger.getLogger().exception("Failed to handle account data get", e, true, BlogEndpoint.class);

            response.setContentType("application/json");
            response.setStatus(500);
            return ResponseUtils.getJsonResponseMessage("Internal Server Error");
        }
    }
    
    @PostMapping(value = "/update", produces = "application/json")
    public static String update(HttpServletRequest request, HttpServletResponse response, @RequestBody String requestBody) {
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
            if (body.has("email")) {
                if (new ReCaptcha(SiteSettings.RECAP_KEY_SITE.get()).isValid(body.getString("gcap")) || new ReCaptcha(SiteSettings.RECAP_KEY_ANDROID.get()).isValid(body.getString("gcap")) || new ReCaptcha(SiteSettings.RECAP_KEY_IOS.get()).isValid(body.getString("gcap"))) {
                    if (AccountDataHandler.get().validLogin(account.getEmail(), body.getString("password"))) {
                        if (!AccountDataHandler.get().emailTaken(body.getString("email"))) {
                            if (!Validator.validEmail(body.getString("email"))) {
                                response.setContentType("application/json");
                                response.setStatus(400);
                                return ResponseUtils.getJsonResponseMessage("Invalid email specified");
                            }
                            account.setEmail(body.getString("email"));
                            account.setEmailConfirmed(false);

                            AccountDataHandler.get().updateAccount(account);
                            //Send confirmation email!!!
                            EmailHandler.getHandler().sendEmailConfirm(body.getString("email"), Generator.generateEmailConfirmationLink(account));

                            //Respond
                            response.setContentType("application/json");
                            response.setStatus(200);

                            JSONObject responseBody = new JSONObject();
                            responseBody.put("message", "Success");

                            return responseBody.toString();
                        } else {
                            //Email taken
                            response.setContentType("application/json");
                            response.setStatus(400);

                            JSONObject responseBody = new JSONObject();
                            responseBody.put("message", "Email Taken");

                            return responseBody.toString();
                        }
                    } else {
                        //Failed to verify login.
                        response.setContentType("application/json");
                        response.setStatus(400);

                        JSONObject responseBody = new JSONObject();
                        responseBody.put("message", "Password Invalid");

                        return responseBody.toString();
                    }
                } else {
                    response.setContentType("application/json");
                    response.setStatus(400);

                    JSONObject responseBody = new JSONObject();
                    responseBody.put("message", "Failed to verify ReCAPTCHA");

                    return responseBody.toString();
                }
            } else if (body.has("password")) {
                if (new ReCaptcha(SiteSettings.RECAP_KEY_SITE.get()).isValid(body.getString("gcap")) || new ReCaptcha(SiteSettings.RECAP_KEY_ANDROID.get()).isValid(body.getString("gcap")) || new ReCaptcha(SiteSettings.RECAP_KEY_IOS.get()).isValid(body.getString("gcap"))) {
                    if (AccountDataHandler.get().validLogin(account.getEmail(), body.getString("old_password"))) {
                        //Update password...
                        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                        String hash = encoder.encode(body.getString("password"));

                        AccountDataHandler.get().updateAccountHash(account, hash);

                        //Invalidate existing sessions...
                        AuthorizationDataHandler.get().removeAuth(account.getAccountId());

                        //Generate new tokens...
                        AccountAuthentication auth = new AccountAuthentication();
                        auth.setAccountId(account.getAccountId());
                        auth.setAccessToken(KeyGenerator.csRandomAlphaNumericString(32));
                        auth.setRefreshToken(KeyGenerator.csRandomAlphaNumericString(32));
                        auth.setExpire(System.currentTimeMillis() + GlobalVars.oneDayMs); //Auth token good for 24 hours, unless manually revoked.
                        AuthorizationDataHandler.get().saveAuth(auth);

                        //Respond
                        response.setContentType("application/json");
                        response.setStatus(200);

                        JSONObject responseBody = new JSONObject();
                        responseBody.put("message", "Success");
                        responseBody.put("credentials", auth.toJson());

                        return responseBody.toString();
                    } else {
                        //Password incorrect
                        response.setContentType("application/json");
                        response.setStatus(400);

                        JSONObject responseBody = new JSONObject();
                        responseBody.put("message", "Password Invalid");

                        return responseBody.toString();
                    }
                } else {
                    //Failed to verify recap
                    response.setContentType("application/json");
                    response.setStatus(400);

                    JSONObject responseBody = new JSONObject();
                    responseBody.put("message", "Failed to verify ReCAPTCHA");

                    return responseBody.toString();
                }
            } else if (body.has("safe_search")) {
                //Update safe search
                account.setSafeSearch(body.getBoolean("safe_search"));
                AccountDataHandler.get().updateAccount(account);

                //Respond
                response.setContentType("application/json");
                response.setStatus(200);

                JSONObject responseBody = new JSONObject();
                responseBody.put("message", "Success");

                return responseBody.toString();
            } else if (body.has("phone_number")) {
                if (!Validator.validPhoneNumber(body.getString("phone_number"))) {
                    response.setContentType("application/json");
                    response.setStatus(400);
                    return ResponseUtils.getJsonResponseMessage("Invalid phone number");
                }

                account.setPhoneNumber(body.getString("phone_number"));
                AccountDataHandler.get().updateAccount(account);

                //Respond
                response.setContentType("application/json");
                response.setStatus(200);

                JSONObject responseBody = new JSONObject();
                responseBody.put("message", "Success");

                return responseBody.toString();
            } else {
                response.setContentType("application/json");
                response.setStatus(400);
                return ResponseUtils.getJsonResponseMessage("Bad Request");
            }
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
