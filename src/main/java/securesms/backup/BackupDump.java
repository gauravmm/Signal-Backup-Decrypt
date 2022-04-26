package securesms.backup;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import securesms.util.ReactionKey;

public class BackupDump {
    public Map<Integer, Map<String, String>> part = new HashMap<Integer, Map<String, String>>();
    public Map<ReactionKey, List<Map<String, String>>> reaction = new HashMap<ReactionKey, List<Map<String, String>>>();
    public List<Map<String, String>> messages = new ArrayList<Map<String, String>>();

    public void finalizeBackup(Path attdir, Path outdir) throws IOException {
        System.err.println("Sorting " + messages.size() + " total messages.");
        messages.sort((a, b) -> a.get("date").compareTo(b.get("date")));

        System.err.println("Generating output");
        Map<Integer, List<JSONObject>> output = new HashMap<Integer, List<JSONObject>>();

        for (Map<String, String> m : messages) {
            // Process messages one at a time.
            // First figure out if the message is an MMS (with media) or SMS (without)
            String s_is_mms = m.remove("is_mms");
            boolean is_mms = "1".equals(s_is_mms);
            // Get the message thread this is associated with:
            int thread = Integer.parseInt(m.remove("thread_id"));
            // Get the message ID:
            int message_id = Integer.parseInt(m.remove("_id"));

            // Handle quotes:
            String quote_author = m.remove("quote_author");
            String quote_id = m.remove("quote_id");
            String quote_body = m.remove("quote_body");

            // Prepare output
            JSONObject out = new JSONObject(m);

            // Handle quotes:
            if (quote_id != null) {
                Map<String, String> quote = new HashMap<>();
                quote.put("author", quote_author);
                quote.put("body", quote_body);
                out.put("quote", quote);
            }

            // Handle attachments
            if (is_mms) {
                Map<String, String> media = this.part.remove(message_id);
                // If there is any media, then handle it:
                if (media != null) {
                    Map<String, String> media_merge = this.handleMedia(attdir, this.getThreadDir(outdir, thread),
                            thread, media);
                    if (media_merge != null)
                        out.put("attachment", media_merge);
                }
            }

            // Handle reactions:
            ReactionKey ref = new ReactionKey(is_mms, message_id);
            List<Map<String, String>> reactions = this.reaction.remove(ref);
            if (reactions != null) {
                // Handle reactions
                List<Map<String, String>> message_reacts = new LinkedList<>();
                for (Map<String, String> r : reactions) {
                    Map<String, String> each_reaction = new HashMap<>();
                    each_reaction.put("author", r.get("author_id"));
                    each_reaction.put("emoji", r.get("emoji"));
                    each_reaction.put("date", r.get("date_sent"));
                    message_reacts.add(each_reaction);
                }
                out.put("reactions", message_reacts);
            }

            // Insert in thread data
            List<JSONObject> thread_data = output.get(thread);
            if (thread_data == null) {
                thread_data = new LinkedList<>();
                output.put(thread, thread_data);
            }
            thread_data.add(out);

        }

        // Dump the messages to file:
        output.forEach((t, l) -> {
            try {
                Path messageFile = this.getThreadDir(outdir, t).resolve("_messages.json");
                BufferedWriter writer = Files.newBufferedWriter(messageFile, StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE);
                JSONArray.writeJSONString(l, writer);
                writer.close();
            } catch (IOException e) {
                System.err.println("Exception in writing messages to file.");
                e.printStackTrace();
            }
        });
    }

    private Path getThreadDir(Path outdir, int thread) throws IOException {
        Path p = outdir.resolve(String.format("%04d", thread));
        try {
            Files.createDirectories(p);
        } catch (IOException e) {
            System.err.println("Cannot create directory for thread output.");
            e.printStackTrace();
            throw e;
        }
        return p;
    }

    private Map<String, String> handleMedia(Path attdir, Path threaddir, int thread, Map<String, String> media) {
        String error = null;
        // Get file extension:
        String mimetype = media.get("ct");
        String extension = "";
        if (mimetype != null) {
            String[] mimetypeparts = mimetype.split("/", 3);
            if (mimetypeparts.length >= 2)
                extension = mimetypeparts[1];
        }

        // If a previous filename is available, then use it. Otherwise generate a new
        // one.
        // Use mid instead of unique_id because it is much shorter.
        String filename = media.get("file_name");
        if (filename == null) {
            filename = media.get("mid") + "." + extension;
        } else {
            filename = media.get("mid") + filename;
        }

        // Move the attachment file to the destination:
        try {
            Files.move(attdir.resolve(media.get("unique_id")), threaddir.resolve(filename));
        } catch (IOException e) {
            System.err
                    .println("Cannot move attachment " + media.get("unique_id") + " to " + threaddir.resolve(filename));
            System.err.println(e);
            error = "Missing file";
        }

        media.remove("unique_id");
        media.remove("_id");
        media.remove("mid");
        media.put("file_name", filename);
        if (error != null)
            media.put("error", error);

        return media;
    }
}
