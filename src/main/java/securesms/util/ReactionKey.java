package securesms.util;

public class ReactionKey {
    public boolean is_mms;
    public Integer message_id;

    public ReactionKey(boolean is_mms, Integer message_id) {
        this.is_mms = is_mms;
        this.message_id = message_id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (is_mms ? 1231 : 1237);
        result = prime * result + ((message_id == null) ? 0 : message_id.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ReactionKey other = (ReactionKey) obj;
        if (is_mms != other.is_mms)
            return false;
        if (message_id == null) {
            if (other.message_id != null)
                return false;
        } else if (!message_id.equals(other.message_id))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ReactionKey [" + is_mms + ": " + message_id + "]";
    }
}
