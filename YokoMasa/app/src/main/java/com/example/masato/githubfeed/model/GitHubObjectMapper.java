package com.example.masato.githubfeed.model;

import android.content.res.Resources;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by Masato on 2018/01/19.
 */

public class GitHubObjectMapper {

    public static List<FeedEntry> mapFeedEntries(String feedString) {
        return XmlFeedParser.parse(feedString);
    }

    public static Profile mapProfile(String profileString) {
        Profile profile = new Profile();
        try {
            JSONObject jsonObject = new JSONObject(profileString);
            profile.name = jsonObject.getString("login");
            profile.iconUrl = jsonObject.getString("avatar_url");
        } catch (JSONException je) {
            je.printStackTrace();
        }
        return profile;
    }

    public static Map<String, String> mapFeedUrls(String feedUrlJson, Resources resources) {
        Map<String, String> urls = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(feedUrlJson);
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (Pattern.matches(".*_url$", key)) {
                    if (!key.equals("user_url")) {
                        urls.put(FeedTitle.getTitleFromIdentifier(key, resources), jsonObject.getString(key));
                    }
                }
            }
        } catch(JSONException je) {
            je.printStackTrace();
        }
        return urls;
    }

}