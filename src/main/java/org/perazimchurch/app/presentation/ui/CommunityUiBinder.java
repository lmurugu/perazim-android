package org.perazimchurch.app.presentation.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import org.perazimchurch.app.community.CommunityLocalStore;
import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.data.remote.ApiClient;
import org.perazimchurch.app.ui.theme.PerazimTheme;
import java.text.DateFormat;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.UUID;

/** Reachable community flows backed by the PC API; cache and drafts never imply delivery. */
public final class CommunityUiBinder {
    private final Activity activity;
    private final Runnable openPrayer;
    private final LinearLayout content;
    private final LinearLayout feed;
    private final TextView status;
    private String owner;
    private String server;
    private CommunityLocalStore store;
    private String filter = "ALL";
    private static final int PICK_PHOTO = 6701;
    private interface PhotoChoice { void selected(String url); }
    private PhotoChoice pendingPhoto;
    private final java.util.concurrent.ExecutorService images = java.util.concurrent.Executors.newSingleThreadExecutor();

    private interface Result { void success(JSONObject result); }
    private interface Failure { void failure(int code, String message); }

    public CommunityUiBinder(Activity activity, Runnable openPrayer) {
        this.activity = activity; this.openPrayer = openPrayer;
        content = column(); feed = column(); status = text("", 13);
        resetScope();
    }
    private void resetScope() {
        owner = session().getActiveUserId(); server = ApiClient.getBaseUrl(activity);
        store = new CommunityLocalStore(activity, server, owner);
    }
    private SessionManager session() { return SessionManager.getInstance(activity); }
    private boolean sameScope() {
        return owner.equals(session().getActiveUserId()) && server.equals(ApiClient.getBaseUrl(activity));
    }
    private boolean signedIn() {
        if (session().isGuest()) {
            alert("Sign in to participate", "You can read public discussions and play games offline. Sign in from Profile to post, use private groups, or message members.");
            return false;
        }
        return true;
    }
    public ScrollView bind() {
        content.setPadding(dp(16), dp(20), dp(16), dp(32));
        content.addView(text("Community", 26));
        content.addView(text("Faith grows when we share life together.", 14));
        row(content, button("Groups", this::showGroups), button("Classes", this::showClasses));
        row(content, button("Messages", this::showConversations), button("Notifications", this::showNotifications));
        row(content, button("Prayer wall", openPrayer), button("Bible games", () ->
                org.perazimchurch.app.presentation.ui.games.GamesHubDialog.show(activity)));
        row(content, button("My profile", this::editProfile), button("PC connection", this::showConnection));
        row(content, button("+ Share a post", () -> composePost(null, null)), button("Saved drafts", this::showDrafts));
        row(content, button("Daily challenge", this::showChallenge), button("Refresh", this::refresh));
        Spinner filters = spinner(new String[]{"All discussions", "General", "Prayer", "Praise", "Events"});
        filters.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int n, long id) {
                filter = new String[]{"ALL", "GENERAL", "PRAYER", "PRAISE", "EVENT"}[n]; refresh();
            }
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        content.addView(filters); content.addView(status); content.addView(feed);
        refresh(); return scroll(content);
    }
    public void refresh() {
        if (!sameScope()) { resetScope(); feed.removeAllViews(); }
        String key = "feed_" + filter;
        renderPosts(feed, store.cached(key).optJSONArray("posts"), null);
        status.setText("Refreshing… Saved drafts remain on this phone until sent.");
        get("api/community/posts?limit=30&type=" + filter, response -> {
            store.cache(key, response); renderPosts(feed, response.optJSONArray("posts"), null);
            status.setText(session().isGuest() ? "Public discussions · Sign in from Profile to join in" : "Connected · " + store.drafts().length() + " saved drafts");
        }, (code, message) -> status.setText(error(code, message) + "\nShowing saved discussions. Tap Refresh to reconnect."));
    }
    private void renderPosts(LinearLayout target, JSONArray posts, String groupId) {
        target.removeAllViews();
        if (posts == null || posts.length() == 0) { target.addView(text("No discussions here yet. Share a thought, prayer, or praise.", 15)); return; }
        for (int i = 0; i < posts.length(); i++) {
            JSONObject post = posts.optJSONObject(i); if (post == null) continue;
            LinearLayout card = card(target);
            card.addView(text((post.optBoolean("is_pinned") ? "Pinned · " : "") + post.optString("type", "GENERAL") + " · " + post.optString("author_name") + "\n" + date(post.optLong("created_at")), 12));
            card.addView(text(post.optString("title"), 19));
            card.addView(text(post.optString("content"), 16));
            if (!post.optString("media_url").isEmpty()) addPhoto(card, post.optString("media_url"));
            if (post.optBoolean("is_answered")) card.addView(text("Prayer answered", 14));
            if ("EVENT".equals(post.optString("type"))) card.addView(text(date(post.optLong("event_date")) + "\n" + post.optString("event_location"), 14));
            row(card, button((post.optBoolean("user_has_amened") ? "Amen ✓ " : "Amen ") + post.optInt("amen_count"), () -> {
                if (!signedIn()) return;
                post("api/community/posts/" + encode(post.optString("id")) + "/amen", new JSONObject(), r -> {
                    postSafe(post, "amen_count", r.optInt("amenCount")); postSafe(post, "user_has_amened", true);
                    renderPosts(target, posts, groupId);
                }, this::showError);
            }), button("Comments · " + post.optInt("comment_count"), () -> showPost(post, groupId)));
        }
    }
    private void showPost(JSONObject post, String groupId) {
        LinearLayout body = column();
        body.addView(text(post.optString("title"), 21)); body.addView(text(post.optString("content"), 16));
        if (!post.optString("media_url").isEmpty()) addPhoto(body,post.optString("media_url"));
        if (owner.equals(post.optString("author_id"))) {
            row(body, button("Edit", () -> editPost(post)), button("Delete", () -> new AlertDialog.Builder(activity)
                    .setTitle("Delete this post?").setMessage("Comments and reactions will also be removed.")
                    .setNegativeButton("Keep", null).setPositiveButton("Delete", (d,w) -> request("DELETE", "api/community/posts/" + encode(post.optString("id")), null,
                            r -> { toast("Post deleted"); refresh(); }, this::showError)).show()));
            if ("PRAYER".equals(post.optString("type")) && !post.optBoolean("is_answered")) body.addView(button("Mark prayer answered", () -> {
                JSONObject value = new JSONObject(); postSafe(value, "isAnswered", true);
                request("PUT", "api/community/posts/" + encode(post.optString("id")), value, r -> { toast("Prayer marked answered"); refresh(); }, this::showError);
            }));
        }
        if ("EVENT".equals(post.optString("type"))) {
            body.addView(text("When: " + date(post.optLong("event_date")) + "\nWhere: " + post.optString("event_location"), 15));
            row(body, button("I'm attending", () -> rsvp(post, "ATTENDING")), button("Can't attend", () -> rsvp(post, "DECLINED")));
        }
        LinearLayout comments = column(); body.addView(comments);
        EditText input = input("Write a thoughtful comment", true); body.addView(input);
        Button send = button("Send comment", () -> {
            if (!signedIn() || !required(input)) return;
            JSONObject value = new JSONObject(); postSafe(value, "id", id("comm")); postSafe(value, "content", input.getText().toString().trim());
            saveAndSend("api/community/posts/" + encode(post.optString("id")) + "/comments", value, "Comment: " + post.optString("title"), r -> {
                input.setText(""); loadComments(comments, post.optString("id")); refresh();
            });
        }); body.addView(send);
        panel("Discussion", body); loadComments(comments, post.optString("id"));
    }
    private void loadComments(LinearLayout target, String postId) {
        target.removeAllViews(); target.addView(text("Loading comments…", 14));
        get("api/community/posts/" + encode(postId) + "/comments", r -> {
            target.removeAllViews(); JSONArray rows = r.optJSONArray("comments");
            if (rows == null || rows.length() == 0) target.addView(text("Start the conversation.", 14));
            if (rows != null) for (int i=0;i<rows.length();i++) { JSONObject c=rows.optJSONObject(i); if(c!=null) {
                LinearLayout card=card(target);card.addView(text(c.optString("author_name") + " · " + date(c.optLong("created_at")),12));card.addView(text(c.optString("content"),16));
            }}
        }, (c,m)->{target.removeAllViews();target.addView(text(error(c,m),14));});
    }
    private void composePost(String groupId, Runnable after) {
        if (!signedIn()) return;
        LinearLayout body=column(); Spinner type=spinner(new String[]{"GENERAL","PRAYER","PRAISE","EVENT"});body.addView(type);
        EditText title=input("Title",false), message=input("Share your thought…",true), location=input("Event location (events only)",false);
        body.addView(title);body.addView(message);body.addView(location);
        String[] photo={""}; TextView photoState=text("No photo attached",13); body.addView(photoState);
        row(body,button("Attach photo",()->pickPhoto(url->{photo[0]=url;photoState.setText("Photo ready to publish");})),button("Remove photo",()->{photo[0]="";photoState.setText("No photo attached");}));
        long[] when={System.currentTimeMillis()+3600000};Button date=button("Event time: "+date(when[0]),()->{});
        date.setOnClickListener(v->chooseDate(when,date));body.addView(date);
        AlertDialog dialog=panel("Share with "+(groupId==null?"the community":"your group"),body);
        body.addView(button("Publish",()->{
            if(!required(title)||!required(message))return;
            JSONObject value=new JSONObject();postSafe(value,"id",id("post"));postSafe(value,"type",type.getSelectedItem().toString());
            postSafe(value,"title",title.getText().toString().trim());postSafe(value,"content",message.getText().toString().trim());postSafe(value,"mediaUrl",photo[0]);
            if(groupId!=null)postSafe(value,"groupId",groupId);
            if("EVENT".equals(type.getSelectedItem())) {postSafe(value,"eventDate",when[0]);postSafe(value,"eventLocation",location.getText().toString().trim());}
            saveAndSend("api/community/posts",value,"Post: "+title.getText(),r->{dialog.dismiss();refresh();if(after!=null)after.run();});
        }));
    }
    private void editPost(JSONObject original) {
        LinearLayout body=column();EditText title=input("Title",false),message=input("Content",true);
        title.setText(original.optString("title"));message.setText(original.optString("content"));body.addView(title);body.addView(message);
        AlertDialog dialog=panel("Edit post",body);body.addView(button("Save changes",()->{
            if(!required(title)||!required(message))return;JSONObject value=new JSONObject();postSafe(value,"title",title.getText().toString().trim());postSafe(value,"content",message.getText().toString().trim());
            request("PUT","api/community/posts/"+encode(original.optString("id")),value,r->{dialog.dismiss();refresh();},this::showError);
        }));
    }
    private void rsvp(JSONObject event,String state) {
        if(!signedIn())return;JSONObject body=new JSONObject();postSafe(body,"status",state);
        post("api/community/posts/"+encode(event.optString("id"))+"/rsvp",body,r->toast("RSVP saved · "+r.optInt("attendingCount")+" attending"),this::showError);
    }
    public void showGroups() {
        if(!signedIn())return;LinearLayout body=column();body.addView(text("Private spaces for study, fellowship, and ministry. The owner adds members.",14));
        body.addView(button("+ Create a group",this::createGroup));LinearLayout list=column();body.addView(list);panel("My groups",body);
        getCached("groups","api/community/groups",r->{list.removeAllViews();JSONArray rows=r.optJSONArray("groups");
            if(rows==null||rows.length()==0)list.addView(text("No groups yet. Create one, or ask a group owner to add you.",15));
            if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject g=rows.optJSONObject(i);if(g!=null){LinearLayout card=card(list);card.addView(text(g.optString("name"),19));card.addView(text(g.optString("description"),14));card.addView(button("Open · "+g.optInt("member_count")+" members",()->showGroup(g)));}}
        });
    }
    private void createGroup() {
        LinearLayout body=column();EditText name=input("Group name",false),description=input("What is this group for?",true);body.addView(name);body.addView(description);
        AlertDialog dialog=panel("Create a private group",body);body.addView(button("Create group",()->{
            if(!required(name))return;JSONObject data=new JSONObject();postSafe(data,"id",id("group"));postSafe(data,"name",name.getText().toString().trim());postSafe(data,"description",description.getText().toString().trim());
            post("api/community/groups",data,r->{dialog.dismiss();showGroup(r.optJSONObject("group"));},this::showError);
        }));
    }
    private void showGroup(JSONObject group) {
        if(group==null)return;String groupId=group.optString("id");LinearLayout body=column();body.addView(text(group.optString("description"),16));
        row(body,button("Members",()->showMembers(group)),button("Schedule class",()->scheduleClass(group)));
        LinearLayout list=column();Runnable load=()->getCached("group_"+groupId,"api/community/posts?groupId="+encode(groupId),r->renderPosts(list,r.optJSONArray("posts"),groupId));
        row(body,button("+ Group post",()->composePost(groupId,load)),button("Refresh",load));body.addView(list);panel(group.optString("name"),body);load.run();
    }
    private void showMembers(JSONObject group) {
        String groupId=group.optString("id");LinearLayout body=column();boolean isOwner=owner.equals(group.optString("created_by"));
        if(isOwner)body.addView(button("Add a member",()->chooseMember(member->{JSONObject data=new JSONObject();postSafe(data,"userId",member.optString("id"));post("api/community/groups/"+encode(groupId)+"/members",data,r->toast("Member added"),this::showError);})));
        LinearLayout list=column();body.addView(list);panel("Group members",body);
        get("api/community/groups/"+encode(groupId)+"/members",r->{list.removeAllViews();JSONArray rows=r.optJSONArray("members");if(rows!=null)for(int i=0;i<rows.length();i++){
            JSONObject m=rows.optJSONObject(i);if(m==null)continue;LinearLayout card=card(list);
            LinearLayout memberHead=new LinearLayout(activity);memberHead.setGravity(Gravity.CENTER_VERTICAL);
            ImageView avatar=memberAvatar(m.optString("avatar_url"),m.optString("user_id"));
            memberHead.addView(avatar,new LinearLayout.LayoutParams(dp(44),dp(44)));
            TextView memberName=text(m.optString("name")+" · "+m.optString("role"),16);memberHead.addView(memberName,new LinearLayout.LayoutParams(0,-2,1));
            card.addView(memberHead);
            if(!this.owner.equals(m.optString("user_id")))card.addView(button("Message",()->showConversation(m.optString("user_id"),m.optString("name"))));
            if(isOwner&&!group.optString("created_by").equals(m.optString("user_id")))card.addView(button("Remove from group",()->new AlertDialog.Builder(activity).setTitle("Remove "+m.optString("name")+"?").setNegativeButton("Keep",null).setPositiveButton("Remove",(d,w)->request("DELETE","api/community/groups/"+encode(groupId)+"/members/"+encode(m.optString("user_id")),null,x->toast("Member removed; reopen roster to refresh"),this::showError)).show()));
        }},this::showError);
    }
    public void showClasses() {
        if(!signedIn())return;LinearLayout body=column();body.addView(text("Schedule teaching inside a group you own. Members see their group's classes here.",14));
        body.addView(button("Choose a group to schedule",this::showGroups));LinearLayout list=column();body.addView(list);panel("Classes",body);
        getCached("classes","api/community/classes",r->{list.removeAllViews();JSONArray rows=r.optJSONArray("classes");
            if(rows==null||rows.length()==0)list.addView(text("No classes scheduled yet.",16));
            if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject c=rows.optJSONObject(i);if(c==null)continue;LinearLayout card=card(list);
                card.addView(text((c.optBoolean("is_cancelled")?"Cancelled · ":"")+c.optString("title"),20));card.addView(text(c.optString("group_name")+" · "+c.optString("teacher_name")+"\n"+date(c.optLong("starts_at"))+" · "+c.optInt("duration_minutes")+" minutes",14));card.addView(text(c.optString("description"),16));
                if(!c.optBoolean("is_cancelled"))card.addView(button("Join video classroom", () -> org.perazimchurch.app.ClassroomCallActivity.open(activity,c.optString("id"),c.optString("title"))));
                if(owner.equals(c.optString("created_by"))&&!c.optBoolean("is_cancelled"))card.addView(button("Cancel class",()->new AlertDialog.Builder(activity).setTitle("Cancel this class?").setNegativeButton("Keep",null).setPositiveButton("Cancel class",(d,w)->request("DELETE","api/community/classes/"+encode(c.optString("id")),null,x->toast("Class cancelled; reopen Classes to refresh"),this::showError)).show()));
            }});
    }
    private void scheduleClass(JSONObject group) {
        if(!owner.equals(group.optString("created_by"))){alert("Group owner schedules classes","Ask this group's owner to schedule a class, or create your own group.");return;}
        LinearLayout body=column();EditText title=input("Class title",false),description=input("Topic, preparation and resources",true),duration=input("Duration in minutes (5–480)",false);duration.setInputType(InputType.TYPE_CLASS_NUMBER);duration.setText("60");
        body.addView(title);body.addView(description);body.addView(duration);long[] when={System.currentTimeMillis()+3600000};Button date=button("Starts: "+date(when[0]),()->{});date.setOnClickListener(v->chooseDate(when,date));body.addView(date);
        AlertDialog dialog=panel("Schedule · "+group.optString("name"),body);body.addView(button("Schedule class",()->{
            if(!required(title))return;int minutes;try{minutes=Integer.parseInt(duration.getText().toString());}catch(Exception e){duration.setError("Enter minutes");return;}if(minutes<5||minutes>480){duration.setError("Use 5–480 minutes");return;}
            JSONObject data=new JSONObject();postSafe(data,"id",id("class"));postSafe(data,"groupId",group.optString("id"));postSafe(data,"title",title.getText().toString().trim());postSafe(data,"description",description.getText().toString().trim());postSafe(data,"startsAt",when[0]);postSafe(data,"durationMinutes",minutes);
            post("api/community/classes",data,r->{dialog.dismiss();showClasses();},this::showError);
        }));
    }
    public void showConversations() {
        if(!signedIn())return;LinearLayout body=column();body.addView(button("+ New private message",()->chooseMember(m->showConversation(m.optString("id"),m.optString("name")))));
        body.addView(text("Sent means stored on the PC server. Read appears only after the recipient opens the chat.",13));LinearLayout list=column();body.addView(list);panel("Private messages",body);
        getCached("conversations","api/community/conversations",r->{list.removeAllViews();JSONArray rows=r.optJSONArray("conversations");if(rows==null||rows.length()==0)list.addView(text("No conversations yet. Start one with a member.",15));
            if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject c=rows.optJSONObject(i);if(c==null)continue;LinearLayout card=card(list);card.addView(text(c.optString("partner_name")+(c.optInt("unread_count")>0?" · "+c.optInt("unread_count")+" unread":""),18));card.addView(text(c.optString("last_message_content"),15));card.addView(button("Open chat",()->showConversation(c.optString("partner_id"),c.optString("partner_name"))));}
        });
    }
    private interface MemberChoice { void selected(JSONObject member); }
    private void chooseMember(MemberChoice choice) {
        LinearLayout body=column();EditText search=input("Search members by name",false);body.addView(search);LinearLayout list=column();body.addView(list);AlertDialog dialog=panel("Choose a member",body);
        Runnable load=()->get("api/community/members?search="+encode(search.getText().toString()),r->{list.removeAllViews();JSONArray rows=r.optJSONArray("members");if(rows==null||rows.length()==0)list.addView(text("No members found.",15));if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject m=rows.optJSONObject(i);if(m!=null)addMemberChoice(list,m,()->{dialog.dismiss();choice.selected(m);});}},this::showError);
        body.addView(button("Search",load),1);load.run();
    }
    private void addMemberChoice(LinearLayout parent,JSONObject member,Runnable selected) {
        LinearLayout item=new LinearLayout(activity);item.setOrientation(LinearLayout.HORIZONTAL);item.setGravity(Gravity.CENTER_VERTICAL);
        ImageView avatar=memberAvatar(member.optString("avatar_url"),member.optString("id"));
        item.addView(avatar,new LinearLayout.LayoutParams(dp(44),dp(44)));
        TextView name=text(member.optString("name"),16);item.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        item.setFocusable(true);item.setContentDescription("Choose member "+member.optString("name"));item.setOnClickListener(v->selected.run());
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,dp(4),0,dp(4));parent.addView(item,params);
    }
    private ImageView memberAvatar(String avatarRef,String userId) {
        ImageView image=new ImageView(activity);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setContentDescription("Profile picture for "+userId);
        int drawable=ProfileAvatarHelper.drawableId(activity,ProfileAvatarHelper.numberFor(avatarRef,userId));
        image.setImageResource(drawable!=0?drawable:org.perazimchurch.app.R.drawable.ic_perazim_logo);
        Object tag=userId+"|"+avatarRef;image.setTag(tag);
        if(!avatarRef.startsWith("/uploads/"))return image;
        String token=session().getAuthToken(),base=ApiClient.getBaseUrl(activity).replaceAll("/+$","");
        if(token==null||token.trim().isEmpty())return image;
        images.execute(()->{Bitmap bitmap=null;HttpURLConnection connection=null;try{
            URL url=new URL(base+avatarRef);if(!ApiClient.allowsProtectedTransport(url))return;
            connection=(HttpURLConnection)url.openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(8000);connection.setReadTimeout(8000);
            connection.setRequestProperty("Authorization","Bearer "+token.trim());connection.setRequestProperty("Accept","image/*");
            int length=connection.getContentLength();if(connection.getResponseCode()==200&&(length<=0||length<=1024*1024)){try(InputStream stream=connection.getInputStream()){BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=4;bitmap=BitmapFactory.decodeStream(stream,null,options);}}
        }catch(Exception ignored){}finally{if(connection!=null)connection.disconnect();}
            Bitmap loaded=bitmap;if(loaded!=null)activity.runOnUiThread(()->{if(!activity.isFinishing()&&sameScope()&&tag.equals(image.getTag()))image.setImageBitmap(loaded);});
        });
        return image;
    }
    public void showConversation(String partnerId,String name) {
        if(!signedIn())return;LinearLayout body=column();LinearLayout messages=column();body.addView(messages);EditText input=input("Private message",true);body.addView(input);
        String[] photo={""};TextView photoState=text("",13);body.addView(photoState);
        row(body,button("Attach photo",()->pickPhoto(url->{photo[0]=url;photoState.setText("Photo ready to send");})),button("Remove photo",()->{photo[0]="";photoState.setText("");}));
        Runnable load=()->getCached("chat_"+partnerId,"api/community/messages/"+encode(partnerId),r->{messages.removeAllViews();JSONArray rows=r.optJSONArray("messages"),read=new JSONArray();
            if(rows==null||rows.length()==0)messages.addView(text("Say hello. Your conversation is private between you and this member.",15));
            if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject m=rows.optJSONObject(i);if(m==null)continue;boolean mine=owner.equals(m.optString("sender_id"));LinearLayout card=card(messages);card.addView(text(mine?"You":m.optString("sender_name",name),12));card.addView(text(m.optString("content"),17));if(!m.optString("media_url").isEmpty())addPhoto(card,m.optString("media_url"));card.addView(text(date(m.optLong("created_at"))+(mine?" · "+m.optString("delivery_status"):""),12));
                if(!mine&&!"READ".equals(m.optString("delivery_status")))read.put(m.optString("id"));
            }
            // A cached render never acknowledges read; only a successful live fetch may.
        });
        row(body,button("Send",()->{
            if(photo[0].isEmpty()&&!required(input))return;JSONObject value=new JSONObject();postSafe(value,"id",id("msg"));postSafe(value,"receiverId",partnerId);postSafe(value,"content",input.getText().toString().trim());postSafe(value,"mediaUrl",photo[0]);
            saveAndSend("api/community/messages",value,"Message to "+name,r->{input.setText("");photo[0]="";photoState.setText("");load.run();});
        }),button("Refresh",()->{load.run();acknowledgeConversation(partnerId);}));
        panel(name,body);load.run();acknowledgeConversation(partnerId);
    }
    private void acknowledgeConversation(String partnerId) {
        get("api/community/messages/"+encode(partnerId),r->{JSONArray rows=r.optJSONArray("messages"),ids=new JSONArray();if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject m=rows.optJSONObject(i);if(m!=null&&owner.equals(m.optString("receiver_id"))&&!"READ".equals(m.optString("delivery_status")))ids.put(m.optString("id"));}
            if(ids.length()>0){JSONObject data=new JSONObject();postSafe(data,"messageIds",ids);postSafe(data,"status","READ");post("api/community/messages/ack",data,x->{},(c,m)->{});}},(c,m)->{});
    }
    private void showDrafts() {
        if(!signedIn())return;LinearLayout body=column();JSONArray drafts=store.drafts();
        if(drafts.length()==0)body.addView(text("No saved drafts. Posts, comments and messages that could not send will be kept here for this account and server.",16));
        for(int i=0;i<drafts.length();i++){JSONObject draft=drafts.optJSONObject(i);if(draft==null)continue;JSONObject value=draft.optJSONObject("body");if(value==null)continue;LinearLayout card=card(body);card.addView(text(draft.optString("label"),17));card.addView(text(value.optString("content"),16));
            row(card,button("Retry sending",()->saveAndSend(draft.optString("endpoint"),value,draft.optString("label"),r->{toast("Sent successfully");refresh();card.setVisibility(View.GONE);})),button("Delete draft",()->new AlertDialog.Builder(activity).setTitle("Delete saved draft?").setNegativeButton("Keep",null).setPositiveButton("Delete",(d,w)->{store.removeDraft(value.optString("id"));card.setVisibility(View.GONE);}).show()));}
        panel("Saved drafts · this account",body);
    }
    public void showNotifications() {
        if(!signedIn())return;LinearLayout body=column();panel("Notifications",body);getCached("notifications","api/community/notifications",r->{body.removeAllViews();JSONArray rows=r.optJSONArray("notifications");
            if(rows==null||rows.length()==0)body.addView(text("You're all caught up.",16));if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject n=rows.optJSONObject(i);if(n==null)continue;LinearLayout card=card(body);card.addView(text((n.optBoolean("is_read")?"":"New · ")+n.optString("title"),18));card.addView(text(n.optString("body"),15));
                if("MESSAGE".equals(n.optString("type")))card.addView(button("Open chat",()->showConversation(n.optString("actor_id"),n.optString("actor_name"))));}
            body.addView(button("Mark all as read",()->post("api/community/notifications/read",new JSONObject(),x->toast("Notifications marked read"),this::showError)));
        });
    }
    private void showChallenge() {
        LinearLayout body=column();panel("Daily Bible challenge",body);getCached("challenge","api/community/challenge/today",r->{body.removeAllViews();JSONObject c=r.optJSONObject("challenge");if(c==null)c=r;
            body.addView(text(c.optString("question","Today's challenge is unavailable. Try again when connected."),20));body.addView(text(c.optString("hint"),14));EditText answer=input("Your answer",false);body.addView(answer);body.addView(button("Check answer",()->{
                if(!signedIn()||!required(answer))return;JSONObject data=new JSONObject();postSafe(data,"answer",answer.getText().toString());post("api/community/challenge/submit",data,x->alert("Daily challenge",x.optString("message")),this::showError);
            }));});
    }
    private void editProfile() {
        if(!signedIn())return;LinearLayout body=column();EditText name=input("Display name",false),bio=input("A little about you",true);
        org.perazimchurch.app.data.local.entity.UserEntity u=session().getCurrentUser();if(u!=null){name.setText(u.getName());bio.setText(u.getBio());}body.addView(name);body.addView(bio);AlertDialog dialog=panel("Community profile",body);
        body.addView(button("Save profile",()->{if(!required(name))return;JSONObject data=new JSONObject();postSafe(data,"name",name.getText().toString().trim());postSafe(data,"bio",bio.getText().toString().trim());
            request("PUT","api/community/profile",data,r->{dialog.dismiss();toast("Profile saved on the server");},this::showError);
        }));
    }
    public void showConnection() {
        LinearLayout body=column();body.addView(text("For a PC-hosted pilot, connect the phone and PC to the same Wi-Fi or hotspot. Enter the PC's address including port 3000. Bluetooth sharing installs the app; it does not connect members to the server.",15));
        EditText address=input("https://your-approved-api-host",false);address.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);address.setText(ApiClient.getBaseUrl(activity));body.addView(address);TextView result=text("",14);body.addView(result);AlertDialog dialog=panel("PC connection",body);
        body.addView(button("Test current connection",()->get("api/health",r->result.setText("Current server is healthy. Changing the address requires a separate confirmation below."),(c,m)->result.setText(error(c,m)))));
        body.addView(button("Save server address",()->{
            String value=address.getText().toString().trim();try{java.net.URI uri=new java.net.URI(value);if(!("http".equals(uri.getScheme())||"https".equals(uri.getScheme()))||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!(uri.getPath()==null||uri.getPath().isEmpty()||"/".equals(uri.getPath())))throw new IllegalArgumentException();}catch(Exception e){address.setError("Enter a server origin, for example http://192.168.1.10:3000");return;}
            while(value.endsWith("/"))value=value.substring(0,value.length()-1);final String selected=value;if(selected.equals(ApiClient.getBaseUrl(activity))){dialog.dismiss();return;}
            new AlertDialog.Builder(activity).setTitle("Switch to this server?").setMessage("You will be signed out. Existing local data and drafts are preserved for their original account and server. Sign in again from Profile; accounts belong to each server. Local HTTP is only for your trusted Wi-Fi pilot.").setNegativeButton("Keep current server",null).setPositiveButton("Switch and sign out",(d,w)->{
                session().logout();ApiClient.setBaseUrl(activity,selected);dialog.dismiss();resetScope();refresh();toast("Server changed. Sign in from Profile.");
            }).show();
        }));
    }
    private void pickPhoto(PhotoChoice choice) {
        if(!signedIn())return;
        pendingPhoto=choice;
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);intent.setType("image/*");intent.addCategory(Intent.CATEGORY_OPENABLE);
        try{activity.startActivityForResult(intent,PICK_PHOTO);}catch(android.content.ActivityNotFoundException e){pendingPhoto=null;alert("Photo picker unavailable","Install or enable a photo picker to attach an image.");}
    }
    public boolean onActivityResult(int requestCode,int resultCode,Intent data) {
        if(requestCode!=PICK_PHOTO)return false;
        PhotoChoice choice=pendingPhoto;pendingPhoto=null;
        if(resultCode!=Activity.RESULT_OK||data==null||data.getData()==null||choice==null||!sameScope())return true;
        Uri uri=data.getData();String expectedOwner=owner,expectedServer=server;
        toast("Preparing photo…");
        images.execute(()->{
            try {
                android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inJustDecodeBounds=true;
                try(java.io.InputStream in=activity.getContentResolver().openInputStream(uri)){android.graphics.BitmapFactory.decodeStream(in,null,options);}
                if(options.outWidth<=0||options.outHeight<=0)throw new java.io.IOException("Choose a readable photo");
                options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>1600)options.inSampleSize*=2;options.inJustDecodeBounds=false;
                android.graphics.Bitmap bitmap;try(java.io.InputStream in=activity.getContentResolver().openInputStream(uri)){bitmap=android.graphics.BitmapFactory.decodeStream(in,null,options);}
                if(bitmap==null)throw new java.io.IOException("Choose a readable photo");
                java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();int quality=85;
                do{bytes.reset();bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,quality,bytes);quality-=10;}while(bytes.size()>1024*1024&&quality>=25);
                bitmap.recycle();if(bytes.size()>1024*1024)throw new java.io.IOException("This photo is too large. Choose a smaller one.");
                JSONObject payload=new JSONObject();payload.put("base64Data",android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP));
                activity.runOnUiThread(()->{if(!expectedOwner.equals(owner)||!expectedServer.equals(server)||!sameScope()||activity.isFinishing())return;
                    post("api/community/media/upload",payload,r->{choice.selected(r.optString("mediaUrl"));toast("Photo attached");},this::showError);});
            }catch(Exception e){activity.runOnUiThread(()->{if(sameScope()&&!activity.isFinishing())alert("Photo not attached","Choose a readable photo under the upload limit, then try again.");});}
        });return true;
    }
    private void addPhoto(LinearLayout target,String url) {
        if(!url.matches("/uploads/att_[a-zA-Z0-9_.-]+"))return;
        android.widget.ImageView view=new android.widget.ImageView(activity);view.setAdjustViewBounds(true);view.setMaxHeight(dp(280));view.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);view.setContentDescription("Community photo");target.addView(view,new LinearLayout.LayoutParams(-1,dp(220)));
        String expectedOwner=owner,expectedServer=server,token=session().getAuthToken();
        images.execute(()->{java.net.HttpURLConnection connection=null;try{
            java.net.URL photoUrl=new java.net.URL(expectedServer+url);if(!ApiClient.allowsProtectedTransport(photoUrl))return;
            connection=(java.net.HttpURLConnection)photoUrl.openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(10000);connection.setReadTimeout(10000);
            if(token!=null&&!token.isEmpty())connection.setRequestProperty("Authorization","Bearer "+token);
            if(connection.getResponseCode()!=200)return;
            android.graphics.Bitmap bitmap;try(java.io.InputStream in=connection.getInputStream()){bitmap=android.graphics.BitmapFactory.decodeStream(in);}
            activity.runOnUiThread(()->{if(expectedOwner.equals(owner)&&expectedServer.equals(server)&&sameScope()&&!activity.isFinishing())view.setImageBitmap(bitmap);});
        }catch(Exception ignored){}finally{if(connection!=null)connection.disconnect();}});
    }
    private void saveAndSend(String endpoint,JSONObject value,String label,Result after) {
        if(!signedIn()||!sameScope())return;store.saveDraft(endpoint,value,label);
        post(endpoint,value,r->{store.removeDraft(value.optString("id"));after.success(r);},(code,message)->{
            alert("Not sent — draft saved",error(code,message)+"\nYour text is saved in Saved drafts. Retry there after reconnecting; it keeps the same identifier to avoid duplicates.");
        });
    }
    private void getCached(String key,String endpoint,Result result) {
        boolean privateGroup=key.startsWith("group_")||"groups".equals(key)||"classes".equals(key);
        JSONObject cached=privateGroup?new JSONObject():store.cached(key);if(cached.length()>0)result.success(cached);
        get(endpoint,r->{store.cache(key,r);result.success(r);},(c,m)->{
            if(c==401||c==403){store.cache(key,new JSONObject());result.success(new JSONObject());}
            toast(error(c,m)+(cached.length()>0?" · Showing saved copy":""));
        });
    }
    private void get(String endpoint,Result result,Failure failure) { request("GET",endpoint,null,result,failure); }
    private void post(String endpoint,JSONObject body,Result result,Failure failure) { request("POST",endpoint,body,result,failure); }
    private void request(String method,String endpoint,JSONObject body,Result result,Failure failure) {
        if(!sameScope()){feed.removeAllViews();status.setText("Account or server changed. Reopen Community.");return;}
        final String expectedOwner=owner,expectedServer=server;
        ApiClient.ApiCallback callback=new ApiClient.ApiCallback(){
            public void onSuccess(int code,JSONObject response){if(expectedOwner.equals(session().getActiveUserId())&&expectedServer.equals(ApiClient.getBaseUrl(activity))&&!activity.isFinishing())result.success(response);}
            public void onError(int code,String message){if(expectedOwner.equals(session().getActiveUserId())&&expectedServer.equals(ApiClient.getBaseUrl(activity))&&!activity.isFinishing())failure.failure(code,message);}
        };
        String token=session().getAuthToken();
        if("GET".equals(method))ApiClient.get(activity,endpoint,token,callback);
        else if("PUT".equals(method))ApiClient.put(activity,endpoint,body,token,callback);
        else if("DELETE".equals(method))ApiClient.delete(activity,endpoint,token,callback);
        else ApiClient.post(activity,endpoint,body,token,callback);
    }
    private String error(int code,String message) {
        if(code==401)return "Your session has expired or belongs to another server. Sign in again from Profile.";
        if(code==403)return "You don't have access to this space. Ask its owner.";
        if(code<=0)return "The PC server could not be reached.";
        return message==null||message.trim().isEmpty()?"Request failed ("+code+")":message;
    }
    private void showError(int code,String message){alert("Couldn't complete the request",error(code,message));}
    private void alert(String title,String message){new AlertDialog.Builder(activity).setTitle(title).setMessage(message).setPositiveButton("OK",null).show();}
    private void toast(String message){Toast.makeText(activity,message,Toast.LENGTH_LONG).show();}
    private void chooseDate(long[] target,Button button){Calendar c=Calendar.getInstance();c.setTimeInMillis(target[0]);new DatePickerDialog(activity,(picker,y,m,d)->{
        c.set(y,m,d);new TimePickerDialog(activity,(time,h,min)->{c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,min);c.set(Calendar.SECOND,0);target[0]=c.getTimeInMillis();button.setText("Starts: "+date(target[0]));},c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),true).show();
    },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();}
    private AlertDialog panel(String title,LinearLayout body){body.setPadding(dp(16),dp(12),dp(16),dp(24));AlertDialog d=new AlertDialog.Builder(activity).setTitle(title).setView(scroll(body)).setNegativeButton("Close",null).create();d.show();if(d.getWindow()!=null){d.getWindow().setBackgroundDrawableResource(android.R.color.black);d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,(int)(activity.getResources().getDisplayMetrics().heightPixels*0.90));d.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}return d;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(activity);l.setOrientation(LinearLayout.VERTICAL);l.setBackgroundColor(PerazimTheme.COLOR_BG_DEEP);return l;}
    private ScrollView scroll(LinearLayout body){ScrollView s=new ScrollView(activity);s.setFillViewport(true);s.setBackgroundColor(PerazimTheme.COLOR_BG_DEEP);s.addView(body);return s;}
    private LinearLayout card(LinearLayout parent){LinearLayout c=column();c.setPadding(dp(14),dp(12),dp(14),dp(12));c.setBackground(PerazimTheme.createCardGradient(dp(14),dp(1)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(8),0,dp(8));parent.addView(c,p);return c;}
    private TextView text(String value,int size){TextView t=new TextView(activity);t.setText(value);t.setTextSize(size);t.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);t.setPadding(0,dp(5),0,dp(7));t.setLineSpacing(dp(2),1.1f);if(size>=19)t.setTypeface(Typeface.DEFAULT_BOLD);return t;}
    private EditText input(String hint,boolean multiline){EditText e=new EditText(activity);e.setHint(hint);e.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);e.setHintTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);e.setTextSize(16);e.setPadding(dp(10),dp(10),dp(10),dp(10));e.setBackgroundColor(PerazimTheme.COLOR_SURFACE_2);e.setInputType(InputType.TYPE_CLASS_TEXT|(multiline?InputType.TYPE_TEXT_FLAG_MULTI_LINE:InputType.TYPE_TEXT_FLAG_CAP_SENTENCES));if(multiline){e.setMinLines(3);e.setGravity(Gravity.TOP);}LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(6));e.setLayoutParams(p);return e;}
    private Button button(String title,Runnable click){Button b=new Button(activity);b.setText(title);b.setAllCaps(false);b.setTextSize(14);b.setMinHeight(dp(44));b.setMinimumWidth(0);b.setTextColor(Color.WHITE);b.setBackground(PerazimTheme.createActivePillGradient(dp(10)));b.setPadding(dp(10),dp(8),dp(10),dp(8));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(2),dp(5),dp(2),dp(5));b.setLayoutParams(p);b.setOnClickListener(v->click.run());return b;}
    private void row(LinearLayout parent,Button a,Button b){LinearLayout r=new LinearLayout(activity);r.setOrientation(LinearLayout.HORIZONTAL);for(Button v:new Button[]{a,b}){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(2),dp(4),dp(2),dp(4));r.addView(v,p);}parent.addView(r);}
    private Spinner spinner(String[] values){Spinner s=new Spinner(activity);ArrayAdapter<String> adapter=new ArrayAdapter<>(activity,android.R.layout.simple_spinner_item,values);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(adapter);s.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PerazimTheme.COLOR_PURPLE_SOFT));s.setPopupBackgroundDrawable(new android.graphics.drawable.ColorDrawable(PerazimTheme.COLOR_SURFACE_2));return s;}
    private boolean required(EditText field){if(field.getText().toString().trim().isEmpty()){field.setError("Please enter some text");return false;}return true;}
    private int dp(int value){return (int)(value*activity.getResources().getDisplayMetrics().density+0.5f);}
    private static String encode(String value){return Uri.encode(value);}
    private static String date(long millis){return millis>0?DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new java.util.Date(millis)):"Time not set";}
    private static String id(String prefix){return prefix+"_"+UUID.randomUUID().toString().replace("-","");}
    private static void postSafe(JSONObject json,String key,Object value){try{json.put(key,value);}catch(Exception error){throw new IllegalArgumentException(error);}}
}
