package securesms.backup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import securesms.util.ReactionKey;

public class BackupDump {
    public Map<String, Map<String, String>> part = new HashMap<String, Map<String, String>>();
    public Map<ReactionKey, List<Map<String, String>>> reaction = new HashMap<ReactionKey, List<Map<String, String>>>();
    public List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
}
