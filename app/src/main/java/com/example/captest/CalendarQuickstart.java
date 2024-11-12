//package com.example.captest;
//
//import android.content.Context;
//
//import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
//import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
//import com.google.api.client.http.javanet.NetHttpTransport;
//import com.google.api.client.json.JsonFactory;
//import com.google.api.client.json.gson.GsonFactory;
//import com.google.api.services.calendar.Calendar;
//import com.google.api.services.calendar.CalendarScopes;
//import com.google.api.services.calendar.model.Event;
//import com.google.api.services.calendar.model.Events;
//import com.google.api.client.util.DateTime;
//
//import java.io.IOException;
//import java.security.GeneralSecurityException;
//import java.util.Collections;
//import java.util.List;
//
//public class CalendarQuickstart {
//    private static final String APPLICATION_NAME = "Google Calendar API Java Quickstart";
//    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
//    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR_READONLY);
//
//    private GoogleAccountCredential credential;
//    private Calendar service;
//
//    public CalendarQuickstart(Context context, String accountName) {
//        credential = GoogleAccountCredential.usingOAuth2(context, SCOPES);
//        credential.setSelectedAccountName(accountName);
//
//        NetHttpTransport HTTP_TRANSPORT = null;
//        try {
//            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//        } catch (GeneralSecurityException | IOException e) {
//            e.printStackTrace();
//        }
//
//        service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
//                .setApplicationName(APPLICATION_NAME)
//                .build();
//    }
//
//    public List<Event> getUpcomingEvents() throws IOException {
//        DateTime now = new DateTime(System.currentTimeMillis());
//        Events events = service.events().list("primary")
//                .setMaxResults(10)
//                .setTimeMin(now)
//                .setOrderBy("startTime")
//                .setSingleEvents(true)
//                .execute();
//        List<Event> items = events.getItems();
//        return (items != null) ? items : Collections.emptyList();
//    }
//}