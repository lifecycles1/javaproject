package com.lifecycles.main.controllers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;

@RestController
public class Webhook {

  @Autowired
  MongoTemplate mongoTemplate;

  @Value("${permanent.token}")
  String permanentToken;

  @Value("${verification.token}")
  String verificationToken;

  @GetMapping(value = "/webhook")
  public String webhook(HttpServletRequest request) {
    return request.getParameter("hub.challenge");
    // Success!!
  }
  
  
  @PostMapping(value = "/webhook")
  @SuppressWarnings("unchecked")
  public void webhookPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      // request.getParam might also work (haven't tested)
      // JSONObject o = new JSONObject(request.getParameter("entry"));
      MongoCollection<Document> collection = mongoTemplate.getCollection("webhook");
      MongoCollection<Document> collection1 = mongoTemplate.getCollection("map");
      Map<String, Object> contacts = new HashMap<String, Object>();
      Map<String, Object> messages = new HashMap<String, Object>();
      Map<String, Object> statuses = new HashMap<String, Object>();
      
      /**
       * STORING
       * RESPONSE
       * MESSAGES
       */
      request.getReader().lines().forEach(line -> {
        try {
          Map<String, Object> result = new ObjectMapper().readValue(line, HashMap.class);
          System.out.print("request body : " + result);
          if (result.get("entry") != null) {
            ArrayList<Object> arr = (ArrayList<Object>) result.get("entry");
            LinkedHashMap<String, Object> o = (LinkedHashMap<String, Object>)arr.get(0);
            ArrayList<Object> changes = (ArrayList<Object>) o.get("changes");
            LinkedHashMap<String, Object> o2 = (LinkedHashMap<String, Object>)changes.get(0);
            Map<String, Object> value = (Map<String, Object>) o2.get("value");
            Map<String, Object> metadata = (Map<String, Object>) value.get("metadata");
            messages.put("display_phone_number", metadata.get("display_phone_number"));
            messages.put("phone_number_id", metadata.get("phone_number_id"));
            if (value.containsKey("statuses")) {
              ArrayList<Object> s = (ArrayList<Object>) value.get("statuses");
              Map<String, Object> ostatuses = (Map<String, Object>) s.get(0);
              statuses.put("status", ostatuses.get("status"));
              if (statuses.get("status").equals("failed")) {
                ArrayList<Object> errors = (ArrayList<Object>) ostatuses.get("errors");
                Map<String, Object> oerrors = (Map<String, Object>) errors.get(0);
                statuses.put("code", oerrors.get("code"));
                statuses.put("title", oerrors.get("title"));
              }
            }
            if (value.containsKey("messages")) {
              ArrayList<Object> c = (ArrayList<Object>) value.get("contacts");
              Map<String, Object> o3 = (Map<String, Object>) c.get(0);
              contacts.put("wa_id", o3.get("wa_id"));
              Map<String, Object> profile = (Map<String, Object>) o3.get("profile");
              contacts.put("name", profile.get("name"));
              ArrayList<Object> m = (ArrayList<Object>) value.get("messages");
              Map<String, Object> o4 = (Map<String, Object>) m.get(0);
              messages.put("id", o4.get("id"));
              messages.put("from", o4.get("from"));
              messages.put("timestamp", o4.get("timestamp"));

              // if message has a type (use cases "contact message" and "location message" do not contain a type)
              if (o4.containsKey("type")) {
                messages.put("type", o4.get("type"));

                // case text (logical && operators required so wrapped in an "if" statement)
                if (o4.get("type").equals("text")) {
                  Map<String, Object> t = (Map<String, Object>) o4.get("text");
                  messages.put("body", t.get("body"));
                  // case (text) with show security notifications
                  if (o4.containsKey("identity")) {
                    Map<String, Object> textidentity = (Map<String, Object>) o4.get("identity");
                    messages.put("acknowledged", textidentity.get("acknowledged"));
                    messages.put("created_timestamp", textidentity.get("created_timestamp"));
                    messages.put("hash", textidentity.get("hash"));
                  }
                  // case (text) with click to whatsapp ads 
                  if (o4.containsKey("referral")) {
                    Map<String, Object> textreferral = (Map<String, Object>) o4.get("referral");
                    messages.put("source_url", textreferral.get("source_url"));
                    messages.put("source_id", textreferral.get("source_id"));
                    messages.put("source_type", textreferral.get("source_type"));
                    messages.put("headline", textreferral.get("headline"));
                    messages.put("body", textreferral.get("body"));
                    messages.put("media_type", textreferral.get("media_type"));
                    messages.put("image_url", textreferral.get("image_url"));
                    messages.put("video_url", textreferral.get("video_url"));
                    messages.put("thumbnail_url", textreferral.get("thumbnail_url"));
                  }
                  // case (text) with product enquiry 
                  if (o4.containsKey("context")) {
                    Map<String, Object> textcontext = (Map<String, Object>) o4.get("context");
                    messages.put("from", textcontext.get("from"));
                    messages.put("id", textcontext.get("id"));
                    Map<String, Object> referred_product = (Map<String, Object>) textcontext.get("referred_product");
                    messages.put("catalog_id", referred_product.get("catalog_id"));
                    messages.put("product_retailer_id", referred_product.get("product_retailer_id"));
                  }
                }

                // all remaining cases
                switch (o4.get("type").toString()) {
                  // case media (image)
                  case "image":
                  Map<String, Object> i = (Map<String, Object>) o4.get("image");
                  messages.put("caption", i.get("caption"));
                  messages.put("mime_type", i.get("mime_type"));
                  messages.put("sha256", i.get("sha256"));
                  messages.put("image_id", i.get("id"));
                  break;
                  // case media (sticker)
                  case "sticker":
                  Map<String, Object> s = (Map<String, Object>) o4.get("sticker");
                  messages.put("sticker_id", s.get("id"));
                  messages.put("animated", s.get("animated"));
                  messages.put("mime_type", s.get("mime_type"));
                  messages.put("sha256", s.get("sha256"));
                  break;
                  // case button
                  case "button":
                  Map<String, Object> context = (Map<String, Object>) o4.get("context");
                  messages.put("from", context.get("from"));
                  messages.put("button_id", context.get("id"));
                  Map<String, Object> button = (Map<String, Object>) o4.get("button");
                  messages.put("text", button.get("text"));
                  messages.put("payload", button.get("payload"));
                  break;
                  // case order
                  case "order":
                  Map<String, Object> order = (Map<String, Object>) o4.get("order");
                  messages.put("catalog_id", order.get("catalog_id"));
                  ArrayList<Object> items = (ArrayList<Object>) order.get("product_items");
                  Map<String, Object> proditems = (Map<String, Object>) items.get(0);
                  messages.put("product_retailer_id", proditems.get("product_retailer_id"));
                  messages.put("quantity", proditems.get("quantity"));
                  messages.put("item_price", proditems.get("item_price"));
                  messages.put("currency", proditems.get("currency"));
                  messages.put("text", order.get("text"));
                  Map<String, Object> context1 = (Map<String, Object>) o4.get("context");
                  messages.put("from_context", context1.get("from"));
                  messages.put("id_context", context1.get("id"));
                  break;
                  // case unknown
                  case "unknown":
                  ArrayList<Object> e = (ArrayList<Object>) o4.get("errors");
                  Map<String, Object> errors = (Map<String, Object>) e.get(0);
                  messages.put("code", errors.get("code"));
                  messages.put("details", errors.get("details"));
                  messages.put("title", errors.get("title"));
                  break;
                }
              }
              // case "location message"
              if (o4.containsKey("location")) {
                  Map<String, Object> l = (Map<String, Object>) o4.get("location");
                  messages.put("latitude", l.get("latitude"));
                  messages.put("longitude", l.get("longitude"));
                  messages.put("name", l.get("name"));
                  messages.put("address", l.get("address"));
              }
              // case "contact message". All attributes converted into a level-1 plain document (de-nested)
              if (o4.containsKey("contacts")) {
                ArrayList<Object> c2 = (ArrayList<Object>)o4.get("contacts");
                Map<String, Object> o5 = (Map<String, Object>) c2.get(0);
                ArrayList<Object> c3 = (ArrayList<Object>)o5.get("addresses");
                Map<String, Object> o6 = (Map<String, Object>) c3.get(0);
                messages.put("city", o6.get("city"));
                messages.put("country", o6.get("country"));
                messages.put("country_code", o6.get("country_code"));
                messages.put("state", o6.get("state"));
                messages.put("street", o6.get("street"));
                messages.put("type", o6.get("type"));
                messages.put("zip", o6.get("zip"));
                messages.put("birthday", o6.get("birthday"));
                ArrayList<Object> c4 = (ArrayList<Object>)o5.get("emails");
                Map<String, Object> o7 = (Map<String, Object>) c4.get(0);
                messages.put("email", o7.get("email"));
                messages.put("emails_type", o7.get("type"));
                Map<String, Object> o8 = (Map<String, Object>) o4.get("name");
                messages.put("formatted_name", o8.get("formatted_name"));
                messages.put("first_name", o8.get("first_name"));
                messages.put("last_name", o8.get("last_name"));
                messages.put("middle_name", o8.get("middle_name"));
                messages.put("suffix", o8.get("suffix"));
                messages.put("prefix", o8.get("prefix"));
                Map<String, Object> o9 = (Map<String, Object>) o4.get("org");
                messages.put("company", o9.get("company"));
                messages.put("department", o9.get("department"));
                messages.put("title", o9.get("title"));
                ArrayList<Object> c5 = (ArrayList<Object>)o5.get("phones");
                Map<String, Object> o10 = (Map<String, Object>) c5.get(0);
                messages.put("phone", o10.get("phone"));
                messages.put("wa_id", o10.get("wa_id"));
                messages.put("phones_type", o10.get("type"));
                ArrayList<Object> c6 = (ArrayList<Object>)o5.get("urls");
                Map<String, Object> o11 = (Map<String, Object>) c6.get(0);
                messages.put("url", o11.get("url"));
                messages.put("url_type", o11.get("type"));
              }
            }
          }
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
      });
      /*
       * FINISHED SAVING RESPONSE MESSAGE TYPES
       */


      /**
       * EXECUTING
       * QUERIES
       * TO DATABASE
       */

       // if status
      if (statuses.size() > 0) {
        if (statuses.get("status").equals("sent")) {
        System.out.println("status sent : " + statuses.get("status"));
        }
        if (statuses.get("status").equals("delivered")) {
        System.out.println("status delivered : " + statuses.get("status"));
        }
        if (statuses.get("status").equals("read")) {
          System.out.println("status read : " + statuses.get("status"));
        }
        if (statuses.get("status").equals("failed")) {
          System.out.println("status failed : " + statuses.get("status"));
          System.out.println("status code : " + statuses.get("code"));
          System.out.println("status title : " + statuses.get("title"));
        }
      }

      // if message
      if (messages.size() > 0) {
        // if location message
        if (messages.get("type").equals("location")) {
          System.out.println("Printing incoming location message : " + messages);
          collection.insertOne(new Document("phone_number_id", messages.get("phone_number_id"))
            .append("display_phone_number", messages.get("display_phone_number")).append("type", messages.get("type"))
            .append("id", messages.get("id")).append("name", contacts.get("name")).append("wa_id", contacts.get("wa_id"))
            .append("from", messages.get("from")).append("latitude", messages.get("latitude"))
            .append("longitude", messages.get("longitude")).append("location_name", messages.get("name"))
            .append("address", messages.get("address")).append("timestamp", messages.get("timestamp")));
        }

        
        // if text message
        if (messages.get("type").equals("text")) {
          System.out.println("Printing incoming text message : " + messages);

          messages.put("name", contacts.get("name"));
          messages.put("wa_id", contacts.get("wa_id"));

          if(messages.get("body").toString().length() > 7 && (messages.get("body").toString().substring(0, 7).equalsIgnoreCase("Bearing"))) {
            // calculate bearing location from original lat/lng and posting both to map collection
            postdata("https://calculatedistance-hejarmyj7q-nw.a.run.app", messages);
          }

          /**
           * 
           * THIS BLOCK OF CODE HAS BEEN MIGRATED TO A GCP CLOUD FUNCTION VIA THE ABOVE URL POST REQUEST
           * CLOUD FUNCTION IS INVOKED VIA AN HTTP TRIGGER
           * AND EXECUTES THE COMMENTED CODE BELOW
           * 
           * ANY MIGRATED METHODS ARE STILL KEPT AT THE BOTTOM OF THE FILE FOR REFERENCE  
           * 
           */

          // ObjectId id = collection.insertOne(new Document("phone_number_id", messages.get("phone_number_id"))
          // .append("display_phone_number", messages.get("display_phone_number")).append("type", messages.get("type"))
          // .append("id", messages.get("id")).append("name", contacts.get("name")).append("wa_id", contacts.get("wa_id"))
          //   .append("from", messages.get("from")).append("body", messages.get("body"))
          //   .append("timestamp", messages.get("timestamp"))).getInsertedId().asObjectId().getValue();
          // System.out.println("Inserted message with id : " + id);
          // if(messages.get("body").toString().length() > 7 && (messages.get("body").toString().substring(0, 7).equalsIgnoreCase("Bearing"))) {
          //   Map<String, Double> bearings = new HashMap<String, Double>(16);
          //   bearings.put("N", 360.0); bearings.put("NNE", 22.5); bearings.put("NE", 45.0);
          //   bearings.put("ENE", 67.5); bearings.put("E", 90.0); bearings.put("ESE", 112.5);
          //   bearings.put("SE", 135.0); bearings.put("SSE", 157.5); bearings.put("S", 180.0);
          //   bearings.put("SSW", 202.5); bearings.put("SW", 225.0); bearings.put("WSW", 247.5);
          //   bearings.put("W", 270.0); bearings.put("WNW", 292.5); bearings.put("NW", 315.0);
          //   bearings.put("NNW", 337.5);
            
          //   //extract bearing directions from message
          //   Matcher upperCase = Pattern.compile("[A-Z]").matcher(messages.get("body").toString());
          //   StringBuilder sb1 = new StringBuilder();
          //   while (upperCase.find()) {
          //     sb1.append(upperCase.group());
          //   }
          //   //extract distance from message
          //   Matcher hasNum = Pattern.compile("\\p{Digit}").matcher(messages.get("body").toString());
          //   StringBuilder sb = new StringBuilder();
          //   while(hasNum.find()) {
          //     sb.append(hasNum.group());
          //   }
          //   if (sb.length() > 0) {
          //     Bson filter = Filters.and(Filters.eq("from", messages.get("from")), Filters.exists("latitude", true));
          //     Document doc = collection.aggregate(Arrays.asList(Aggregates.match(filter), Aggregates.sort(Sorts.ascending("latitude")))).first();
          //     ArrayList<Double> newcoords = new ArrayList<Double>();
          //     if (bearings.containsKey(sb1.substring(1))) {
          //       newcoords = calculateDistance(Integer.parseInt(sb.toString()), bearings.get(sb1.substring(1)), 
          //         Double.parseDouble(doc.get("latitude").toString()), Double.parseDouble(doc.get("longitude").toString()));
                
          //       //insert new location message into a new collection called "map"
          //       collection1.insertOne(new Document("_id", id).append("phone_number_id", messages.get("phone_number_id"))
          //         .append("display_phone_number", messages.get("display_phone_number")).append("id", messages.get("id"))
          //         .append("name", contacts.get("name")).append("wa_id", contacts.get("wa_id"))
          //         .append("from", messages.get("from")).append("body", messages.get("body"))
          //         .append("latitude", Double.parseDouble(doc.get("latitude").toString()))
          //         .append("longitude", Double.parseDouble(doc.get("longitude").toString())).append("latitude1", newcoords.get(0))
          //         .append("longitude1", newcoords.get(1)).append("timestamp", messages.get("timestamp")));
          //     }
          //   }
          // }
          response.setStatus(200);
        }

        // if image message
        if (messages.get("type").equals("image")) {
          System.out.println("Printing incoming image message : " + messages);

          messages.put("name", contacts.get("name"));
          messages.put("wa_id", contacts.get("wa_id"));
          // retrieve image url from facebook, download image, upload to google cloud storage bucket, and append image url to original message in webhook collection
          postdata("https://processimage-hejarmyj7q-nw.a.run.app", messages);

          /**
           * 
           * THIS BLOCK OF CODE HAS BEEN MIGRATED TO A GCP CLOUD FUNCTION VIA THE ABOVE URL POST REQUEST
           * CLOUD FUNCTION IS INVOKED VIA AN HTTP TRIGGER
           * AND EXECUTES THE COMMENTED CODE BELOW
           * 
           * ANY MIGRATED METHODS ARE STILL KEPT AT THE BOTTOM OF THE FILE FOR REFERENCE  
           * 
           */

          // ObjectId id = collection.insertOne(new Document("phone_number_id", messages.get("phone_number_id"))
          //   .append("display_phone_number", messages.get("display_phone_number")).append("type", messages.get("type"))
          //   .append("id", messages.get("id")).append("name", contacts.get("name")).append("wa_id", contacts.get("wa_id"))
          //   .append("from", messages.get("from")).append("caption", messages.get("caption"))
          //   .append("mime_type", messages.get("mime_type")).append("sha256", messages.get("sha256"))
          //   .append("image_id", messages.get("image_id")).append("timestamp", messages.get("timestamp"))).getInsertedId().asObjectId().getValue();

          // String imageUrl = grabPublicUrl(messages.get("image_id").toString());
          // collection.updateOne(Filters.eq("_id", id), Updates.set("image_url", imageUrl));
          
          response.setStatus(200);    
        }
      }
      response.setStatus(200);
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      response.setStatus(500);
    }
  }

  /**
   * 
   * all the below methods besides (postdata) are not in use anymore (they have been migrated to a cloud function)
   * 
   */
  public static ArrayList<Double> calculateDistance(Integer yards, Double bearing, Double lat, Double lng) {
    Double meters = yards * 0.9144;
    Double calcDist = meters / 6371000;
    Double bearingRadian = bearing * (Math.PI / 180);
    Double latRadian = lat * (Math.PI / 180);
    Double lngRadian = lng * (Math.PI / 180);

    Double latResult = Math.asin(Math.sin(latRadian) * Math.cos(calcDist) + Math.cos(latRadian) * Math.sin(calcDist) * Math.cos(bearingRadian));
    Double a = Math.atan2(Math.sin(bearingRadian) * Math.sin(calcDist) * Math.cos(latRadian), Math.cos(calcDist) - Math.sin(latRadian) * Math.sin(latResult));

    Double lngResult = ((lngRadian + a + 3 * Math.PI) % (2 * Math.PI)) - Math.PI;

    Double latDegrees = latResult * (180 / Math.PI);
    Double lngDegrees = lngResult * (180 / Math.PI);
    ArrayList<Double> result = new ArrayList<Double>();
    result.add(latDegrees);
    result.add(lngDegrees);
    return result;
  }

  public String grabPublicUrl(String imageId) throws IOException, InterruptedException {
    String res = fetchImageDownloadLink("https://graph.facebook.com/v14.0/" + imageId);
    Map<String, Object> result = new ObjectMapper().readValue(res, HashMap.class);
    Blob image = downloadBinaryAndUploadToGcp(result, imageId + ".jpg");
    String imageUrl = "https://storage.googleapis.com/wa_images1/" + imageId + ".jpg";
    return imageUrl;
  }

  public String fetchImageDownloadLink(String url) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
      .header("Authorization", "Bearer EAAJ7RHZAurc8BANdtD4aZATlUZBxR4eiafUF6VHw2bsZCBNQ0AlSZCQctnpzFLZChPXJYHdlPCfrl6Tf1xNO3AlcF1SrEcDQWTd6AjU4WoSmovqzsCuoCzT8q7a9KfV0y80ZAVpZCjG6KZCbkglroKxDoyajkFHIaZAxktOlfKKDRHNfeNArt3sYlCyHHCXl2kXJux4jvtlysGhQZDZD")
      .uri(URI.create(url))
      .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    // System.out.println(response.body());
    return response.body();
  }

  public Blob downloadBinaryAndUploadToGcp(Map<String, Object> result, String name) throws IOException, InterruptedException {
    String url = result.get("url").toString();
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest req = HttpRequest.newBuilder()
      .header("Authorization", "Bearer EAAJ7RHZAurc8BANdtD4aZATlUZBxR4eiafUF6VHw2bsZCBNQ0AlSZCQctnpzFLZChPXJYHdlPCfrl6Tf1xNO3AlcF1SrEcDQWTd6AjU4WoSmovqzsCuoCzT8q7a9KfV0y80ZAVpZCjG6KZCbkglroKxDoyajkFHIaZAxktOlfKKDRHNfeNArt3sYlCyHHCXl2kXJux4jvtlysGhQZDZD")
      .uri(URI.create(url))
      .build();
    HttpResponse<InputStream> res1 = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
    InputStream is = res1.body();
    byte[] buff = new byte[1024];
    int bytesRead = 0;
    ByteArrayOutputStream bao = new ByteArrayOutputStream();
    while((bytesRead = is.read(buff)) != -1) {
      bao.write(buff, 0, bytesRead);
    }
    byte[] data = bao.toByteArray();
    ByteArrayInputStream bin = new ByteArrayInputStream(data);
    Storage storage = StorageOptions.newBuilder().setProjectId("whatsapp-webhook-366900").build().getService();
    BlobId blobId = BlobId.of("wa_images1", name);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("image/jpeg").build();
    return storage.createFrom(blobInfo, bin);
  }

  public static String postdata(String url, Map<String, Object> data) throws IOException, InterruptedException {
    ObjectMapper objectMapper = new ObjectMapper();
    String requestBody = objectMapper.writeValueAsString(data);
    
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(requestBody))
      .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    // System.out.println(response.body());
    return response.body();
  }
}